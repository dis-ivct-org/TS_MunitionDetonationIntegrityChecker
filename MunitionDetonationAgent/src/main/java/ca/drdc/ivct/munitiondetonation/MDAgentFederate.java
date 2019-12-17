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

import hla.rti1516e.exceptions.RTIexception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.ConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;

public class MDAgentFederate {
	private static Logger logger = LoggerFactory.getLogger(MDAgentFederate.class);

	private Controller controller;

	public void setup() {
		controller = new Controller();
		BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Ensure the TC is started and awaiting interactions before proceeding.");
		System.out.println("Enter 'Y' to start.");

		try {
			boolean doTest = false;
			while (doTest == false) {
				String in = inputReader.readLine();
				if (in.equalsIgnoreCase("y")) {
					doTest = true;
				}
			}

			controller.execute(null);
		} catch (ConfigurationException | IOException | RTIexception e) {
			logger.error(e.getMessage(), e);
			stop();
		}

		System.out.println("Press Q and Enter to exit");
		while (true) {
			String in;
			try {
				in = inputReader.readLine();
				if (in.equalsIgnoreCase("q")) {
					stop();
					break;
				}
			} catch (IOException ignore) {}
		}
	}

	public void stop() {
		controller.stop();
	}

	public static void main(String[] args) {
		MDAgentFederate federate = new MDAgentFederate();
		federate.setup();
	}
}
