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
package org.apache.freemarker.manual.examples;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.templateresolver.impl.ClassTemplateLoader;
import org.junit.Test;

public class GettingStartedExample {

    @Test
    public void main() throws Exception {
        /* ------------------------------------------------------------------------ */    
        /* You should do this ONLY ONCE in the whole application life-cycle:        */    
    
        /* Create the configuration singleton (using builder pattern) */
        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                .templateLoader(new ClassTemplateLoader(
                        GettingStartedExample.class, ""))
                .build();

        /* ------------------------------------------------------------------------ */    
        /* You usually do these for MULTIPLE TIMES in the application life-cycle:   */    

        /* Create a data-model */
        Map<String, Object> root = new HashMap<>();
        root.put("user", "Big Joe");
        Product latest = new Product();
        latest.setUrl("products/greenmouse.html");
        latest.setName("green mouse");
        root.put("latestProduct", latest);

        /* Get the template (uses cache internally) */
        Template temp = cfg.getTemplate("test.ftlh");

        /* Merge data-model with template */
        Writer out = new OutputStreamWriter(System.out);
        temp.process(root, out);
        // Note: Depending on what `out` is, you may need to call `out.close()`.
        // This is usually the case for file output, but not for servlet output.
    }
    
}
