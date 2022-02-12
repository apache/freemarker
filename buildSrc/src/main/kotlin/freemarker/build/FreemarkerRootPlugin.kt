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

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Properties
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.filter
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources

private val RESOURCE_TEMPLATES_PATH = listOf("freemarker-core", "src", "main", "resource-templates")
private val VERSION_PROPERTIES_PATH = listOf("freemarker", "version.properties")

class FreemarkerVersionDef(versionFileTokens: Map<String, String>, versionProperties: Map<String, String>) {
    val versionFileTokens: Map<String, String>
    val versionProperties: Map<String, String>
    val version: String
    val displayVersion: String

    init {
        this.versionFileTokens = versionFileTokens.toMap()
        this.versionProperties = versionProperties.toMap()
        this.version = this.versionProperties["mavenVersion"]!!
        this.displayVersion = this.versionProperties["version"]!!
    }
}

private fun withChildPaths(root: Path, children: List<String>): Path {
    return children
            .fold(root) { parent, child -> parent.resolve(child) }
}

open class FreemarkerRootPlugin : Plugin<Project> {
    private class Configurer(
        private val project: Project,
        private val ext: FreemarkerRootExtension
        ) {

        private val tasks = project.tasks
        private val java = project.extensions.getByType<JavaPluginExtension>()
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

            val resourceTemplatesDir = withChildPaths(project.projectDir.toPath(), RESOURCE_TEMPLATES_PATH)

            tasks.named<ProcessResources>(JavaPlugin.PROCESS_RESOURCES_TASK_NAME) {
                with(project.copySpec {
                    from(resourceTemplatesDir)
                    filter<ReplaceTokens>(mapOf("tokens" to ext.versionDef.versionFileTokens))
                })
            }

            tasks.named<Jar>(mainSourceSet.sourcesJarTaskName) {
                from(resourceTemplatesDir)
            }

            tasks.withType<org.nosphere.apache.rat.RatTask>() {
                doLast {
                    println("RAT (${name} task) report was successful: ${reportDir.get().asFile.toPath().resolve("index.html").toUri()}")
                }
            }
        }
    }

    override fun apply(project: Project) {
        project.pluginManager.apply("java-library")
        project.pluginManager.apply("org.nosphere.apache.rat")

        val ext = FreemarkerRootExtension(readVersions(project), project.providers)

        Configurer(project, ext).configure()

        project.afterEvaluate {
            // We are setting this, so the pom.xml will be generated properly
            System.setProperty("line.separator", "\n")
        }
    }

    private fun readVersions(project: Project): FreemarkerVersionDef {
        val developmentBuild = project.providers
                .gradleProperty("developmentBuild")
                .map { it.toBoolean() }
                .getOrElse(false)

        if (developmentBuild) {
            println("DEVELOPMENT BUILD: Using EPOCH as timestamp.")
        }

        val buildTimeStamp = if (developmentBuild) Instant.EPOCH else Instant.now()

        val buildTimeStampUtc = buildTimeStamp.atOffset(ZoneOffset.UTC)
        val versionFileTokens = mapOf(
            "timestampNice" to buildTimeStampUtc.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")),
            "timestampInVersion" to buildTimeStampUtc.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
        )

        val versionPropertiesPath = withChildPaths(project.projectDir.toPath(), RESOURCE_TEMPLATES_PATH + VERSION_PROPERTIES_PATH)

        val versionPropertiesTemplate = Properties()
        Files.newBufferedReader(versionPropertiesPath, StandardCharsets.ISO_8859_1).use {
            versionPropertiesTemplate.load(it)
        }

        val versionProperties = HashMap<String, String>()
        versionPropertiesTemplate.forEach { (key, value) ->
            var updatedValue = value.toString()
            for (token in versionFileTokens) {
                updatedValue = updatedValue.replace("@${token.key}@", token.value)
            }
            versionProperties[key.toString()] = updatedValue.trim()
        }
        return FreemarkerVersionDef(versionFileTokens, versionProperties)
    }
}
