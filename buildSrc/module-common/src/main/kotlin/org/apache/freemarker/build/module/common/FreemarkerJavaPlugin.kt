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

import org.apache.freemarker.build.core.FreemarkerCommonPlugin
import org.apache.freemarker.build.core.getFreemarkerVersionService
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

fun getDefaultJavaVersion(project: Project): JavaLanguageVersion {
    return project.providers
        .gradleProperty("freemarkerDefaultJavaVersion")
        .getOrElse("17")
        .let { JavaLanguageVersion.of(it) }
}

class FreemarkerJavaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.run {
            apply(FreemarkerCommonPlugin::class)
            apply("java-library")
        }

        project.extensions.add(
            "freemarkerJava",
            FreemarkerJavaExtension(project, project.getFreemarkerVersionService())
        )

        val configureExcludedDependencies = { configName: String ->
            project.configurations.named(configName).configure {
                configureExcludedDependencies(this)
            }
        }

        val configureBannedDependencies = { configName: String ->
            project.configurations.named(configName).configure {
                configureBannedDependencies(project, this)
            }
        }

        project.configure<JavaPluginExtension> {
            sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).configure {
                configureBannedDependencies(compileClasspathConfigurationName)
                configureBannedDependencies(runtimeClasspathConfigurationName)
            }

            sourceSets.all {
                configureExcludedDependencies(compileClasspathConfigurationName)
                configureExcludedDependencies(runtimeClasspathConfigurationName)
            }

            toolchain {
                languageVersion.set(getDefaultJavaVersion(project))
            }
        }

        project.tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
        }
    }
}
