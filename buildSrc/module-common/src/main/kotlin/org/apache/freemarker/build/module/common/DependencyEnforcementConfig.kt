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

package org.apache.freemarker.build.module.common

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionSelector
import org.gradle.kotlin.dsl.exclude

fun configureExcludedDependencies(configuration: Configuration) {
    // xml-apis is part of Java SE since version 1.4:
    configuration.exclude(group = "xml-apis", module = "xml-apis")

    configuration.exclude(group = "commons-logging", module = "commons-logging")
}

fun configureBannedDependencies(project: Project, configuration: Configuration) {
    fun ModuleVersionSelector.ensureNot(group: String, name: String) {
        if (group == this.group && name == this.name) {
            throw UnsupportedOperationException(
                "Banned library in the dependency graph: ${group}:${name}. "
                        + "Use `gradlew ${project.path}:dependencies --configuration ${configuration.name}`" +
                        " to find who pulls it in then exclude it there."
            )
        }
    }

    configuration.resolutionStrategy {
        failOnNonReproducibleResolution()

        eachDependency {
            target.apply {
                ensureNot("org.slf4j", "slf4j-log4j1")
                ensureNot("org.slf4j", "slf4j-jdk14")
                ensureNot("log4j", "log4j")
            }
        }
    }
}
