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

package org.apache.freemarker.build.core

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.withType

class FreemarkerCommonPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("base")
        project.group = "org.apache.freemarker"

        val rootProject = project.rootProject
        project.tasks.withType<Jar> {
            from(rootProject.files("src/dist/jar/META-INF")) {
                into("META-INF")
            }
            from(rootProject.files("NOTICE")) {
                into("META-INF")
            }
        }
    }
}
