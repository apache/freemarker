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
    projectTitle.set("Apache FreeMarker Spring Framework support")
}

description = "FreeMarker template engine, Spring Framework support." +
        " This is an optional module, mostly useful in frameworks based on Spring Framework."

dependencies {
    api(project(":freemarker-core"))
    api(project(":freemarker-servlet"))

    val springVersion = "6.0.15"

    compileOnly("org.springframework:spring-core:$springVersion")
    compileOnly("org.springframework:spring-beans:$springVersion")
    compileOnly("org.springframework:spring-context:$springVersion")
    compileOnly("org.springframework:spring-web:$springVersion")
    compileOnly("org.springframework:spring-webmvc:$springVersion")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")
    compileOnly("jakarta.servlet.jsp:jakarta.servlet.jsp-api:3.1.0")
    compileOnly("jakarta.el:jakarta.el-api:5.0.0")

    testImplementation("org.springframework:spring-core:$springVersion")
    testImplementation("org.springframework:spring-beans:$springVersion")
    testImplementation("org.springframework:spring-context:$springVersion")
    testImplementation("org.springframework:spring-web:$springVersion")
    testImplementation("org.springframework:spring-webmvc:$springVersion")
    testImplementation("org.springframework:spring-test:$springVersion")
    testImplementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
    testImplementation("jakarta.servlet.jsp:jakarta.servlet.jsp-api:3.1.0")
    testImplementation("jakarta.el:jakarta.el-api:5.0.0")
    testImplementation(project(":freemarker-test-utils"))
}
