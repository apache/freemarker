import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import java.util.Arrays

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
    java
    application
    id("org.graalvm.buildtools.native") version "0.10.3"
}

group = "org.freemarker"
version = rootProject.version

val graalSdkVersion = "24.1.1"
val junitJupiterVersion = "5.11.4"

val profile = findProperty("profile") as String? ?: "default"

dependencies {
    implementation(project(":"))
    implementation("org.python:jython:2.5.0")
    compileOnly("org.graalvm.sdk:graal-sdk:$graalSdkVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

application {
    mainClass.set("org.freemarker.core.graal.HelloFreeMarker")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("PASSED", "FAILED", "SKIPPED")
    }
}

graalvmNative {
    binaries.all {
        fallback.set(false)
        verbose.set(true)
        resources.autodetect()
        buildArgs.add( "-H:ReflectionConfigurationFiles=$projectDir/src/main/config/reflect-config.json" )
        jvmArgs()
    }
}
