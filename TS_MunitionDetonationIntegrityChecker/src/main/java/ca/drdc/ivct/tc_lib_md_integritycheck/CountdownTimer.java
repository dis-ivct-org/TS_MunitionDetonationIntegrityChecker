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

import org.slf4j.Logger;

/**
 * Wait for a determined amount of time.
 * 
 * @author laurenceO
 */
public class CountdownTimer implements Runnable{
    private volatile int countdownSec; // in seconds
    private final int initialCountdown;
    private Logger logger;
    
    /**
     * @param countdownSec number of second to countdown
     * @param logger the logger 
     */
    public CountdownTimer(int countdownSec, Logger logger){
        this.initialCountdown = countdownSec;
        this.countdownSec = countdownSec;
        this.logger = logger;
    }
    
    public void resetTimer(){
        this.countdownSec = initialCountdown;
    }
    
    @Override
    public void run() {
        while(countdownSec > 0){
            try {
                // Sleep one second.
                Thread.sleep(1000); 
                countdownSec--;
                logger.info("Listening for another {} seconds.", countdownSec);
            } catch (InterruptedException e) {
                logger.warn("Counter thread interrupted", e);
                // Restore interrupted state...
                Thread.currentThread().interrupt();
            } 
        }
    }
}