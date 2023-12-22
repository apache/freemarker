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

import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing
import com.google.common.io.Files
import java.io.File
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property

open class ChecksumFileTask @Inject constructor(
    layout: ProjectLayout,
    objects: ObjectFactory
): DefaultTask() {

    enum class Algorithm(
        internal val hashFunction: HashFunction,
        private val extension: String) {

        SHA256(Hashing.sha256(), "sha256"),
        SHA384(Hashing.sha384(), "sha384"),
        SHA512(Hashing.sha512(), "sha512");

        internal fun getHashFile(file: File): File {
            val path = file.toPath()
            return path.resolveSibling("${path.fileName}.$extension").toFile()
        }
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    val inputFile = objects.fileProperty()

    @Input
    val algorithm = objects.property<Algorithm>().value(Algorithm.SHA512)

    @OutputFile
    val outputHashFile = objects.fileProperty().value(inputFile
        .zip(algorithm) { inputFileValue, algValue -> algValue.getHashFile(inputFileValue.asFile) }
        .let { layout.file(it) }
    )

    @TaskAction
    fun createHash() {
        val hash = Files.asByteSource(inputFile.get().asFile)
            .hash(algorithm.get().hashFunction)

        Files.write(
            hash.toString().toByteArray(StandardCharsets.US_ASCII),
            outputHashFile.get().asFile
        )
    }
}
