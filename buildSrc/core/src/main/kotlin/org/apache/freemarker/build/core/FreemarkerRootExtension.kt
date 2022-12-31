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

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.setProperty

const val FREEMARKER_JAVADOC_CLASSPATH_CONFIG_NAME = "javadocClasspath"

open class FreemarkerRootExtension(private val project: Project, versionService: FreemarkerVersionService) {
    val versionDef = versionService.versionDef
    val developmentBuild = versionService.developmentBuild
    val doSignPackages = versionService.doSignPackages

    private val _distributedProjectPaths = project.objects.setProperty<String>()
    val distributedProjectPaths: Provider<Set<String>> = _distributedProjectPaths

    private val _javadocProjectPaths = project.objects.setProperty<String>()
    val javadocProjectPaths: Provider<Set<String>> = _javadocProjectPaths

    fun isPublishedVersion(): Boolean {
        return !versionDef.version.endsWith("-SNAPSHOT") ||
                !versionDef.displayVersion.endsWith("-nightly")
    }

    fun registerPublishedShort(shortName: String, javadoc: Boolean = true) {
        val publishedProject = getProjectFromShortName(shortName)
        val path = publishedProject.path

        _distributedProjectPaths.add(path)
        project.dependencies.add(FREEMARKER_PUBLISHED_PROJECTS_CONFIGURATION_NAME, publishedProject)

        if (javadoc) {
            _javadocProjectPaths.add(path)
            project.dependencies.apply {
                val publishedProjectCompile = project(mapOf(
                    "path" to path,
                    "configuration" to FREEMARKER_JAVADOC_CLASSPATH_CONFIG_NAME
                ))
                add(FREEMARKER_JAVADOC_PROJECTS_CONFIGURATION_NAME, publishedProjectCompile)
            }
        }
    }

    private fun getProjectFromShortName(shortName: String): Project = project.project(":freemarker-${shortName}")
}
