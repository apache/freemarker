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

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.JvmTestSuitePlugin
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.plugins.jvm.JvmTestSuiteTarget
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.testing.base.TestingExtension
import java.util.concurrent.atomic.AtomicBoolean

private const val TEST_UTILS_SOURCE_SET_NAME = "test-utils"

internal class JavaProjectContext constructor(
    val project: Project
) {
    val providers = project.providers
    val tasks = project.tasks
    val libs = project.the<VersionCatalogsExtension>().named("libs")
    val testing = project.the<TestingExtension>()
    val javaToolchains = project.the<JavaToolchainService>()
    val java = project.the<JavaPluginExtension>()
    val sourceSets = java.sourceSets

    val mainSourceSet = sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).get()

    fun version(versionStr: String): String {
        return libs
            .findVersion(versionStr)
            .orElseThrow { NoSuchElementException("Missing version for $versionStr") }
            .requiredVersion
    }

    fun inheritConfig(child: SourceSet, parent: SourceSet, nameProvider: ((SourceSet) -> String)) {
        val configurations = project.configurations
        val childConfigRef = configurations.named(nameProvider.invoke(child))
        childConfigRef.configure {
            extendsFrom(configurations.named(nameProvider.invoke(parent)).get())
        }
    }

    fun inheritCompileRuntime(child: SourceSet, parent: SourceSet) {
        inheritConfig(child, parent, SourceSet::getCompileClasspathConfigurationName)
        inheritConfig(child, parent, SourceSet::getRuntimeClasspathConfigurationName)
    }

    fun inheritCompileRuntimeAndOutput(child: SourceSet, parent: SourceSet) {
        project.dependencies {
            add(child.implementationConfigurationName, parent.output)
        }

        inheritCompileRuntime(child, parent)
    }
}

