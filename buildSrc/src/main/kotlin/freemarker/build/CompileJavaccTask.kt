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
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.TreeSet
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.EmptyFileVisitor
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import org.gradle.kotlin.dsl.submit
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor

private const val JAVACC_MAIN_CLASS = "org.javacc.parser.Main"

data class JavaccReplacePattern(val relPath: String, val pattern: String, val replacement: String) : java.io.Serializable

open class CompileJavaccTask @Inject constructor(
    private val fs: FileSystemOperations,
    private val execOps: ExecOperations,
    private val exec: WorkerExecutor,
    layout: ProjectLayout,
    objects: ObjectFactory
) : DefaultTask() {

    @InputDirectory
    @SkipWhenEmpty
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    val sourceDirectory = objects.directoryProperty().value(layout.projectDirectory.dir("src/main/javacc"))

    @Input
    val javaccVersion = objects.property<String>().value("7.0.12")

    private val javaccClasspath = objects.fileCollection().apply {
        val dependencies = project.dependencies
        val configurations = project.configurations

        from(javaccVersion.map { versionValue ->
            dependencies
                .create("net.java.dev.javacc:javacc:${versionValue}")
                .let { configurations.detachedConfiguration(it) }
        })
    }

    @Input
    val fileNameOverrides = objects.setProperty<String>()

    @Input
    val replacePatterns = objects.setProperty<JavaccReplacePattern>()

    @OutputDirectory
    val destinationDirectory = objects.directoryProperty().value(layout.buildDirectory.dir("generated/javacc"))

    fun replacePattern(relPath: String, pattern: String, replacement: String) {
        replacePatterns.add(JavaccReplacePattern(relPath, pattern, replacement))
    }

    private fun hasJavaccOnClasspath(): Boolean {
        try {
            Class.forName(JAVACC_MAIN_CLASS)
            return true
        } catch (ex: ClassNotFoundException) {
            return false
        }
    }

    @TaskAction
    fun compileFiles() {
        fs.delete { delete(destinationDirectory) }

        val destRoot = destinationDirectory.get().asFile

        compileFiles(destRoot)
        deleteOverriddenFiles(destRoot)
        updateFiles(destRoot)
    }

    private fun compileFiles(destRoot: File) {
        withJavaccRunner {
            sourceDirectory.asFileTree.visit(object : EmptyFileVisitor() {
                override fun visitFile(fileDetails: FileVisitDetails) {
                    val outputDir = fileDetails.relativePath.parent.getFile(destRoot)
                    Files.createDirectories(outputDir.toPath())

                    runJavacc(listOf(
                        "-OUTPUT_DIRECTORY=$outputDir",
                        fileDetails.file.toString()
                    ))
                }
            })
        }
    }

    private fun deleteOverriddenFiles(destRoot: File) {
        val fileNameOverridesSnapshot = fileNameOverrides.get()
        val deletedFileNames = HashSet<String>()

        Files.walkFileTree(destRoot.toPath(), object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val fileName = file.fileName.toString()
                if (fileNameOverridesSnapshot.contains(fileName)) {
                    deletedFileNames.add(fileName)
                    Files.delete(file)
                }
                return FileVisitResult.CONTINUE
            }
        })

        val unusedFileNames = TreeSet(fileNameOverridesSnapshot)
        unusedFileNames.removeAll(deletedFileNames)
        if (unusedFileNames.isNotEmpty()) {
            logger.warn("Javacc did not generate the following files," +
                    " even though they are marked as overridden: $unusedFileNames")
        }
    }

    private fun updateFiles(destRoot: File) {
        replacePatterns.get().groupBy { it.relPath }.forEach { (relPath, patternDefs) ->
            val file = destRoot.toPath().resolve(relPath.replace("/", File.separator))

            val encoding = StandardCharsets.ISO_8859_1
            val origContent = String(Files.readAllBytes(file), encoding)
            var adjContent = origContent
            for (patternDef in patternDefs) {
                val prevContent = adjContent
                adjContent = adjContent.replace(patternDef.pattern, patternDef.replacement)
                if (prevContent == adjContent) {
                    logger.warn("$file was not modified, because it does not contain the requested token: '${patternDef.pattern}'")
                }
            }

            if (origContent != adjContent) {
                Files.write(file, adjContent.toByteArray(encoding))
            }
        }
    }

    private fun withJavaccRunner(action: JavaccRunner.() -> Unit) {
        if (hasJavaccOnClasspath()) {
            logger.warn("Found Javacc (${JAVACC_MAIN_CLASS}) on classpath. Switching to process isolation," +
                    " because Javacc relies on static fields. Consider removing Javacc from the classpath" +
                    " to improve performance."
            )
            withProcessJavaccRunner(action)
        } else {
            withClasspathJavaccRunner(action)
        }
    }

    private fun withProcessJavaccRunner(action: JavaccRunner.() -> Unit) {
        action.invoke { actionArgs ->
            val execResult = execOps.javaexec {
                classpath = javaccClasspath
                mainClass.set(JAVACC_MAIN_CLASS)
                args = actionArgs
                isIgnoreExitValue = true
            }

            checkJavaccError(execResult.exitValue)
        }
    }

    private fun withClasspathJavaccRunner(action: JavaccRunner.() -> Unit) {
        val workQueue = exec.classLoaderIsolation { classpath.from(javaccClasspath) }
        action.invoke { actionArgs ->
            workQueue.submit(JavaccRunnerWorkAction::class) { arguments.set(actionArgs) }
        }
        workQueue.await()
    }

    private fun interface JavaccRunner {
        fun runJavacc(args: List<String>)
    }

    interface JavaccCommandLineParameters : WorkParameters {
        val arguments: ListProperty<String>
    }

    abstract class JavaccRunnerWorkAction @Inject constructor() : WorkAction<JavaccCommandLineParameters> {
        override fun execute() {
            Class.forName(JAVACC_MAIN_CLASS)
                .getMethod("mainProgram", Array<String>::class.java)
                .invoke(null, parameters.arguments.get().toTypedArray())
                .let { checkJavaccError(it as Int) }
        }
    }
}

private fun checkJavaccError(errorCode: Int) {
    if (errorCode != 0) {
        throw IllegalStateException("Javacc failed with error code: $errorCode")
    }
}
