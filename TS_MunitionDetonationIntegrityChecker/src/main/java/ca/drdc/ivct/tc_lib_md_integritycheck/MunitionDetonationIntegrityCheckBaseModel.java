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

package ca.drdc.ivct.tc_lib_md_integritycheck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ca.drdc.ivct.coders.warfare.MunitionDetonationCoder;
import ca.drdc.ivct.fom.utils.MunitionDetonationEqualUtils;
import ca.drdc.ivct.fom.warfare.MunitionDetonation;
import org.slf4j.Logger;

import de.fraunhofer.iosb.tc_lib.IVCT_RTIambassador;
import de.fraunhofer.iosb.tc_lib.TcFailed;
import de.fraunhofer.iosb.tc_lib.TcInconclusive;
import de.fraunhofer.iosb.tc_lib.IVCT_BaseModel;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ParameterHandle;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.FederateHandle;
import hla.rti1516e.LogicalTime;
import hla.rti1516e.MessageRetractionHandle;
import hla.rti1516e.OrderType;
import hla.rti1516e.TransportationTypeHandle;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.exceptions.FederateHandleNotKnown;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.FederateServiceInvocationsAreBeingReportedViaMOM;
import hla.rti1516e.exceptions.InvalidInteractionClassHandle;
import hla.rti1516e.exceptions.InvalidFederateHandle;
import hla.rti1516e.exceptions.InteractionClassNotDefined;
import hla.rti1516e.exceptions.NameNotFound;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.RTIinternalError;
import hla.rti1516e.exceptions.RestoreInProgress;
import hla.rti1516e.exceptions.SaveInProgress;

/**
 *  Base Model container for Integrity check testing.
 *  
 *  Use the IVCT ambassador to connect to the federation and gather MunitionDetonationInteractions from the federation.
 * 
 * @author laurenceo
 */
public class MunitionDetonationIntegrityCheckBaseModel extends IVCT_BaseModel {

    private IVCT_RTIambassador ivctRti;
    private final Map<String, MunitionDetonation> munitionDetonationMap = new HashMap<>();

    private Logger logger;

    private InteractionClassHandle munitionDetonationInteractionClassHandle;

    private ParameterHandle articulatedPartDataHandle;
    private ParameterHandle detonationLocParameterHandle;
    private ParameterHandle detonationResultParameterHandle;
    private ParameterHandle eventIdParameterHandle;
    private ParameterHandle firingObjIdParameterHandle;
    private ParameterHandle finalVelVectorParameterHandle;
    private ParameterHandle fuseTypeParameterHandle;
    private ParameterHandle munitionObjIdParameterHandle;
    private ParameterHandle munitionTypeParameterHandle;
    private ParameterHandle quantityFiredParameterHandle;
    private ParameterHandle rateOfFireParameterHandle;
    private ParameterHandle relativeDetonationParameterHandle;
    private ParameterHandle targetObjIdParameterHandle;
    private ParameterHandle warheadTypeParameterHandle;

    private static final String MUNITION_DETONATION = "MunitionDetonation";
    private static final String ARTICULATED_PART_DATA = "ArticulatedPartData";
    private static final String DETONATION_LOC = "DetonationLocation";
    private static final String DETONATION_RESULT = "DetonationResultCode";
    private static final String EVENT_ID = "EventIdentifier";
    private static final String FIRING_OBJ_ID = "FiringObjectIdentifier";
    private static final String FINAL_VELOCITY = "FinalVelocityVector";
    private static final String FUSE_TYPE = "FuseType";
    private static final String MUNIT_OBJ_ID = "MunitionObjectIdentifier";
    private static final String MUNIT_TYPE = "MunitionType";
    private static final String QUANT_FIRED = "QuantityFired";
    private static final String RATE_OF_FIRE = "RateOfFire";
    private static final String RELATIVE_DETONATION_LOC = "RelativeDetonationLocation";
    private static final String TARGET_OBJ_ID = "TargetObjectIdentifier";
    private static final String WARHEAD_TYPE = "WarheadType";

    private Map<String, Double> spatialThresold;

    /**
     * @param logger reference to a logger
     * @param ivctRti reference to the RTI ambassador
     * @param ivctTcParam ivct_TcParam
     */
    public MunitionDetonationIntegrityCheckBaseModel(Logger logger, IVCT_RTIambassador ivctRti, IntegrityCheckTcParam ivctTcParam) {
        super(ivctRti, logger, ivctTcParam);
        this.logger = logger;
        this.ivctRti = ivctRti;
        this.spatialThresold = ivctTcParam.getSpatialValueThreshold();
    }

