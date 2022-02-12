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

import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.signing.SigningExtension

open class SignatureTask @Inject constructor(
    objects: ObjectFactory
) : DefaultTask() {

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    val inputFile: RegularFileProperty

    @OutputFile
    val outputFile: Provider<File>

    private val signing : SigningExtension

    init {
        this.inputFile = objects.fileProperty()
        this.outputFile = this.inputFile.map { f -> File("${f.asFile}.asc") }
        this.signing = project.extensions.getByType()
    }

    @TaskAction
    fun signFile() {
        signing.sign(inputFile.get().asFile)
    }
}