class FreemarkerModuleDef internal constructor(
    private val context: JavaProjectContext,
    private val ext: FreemarkerRootExtension,
    private val generated: Boolean,
    val sourceSetName: String,
    val compilerVersion: JavaLanguageVersion
) {
    val main = sourceSetName == SourceSet.MAIN_SOURCE_SET_NAME

    val sourceSet = context.sourceSets.maybeCreate(sourceSetName)

    val sourceSetRootDirName = "freemarker-${if (main) "core" else sourceSetName.camelCaseToDashed()}"
    val sourceSetSrcPath = sourceSetRoot(context, generated, sourceSetRootDirName)

    fun generateJakartaSources(
        baseSourceSetName: String,
        sourceSetKind: String = SourceSet.MAIN_SOURCE_SET_NAME,
        targetSourceSet: SourceSet = sourceSet
    ): List<TaskProvider<JakartaSourceRootGeneratorTask>> {
        val baseSourceSetRef = context.sourceSets.named(baseSourceSetName)
        val taskNameClassifier = if (SourceSet.MAIN_SOURCE_SET_NAME == sourceSetKind) {
            ""
        } else {
            sourceSetKind.replaceFirstChar { it.uppercaseChar() }
        }

        val generateJakartaSources = context.tasks
            .register<JakartaSourceRootGeneratorTask>("generateJakarta${taskNameClassifier}Sources") {
                sourceDirectory.set(baseSourceSetRef.get().java.srcDirs.single())
                destinationDirectory.set(project.file(sourceSetSrcPath).resolve(sourceSetKind).resolve("java"))
            }
        targetSourceSet.java.srcDir(generateJakartaSources)

        val generateJakartaResources = context.tasks
            .register<JakartaSourceRootGeneratorTask>("generateJakarta${taskNameClassifier}Resources") {
                sourceDirectory.set(baseSourceSetRef.get().resources.srcDirs.single())
                destinationDirectory.set(project.file(sourceSetSrcPath).resolve(sourceSetKind).resolve("resources"))
            }
        targetSourceSet.resources.srcDir(generateJakartaResources)
        return listOf(generateJakartaSources, generateJakartaResources)
    }

    private fun sourceSetRoot(
        context: JavaProjectContext,
        generated: Boolean,
        sourceSetRootDirName: String
    ): String {
        return if (generated) {
            context.project.layout.buildDirectory.get().asFile
                .resolve("generated")
                .resolve(sourceSetRootDirName)
                .toString()
        } else {
            "${sourceSetRootDirName}/src"
        }
    }

    fun addDependencySourceSet(dependencySourceSetName: String) {
        val dependencySourceSet = context.sourceSets.named(dependencySourceSetName).get();
        context.inheritCompileRuntimeAndOutput(sourceSet, dependencySourceSet)
    }

    fun enableTests(testJavaVersion: String = ext.testJavaVersion) =
        configureTests(JavaLanguageVersion.of(testJavaVersion))

    private fun configureTests(testJavaVersion: JavaLanguageVersion): NamedDomainObjectProvider<JvmTestSuite> {
        val testSuitRef = getOrCreateTestSuiteRef()
        testSuitRef.configure {
            useJUnit(context.version("junit"))

            configureSources(sources, testJavaVersion)
            targets.all { configureTarget(this, sources, testJavaVersion) }
        }
        return testSuitRef
    }

    private fun getOrCreateTestSuiteRef(): NamedDomainObjectProvider<JvmTestSuite> {
        val suites = context.testing.suites
        return if (main) {
            suites.named<JvmTestSuite>(JvmTestSuitePlugin.DEFAULT_TEST_SUITE_NAME)
        } else {
            suites.register("${sourceSetName}Test", JvmTestSuite::class.java)
        }
    }

    private fun testUtils(): SourceSet {
        val testUtilsRef = context.sourceSets.named(TEST_UTILS_SOURCE_SET_NAME)
        if (!testUtilsRef.isPresent) {
            throw IllegalStateException("Forgot to configure the ${TEST_UTILS_SOURCE_SET_NAME} source set." +
                    " Call the configureTestUtils method.")
        }
        return testUtilsRef.get()
    }

    private fun configureSources(sources: SourceSet, testJavaVersion: JavaLanguageVersion) {
        sources.apply {
            if (generated) {
                java.setSrcDirs(emptyList<String>())
                resources.setSrcDirs(emptyList<String>())
            } else {
                val testSrcPath = "${sourceSetSrcPath}/test"
                java.setSrcDirs(listOf("${testSrcPath}/java"))
                resources.setSrcDirs(listOf("${testSrcPath}/resources"))
            }

            if (!main) {
                context.inheritCompileRuntimeAndOutput(this, sourceSet)
            }

            context.inheritCompileRuntimeAndOutput(this, testUtils())

            // Because of the compileOnly hacks on the source sets, we have to add the compilation classpath to runtime.
            val configurations = context.project.configurations
            configurations.named(runtimeClasspathConfigurationName) {
                extendsFrom(configurations.named(compileClasspathConfigurationName).get())
            }

            context.tasks.named<JavaCompile>(compileJavaTaskName) {
                javaCompiler.set(context.javaToolchains.compilerFor {
                    languageVersion.set(testJavaVersion)
                })
            }
        }
    }

    private fun configureTarget(target: JvmTestSuiteTarget, sources: SourceSet, testRunnerJavaVersion: JavaLanguageVersion) {
        target.apply {
            testTask.configure {
                description = "Runs the tests in ${sourceSetRootDirName}."
                val processResourcesName = sources.processResourcesTaskName
                val resourcesDestDir = context.tasks
                    .named<ProcessResources>(processResourcesName)
                    .get()
                    .destinationDir
                    .toString()
                systemProperty("freemarker.test.resourcesDir", resourcesDestDir)

                javaLauncher.set(context.javaToolchains.launcherFor {
                    languageVersion.set(testRunnerJavaVersion)
                })
            }

            context.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { dependsOn(testTask) }
        }
    }
}

