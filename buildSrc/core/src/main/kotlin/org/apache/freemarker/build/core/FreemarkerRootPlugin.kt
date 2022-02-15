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
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin

const val FREEMARKER_PUBLISHED_PROJECTS_CONFIGURATION_NAME = "publishedProjects"
const val FREEMARKER_JAVADOC_PROJECTS_CONFIGURATION_NAME = "javadocProjects"

open class FreemarkerRootPlugin : Plugin<Project> {
    private class Configurer(
        private val project: Project,
        private val versionService: FreemarkerVersionService
        ) {

        private val tasks = project.tasks

        fun configure() {
            project.version = versionService.versionDef.version

            tasks.withType<org.nosphere.apache.rat.RatTask>() {
                doLast {
                    println("RAT (${name} task) report was successful: ${reportDir.get().asFile.toPath().resolve("index.html").toUri()}")
                }
            }
        }
    }

    override fun apply(project: Project) {
        project.pluginManager.apply(FreemarkerCommonPlugin::class)
        project.pluginManager.apply("org.nosphere.apache.rat")

        val providers = project.providers

        val getBooleanProperty = { name: String, defaultValue: Boolean ->
            providers
                    .gradleProperty(name)
                    .map { it.toBoolean() }
                    .orElse(defaultValue)
        }

        project.configurations.apply {
            create(FREEMARKER_PUBLISHED_PROJECTS_CONFIGURATION_NAME)
            create(FREEMARKER_JAVADOC_PROJECTS_CONFIGURATION_NAME)
        }

        val versionService = project.gradle
                .sharedServices
                .registerIfAbsent(FREEMARKER_VERSION_SERVICE_NAME, FreemarkerVersionService::class.java) {
                    parameters.apply {
                        developmentBuild.set(getBooleanProperty("developmentBuild", false))
                        doSignPackage.set(getBooleanProperty("signPublication", true))
                        rootDir.set(project.rootDir)
                    }
                }
                .get()

        val ext = FreemarkerRootExtension(project, versionService)
        project.extensions.add("freemarkerRoot", ext)

        configureReleaseVerification(project, ext)

        Configurer(project, versionService).configure()

        project.afterEvaluate {
            // We are setting this, so the pom.xml will be generated properly
            System.setProperty("line.separator", "\n")
        }
    }

    private fun configureReleaseVerification(project: Project, ext: FreemarkerRootExtension) {
        val verifyReleaseSetup = project.tasks.register("verifyReleaseSetup") {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Basic checks to check to avoid accidental development configuration"

            doLast {
                if (ext.developmentBuild) {
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

fun configureRequirePublished(project: Project) {
    val tasks = project.tasks
    val checkListedAsPublished = tasks.register("checkListedAsPublished") {
        description = "Verifies that the project was listed as published in the root project."
        group = LifecycleBasePlugin.VERIFICATION_GROUP

        val ourPath = project.path
        val rootDir = project.rootDir.toPath()
        val distributedProjectPaths = project.rootProject.the<FreemarkerRootExtension>().distributedProjectPaths
        doLast {
            if (ourPath !in distributedProjectPaths.get()) {
                throw IllegalStateException("${ourPath} was registered as a published Java project," +
                        " but it is not listed as a published project" +
                        " in ${rootDir.resolve("build.gradle.kts")}")
            }
        }
    }

    tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
        dependsOn(checkListedAsPublished)
    }
}