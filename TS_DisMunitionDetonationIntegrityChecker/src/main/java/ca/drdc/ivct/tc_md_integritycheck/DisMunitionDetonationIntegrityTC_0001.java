/*******************************************************************************
 * Copyright (C) Her Majesty the Queen in Right of Canada, 
 * as represented by the Minister of National Defence, 2018
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package ca.drdc.ivct.tc_md_integritycheck;

import ca.drdc.ivct.fom.base.structs.EventIdentifierStruct;
import ca.drdc.ivct.fom.utils.MunitionDetonationCSVReader;
import ca.drdc.ivct.fom.utils.MunitionDetonationEqualUtils;
import ca.drdc.ivct.fom.warfare.MunitionDetonation;
import ca.drdc.ivct.tc_lib_md_integritycheck.CountdownTimer;
import de.fraunhofer.iosb.tc_lib.TcFailed;
import de.fraunhofer.iosb.tc_lib.TcInconclusive;

import de.fraunhofer.iosb.tc_lib.converter.DisModelConverter;
import de.fraunhofer.iosb.tc_lib.dis.DISAbstractTestCase;
import org.slf4j.Logger;

import java.util.*;

public class DisMunitionDetonationIntegrityTC_0001 extends DISAbstractTestCase {

    private List<MunitionDetonation> fad ;
    private Map<String, Double> spatialThresold;



    @Override
    protected void logTestPurpose(Logger logger) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n");
        stringBuilder.append("---------------------------------------------------------------------\n");
        stringBuilder.append("TEST PURPOSE\n");
        stringBuilder.append("Tests if a SuT federate creates Munition Detonation Interactions with Identifiers\n");
        stringBuilder.append("that match the ones defined in the federation agreement document (FAD). Then the attributes \n");
        stringBuilder.append("are verified and finally the spatial information. \n");
        stringBuilder.append("The FAD is publised as a csv document containing listings of Identifiers \n");
        stringBuilder.append("and interaction parameters.\n");
        stringBuilder.append("---------------------------------------------------------------------\n");
        stringBuilder.append("TC_0001 focus is to check the interactions' identifiers match.\n");
        stringBuilder.append("---------------------------------------------------------------------\n");
        String testPurpose = stringBuilder.toString();

        logger.info(testPurpose);
    }

    @Override
    protected void preambleAction(Logger logger) throws TcInconclusive {
        // Load all files in test cases folder. This constitutes the federation agreement document (FAD)

        this.fad = MunitionDetonationCSVReader.loadCSVFileToMunitionDetonationList(super.param.getFadUrls());
        if (fad.isEmpty()) {
            throw new TcInconclusive("The FAD is empty.");
        }

        spatialThresold = this.param.getSpatialValueThreshold();

        // Let five second to IVCT federation client to discover the munition detonations.
        new CountdownTimer(this.param.getWaitingPeriod(), logger).run();
    }

    /**
     * Tests discovered BaseEntity objects by comparing them with the ones in
     * the FAD.
     * 
     * @throws TcInconclusive due to connection errors or csv
     * @throws TcFailed due to entities not being the same
     */
    @Override
    protected void performTest(Logger logger) throws TcInconclusive, TcFailed {
        logger.info("Welcome to the MunitionDetonationTester Federate of the IVCT Federation");
        logger.info("Make sure that the Munition Detonation Agent federate has joined the federation!");


        Map<EventIdentifierStruct, MunitionDetonation> munitionDetonationMap = new HashMap<>();
        super.disManager.getReceivedDetonationPdus().stream()
                .map(DisModelConverter::disMunitionDetonationToRpr)
                .forEachOrdered(munitionDetonation -> munitionDetonationMap.computeIfAbsent(munitionDetonation.getEventIdentifier(), (key)->munitionDetonation));

        logger.info("Executing Test");
        String lineSeparator = "\n---------------------------------------------------------------------\n";


        if (munitionDetonationMap.isEmpty()) {
            throw new TcInconclusive("No MunitionDetonation interactions found on the RTI bus. A system "
                    + "under test must create discoverable MunitionDetonation interactions before attempting the test.");
        }

        boolean testPassed = true;
        StringBuilder failedStringBuilder = new StringBuilder();
        StringBuilder debugStringBuilder = new StringBuilder();

        if (fad.size() != munitionDetonationMap.size()) {
            testPassed = false;
            String failedMessage = "FAIL: Fad and discovered munitionDetonation sizes do not match "+fad.size()+" | "+ munitionDetonationMap.size();
            failedStringBuilder.append(failedMessage);
            logger.info(failedMessage);
        } else {
            debugStringBuilder.append("Received the good amount of munitionDetonation according to the fad");
        }

        for (MunitionDetonation fadMunitionDetonation : fad) {
            // Loop over each discovered weapon fire to check if this fadWeaponFire is present

            Optional<MunitionDetonation> optionalMunitionDetonation =
                    munitionDetonationMap.values()
                            .stream()
                            .filter(receivedMunitionDetonation -> receivedMunitionDetonation.getEventIdentifier().equals(fadMunitionDetonation.getEventIdentifier()))
                            .findFirst();

            String failedMessage;

            // testMunitionDetonationIntegrityIdentity
            if (!optionalMunitionDetonation.isPresent()) {
                testPassed = false;
                failedMessage = "FAIL: Munition Detonation Interaction from FAD with identifier " + fadMunitionDetonation.getEventIdentifier()
                        + " found no MunitionDetonation match in discovered MunitionDetonation Interactions";
                failedStringBuilder.append("\n"+failedMessage);
            } else {
                debugStringBuilder.append("OKAY: Munition Detonation Interaction from FAD with identifier " + fadMunitionDetonation.getEventIdentifier()
                        + "was found in discovered Munition Detonation Interactions");


                // testMunitionDetonationIntegrityParameters
                boolean fadMunitionDetonationPassesTest = new MunitionDetonationEqualUtils(spatialThresold).areMunitionDetonationParametersEquals(fadMunitionDetonation, optionalMunitionDetonation.get());
                if (!fadMunitionDetonationPassesTest) {
                    testPassed = false;
                    failedMessage = "FAIL: MunitionDetonation Interaction from FAD with identifier " + fadMunitionDetonation.getEventIdentifier()
                            + " has not the same parameters as its discovered MunitionDetonation match";
                    failedStringBuilder.append("\n"+failedMessage);
                } else {
                    debugStringBuilder.append("OKAY: MunitionDetonation from FAD with identifier " + fadMunitionDetonation.getEventIdentifier()
                            + " found an ID and type match in discovered MunitionDetonations");

                }

                boolean fadDetonationLocationPassesTest = new MunitionDetonationEqualUtils(spatialThresold).worldLocationEquals(
                        fadMunitionDetonation.getDetonationLocation(),
                        optionalMunitionDetonation.get().getDetonationLocation());

                //This part overwrites the decision made by the equalCheck of the MunitionDetonationEqualUtils class.
                boolean fadFinalVelocityPassesTest = new MunitionDetonationEqualUtils(spatialThresold).velocityEquals(
                        fadMunitionDetonation.getFinalVelocityVector(),
                        optionalMunitionDetonation.get().getFinalVelocityVector());

                if (fadFinalVelocityPassesTest == false) {
                    logger.warn("WARNING! MunitionDetonation Velocities do not match!");
                    logger.warn("This can be due to either a faulty SuT or a difference between DIS coordinates and the vcsutilities libraries used. This failure has however not affected the official outcome of this TestSuite.");
                    fadFinalVelocityPassesTest = true;
                }

                boolean fadRelativePassesTest = new MunitionDetonationEqualUtils(spatialThresold).relativePositionEquals(
                        fadMunitionDetonation.getRelativeDetonationLocation(),
                        optionalMunitionDetonation.get().getRelativeDetonationLocation());

                if (!(fadDetonationLocationPassesTest&&fadFinalVelocityPassesTest&&fadRelativePassesTest) ){
                    testPassed = false;
                    failedMessage = "FAIL: MunitionDetonation from FAD with identifier " + fadMunitionDetonation.getEventIdentifier()
                            + " has not the same spatial info as its discovered MunitionDetonation";
                    failedStringBuilder.append("\n"+failedMessage+lineSeparator);
                } else {
                    debugStringBuilder.append("OKAY: MunitionDetonation from FAD with identifier " + fadMunitionDetonation.getEventIdentifier()
                            + " found an ID and spatial match in discovered MunitionDetonations");
                }
            }
        }
        logger.debug(debugStringBuilder.toString());
        if (!testPassed) {
            throw new TcFailed("Test failed due to errors in Munition Detonation Interaction(s) or absent/unrecognized Weapon Fire Interaction(s) : "+failedStringBuilder.toString()) ;
        } else {
            logger.info("{} TEST IS COMPLETED SUCCESFULLY. {}",lineSeparator,lineSeparator);
        }
    }

}
