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

package org.freemarker.core.graal;

import freemarker.log.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HelloFreeMarkerTest {

    private final static Logger log = Logger.getLogger(HelloFreeMarker.class.getName());

    @Test
    public void testMain() {
        try {
            HelloFreeMarker.main(new String[0]);
            Assertions.assertTrue( Boolean.TRUE );
        } catch (Exception e) {
            String message = String.format( "Error : %s", e );
            log.error( message , e );
            Assertions.fail( message );
        }
    }

}
