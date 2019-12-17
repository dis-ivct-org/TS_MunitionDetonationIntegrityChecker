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

import java.text.ParseException;

/**
 * Represent a host ip address and a port
 */
public class Host {

    private String ipAdress;
    private int port;

    public Host(String ipAdress, int port) {
        super();
        this.ipAdress = ipAdress;
        this.port = port;
    }

    /**
     * 
     * @param host a string with a ipAddress:port
     * @throws ParseException if ipaddress or port is wrong
     */
    public Host(String host) throws ParseException {
        super();
        String[] hostPort = host.split(":");
        
        if (hostPort.length != 2) {
            throw new ParseException("Error parsing the host", host.length());
        }
        this.ipAdress = hostPort[0];
        try {
            this.port = Integer.parseInt(hostPort[1]);
        }catch(NumberFormatException e) {
            throw new ParseException("Error parsing the port",host.length()+1);
        }
    }
    
    public String getIpAdress() {
        return ipAdress;
    }

    public void setIpAdress(String ipAdress) {
        this.ipAdress = ipAdress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