    /**
     * @param federateHandle the federate handle
     * @return the federate name or null
     */
    public String getFederateName(FederateHandle federateHandle) {

        try {
            return this.ivctRti.getFederateName(federateHandle);
        } catch (InvalidFederateHandle | FederateHandleNotKnown | FederateNotExecutionMember | NotConnected | RTIinternalError e) {
            logger.error("Error extracting federate name from the ambassador",e);
            return null;
        }

    }

    /**
     * @return true means error, false means correct
     */
    public boolean init() {

        try {
            this.subscribeInteraction();

        } catch (RTIinternalError e) {
            e.printStackTrace();
            return true;
        } catch (NotConnected e) {
            this.logger.error("Cannot retreive handles. No connection to RTI.");
            e.printStackTrace();
            return true;
        } catch (SaveInProgress e) {
            this.logger.error("init Error. Save is in progress.");
            e.printStackTrace();
            return true;
        } catch (RestoreInProgress e) {
            this.logger.error("init Error. Restore is in progress.");
            e.printStackTrace();
            return true;
        } catch (FederateNotExecutionMember e) {
            this.logger.error("init Error. Federate is not execution member.");
            e.printStackTrace();
            return true;
        } catch (FederateServiceInvocationsAreBeingReportedViaMOM e) {
            this.logger.error("init Error. FederateService MOM problem.");
            e.printStackTrace();
            return true;
        }

        return false;
    }


    private void getHandles() throws RTIinternalError, NotConnected {
        try {

            munitionDetonationInteractionClassHandle = ivctRti.getInteractionClassHandle(MUNITION_DETONATION);

            articulatedPartDataHandle = ivctRti.getParameterHandle(munitionDetonationInteractionClassHandle, ARTICULATED_PART_DATA);
            detonationLocParameterHandle = ivctRti.getParameterHandle(munitionDetonationInteractionClassHandle, DETONATION_LOC);
            detonationResultParameterHandle = ivctRti.getParameterHandle(munitionDetonationInteractionClassHandle, DETONATION_RESULT);
            eventIdParameterHandle = ivctRti.getParameterHandle(munitionDetonationInteractionClassHandle, EVENT_ID);
            firingObjIdParameterHandle = ivctRti.getParameterHandle(munitionDetonationInteractionClassHandle, FIRING_OBJ_ID);
            finalVelVectorParameterHandle = ivctRti.getParameterHandle(munitionDetonationInteractionClassHandle, FINAL_VELOCITY);
            fuseTypeParameterHandle = ivctRti.getParameterHandle(munitionDetonationInteractionClassHandle, FUSE_TYPE);
            munitionObjIdParameterHandle = ivctRti.getParameterHandle(munitionDetonationInteractionClassHandle, MUNIT_OBJ_ID);
            munitionTypeParameterHandle = ivctRti.getParameterHandle(munitionDetonationInteractionClassHandle, MUNIT_TYPE);
            quantityFiredParameterHandle = ivctRti.getParameterHandle(munitionDetonationInteractionClassHandle, QUANT_FIRED);
            rateOfFireParameterHandle = ivctRti.getParameterHandle(munitionDetonationInteractionClassHandle, RATE_OF_FIRE);
            relativeDetonationParameterHandle = ivctRti.getParameterHandle(munitionDetonationInteractionClassHandle, RELATIVE_DETONATION_LOC);
            targetObjIdParameterHandle = ivctRti.getParameterHandle(munitionDetonationInteractionClassHandle, TARGET_OBJ_ID);
            warheadTypeParameterHandle = ivctRti.getParameterHandle(munitionDetonationInteractionClassHandle, WARHEAD_TYPE);


        } catch (NameNotFound e) {
            throw new RTIinternalError("HlaInterfaceFailure", e);
        } catch (FederateNotExecutionMember e) {
            throw new RTIinternalError("HlaInterfaceFailure", e);
        } catch (InvalidInteractionClassHandle e) {
            throw new RTIinternalError("HlaInterfaceFailure", e);
        }
    }

    private void subscribeInteraction() throws FederateServiceInvocationsAreBeingReportedViaMOM, SaveInProgress, RestoreInProgress, FederateNotExecutionMember, NotConnected, RTIinternalError, FederateServiceInvocationsAreBeingReportedViaMOM {

        getHandles();

        try {
            ivctRti.subscribeInteractionClass(munitionDetonationInteractionClassHandle);
        } catch (InteractionClassNotDefined e) {
            throw new RTIinternalError("HlaInterfaceFailure", e);
        }
    }


