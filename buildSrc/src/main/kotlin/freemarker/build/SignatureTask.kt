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
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.signing.SigningExtension

open class SignatureTask @Inject constructor(
    objects: ObjectFactory
) : DefaultTask() {
    @Input
    val signatureConfiguration: Property<SignatureConfiguration> = objects.property()

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    val inputFile: RegularFileProperty = objects.fileProperty()

    @OutputFile
    val outputFile: Provider<File> = this.inputFile.map { f -> File("${f.asFile}.asc") }

    private val signing : SigningExtension = project.the()

    @TaskAction
    fun signFile() {
        val config = signatureConfiguration.get()
        if (config.needSignature()) {
            config.configure(signing)
            signing.sign(inputFile.get().asFile)
        }
    }
}
