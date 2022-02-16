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
    `freemarker-published-java`
}

freemarkerJava {
    projectTitle.set("Apache FreeMarker Core")
}

description = "FreeMarker template engine, core module." +
        " This module covers all basic functionality, and is all that's needed for many applications."

dependencies {
    compileOnly(libs.jrebel)

    api(libs.slf4jApi)
}

val compileJavacc = tasks.register<org.apache.freemarker.build.core.CompileJavaccTask>("compileJavacc") {
    sourceDirectory.set(file("src/main/javacc"))
    destinationDirectory.set(buildDir.toPath().resolve("generated").resolve("javacc").toFile())
    javaccVersion.set("6.1.2")

    fileNameOverrides.addAll(
        "ParseException.java",
        "TokenMgrError.java"
    )

    val basePath = "org/apache/freemarker/core"

    replacePattern(
        "${basePath}/FMParser.java",
        "enum",
        "ENUM"
    )
    replacePattern(
        "${basePath}/FMParserConstants.java",
        "public interface FMParserConstants",
        "interface FMParserConstants"
    )
    replacePattern(
        "${basePath}/Token.java",
        "public class Token",
        "class Token"
    )
    replacePattern(
        "${basePath}/SimpleCharStream.java",
        "public class SimpleCharStream",
        "final class SimpleCharStream"
    )
}
sourceSets.main.get().java.srcDir(compileJavacc)

val resourceTemplatesDir = freemarkerJava.resourceTemplatesDir
tasks.named<ProcessResources>(JavaPlugin.PROCESS_RESOURCES_TASK_NAME) {
    with(project.copySpec {
        from(resourceTemplatesDir)
        filter<org.apache.tools.ant.filters.ReplaceTokens>(mapOf("tokens" to freemarkerJava.versionDef.versionFileTokens))
    })
}

val mainSourceSet = sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).get()
tasks.named<Jar>(mainSourceSet.sourcesJarTaskName) {
    from(resourceTemplatesDir)
}
