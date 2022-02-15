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

plugins {
    `freemarker-published-java`
}

freemarkerJava {
    projectTitle.set("Apache FreeMarker DOM support")
}

description = "FreeMarker template engine, W3C DOM (XML) wrapping support." +
        " This is an optional module, useful when the data-model can contain variables that are XML nodes."

dependencies {
    api(project(":freemarker-core"))
    api(libs.legacyFreemarker)

    compileOnly(libs.jaxen)
    compileOnly(libs.saxpath)
    compileOnly(libs.xalan)

    testRuntimeOnly(libs.jaxen)
    testRuntimeOnly(libs.saxpath)
    testRuntimeOnly(libs.xalan)
    testImplementation(project(":freemarker-test-utils"))
}
