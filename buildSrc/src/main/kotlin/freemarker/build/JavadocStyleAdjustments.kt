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

import java.nio.charset.Charset
import java.nio.file.Files
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.tasks.javadoc.Javadoc

class JavadocStyleAdjustments: Action<Task> {
    override fun execute(task: Task) {
        val javadoc = task as Javadoc
        val stylesheetPath = javadoc.destinationDir!!.toPath().resolve("stylesheet.css")
        task.logger.info("Fixing JDK 8 ${stylesheetPath}")

        val ddSelectorStart = "(?:\\.contentContainer\\s+\\.(?:details|description)|\\.serializedFormContainer)\\s+dl\\s+dd\\b.*?\\{[^\\}]*\\b"
        val ddPropertyEnd = "\\b.+?;"

        val fixRules = listOf(
            Pair(Regex("/\\* (Javadoc style sheet) \\*/"), "/\\* \\1 - JDK 8 usability fix regexp substitutions applied \\*/"),
            Pair(Regex("@import url\\('resources/fonts/dejavu.css'\\);\\s*"), ""),
            Pair(Regex("['\"]DejaVu Sans['\"]"), "Arial"),
            Pair(Regex("['\"]DejaVu Sans Mono['\"]"), "'Courier New'"),
            Pair(Regex("['\"]DejaVu Serif['\"]"), "Arial"),
            Pair(Regex("(?<=[\\s,:])serif\\b"), "sans-serif"),
            Pair(Regex("(?<=[\\s,:])Georgia,\\s*"), ""),
            Pair(Regex("['\"]Times New Roman['\"],\\s*"), ""),
            Pair(Regex("(?<=[\\s,:])Times,\\s*"), ""),
            Pair(Regex("(?<=[\\s,:])Arial\\s*,\\s*Arial\\b"), ""),
            Pair(Regex("(${ddSelectorStart})margin${ddPropertyEnd}"), "\\1margin: 5px 0 10px 20px;"),
            Pair(Regex("(${ddSelectorStart})font-family${ddPropertyEnd}"), "\\1")
        )

        val javadocEncoding = Charset.forName(javadoc.options.encoding)

        var stylesheetContent = String(Files.readAllBytes(stylesheetPath), javadocEncoding)
        for (rule in fixRules) {
            val prevContent = stylesheetContent
            stylesheetContent = stylesheetContent.replace(rule.first, rule.second)

            if (prevContent == stylesheetContent) {
                task.logger.warn("Javadoc style sheet did not contain anything matching: ${rule.first}")
            }
        }

        Files.write(stylesheetPath, stylesheetContent.toByteArray(javadocEncoding))
    }
}
