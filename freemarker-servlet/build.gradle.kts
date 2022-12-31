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

    // Because of the limitations of Eclipse dependency handling, we have to use the dependency artifacts from
    // Jetty ${jettyVersion} here, which is the Jetty version used for the tests. When the jettyVersion changes, run
    // `gradlew :freemarker-servlet:dependencies` and copy-paste the exact versions to here:
    compileOnly("org.eclipse.jetty.orbit:javax.servlet:3.0.0.v201112011016")
    compileOnly("org.eclipse.jetty.orbit:javax.servlet.jsp:2.2.0.v201112011158")
    compileOnly("org.eclipse.jetty.orbit:javax.el:2.2.0.v201108011116")

    // When changing this, the non-test org.eclipse.jetty.orbit dependencies must be updated as well! Thus, it must use
    // exactly the same Servlet/JSP-related specification versions as the minimal requirements of FreeMarker.
    val jettyVersion = "8.1.22.v20160922"

    testImplementation("org.eclipse.jetty:jetty-server:$jettyVersion")
    testImplementation("org.eclipse.jetty:jetty-webapp:$jettyVersion")
    testImplementation("org.eclipse.jetty:jetty-jsp:$jettyVersion")
    testImplementation("org.eclipse.jetty:jetty-util:$jettyVersion")
    // Jetty also contains the servlet-api and jsp-api classes

    testImplementation("displaytag:displaytag:1.2") {
        exclude(group = "com.lowagie", module = "itext")
        // We manage logging centrally:
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "org.slf4j", module = "jcl104-over-slf4j")
        exclude(group = "log4j", module = "log4j")
    }

    // Override Java 9 incompatible version (coming from displaytag):
    testImplementation("commons-lang:commons-lang:2.6")

    val springVersion = "2.5.6.SEC03"
    testImplementation("org.springframework:spring-core:$springVersion")
    testImplementation("org.springframework:spring-test:$springVersion")

    testImplementation(project(":freemarker-test-utils"))
}
