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
    projectTitle.set("Apache FreeMarker Servlet and JSP support")
}

description = "FreeMarker template engine, Servlet and JSP support." +
        " This is an optional module, mostly useful in frameworks based on JSP Model-2 architecture," +
        " or when custom JSP tags need to be called from templates."

dependencies {
    api(project(":freemarker-core"))
    api(libs.legacyFreemarker)

    // Servlet, JSP, and EL related classes
    compileOnly("jakarta.servlet:jakarta.servlet-api:5.0.0")
    compileOnly("jakarta.servlet.jsp:jakarta.servlet.jsp-api:3.0.0")
    compileOnly("jakarta.el:jakarta.el-api:4.0.0")

    // Jetty 12 is Servlet 6 (jakarta), Jetty 11 was Servlet 5 (also jakarta). But Spring Servlet API mocks,
    // which we use in tests, jump from Servlet 4 (javax) to 6 (jakarta). So, we have to go with 12.
    // (Note that Jetty artifact names, package names were completely changed in 12, also some old API-s
    // were replaced with something else. So our test utility code is affected by this as well.)
    val jettyVersion = "11.0.19"
    testImplementation("org.eclipse.jetty:jetty-webapp:$jettyVersion")
    testImplementation("org.eclipse.jetty:jetty-annotations:$jettyVersion")
    testImplementation("org.eclipse.jetty:apache-jsp:$jettyVersion")
    testImplementation("org.eclipse.jetty:jetty-slf4j-impl:$jettyVersion")
    // Jetty also contains the servlet-api and jsp-api and el-api classes

    // JSP JSTL (not included in Jetty):
    testImplementation("org.glassfish.web:jakarta.servlet.jsp.jstl:3.0.1")

    testImplementation("com.github.hazendaz:displaytag:3.0.0-M2") {
        exclude(group = "com.lowagie", module = "itext")
        // We manage logging centrally:
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "org.slf4j", module = "jcl104-over-slf4j")
        exclude(group = "log4j", module = "log4j")
    }

    val springVersion = "6.1.2"
    testImplementation("org.springframework:spring-core:$springVersion")
    testImplementation("org.springframework:spring-web:$springVersion")
    testImplementation("org.springframework:spring-test:$springVersion")

    testImplementation(project(":freemarker-test-utils"))
}
