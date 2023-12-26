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
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.setProperty
import org.gradle.kotlin.dsl.the
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
    val sourceSetName: String,
    val compilerVersion: JavaLanguageVersion
) {
    val main = sourceSetName == SourceSet.MAIN_SOURCE_SET_NAME
    val baseDirName = if (main) "core" else sourceSetName.camelCaseToDashed()

    val sourceSet = context.sourceSets.maybeCreate(sourceSetName)

    val sourceSetRootDirName = "freemarker-${baseDirName}"
    val sourceSetSrcPath = "${sourceSetRootDirName}/src"

    fun enableTests(testJavaVersion: String = ext.testJavaVersion) {
        configureTests(JavaLanguageVersion.of(testJavaVersion))
    }

    private fun configureTests(testJavaVersion: JavaLanguageVersion) {
        getOrCreateTestSuiteRef().configure {
            useJUnit(context.version("junit"))

            configureSources(sources, testJavaVersion)
            targets.all { configureTarget(this, sources, testJavaVersion) }
        }
    }

    private fun getOrCreateTestSuiteRef(): NamedDomainObjectProvider<JvmTestSuite> {
        val suites = context.testing.suites
        if (main) {
            return suites.named<JvmTestSuite>(JvmTestSuitePlugin.DEFAULT_TEST_SUITE_NAME)
        } else {
            return suites.register("${sourceSetName}Test", JvmTestSuite::class.java)
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
            val testSrcPath = "${sourceSetSrcPath}/test"
            java.setSrcDirs(listOf("${testSrcPath}/java"))
            resources.setSrcDirs(listOf("${testSrcPath}/resources"))

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
        if (testUtilsConfigured.compareAndSet(false, true)) {
            configureTestUtils()
        }

        allConfiguredSourceSetNamesRef.add(sourceSetName)

        FreemarkerModuleDef(context, this, sourceSetName, JavaLanguageVersion.of(sourceSetVersion)).apply {
            val sourceSetSrcMainPath = "${sourceSetSrcPath}/main"

            sourceSet.apply {
                java.setSrcDirs(listOf("${sourceSetSrcMainPath}/java"))
                resources.setSrcDirs(listOf("${sourceSetSrcMainPath}/resources"))
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
