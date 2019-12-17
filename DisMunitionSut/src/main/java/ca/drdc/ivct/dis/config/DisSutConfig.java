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
package ca.drdc.ivct.dis.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisSutConfig {

    private static Logger logger = LoggerFactory.getLogger(DisSutConfig.class);

    private static final String TEST_CASE_DIR = "testcaseDir";
    private static final String BROADCAST_NETWORK = "broadCastNetwork";
    private static final String DEFAULT_TEST_CASE_DIR = "testcases";

    /**
     * host to listen for
     */
    private List<Host> hosts;

    /**
     * broadCast IP and port
     */
    private Host broadCastNetwork;

    /**
     * List of files containing the entities
     */
    private ArrayList<URL> testcaseList;

    /**
     * load all configuration for the Sut
     * @param fileName config file from the resource folder
     */
    public DisSutConfig(String fileName) {

        URL configFileUrl = this.getClass().getResource(fileName);
        logger.info("Found config file at: {}", configFileUrl.getPath());

        File configFile = new File(configFileUrl.getFile());
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(configFile));
        } catch (IOException e) {
            logger.error("Could not find a valid properties file. {} ", configFile.toString(), e);
        }

        try {
            broadCastNetwork = new Host(properties.getProperty(BROADCAST_NETWORK, "255.255.255.255:30001"));
        } catch (ParseException e) {
            logger.error("Error parsing the broadCastNetwork" + configFile.toString(), e);
        }

        String testcaseDir = properties.getProperty(TEST_CASE_DIR, DEFAULT_TEST_CASE_DIR);

        URL testcaseDirFileUrl = this.getClass().getResource("/" + testcaseDir + "/");
        File testcaseDirFile = null;
        try {
            testcaseDirFile = new File(testcaseDirFileUrl.toURI());
        } catch (URISyntaxException e) {
            testcaseDirFile = new File(testcaseDirFileUrl.getPath());
        }

        testcaseList = new ArrayList<>();
        if (testcaseDirFile.isDirectory()) {
            File[] files = testcaseDirFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    try {
                        testcaseList.add(file.toURI().toURL());
                    } 
                    catch (MalformedURLException e) {
                        logger.error("File {} does not exist",file);
                    }
                    catch (SecurityException e) {
                        logger.error("Permission denied ffor file {}",file);
                    }
                }
            }
        }
    }
    
    public List<URL> getTestcaseList() {
        return testcaseList;
    }

    public List<Host> getHosts() {
        return hosts;
    }

    public Host getBroadCastNetwork() {
        return broadCastNetwork;
    }

}
