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

import javax.inject.Inject
import org.freemarker.docgen.core.Transform
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property

open class ManualBuildTask @Inject constructor(
    layout: ProjectLayout,
    objects: ObjectFactory
) : DefaultTask() {

    @InputDirectory
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.NONE)
    val inputDirectory = objects.directoryProperty()

    @Input
    val offline = objects.property<Boolean>().value(true)

    @Input
    val locale = objects.property<String>().value("unknown")

    @OutputDirectory
    val destinationDirectory = this.offline
        .zip(layout.buildDirectory) { offlineValue, buildDirValue ->
            buildDirValue.asFile.toPath().resolve("manual-${if (offlineValue) "offline" else "online"}")
        }
        .zip(this.locale) { localelessDirValue, localeValue -> localelessDirValue.resolve(localeValue).toFile() }
        .let { objects.directoryProperty().value(layout.dir(it)) }

    @TaskAction
    fun buildManual() {
        val transform = Transform()
        transform.offline = offline.get()
        transform.sourceDirectory = inputDirectory.get().asFile
        transform.destinationDirectory = destinationDirectory.get().asFile

        transform.execute()
    }
}
