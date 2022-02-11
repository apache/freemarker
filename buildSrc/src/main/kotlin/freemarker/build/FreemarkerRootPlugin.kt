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

package freemarker.build

import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.filter
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.jvm.tasks.ProcessResources

open class FreemarkerRootPlugin : Plugin<Project> {
    private class Configurer(
        private val project: Project,
        private val ext: FreemarkerRootExtension
        ) {

        private val tasks = project.tasks
        private val java = project.the<JavaPluginExtension>()
        private val mainSourceSet = java.sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).get()

        fun configure() {
            project.version = ext.versionDef.version
            project.extensions.add("freemarkerRoot", ext)

            project.configure<JavaPluginExtension> {
                withSourcesJar()
                withJavadocJar()

                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(ext.defaultJavaVersion))
                }
            }

            tasks.apply {
                val resourceTemplatesDir = ext.versionService.resourceTemplatesDir

                named<ProcessResources>(JavaPlugin.PROCESS_RESOURCES_TASK_NAME) {
                    with(project.copySpec {
                        from(resourceTemplatesDir)
                        filter<ReplaceTokens>(mapOf("tokens" to ext.versionDef.versionFileTokens))
                    })
                }

                named<Jar>(mainSourceSet.sourcesJarTaskName) {
                    from(resourceTemplatesDir)
                }

                withType<org.nosphere.apache.rat.RatTask>() {
                    doLast {
                        println("RAT (${name} task) report was successful: ${reportDir.get().asFile.toPath().resolve("index.html").toUri()}")
                    }
                }
            }

            configureReleaseVerification()
        }

        private fun configureReleaseVerification() {
            val verifyReleaseSetup = project.tasks.register("verifyReleaseSetup") {
                group = LifecycleBasePlugin.VERIFICATION_GROUP
                description = "Basic checks to check to avoid accidental development configuration"

                doLast {
                    if (ext.versionService.developmentBuild) {
                        throw IllegalStateException("The development build configuration is active!")
                    }
                    if (!ext.doSignPackages.get()) {
                        throw IllegalStateException("Package signing is disabled!")
                    }
                }
            }

            if (ext.isPublishedVersion()) {
                project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
                    dependsOn(verifyReleaseSetup)
                }
            }

            project.tasks.withType<AbstractPublishToMaven>() {
                shouldRunAfter(verifyReleaseSetup)
            }
        }
    }

    override fun apply(project: Project) {
        project.pluginManager.apply("java-library")
        project.pluginManager.apply("org.nosphere.apache.rat")

        val versionService = FreemarkerVersionService(project.layout, project.providers)
        val ext = FreemarkerRootExtension(project, versionService)

        Configurer(project, ext).configure()

        project.afterEvaluate {
            // We are setting this, so the pom.xml will be generated properly
            System.setProperty("line.separator", "\n")
        }
    }
}