    private void receiveInteraction(InteractionClassHandle interactionClass, ParameterHandleValueMap theParameters) {
        if (interactionClass.equals(munitionDetonationInteractionClassHandle)) {

            MunitionDetonationCoder munitionDetonationCoder = new MunitionDetonationCoder(ivctRti.getEncoderFactory());
            MunitionDetonation munitionDetonation = new MunitionDetonation();

            try {
                munitionDetonation.setArticulatedPartData(munitionDetonationCoder.decodeArticulatedParameterStructArray(theParameters.get(articulatedPartDataHandle)));
                munitionDetonation.setDetonationLocation(munitionDetonationCoder.decodeDetonationLocation(theParameters.get(detonationLocParameterHandle)));
                munitionDetonation.setDetonationResultCode(munitionDetonationCoder.decodeDetonationResultCode(theParameters.get(detonationResultParameterHandle)));
                munitionDetonation.setEventIdentifier(munitionDetonationCoder.decodeEventIdentifier(theParameters.get(eventIdParameterHandle)));
                munitionDetonation.setFiringObjectIdentifier(munitionDetonationCoder.decodeFiringObjectId(theParameters.get(firingObjIdParameterHandle)));
                munitionDetonation.setFinalVelocityVector(munitionDetonationCoder.decodeFinalVelocity(theParameters.get(finalVelVectorParameterHandle)));
                munitionDetonation.setFuseType(munitionDetonationCoder.decodeFuseType(theParameters.get(fuseTypeParameterHandle)));
                munitionDetonation.setMunitionObjectIdentifier(munitionDetonationCoder.decodeMunitionObjectIdentifier(theParameters.get(munitionObjIdParameterHandle)));
                munitionDetonation.setMunitionType( munitionDetonationCoder.decodeMunitionType(theParameters.get(munitionTypeParameterHandle)));
                munitionDetonation.setQuantityFired(munitionDetonationCoder.decodeQuantityFired(theParameters.get(quantityFiredParameterHandle)));
                munitionDetonation.setRateOfFire(munitionDetonationCoder.decodeRateOfFire(theParameters.get(rateOfFireParameterHandle)));
                munitionDetonation.setRelativeDetonationLocation(munitionDetonationCoder.decodeRelativePosition(theParameters.get(relativeDetonationParameterHandle)));
                munitionDetonation.setTargetObjectIdentifier(munitionDetonationCoder.decodeTargetObjectIdentifier(theParameters.get(targetObjIdParameterHandle)));
                munitionDetonation.setWarheadType(munitionDetonationCoder.decodeWarheadType(theParameters.get(warheadTypeParameterHandle)));


            } catch(DecoderException e) {
                System.out.println(e.toString());
                return;
            }

            munitionDetonationMap.put(munitionDetonation.getEventIdentifier().toString(), munitionDetonation);
        }
    }


    @Override
    public void receiveInteraction(InteractionClassHandle interactionClass,
                                   ParameterHandleValueMap theParameters, byte[] userSuppliedTag, OrderType sentOrdering,
                                   TransportationTypeHandle theTransport, SupplementalReceiveInfo receiveInfo)
            throws FederateInternalError {
        receiveInteraction(interactionClass, theParameters);
    }

    @Override
    public void receiveInteraction(InteractionClassHandle interactionClass,
                                   ParameterHandleValueMap theParameters, byte[] userSuppliedTag, OrderType sentOrdering,
                                   TransportationTypeHandle theTransport, LogicalTime theTime, OrderType receivedOrdering,
                                   SupplementalReceiveInfo receiveInfo) throws FederateInternalError {
        receiveInteraction(interactionClass, theParameters);
    }

    @Override
    public void receiveInteraction(InteractionClassHandle interactionClass,
                                   ParameterHandleValueMap theParameters, byte[] userSuppliedTag, OrderType sentOrdering,
                                   TransportationTypeHandle theTransport, LogicalTime theTime, OrderType receivedOrdering,
                                   MessageRetractionHandle retractionHandle, SupplementalReceiveInfo receiveInfo)
            throws FederateInternalError {
        receiveInteraction(interactionClass, theParameters);
    }