class FreemarkerRootExtension constructor(
    project: Project,
    val versionService: FreemarkerVersionService
) {

    private val context = JavaProjectContext(project)

    val versionDef = versionService.versionDef

    val javaVersion = context.providers
        .gradleProperty("freemarker.javaVersion")
        .get()

    val testJavaVersion = context.providers
        .gradleProperty("freemarker.test.javaVersion")
        .get()

    val javadocJavaVersion = context.providers
        .gradleProperty("freemarker.javadoc.javaVersion")
        .get()

    val signMethod = context.providers
        .gradleProperty("freemarker.signMethod")
        .map { SignatureConfiguration.valueOf(it.uppercase()) }
        .get()

    val allowUnsignedReleaseBuild = context.providers
        .gradleProperty("allowUnsignedReleaseBuild")
        .map { it.toBoolean() }
        .getOrElse(false)

    private val allConfiguredSourceSetNamesRef = project.objects.setProperty<String>()
    val allConfiguredSourceSetNames: Provider<Set<String>> = allConfiguredSourceSetNamesRef

    private val tasks = project.tasks
    private val java = project.the<JavaPluginExtension>()
    private val sourceSets = java.sourceSets
    private val testUtilsConfigured = AtomicBoolean(false)

    fun isPublishedVersion(): Boolean {
        return !versionDef.version.endsWith("-SNAPSHOT") ||
                !versionDef.displayVersion.contains("-nightly")
    }

    private fun configureTestUtils() {
        sourceSets.register(TEST_UTILS_SOURCE_SET_NAME) {
            val baseDir = "freemarker-${TEST_UTILS_SOURCE_SET_NAME}/src/main"
            java.setSrcDirs(listOf("${baseDir}/java"))
            resources.setSrcDirs(listOf("${baseDir}/resources"))

            tasks.named<JavaCompile>(compileJavaTaskName) {
                javaCompiler.set(context.javaToolchains.compilerFor {
                    languageVersion.set(JavaLanguageVersion.of(testJavaVersion))
                })
            }
        }
    }

    fun configureGeneratedSourceSet(
        sourceSetName: String,
        configuration: FreemarkerModuleDef.() -> Unit = { }
    ) {
        configureGeneratedSourceSet(sourceSetName, javaVersion, configuration)
    }

    fun configureGeneratedSourceSet(
        sourceSetName: String,
        sourceSetVersion: String,
        configuration: FreemarkerModuleDef.() -> Unit = { }
    ) {
        configureSourceSet(true, sourceSetName, sourceSetVersion, configuration)
    }

    fun configureSourceSet(
        sourceSetName: String,
        configuration: FreemarkerModuleDef.() -> Unit = { }
    ) {
        configureSourceSet(sourceSetName, javaVersion, configuration)
    }

    fun configureSourceSet(
        sourceSetName: String,
        sourceSetVersion: String,
        configuration: FreemarkerModuleDef.() -> Unit = { }
    ) {
        configureSourceSet(false, sourceSetName, sourceSetVersion, configuration)
    }

    private fun configureSourceSet(
        generated: Boolean,
        sourceSetName: String,
        sourceSetVersion: String,
        configuration: FreemarkerModuleDef.() -> Unit = { }
    ) {
        if (testUtilsConfigured.compareAndSet(false, true)) {
            configureTestUtils()
        }

        allConfiguredSourceSetNamesRef.add(sourceSetName)

        FreemarkerModuleDef(context, this, generated, sourceSetName, JavaLanguageVersion.of(sourceSetVersion)).apply {
            sourceSet.apply {
                if (generated) {
                    java.setSrcDirs(emptyList<String>())
                    resources.setSrcDirs(emptyList<String>())
                } else {
                    val sourceSetSrcMainPath = "${sourceSetSrcPath}/main"
                    java.setSrcDirs(listOf("${sourceSetSrcMainPath}/java"))
                    resources.setSrcDirs(listOf("${sourceSetSrcMainPath}/resources"))
                }
            }

            if (!main) {
                context.apply {
                    inheritCompileRuntimeAndOutput(sourceSet, mainSourceSet)

                    tasks.apply {
                        named<Jar>(mainSourceSet.sourcesJarTaskName) { from(sourceSet.allSource) }
                        named<Jar>(JavaPlugin.JAR_TASK_NAME) { from(sourceSet.output) }
                        named<Javadoc>(JavaPlugin.JAVADOC_TASK_NAME) { source(sourceSet.java) }
                    }

                    project.dependencies { add(sourceSet.compileOnlyConfigurationName, mainSourceSet.output) }
                }
            }

            tasks.named<JavaCompile>(sourceSet.compileJavaTaskName) {
                javaCompiler.set(context.javaToolchains.compilerFor {
                    languageVersion.set(compilerVersion)
                })
            }

            configuration.invoke(this)
        }
    }
}
