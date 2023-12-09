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
    compileOnly("javax.servlet:javax.servlet-api:3.1.0")
    compileOnly("javax.servlet.jsp:javax.servlet.jsp-api:2.3.3")
    compileOnly("javax.el:javax.el-api:3.0.0") // EL is not included in jsp-api anymore (was there in jsp-api 2.1)

    // Chose the Jetty version very carefully, as it should implement the same Servlet API, JSP API, and EL API
    // than what we declare above, because the same classes will come from Jetty as well. For example, Jetty depends
    // on org.mortbay.jasper:apache-el, which contains the javax.el classes, along with non-javax.el classes, so you
    // can't even exclude it. Similarly, org.eclipse.jetty:apache-jsp contains the JSP API javax.servlet.jsp classes,
    // yet again along with other classes. Anyway, this mess is temporary, as we will migrate to Jakarta, and only
    // support that.
    val jettyVersion = "9.4.53.v20231009"
    testImplementation("org.eclipse.jetty:jetty-server:$jettyVersion")
    testImplementation("org.eclipse.jetty:jetty-webapp:$jettyVersion")
    testImplementation("org.eclipse.jetty:jetty-util:$jettyVersion")
    testImplementation("org.eclipse.jetty:apache-jsp:$jettyVersion")
    // Jetty also contains the servlet-api and jsp-api classes

    // JSP JSTL (not included in Jetty):
    val apacheStandardTaglibsVersion = "1.2.5"
    testImplementation("org.apache.taglibs:taglibs-standard-impl:$apacheStandardTaglibsVersion")
    testImplementation("org.apache.taglibs:taglibs-standard-spec:$apacheStandardTaglibsVersion")

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