    /**
     * @param fad The list of munition detonation interactions included in the fad.
     * @return true means error, false means correct
     * @throws TcFailed due to discrepancies between fad and discovered MunitionDetonations
     * @throws TcInconclusive when no MunitionDetonations are found
     */
    public boolean testMunitionDetonationIntegrityIdentity(List<MunitionDetonation> fad) throws TcFailed, TcInconclusive {

        logger.info("Executing Test");

        String lineSeparator = "\n---------------------------------------------------------------------\n";

        if (munitionDetonationMap.isEmpty()) {
            throw new TcInconclusive("No MunitionDetonation interactions found on the RTI bus. A system "
                    + "under test must create discoverable MunitionDetonation interactions before attempting the test.");
        }
        // Traverse the discovered MunitionDetonation interactions.
        boolean testPassed = true;
        boolean fadMunitionDetPassesTest;
        StringBuilder failedStringBuilder = new StringBuilder();
        if (fad.size() != munitionDetonationMap.size()) {
            testPassed = false;
            String failedMessage = "FAIL: Fad and discovered MunitionDetonation sizes do not match";
            failedStringBuilder.append(failedMessage);
            logger.info(failedMessage);
        }

        for (MunitionDetonation fadMunitionDetonation : fad) {
            // Loop over each discovered Munition Detonation fire to check if this fadMunitionDetonation is present
            fadMunitionDetPassesTest = munitionDetonationMap.values().stream()
                    .anyMatch(discoveredMunitionDetonation -> discoveredMunitionDetonation.getEventIdentifier().equals(fadMunitionDetonation.getEventIdentifier()));
            String failedMessage;
            if (!fadMunitionDetPassesTest) {
                testPassed = false;
                failedMessage = "FAIL: Munition Detonation Interaction from FAD with identifier " + fadMunitionDetonation.getEventIdentifier()
                                     + " found no MunitionDetonation match in discovered MunitionDetonation Interactions";
                failedStringBuilder.append("\n"+failedMessage);
            } else {
                failedMessage = "OKAY: Munition Detonation Interaction from FAD with identifier " + fadMunitionDetonation.getEventIdentifier()
                                       + "was found in discovered Munition Detonation Interactions";
                failedStringBuilder.append("\n"+failedMessage);
            }
            logger.info(lineSeparator+failedMessage+lineSeparator);

        }
        if (!testPassed) {
            throw new TcFailed("Test failed due to errors in Munition Detonation Interaction(s) or absent/unrecognized Munition Detonation Interaction(s) : "+failedStringBuilder.toString()) ;
        } else {
            logger.info("{} TEST IS COMPLETED SUCCESFULLY. {}",lineSeparator,lineSeparator);
            return false;
        }
    }

    /**
     * @param fad The list of MunitionDetonation interactions included in the fad.
     * @return true means error, false means correct
     * @throws TcFailed due to discrepancies between fad and discovered MunitionDetonation
     * @throws TcInconclusive when no MunitionDetonation are found
     */
    public boolean testMunitionDetonationIntegrityParameters(List<MunitionDetonation> fad) throws TcFailed, TcInconclusive {

        logger.info("Executing Test");

        if (munitionDetonationMap.isEmpty()) {
            throw new TcInconclusive("No Munition Detonation Interactions found on the RTI bus. A system "
                    + "under test must create discoverable MunitionDetonation objects before attempting the test.");
        }
        // Traverse the discovered MunitionDetonation objects.
        boolean testPassed = true;
        boolean fadMunitionDetonationPassesTest;
        for (MunitionDetonation fadMunitionDetonation : fad) {
            // Loop over each discovered MunitionDetonation to check if this MunitionDetonation is present
            Optional<MunitionDetonation> optionalMunitionDetonation =
                    munitionDetonationMap.values()
                    .stream()
                    .filter(discoveredMunitionDetonation -> discoveredMunitionDetonation.getEventIdentifier().equals(fadMunitionDetonation.getEventIdentifier()))
                    .findFirst();

            if (!optionalMunitionDetonation.isPresent()) {
                testPassed = false;
                logger.info("---------------------------------------------------------------------");
                logger.info("FAIL: MunitionDetonation Interaction from FAD with identifier " + fadMunitionDetonation.getEventIdentifier()
                        + " found no ID match in discovered MunitionDetonation Interactions");
                logger.info("---------------------------------------------------------------------");
            } else {
              
                fadMunitionDetonationPassesTest = new MunitionDetonationEqualUtils(spatialThresold).areMunitionDetonationParametersEquals(fadMunitionDetonation, optionalMunitionDetonation.get());
                if (!fadMunitionDetonationPassesTest) {
                    testPassed = false;
                    logger.info("---------------------------------------------------------------------");
                    logger.info("FAIL: MunitionDetonation Interaction from FAD with identifier " + fadMunitionDetonation.getEventIdentifier()
                            + " has not the same parameters as its discovered MunitionDetonation match");
                    logger.info("---------------------------------------------------------------------");
                } else {
                    logger.info("---------------------------------------------------------------------");
                    logger.info("OKAY: MunitionDetonation from FAD with identifier " + fadMunitionDetonation.getEventIdentifier()
                            + " found an ID and type match in discovered MunitionDetonations");
                    logger.info("---------------------------------------------------------------------");
                }
            }
        }
        if (!testPassed) {
            throw new TcFailed("Test failed due to errors in Munition Detonation Interaction(s) or absent/unrecognized Munition Detonation Interaction(s).");
        } else {
            logger.info("---------------------------------------------------------------------");
            logger.info("TEST IS COMPLETED SUCCESFULLY.");
            logger.info("---------------------------------------------------------------------");
            return false;
        }
    }
    
