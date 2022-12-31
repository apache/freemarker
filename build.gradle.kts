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
    `freemarker-root`
    `java-base`
    signing
}

freemarkerRoot {
    registerPublishedShort("core")
    registerPublishedShort("dom")
    registerPublishedShort("servlet")
    registerPublishedShort("spring")

    registerPublishedShort("converter", javadoc = false)
}

val javadoc = tasks.register(JavaPlugin.JAVADOC_TASK_NAME, Javadoc::class) {
    org.apache.freemarker.build.module.common.configureJavadocDefaults(this)

    title = "FreeMarker ${freemarkerRoot.versionDef.displayVersion} API"
    setDestinationDir(File(buildDir, name))

    val javadocConfig = configurations.named("javadocProjects").get()
    val jarDependencies = javadocConfig
        .getTaskDependencyFromProjectDependency(true, JavaPlugin.JAR_TASK_NAME)

    dependsOn(jarDependencies)

    classpath = javadocConfig

    val getSourceRoots: ((Project) -> Set<File>) = { sourceRootProject ->
        sourceRootProject
            .the<JavaPluginExtension>()
            .sourceSets
            .named(SourceSet.MAIN_SOURCE_SET_NAME)
            .get()
            .allJava
            .srcDirs
    }

    val sourceRoots = freemarkerRoot.javadocProjectPaths
        .map { paths -> paths.flatMap { getSourceRoots(project.project(it)) } }

    source(sourceRoots)
    // Javadoc would fail due to all the package.html files, and only processes java files
    // The default javadoc task of the java plugin does this filtering too.
    source = source.matching { include("**/*.java") }

    javadocTool.set(project.the<JavaToolchainService>().javadocToolFor {
        languageVersion.set(org.apache.freemarker.build.module.common.getDefaultJavaVersion(project))
    })
}

fun registerDistSupportTasks(archiveTask: TaskProvider<Tar>) {
    val signTask = tasks.register<org.apache.freemarker.build.core.SignatureTask>("${archiveTask.name}Signature") {
        inputFile.set(archiveTask.flatMap { task -> task.archiveFile })
    }

    val checksumTask = tasks.register<org.apache.freemarker.build.core.ChecksumFileTask>("${archiveTask.name}Checksum") {
        inputFile.set(signTask.flatMap(org.apache.freemarker.build.core.SignatureTask::inputFile))
    }

    tasks.named(LifecycleBasePlugin.BUILD_TASK_NAME) {
        dependsOn(archiveTask)
        dependsOn(checksumTask)

        if (freemarkerRoot.doSignPackages.get()) {
            dependsOn(signTask)
        }
    }
}

fun registerCommonFiles(tar: Tar) {
    tar.from("README.md") {
        filter { content -> content.replace("{version}", freemarkerRoot.versionDef.displayVersion) }
    }

    tar.from(files("NOTICE", "RELEASE-NOTES"))
}

val distArchiveBaseName = "apache-${name}"
val distDir = buildDir.toPath().resolve("distributions")

val distBin = tasks.register<Tar>("distBin") {
    val manualDependencies = configurations
        .detachedConfiguration(dependencies.create(project(":freemarker-manual")))
        .getTaskDependencyFromProjectDependency(true, "manualOffline")
    val jarDependencies = configurations
        .named("publishedProjects")
        .get()
        .getTaskDependencyFromProjectDependency(true, JavaPlugin.JAR_TASK_NAME)

    dependsOn(manualDependencies)
    dependsOn(jarDependencies)

    compression = Compression.GZIP
    archiveBaseName.set(distArchiveBaseName)
    destinationDirectory.set(distDir.toFile())
    archiveAppendix.set("bin")

    registerCommonFiles(this)

    from("src/dist/bin") {
        exclude("rat-excludes")
    }

    val tar = this

    from(providers.provider { jarDependencies.getDependencies(tar) })

    from(providers.provider { manualDependencies.getDependencies(tar) }) {
        into("documentation/_html")
    }

    from(javadoc) {
        into("documentation/_html/api")
    }
}
registerDistSupportTasks(distBin)

val distSrc = tasks.register<Tar>("distSrc") {
    compression = Compression.GZIP
    archiveBaseName.set(distArchiveBaseName)
    destinationDirectory.set(distDir.toFile())
    archiveAppendix.set("src")

    registerCommonFiles(this)

    from(files("LICENSE"))

    from(projectDir) {
        includeEmptyDirs = false
        include(
                "src/**",
                "*/src/**",
                "*/*/src/**",
                "**/*.kts",
                "*.txt",
                "osgi.bnd",
                "rat-excludes"
        )
        exclude(
                "/build",
                "/*/build",
                "/gradle/wrapper",
                "/gradlew*",
                "**/*.bak",
                "**/*.~*",
                "*/*.*~"
        )
    }
}
registerDistSupportTasks(distSrc)

fun readExcludeFile(excludeFile: File): List<String> {
    return excludeFile.useLines { lines ->
        lines
            .map { it.trim() }
            .filter { !it.startsWith("#") && !it.isEmpty() }
            .toList()
    }
}

tasks.named<org.nosphere.apache.rat.RatTask>("rat") {
    inputDir.set(projectDir)
    excludes.addAll(readExcludeFile(file("rat-excludes")))
}

fun registerDistRatTask(taskName: String, excludeFile: File, srcArchiveTaskRef: TaskProvider<Tar>) {
    val inputTaskName = "${taskName}Prep"
    val ratInputTask = tasks.register<Sync>(inputTaskName) {
        dependsOn(srcArchiveTaskRef)

        destinationDir = buildDir.toPath().resolve("rat-prep").resolve(taskName).toFile()
        from(tarTree(srcArchiveTaskRef.flatMap { it.archiveFile }))
    }

    val ratTask = tasks.register<org.nosphere.apache.rat.RatTask>(taskName) {
        dependsOn(ratInputTask)

        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "RAT report for the output of ${srcArchiveTaskRef.name}"
        inputDir.set(layout.dir(ratInputTask.map { it.destinationDir }))
        excludes.addAll(readExcludeFile(excludeFile))
    }
    tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
        dependsOn(ratTask)
    }
}

registerDistRatTask("ratDistBin", file("src/dist/bin/rat-excludes"), distBin)
registerDistRatTask("ratDistSrc", file("rat-excludes"), distSrc)
