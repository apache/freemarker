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

import java.io.StringWriter;

import freemarker.log.Logger;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.Version;

public class HelloFreeMarker {
    // To test freemarker.log.Logger under GraalVM native
    private final static Logger log = Logger.getLogger(HelloFreeMarker.class.getName());

    public static void main(String[] args) throws Exception {
        // To test native configuration
        Class.forName("freemarker.ext.jython.JythonModel");

        try (StringWriter buffer = new StringWriter()) {
            // Creates FreeMarker configuration
            Version version = new Version(Configuration.getVersion().toString());  // using latest version
            Configuration cfg = new Configuration(version);
            cfg.setClassForTemplateLoading(HelloDataModel.class, "/templates");

            // Creates dataModel model
            HelloDataModel dataModel = new HelloDataModel();
            dataModel.setName("FreeMarker GraalVM Native Demo");
            dataModel.setVersion(version.toString());

            // Process template
            Template template = cfg.getTemplate("hello-world.ftlh");
            template.process(dataModel, buffer);
            log.info(String.format("result :\n%s", buffer));
        }
    }

}