    /**
     * @param fad The list of MunitionDetonation interactions included in the fad.
     * @return true means error, false means correct
     * @throws TcFailed due to discrepancies between fad and discovered MunitionDetonations
     * @throws TcInconclusive when no MunitionDetonations are found
     */
    public boolean testMunitionDetonationIntegritySpatial(List<MunitionDetonation> fad) throws TcFailed, TcInconclusive {

        logger.info("Executing Test");

        if (munitionDetonationMap.isEmpty()) {
            throw new TcInconclusive("No MunitionDetonation Interactions found on the RTI bus. A system "
                    + "under test must create discoverable MunitionDetonation objects before attempting the test.");
        }
        // Traverse the discovered MunitionDetonation Interactions.
        boolean testPassed = true;
        for (MunitionDetonation fadMunitionDet : fad) {
            // Loop over each discovered MunitionDetonation to check if this fadMunitionDet is present
            Optional<MunitionDetonation> optionalMunitionDetonation =
                    munitionDetonationMap.values()
                    .stream()
                    .filter(discoveredMunitionDet -> discoveredMunitionDet.getEventIdentifier().equals(fadMunitionDet.getEventIdentifier()))
                    .findFirst();
            
            if (!optionalMunitionDetonation.isPresent()) {
                testPassed = false;
                logger.info("---------------------------------------------------------------------");
                logger.info("FAIL: MunitionDetonation from FAD with identifier " + fadMunitionDet.getEventIdentifier()
                        + " found no ID match in discovered MunitionDetonations");
                logger.info("---------------------------------------------------------------------");
            } else {
                logger.debug("\nFrom fad:{}\nFrom fed:{}",
                        fadMunitionDet.getDetonationLocation(),
                        optionalMunitionDetonation.get().getDetonationLocation());


                boolean fadDetonationLocationPassesTest = new MunitionDetonationEqualUtils(spatialThresold).worldLocationEquals(
                        fadMunitionDet.getDetonationLocation(),
                        optionalMunitionDetonation.get().getDetonationLocation());

                boolean fadFinalVelocityPassesTest = new MunitionDetonationEqualUtils(spatialThresold).velocityEquals(
                        fadMunitionDet.getFinalVelocityVector(),
                        optionalMunitionDetonation.get().getFinalVelocityVector());

                //This part overwrites the decision made by the equalCheck of the MunitionDetonationEqualUtils class.
                if (fadFinalVelocityPassesTest == false) {
                    logger.warn("WARNING! MunitionDetonation Velocities do not match!");
                    logger.warn("This can be due to either a faulty SuT or a difference between DIS coordinates and the vcsutilities libraries used. This failure has however not affected the official outcome of this TestSuite.");
                    fadFinalVelocityPassesTest = true;
                }



                boolean fadRelativePassesTest = new MunitionDetonationEqualUtils(spatialThresold).relativePositionEquals(
                        fadMunitionDet.getRelativeDetonationLocation(),
                        optionalMunitionDetonation.get().getRelativeDetonationLocation());

                if (!(fadDetonationLocationPassesTest&&fadFinalVelocityPassesTest&&fadRelativePassesTest) ){
                    testPassed = false;
                    logger.info("---------------------------------------------------------------------");
                    logger.info("FAIL: MunitionDetonation from FAD with identifier " + fadMunitionDet.getEventIdentifier()
                            + " has not the same spatial info as its discovered MunitionDetonation");
                    logger.info("---------------------------------------------------------------------");
                } else {
                    logger.info("---------------------------------------------------------------------");
                    logger.info("OKAY: MunitionDetonation from FAD with identifier " + fadMunitionDet.getEventIdentifier()
                            + " found an ID and spatial match in discovered MunitionDetonations");
                    logger.info("---------------------------------------------------------------------");
                }
            }
        }
        if (!testPassed) {
            throw new TcFailed("Test failed due to errors in MunitionDetonation Interaction(s) or absent/unrecognized MunitionDetonation(s).");
        } else {
            logger.info("---------------------------------------------------------------------");
            logger.info("TEST IS COMPLETED SUCCESFULLY.");
            logger.info("---------------------------------------------------------------------");
            return false;
        }
    }

}
