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

import ca.drdc.ivct.fom.utils.MunitionDetonationCSVReader;
import ca.drdc.ivct.fom.warfare.MunitionDetonation;
import ca.drdc.ivct.tc_lib_md_integritycheck.CountdownTimer;
import ca.drdc.ivct.tc_lib_md_integritycheck.IntegrityCheckTcParam;
import ca.drdc.ivct.tc_lib_md_integritycheck.MunitionDetonationIntegrityCheckBaseModel;
import de.fraunhofer.iosb.tc_lib.*;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;

public class MunitionDetonationIntegrityTC_0001 extends AbstractTestCase {

    private static final String FEDERATE_NAME = "MunitionDetonationTester";
    private IntegrityCheckTcParam tcParam;
    private IVCT_RTIambassador ivctRtiAmbassador;
    private IVCT_LoggingFederateAmbassador loggingFedAmbassador;
    private MunitionDetonationIntegrityCheckBaseModel munitionDetonationModel;
    private List<MunitionDetonation> fad ;

    @Override
    protected IVCT_BaseModel getIVCT_BaseModel(String tcParamJson, Logger logger) throws TcInconclusive {
        tcParam = new IntegrityCheckTcParam(tcParamJson);
        ivctRtiAmbassador = IVCT_RTI_Factory.getIVCT_RTI(logger);
        munitionDetonationModel = new MunitionDetonationIntegrityCheckBaseModel(logger, ivctRtiAmbassador, tcParam);
        loggingFedAmbassador = new IVCT_LoggingFederateAmbassador(munitionDetonationModel, logger);

        return munitionDetonationModel;
    }

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
        logger.info("Attempting to connect to RTI with federate: {}", FEDERATE_NAME);
        // Initiate RTI
        munitionDetonationModel.initiateRti(FEDERATE_NAME, loggingFedAmbassador);

        // Get handles and publish / subscribe interactions
        if (munitionDetonationModel.init()) {
            throw new TcInconclusive("munitionDetonationModel.init() failed to execute");
        }

        // Load all files in test cases folder. This constitutes the federation agreement document (FAD)

        this.fad = MunitionDetonationCSVReader.loadCSVFileToMunitionDetonationList(Arrays.asList(tcParam.getFadUrl()));
        if (fad.isEmpty()) {
            throw new TcInconclusive("The FAD is empty.");
        }


        // Let five second to IVCT federation client to discover the munition detonations.
        new CountdownTimer(tcParam.getWaitingPeriod(), logger).run();
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

        munitionDetonationModel.testMunitionDetonationIntegrityIdentity(fad);
        munitionDetonationModel.testMunitionDetonationIntegrityParameters(fad);
        munitionDetonationModel.testMunitionDetonationIntegritySpatial(fad);
    }

    @Override
    protected void postambleAction(Logger logger) throws TcInconclusive {
        munitionDetonationModel.terminateRti();
        munitionDetonationModel = new MunitionDetonationIntegrityCheckBaseModel(logger, ivctRtiAmbassador, tcParam);
    }

}
