/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package freemarker.test;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prints and logs what the JUnit test are running on (doesn't actually test anything). 
 */
public class RuntimeEnvironmentReporterTest {
    
    private Logger log = LoggerFactory.getLogger(RuntimeEnvironmentReporterTest.class);
    
    @Test
    public void logRuntimeEnvironment() {
        for (String propName : new String[] {
                "java.version", "java.vendor", "java.vm.name", "java.home",
                "os.name", "os.arch", "os.version" }) {
            String propValue = System.getProperty(propName);
            System.out.println(propName + ": " + propValue);
            log.info("{}: {}", propName, propValue);
        }
    }

}
