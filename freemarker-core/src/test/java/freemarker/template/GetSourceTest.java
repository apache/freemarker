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
package freemarker.template;

import static org.junit.Assert.*;

import org.junit.Test;

public class GetSourceTest {

    
    @Test
    public void testGetSource() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
        
        {
            // Note: Default tab size is 8.
            Template t = new Template(null, "a\n\tb\nc", cfg);
            // A historical quirk we keep for B.C.: it repaces tabs with spaces.
            assertEquals("a\n        b\nc", t.getSource(1, 1, 1, 3));
        }
        
        {
            cfg.setTabSize(4);
            Template t = new Template(null, "a\n\tb\nc", cfg);
            assertEquals("a\n    b\nc", t.getSource(1, 1, 1, 3));
        }
        
        {
            cfg.setTabSize(1);
            Template t = new Template(null, "a\n\tb\nc", cfg);
            // If tab size is 1, it behaves as it always should have: it keeps the tab.
            assertEquals("a\n\tb\nc", t.getSource(1, 1, 1, 3));
        }
    }
    
}
