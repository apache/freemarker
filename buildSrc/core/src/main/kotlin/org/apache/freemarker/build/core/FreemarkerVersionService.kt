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

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Properties
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

const val FREEMARKER_VERSION_SERVICE_NAME = "freemarker-version-service"

private val RESOURCE_TEMPLATES_PATH = listOf("freemarker-core", "src", "main", "resource-templates")
private val VERSION_PROPERTIES_PATH = listOf("org", "apache", "freemarker", "core", "version.properties")

fun Project.getFreemarkerVersionService(): FreemarkerVersionService {
    return gradle.sharedServices
            .registrations
            .named(FREEMARKER_VERSION_SERVICE_NAME)
            .get()
            .service
            .get() as FreemarkerVersionService
}

class FreemarkerVersionDef(versionFileTokens: Map<String, String>, versionProperties: Map<String, String>) {
    val versionFileTokens = versionFileTokens.toMap()
    val versionProperties = versionProperties.toMap()
    val version = this.versionProperties["mavenVersion"]!!
    val displayVersion = this.versionProperties["version"]!!
}

abstract class FreemarkerVersionService : BuildService<FreemarkerVersionService.Parameters> {

    interface Parameters : BuildServiceParameters {
        val rootDir: Property<File>
        val developmentBuild: Property<Boolean>
        val doSignPackage: Property<Boolean>
    }

    val developmentBuild = parameters.developmentBuild.get()

    val doSignPackages = parameters.doSignPackage

    val buildTimeStamp = if (developmentBuild) Instant.EPOCH else Instant.now()

    val resourceTemplatesDir = parameters.rootDir.get()
            .toPath()
            .withChildren(RESOURCE_TEMPLATES_PATH)

    val versionFileTokens = buildTimeStamp.atOffset(ZoneOffset.UTC).let { buildTimeStampUtc ->
        mapOf(
            "timestampNice" to buildTimeStampUtc.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")),
            "timestampInVersion" to buildTimeStampUtc.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
        )
    }

    val versionDef = run {
        val versionPropertiesPath = resourceTemplatesDir.withChildren(VERSION_PROPERTIES_PATH)

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

        FreemarkerVersionDef(versionFileTokens, versionProperties)
    }
}
