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
import org.gradle.api.file.EmptyFileVisitor
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.setProperty

open class JakartaSourceRootGeneratorTask @Inject constructor(
    private val fs: FileSystemOperations,
    objects: ObjectFactory
) : DefaultTask() {

    @InputDirectory
    @SkipWhenEmpty
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    val sourceDirectory = objects.directoryProperty()

    @Input
    val packageMappings = objects.mapProperty<String, String>()

    @Input
    val fileNameMappings = objects.mapProperty<String, String>()

    @Input
    val noAutoReplacePackages = objects.setProperty<String>().value(setOf())

    @Input
    val replacements = objects.mapProperty<String, String>()

    @OutputDirectory
    val destinationDirectory = objects.directoryProperty()

    private fun toNewPath(oldPath: List<String>, origToNewPackage: Map<String, String>): List<String> {
        for (oldPackageEndIndex in oldPath.size downTo 1) {
            val oldPackageName = oldPath.subList(0, oldPackageEndIndex).joinToString(".")
            val newPackageName = origToNewPackage[oldPackageName]
            if (newPackageName != null) {
                return newPackageName.split('.') + oldPath.subList(oldPackageEndIndex, oldPath.size)
            }
        }
        return oldPath
    }

    private fun toPackagePath(packageName: String) = packageName.replace('.', '/')

    private fun allReplacements(origToNewPackage: Map<String, String>): Map<String, String> {
        val allReplacements = LinkedHashMap(origToNewPackage)
        val skippedPackageReplacements = noAutoReplacePackages.get()
        origToNewPackage.forEach { (origPackage, newPackage) ->
            if (!skippedPackageReplacements.contains(origPackage)) {
                allReplacements[toPackagePath(origPackage)] = toPackagePath(newPackage)
            }
        }
        skippedPackageReplacements.forEach(allReplacements::remove)
        allReplacements.putAll(replacements.get())
        return allReplacements
    }

    @TaskAction
    fun copyFiles() {
        val fileNameMappingsCapture: Map<String, String> = fileNameMappings.get()
        val origToNewPackage: Map<String, String> = packageMappings.get()

        val allReplacements = allReplacements(origToNewPackage)

        val destRoot = destinationDirectory.get().asFile
        fs.delete { delete(destRoot) }

        sourceDirectory.asFileTree.visit(object : EmptyFileVisitor() {
            override fun visitFile(fileDetails: FileVisitDetails) {
                val relPath = fileDetails.relativePath

                val newPackage = toNewPath(relPath.parent.segments.asList(), origToNewPackage)

                val srcPath = fileDetails.file
                var fileContent = srcPath.readText()
                allReplacements.forEach { (key, value) ->
                    fileContent = fileContent.replace(key, value)
                }

                val destName = fileNameMappingsCapture[srcPath.name] ?: srcPath.name
                val destPath = destRoot
                    .resolve(newPackage.joinToString(File.separator))
                    .resolve(destName)

                destPath.parentFile.mkdirs()
                destPath.writeText(applyJakartaPreprocessingBasedOnName(fileContent, destName))
            }
        })
    }

    private fun isJakartaDirective(line: String, directive: String): Boolean {
        if (!line.startsWith(directive)) {
            return false
        }
        return line.substring(directive.length).trim() == "jakarta"
    }

    private fun lineType(line: String, lineComment: LineCommentType): SourceLineType {
        val uncommented = lineComment
            .uncommentIfLineComment(line)
            ?: return SourceLineType.OTHER

        val directiveLine = uncommented.trim()
        if (isJakartaDirective(directiveLine, "#if")) {
            return SourceLineType.IF_START
        }
        if (isJakartaDirective(directiveLine, "#else")) {
            return SourceLineType.ELSE
        }
        if (isJakartaDirective(directiveLine, "#endif")) {
            return SourceLineType.ENDIF
        }
        return SourceLineType.OTHER
    }

    private fun extension(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex < 0) "" else fileName.substring(dotIndex + 1)
    }

    private fun applyJakartaPreprocessingBasedOnName(input: String, fileName: String): String {
        val lineCommentType = when (extension(fileName)) {
            "java" -> LineCommentType.C_LIKE
            "jsp" -> LineCommentType.JSP_LIKE
            "xml", "html", "tld" -> LineCommentType.XML_LIKE
            else -> return input
        }
        return applyJakartaPreprocessing(input, lineCommentType)
    }

    private fun applyJakartaPreprocessing(input: String, lineComment: LineCommentType): String {
        val output = StringBuilder()
        val modified = applyJakartaPreprocessing(input, output, lineComment)
        return if (modified) output.toString() else input
    }

    private fun applyJakartaPreprocessing(
        input: String,
        output: StringBuilder,
        lineComment: LineCommentType
    ): Boolean {
        var modified = false
        var mode = SourceProcessingMode.OTHER

        input.lineSequence().forEach { line ->
            when (lineType(line, lineComment)) {
                SourceLineType.IF_START -> {
                    if (mode != SourceProcessingMode.OTHER) {
                        throw IllegalStateException("Nested #if is not supported")
                    }
                    mode = SourceProcessingMode.JAKARTA_BLOCK
                }
                SourceLineType.ELSE -> {
                    if (mode != SourceProcessingMode.JAKARTA_BLOCK) {
                        throw IllegalStateException("Unexpected #else")
                    }
                    mode = SourceProcessingMode.NON_JAKARTA_BLOCK
                }
                SourceLineType.ENDIF -> {
                    if (mode == SourceProcessingMode.OTHER) {
                        throw IllegalStateException("Unexpected #endif")
                    }
                    mode = SourceProcessingMode.OTHER
                }
                SourceLineType.OTHER -> {
                    when (mode) {
                        SourceProcessingMode.JAKARTA_BLOCK -> {
                            modified = true
                            output.append(lineComment.uncomment(line))
                            output.append('\n')
                        }
                        SourceProcessingMode.NON_JAKARTA_BLOCK -> {
                            modified = true
                        }
                        SourceProcessingMode.OTHER -> {
                            output.append(line)
                            output.append('\n')
                        }
                    }
                }
            }
        }
        if (mode != SourceProcessingMode.OTHER) {
            throw IllegalStateException("Unterminated #if")
        }
        return modified
    }

    private enum class SourceProcessingMode {
        JAKARTA_BLOCK, NON_JAKARTA_BLOCK, OTHER
    }

    private enum class SourceLineType {
        IF_START, ELSE, ENDIF, OTHER
    }

    private enum class LineCommentType {
        C_LIKE {
            override fun uncommentIfLineComment(line: String): String? =
                uncommentIfLineComment(line, "//")
        },
        XML_LIKE {
            override fun uncommentIfLineComment(line: String): String? =
                uncommentIfLineComment(line, "<!--", "-->")
        },
        JSP_LIKE {
            override fun uncommentIfLineComment(line: String): String? =
                uncommentIfLineComment(line, "<%--", "--%>")
        };

        protected fun uncommentIfLineComment(line: String, commentOpen: String): String? {
            val commentIndex = line.indexOf(commentOpen)
            if (commentIndex < 0) {
                return null
            }

            val preCommentLine = line.substring(0, commentIndex)
            if (preCommentLine.trim().isNotEmpty()) {
                return null
            }
            return preCommentLine + line.substring(commentIndex + commentOpen.length)
        }

        protected fun uncommentIfLineComment(line: String, commentOpen: String, commentClose: String): String? {
            val noOpenLine = uncommentIfLineComment(line, commentOpen)
                ?: return null

            val commentCloseIndex = noOpenLine.lastIndexOf(commentClose)
            if (commentCloseIndex < 0) {
                return null
            }
            if (noOpenLine.substring(commentCloseIndex + commentClose.length).trim().isNotEmpty()) {
                return null
            }
            return noOpenLine.substring(0, commentCloseIndex)
        }

        abstract fun uncommentIfLineComment(line: String): String?

        fun uncomment(line: String): String =
            uncommentIfLineComment(line) ?: throw IllegalArgumentException("Not a line comment: $line")
    }
}
