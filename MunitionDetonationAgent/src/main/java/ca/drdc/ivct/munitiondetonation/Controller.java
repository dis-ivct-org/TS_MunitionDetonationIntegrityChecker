/*
* Copyright (C) Her Majesty the Queen in Right of Canada, as represented by the Minister of National Defence, 2017
*
* Contract:  W7707-145677/001/HAL
*            Call-up 16 
* Author:    OODA Technologies Inc.
* Version:   1.0
* Date:      March 31, 2017
*
*/
 
package ca.drdc.ivct.munitiondetonation;


import ca.drdc.ivct.fom.utils.MunitionDetonationCSVReader;
import ca.drdc.ivct.fom.warfare.MunitionDetonation;
import ca.drdc.ivct.munitiondetonation.hlamodule.HlaInterface;
import hla.rti1516e.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.util.List;


public class Controller {
	private static Logger logger = LoggerFactory.getLogger(Controller.class);

	private HlaInterface hlaInterface;

	public void execute(MDAgentConfig config) throws ConfigurationException, IOException, RTIexception {

		if (config == null) {
			String externalResourceFolder = System.getenv("IVCT_CONF");
			if (externalResourceFolder == null) {
				throw new ConfigurationException("IVCT_CONF is not defined");
			}
			config = new MDAgentConfig(externalResourceFolder + "/IVCTsut/MunitionDetonationAgent/resources");

		}

		hlaInterface = new HlaInterface();

		try {
			hlaInterface.start(config.getLocalSettingsDesignator(), config.getFom().getAbsolutePath(), config.getFederationName(), config.getFederateName());
		} catch (RTIexception e) {
			logger.error("Could not connect to the RTI using the local settings designator {}", config.getLocalSettingsDesignator(), e);
			throw e;
		}

		// Load all files in testcases folder. This constitutes the federation agreement document (FAD)
		List<MunitionDetonation> fad = MunitionDetonationCSVReader.loadCSVFileToMunitionDetonationList(config.getTestcaseList());

		// Create MunitionDetonation objects in RTI
		for (MunitionDetonation munitionDetonation : fad) {
			try {
				hlaInterface.createMunitionDetonation(munitionDetonation);
				logger.info("Created Munition Detonation {}", munitionDetonation);
			} catch (FederateNotExecutionMember | RestoreInProgress | SaveInProgress | NotConnected | RTIinternalError e) {
				logger.error("Error creating a munition detonation", e);
			}
		}
	}

	public void stop() {
		try {
			if (hlaInterface == null) {
				logger.warn("Controller.stop: hlaInterface doesn't exist!");
				return;
			}
			hlaInterface.stop();
		} catch (RTIexception e) {
			logger.error(e.getMessage(), e);
		}

	}
}
