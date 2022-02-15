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

rootProject.name = "freemarker3"

apply(from = rootDir.toPath().resolve("gradle").resolve("repositories.gradle.kts"))

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("defaultJava", "8")

            version("junit", "4.12")
            version("slf4j", "1.7.25")

            library("legacyFreemarker", "org.freemarker:freemarker-gae:2.3.28")

            library("commonsIo", "commons-io:commons-io:2.5")
            library("commonsLang", "org.apache.commons:commons-lang3:3.6")
            library("commonsCollections", "commons-collections:commons-collections:3.1")
            library("commonsCli", "commons-cli:commons-cli:1.4")
            library("findbugs", "com.google.code.findbugs:annotations:3.0.0")
            library("guava", "com.google.guava:guava-jdk5:17.0")
            library("jaxen", "jaxen:jaxen:1.0-FCS")
            library("jrebel", "org.zeroturnaround:javarebel-sdk:1.2.2")
            library("saxpath", "saxpath:saxpath:1.0-FCS")
            library("xalan", "xalan:xalan:2.7.0")

            library("logback", "ch.qos.logback:logback-classic:1.1.8")
            library("janino", "org.codehaus.janino:janino:3.0.6")
            library("slf4jApi", "org.slf4j", "slf4j-api").versionRef("slf4j")
            library("slf4jJcl", "org.slf4j", "jcl-over-slf4j").versionRef("slf4j")
            library("slf4jLog4j", "org.apache.logging.log4j:log4j-to-slf4j:2.8.2")

            library("junit", "junit", "junit").versionRef("junit")
            library("hamcrest", "org.hamcrest:hamcrest-library:1.3")
        }
    }
}

rootDir
    .listFiles()
    ?.filter { candidate ->
        candidate.name.startsWith("freemarker-") &&
                candidate.isDirectory &&
                File(candidate, "build.gradle.kts").exists()
    }
    ?.forEach { dir ->
        val subprojectName = dir.name
        include(subprojectName)
        project(":${subprojectName}").projectDir = dir
    }
