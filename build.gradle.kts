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

import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Properties
import java.util.TreeSet
import java.util.stream.Collectors

buildscript {
    dependencies {
        classpath("org.apache.freemarker.docgen:freemarker-docgen-core:0.0.2-SNAPSHOT")
    }
}

plugins {
    `java-library`
    `maven-publish`
    signing
    id("biz.aQute.bnd.builder") version "6.1.0"
    id("org.nosphere.apache.rat") version "0.7.0"
}

group = "org.freemarker"

val buildTimeStamp = Instant.now()
val buildTimeStampUtc = buildTimeStamp.atOffset(ZoneOffset.UTC)
val versionFileTokens = mapOf(
        "timestampNice" to buildTimeStampUtc.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")),
        "timestampInVersion" to buildTimeStampUtc.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
)
val resourceTemplatesDir = file("freemarker-core/src/main/resource-templates")
val versionPropertiesTemplate = Properties()
Files.newBufferedReader(resourceTemplatesDir.toPath().resolve("freemarker").resolve("version.properties"), StandardCharsets.ISO_8859_1).use {
    versionPropertiesTemplate.load(it)
}
val versionProperties = HashMap<String, String>()
versionPropertiesTemplate.forEach { (key, value) ->
    var updatedValue = value.toString()
    for (token in versionFileTokens) {
        updatedValue = updatedValue.replace("@${token.key}@", token.value)
    }
    versionProperties[key.toString()] = updatedValue.trim()
}
version = versionProperties["mavenVersion"]!!
val displayVersion = versionProperties["version"]!!

val freemarkerCompilerVersionOverrideRef = providers.gradleProperty("freemarkerCompilerVersionOverride")
val defaultJavaVersion = freemarkerCompilerVersionOverrideRef
    .orElse(providers.gradleProperty("freemarkerDefaultJavaVersion"))
    .getOrElse("8")
val testJavaVersion = providers.gradleProperty("freeMarkerTestJavaVersion")
    .getOrElse("16")
val testRunnerJavaVersion = providers.gradleProperty("freeMarkerTestRunnerJavaVersion")
    .getOrElse(testJavaVersion)
val javadocJavaVersion = providers.gradleProperty("freeMarkerJavadocJavaVersion")
    .getOrElse(defaultJavaVersion)
val doSignPackages = providers.gradleProperty("signPublication").map { it.toBoolean() }.orElse(true)

java {
    withSourcesJar()
    withJavadocJar()

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(defaultJavaVersion))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

data class JavaccReplacePattern(val relPath: String, val pattern: String, val replacement: String) : java.io.Serializable

open class CompileJavacc @Inject constructor(
    private val fs: FileSystemOperations,
    private val exec: ExecOperations,
    layout: ProjectLayout,
    objects: ObjectFactory
) : DefaultTask() {

    @InputDirectory
    val sourceDirectory: DirectoryProperty

    @OutputDirectory
    val destinationDirectory: DirectoryProperty

    @InputFiles
    var classpath: FileCollection

    @Input
    val fileNameOverrides: SetProperty<String>

    @Input
    val replacePatterns: SetProperty<JavaccReplacePattern>

    init {
        this.sourceDirectory = objects.directoryProperty().value(layout.projectDirectory.dir("src/main/javacc"))
        this.destinationDirectory = objects.directoryProperty().value(layout.buildDirectory.dir("generated/javacc-tmp"))
        this.classpath = objects.fileCollection()
        this.fileNameOverrides = objects.setProperty()
        this.replacePatterns = objects.setProperty()
    }

    @TaskAction
    fun compileFiles() {
        fs.delete { delete(destinationDirectory) }

        val destRoot = destinationDirectory.get().asFile
        val javaccClasspath = classpath

        sourceDirectory.asFileTree.visit(object : EmptyFileVisitor() {
            override fun visitFile(fileDetails: FileVisitDetails) {
                val outputDir = fileDetails.relativePath.parent.getFile(destRoot)
                Files.createDirectories(outputDir.toPath())

                val execResult = exec.javaexec {
                    classpath = javaccClasspath
                    mainClass.set("org.javacc.parser.Main")
                    args = listOf(
                        "-OUTPUT_DIRECTORY=$outputDir",
                        fileDetails.file.toString()
                    )
                    isIgnoreExitValue = true
                }

                val exitCode = execResult.exitValue
                if (exitCode != 0) {
                    throw IllegalStateException("Javacc failed with error code: $exitCode")
                }
            }
        })

        val fileNameOverridesSnapshot = fileNameOverrides.get()
        val deletedFileNames = HashSet<String>()

        Files.walkFileTree(destRoot.toPath(), object : SimpleFileVisitor<java.nio.file.Path>() {
            override fun visitFile(file: java.nio.file.Path, attrs: BasicFileAttributes): FileVisitResult {
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
            logger.warn("Javacc did not generate the following files, even though they are marked as overriden: ${unusedFileNames}")
        }

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
}

val compileJavacc = tasks.register<CompileJavacc>("compileJavacc") {
    sourceDirectory.set(file("freemarker-core/src/main/javacc"))
    destinationDirectory.set(buildDir.toPath().resolve("generated").resolve("javacc").toFile())
    classpath = configurations.detachedConfiguration(dependencies.create("net.java.dev.javacc:javacc:7.0.12"))
    fileNameOverrides.addAll(
        "ParseException.java",
        "TokenMgrError.java"
    )

    val basePath = "freemarker/core"
    replacePatterns.addAll(
        JavaccReplacePattern(
            "${basePath}/FMParser.java",
            "enum",
            "ENUM"
        ),
        JavaccReplacePattern(
            "${basePath}/FMParserConstants.java",
            "public interface FMParserConstants",
            "interface FMParserConstants"
        ),
        JavaccReplacePattern(
            "${basePath}/Token.java",
            "public class Token",
            "class Token"
        ),
        // FIXME: This does nothing at the moment.
        JavaccReplacePattern(
            "${basePath}/SimpleCharStream.java",
            "public final class SimpleCharStream",
            "final class SimpleCharStream"
        )
    )
}

fun <T> concatLists(vararg lists: List<T>): List<T> {
    val concatenated = ArrayList<T>()
    for (list in lists) {
        concatenated.addAll(list)
    }
    return concatenated
}

val allSourceSetNames = ArrayList<String>()

fun configureSourceSet(sourceSetName: String, defaultCompilerVersionStr: String) {
    allSourceSetNames.add(sourceSetName)

    val compilerVersion = freemarkerCompilerVersionOverrideRef
        .orElse(providers.gradleProperty("java${defaultCompilerVersionStr}CompilerOverride"))
        .getOrElse(defaultCompilerVersionStr)
        .let { JavaLanguageVersion.of(it) }

    val baseDirName = if (sourceSetName == SourceSet.MAIN_SOURCE_SET_NAME) "core" else sourceSetName

    val sourceSet = sourceSets.maybeCreate(sourceSetName)
    sourceSet.java.setSrcDirs(listOf("freemarker-${baseDirName}/src/main/java"))
    sourceSet.resources.setSrcDirs(listOf("freemarker-${baseDirName}/src/main/resources"))

    val mainSourceSet = sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).get()

    if (sourceSetName == SourceSet.MAIN_SOURCE_SET_NAME) {
        sourceSet.java.srcDir(compileJavacc)
    } else {
        tasks.named<Jar>(mainSourceSet.sourcesJarTaskName) {
            from(sourceSet.allSource)
        }

        val compileOnlyConfigName = "${sourceSetName}${JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME.capitalize()}"

        configurations.getByName(compileOnlyConfigName) {
            extendsFrom(configurations.named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).get())
        }

        tasks.named<Jar>(JavaPlugin.JAR_TASK_NAME) {
            from(sourceSet.output)
        }

        tasks.named<Javadoc>(JavaPlugin.JAVADOC_TASK_NAME) {
            source(sourceSet.java)
        }

        dependencies {
            add(compileOnlyConfigName, mainSourceSet.output)
            testImplementation(sourceSet.output)
        }
    }

    if (compilerVersion != java.toolchain.languageVersion.get()) {
        tasks.named<JavaCompile>(sourceSet.compileJavaTaskName) {
            javaCompiler.set(javaToolchains.compilerFor {
                languageVersion.set(compilerVersion)
            })
        }
    }
}

configureSourceSet(SourceSet.MAIN_SOURCE_SET_NAME, "8")
configureSourceSet("jsp20", "8")
configureSourceSet("jsp21", "8")
configureSourceSet("jython20", "8")
configureSourceSet("jython22", "8")
configureSourceSet("jython25", "8")
configureSourceSet("core16", "16")

sourceSets {
    test {
        val baseDir = "freemarker-test/src/test"
        java.setSrcDirs(listOf("${baseDir}/java"))
        resources.setSrcDirs(listOf("${baseDir}/resources"))
    }
}

tasks.named<JavaCompile>(sourceSets["test"].compileJavaTaskName) {
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(testJavaVersion))
    })
}

tasks.named<Javadoc>(JavaPlugin.JAVADOC_TASK_NAME) {
    val coreExcludes = setOf(
            "LocalContext.java",
            "CollectionAndSequence.java",
            "Comment.java",
            "DebugBreak.java",
            "Expression.java",
            "LibraryLoad.java",
            "Macro.java",
            "ReturnInstruction.java",
            "StringArraySequence.java",
            "TemplateElement.java",
            "TemplateObject.java",
            "TextBlock.java",
            "ReturnInstruction.java",
            "TokenMgrError.java"
    )

    val logExcludes = setOf(
            "SLF4JLoggerFactory.java",
            "CommonsLoggingLoggerFactory.java"
    )

    val compileJavaccDestDir = compileJavacc.get().destinationDirectory.get().asFile
    setSource(source.filter { f ->
        val fileName = f.name
        val parentDirName = f.parentFile?.name
        !(f.startsWith(compileJavaccDestDir) ||
                fileName.startsWith("_") && fileName.endsWith(".java") ||
                fileName == "SunInternalXalanXPathSupport.java" ||
                parentDirName == "core" && coreExcludes.contains(fileName) ||
                parentDirName == "template" && fileName == "EmptyMap.java" ||
                parentDirName == "log" && logExcludes.contains(fileName)
                )
    })
}

tasks.named<Test>(JavaPlugin.TEST_TASK_NAME) {
    val processResourcesName = sourceSets[SourceSet.TEST_SOURCE_SET_NAME].processResourcesTaskName
    val resourcesDestDir = tasks.named<ProcessResources>(processResourcesName).get().destinationDir.toString()
    systemProperty("freemarker.test.resourcesDir", resourcesDestDir)

    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(testRunnerJavaVersion))
    })
}

configurations {
    compileOnly {
        exclude(group = "xml-apis", module = "xml-apis")
    }
    testImplementation {
        // Excluding to avoid test failures due to SAX parser conflict.
        exclude(group = "pull-parser", module = "pull-parser")
    }

    register("combinedClasspath") {
        extendsFrom(named("jython25CompileOnly").get())
        extendsFrom(named("jsp21CompileOnly").get())
    }

    testImplementation {
        extendsFrom(named("jython25CompileOnly").get())
    }
}

tasks.named<Jar>(sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).get().sourcesJarTaskName) {
    from(compileJavacc.flatMap { it.sourceDirectory })
    from(resourceTemplatesDir)

    from(files("LICENSE", "NOTICE")) {
        into("META-INF")
    }
}


tasks.named<ProcessResources>("processResources") {
    with(copySpec {
        from(resourceTemplatesDir)
        filter<org.apache.tools.ant.filters.ReplaceTokens>(mapOf("tokens" to versionFileTokens))
    })
}

val rmicOutputDir: java.nio.file.Path = buildDir.toPath().resolve("rmic")

tasks.register("rmic") {
    val rmicClasspath = objects.fileCollection()

    val sourceSet = sourceSets[SourceSet.MAIN_SOURCE_SET_NAME]
    val compileTaskName = sourceSet.compileJavaTaskName
    val compileTaskRef = tasks.named<JavaCompile>(compileTaskName)

    rmicClasspath.from(compileTaskRef.map { it.classpath })
    rmicClasspath.from(compileTaskRef.map { it.outputs })

    inputs.files(rmicClasspath)
    outputs.dir(rmicOutputDir)

    doLast {
        val allClassesDirs = sourceSet.output.classesDirs

        val rmicRelSrcPath = listOf("freemarker", "debug", "impl")
        val rmicSrcPattern = "Rmi*Impl.class"

        val rmicRelSrcPathStr = rmicRelSrcPath.joinToString(separator = File.separator)
        val classesDir = allClassesDirs.find { candidateDir ->
            Files.newDirectoryStream(candidateDir.toPath().resolve(rmicRelSrcPathStr), rmicSrcPattern).use { files ->
                val firstFile = files.first { Files.isRegularFile(it) }
                firstFile != null
            }
        }

        if (classesDir != null) {
            ant.withGroovyBuilder {
                "rmic"(
                    "classpath" to rmicClasspath.asPath,
                    "base" to classesDir.toString(),
                    "destDir" to rmicOutputDir.toString(),
                    "includes" to "${rmicRelSrcPath.joinToString("/")}/$rmicSrcPattern",
                    "verify" to "yes",
                    "stubversion" to "1.2"
                )
            }
        } else {
            throw IllegalStateException("Couldn't find classes dir in ${allClassesDirs.asPath}")
        }
    }
}

// This source set is only needed, because the OSGI plugin supports only a single sourceSet.
// We are deleting it, because otherwise it would fool IDEs that a source root has multiple owners.
val osgiSourceSet = sourceSets.create("osgi") {
    val otherSourceSets = allSourceSetNames.map { name -> sourceSets.named(name).get() }

    java {
        setSrcDirs(otherSourceSets.flatMap { s -> s.java.srcDirs })
    }
    resources {
        setSrcDirs(otherSourceSets.flatMap { s -> s.resources.srcDirs })
    }
}
val osgiClasspath = configurations.named("combinedClasspath").get()
osgiSourceSet.compileClasspath = osgiClasspath
osgiSourceSet.runtimeClasspath = osgiClasspath
sourceSets.remove(osgiSourceSet)

tasks.named<Jar>(JavaPlugin.JAR_TASK_NAME) {
    from(rmicOutputDir)

    configure<aQute.bnd.gradle.BundleTaskExtension> {
        bndfile.set(file("osgi.bnd"))

        setSourceSet(osgiSourceSet)
        properties.putAll(versionProperties)
        properties.put("moduleOrg", project.group.toString())
        properties.put("moduleName", project.name)
    }
}

tasks.named<Javadoc>(JavaPlugin.JAVADOC_TASK_NAME) {
    javadocTool.set(javaToolchains.javadocToolFor {
        languageVersion.set(JavaLanguageVersion.of(javadocJavaVersion))
    })

    val javadocEncoding = StandardCharsets.UTF_8

    options {
        locale = "en_US"
        encoding = javadocEncoding.name()
        windowTitle = "FreeMarker ${displayVersion} API"

        val extraOptions = this as StandardJavadocDocletOptions
        extraOptions.links("https://docs.oracle.com/en/java/javase/16/docs/api/")

        extraOptions.author(true)
        extraOptions.version(true)
        extraOptions.docEncoding = javadocEncoding.name()
        extraOptions.charSet = javadocEncoding.name()
        extraOptions.docTitle = "FreeMarker ${displayVersion}"

        // There are too many to check
        extraOptions.addStringOption("Xdoclint:-missing", "-quiet")
    }

    classpath = files(configurations.named("combinedClasspath"))

    doLast {
        val stylesheetPath = destinationDir!!.toPath().resolve("stylesheet.css")
        logger.info("Fixing JDK 8 ${stylesheetPath}")

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

        var stylesheetContent = String(Files.readAllBytes(stylesheetPath), javadocEncoding)
        for (rule in fixRules) {
            val prevContent = stylesheetContent
            stylesheetContent = stylesheetContent.replace(rule.first, rule.second)

            if (prevContent == stylesheetContent) {
                logger.warn("Javadoc style sheet did not contain anything matching: ${rule.first}")
            }
        }

        Files.write(stylesheetPath, stylesheetContent.toByteArray(javadocEncoding))
    }
}

fun registerManualTask(taskName: String, locale: String, offline: Boolean) {
    val manualTaskRef = tasks.register(taskName) {
        val inputDir = file("freemarker-manual/src/main/docgen/${locale}")
        inputs.dir(inputDir)

        val outputDir = buildDir.toPath().resolve("manual-${if (offline) "offline" else "online"}").resolve(locale)
        outputs.dir(outputDir)

        description = if (offline) "Build the Manual for offline use" else "Build the Manual to be upload to the FreeMarker homepage"

        doLast {
            val transform = org.freemarker.docgen.core.Transform()
            transform.offline = offline
            transform.sourceDirectory = inputDir
            transform.destinationDirectory = outputDir.toFile()

            transform.execute()
        }
    }

    tasks.named(LifecycleBasePlugin.BUILD_TASK_NAME) { dependsOn(manualTaskRef) }
}

registerManualTask("manualOffline", "en_US", true)
registerManualTask("manualOnline", "en_US", false)

afterEvaluate {
    // We are setting this, so the pom.xml will be generated properly
    System.setProperty("line.separator", "\n")
}

publishing {
    repositories {
        maven {
            val snapshot = project.version.toString().endsWith("-SNAPSHOT")
            val defaultDeployUrl = if (snapshot) "https://repository.apache.org/content/repositories/snapshots" else "https://repository.apache.org/service/local/staging/deploy/maven2"
            setUrl(providers.gradleProperty("freemarkerDeployUrl").getOrElse(defaultDeployUrl))
            name = providers.gradleProperty("freemarkerDeployServerId").getOrElse("apache.releases.https")
        }
    }

    publications {
        val mainPublication = create<MavenPublication>("main") {
            from(components.getByName("java"))
            pom {
                withXml {
                    val headerComment = asElement().ownerDocument.createComment("""

                        Licensed to the Apache Software Foundation (ASF) under one
                        or more contributor license agreements.  See the NOTICE file
                        distributed with this work for additional information
                        regarding copyright ownership.  The ASF licenses this file
                        to you under the Apache License, Version 2.0 (the
                        "License"); you may not use this file except in compliance
                        with the License.  You may obtain a copy of the License at
                        
                          http://www.apache.org/licenses/LICENSE-2.0
                        
                        Unless required by applicable law or agreed to in writing,
                        software distributed under the License is distributed on an
                        "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
                        KIND, either express or implied.  See the License for the
                        specific language governing permissions and limitations
                        under the License.

                        """.trimIndent().prependIndent("  ")
                    )
                    asElement().insertBefore(headerComment, asElement().firstChild)
                }

                packaging = "jar"
                name.set("Apache FreeMarker")
                description.set("""

                    Google App Engine compliant variation of FreeMarker.
                    FreeMarker is a "template engine"; a generic tool to generate text output based on templates.
                    """.trimIndent().prependIndent("    ") + "\n  "
                )
                url.set("https://freemarker.apache.org/")

                organization {
                    name.set("Apache Software Foundation")
                    url.set("http://apache.org")
                }

                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }

                scm {
                    connection.set("scm:git:https://git-wip-us.apache.org/repos/asf/freemarker.git")
                    developerConnection.set("scm:git:https://git-wip-us.apache.org/repos/asf/freemarker.git")
                    url.set("https://git-wip-us.apache.org/repos/asf?p=freemarker.git")
                    tag.set("v${project.version}")
                }

                issueManagement {
                    system.set("jira")
                    url.set("https://issues.apache.org/jira/browse/FREEMARKER/")
                }

                mailingLists {
                    mailingList {
                        name.set("FreeMarker developer list")
                        post.set("dev@freemarker.apache.org")
                        subscribe.set("dev-subscribe@freemarker.apache.org")
                        unsubscribe.set("dev-unsubscribe@freemarker.apache.org")
                        archive.set("http://mail-archives.apache.org/mod_mbox/freemarker-dev/")
                    }
                    mailingList {
                        name.set("FreeMarker commit and Jira notifications list")
                        post.set("notifications@freemarker.apache.org")
                        subscribe.set("notifications-subscribe@freemarker.apache.org")
                        unsubscribe.set("notifications-unsubscribe@freemarker.apache.org")
                        archive.set("http://mail-archives.apache.org/mod_mbox/freemarker-notifications/")
                    }
                    mailingList {
                        name.set("FreeMarker management private")
                        post.set("private@freemarker.apache.org")
                    }
                }
            }
        }
        if (doSignPackages.get()) {
            signing.sign(mainPublication)
        }
    }
}

val distArchiveBaseName = "apache-${name}"
val distDir = buildDir.toPath().resolve("distributions")

fun registerSignatureTask(archiveTask: TaskProvider<Tar>) {
    val signTask = tasks.register(archiveTask.name + "Signature") {
        val archiveFileRef = archiveTask.flatMap { task -> task.archiveFile }

        inputs.file(archiveFileRef)
        outputs.file(archiveFileRef.map { f -> File(f.asFile.toString() + ".asc") })

        doLast {
            signing.sign(archiveTask.get().archiveFile.get().asFile)
        }
    }

    tasks.named(LifecycleBasePlugin.BUILD_TASK_NAME) {
        dependsOn(archiveTask)

        if (doSignPackages.get()) {
            dependsOn(signTask)
        }
    }
}

fun registerCommonFiles(tar: Tar) {
    tar.from("README.md") {
        val displayVersion = versionProperties.get("version")!!
        filter { content -> content.replace("{version}", displayVersion) }
    }

    tar.from(files("NOTICE", "RELEASE-NOTES"))
}

val distBin = tasks.register<Tar>("distBin") {
    compression = Compression.GZIP
    archiveBaseName.set(distArchiveBaseName)
    destinationDirectory.set(distDir.toFile())
    archiveAppendix.set("bin")

    registerCommonFiles(this)

    from("src/dist/bin") {
        exclude("rat-excludes")
    }

    val jarFile = tasks.named<Jar>("jar").flatMap { jar -> jar.archiveFile }
    from(jarFile) {
        rename { name -> "freemarker.jar" }
    }

    from(tasks.named("manualOffline")) {
        into("documentation/_html")
    }

    from(tasks.named(JavaPlugin.JAVADOC_TASK_NAME)) {
        into("documentation/_html/api")
    }
}
registerSignatureTask(distBin)

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
registerSignatureTask(distSrc)

fun readExcludeFile(excludeFile: File): List<String> {
    Files.lines(excludeFile.toPath()).use { lines ->
        return lines
            .map { it.trim() }
            .filter { !it.startsWith("#") && !it.isEmpty() }
            .collect(Collectors.toList())
    }
}

tasks.withType<org.nosphere.apache.rat.RatTask>() {
    doLast {
        println("RAT (${name} task) report was successful: ${reportDir.get().asFile.toPath().resolve("index.html").toUri()}")
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

// Choose the Jetty version very carefully, as it should implement the same Servlet API, JSP API, and EL API
// what we declare below, because the same classes will come from Jetty as well. For example, Jetty depends
// on org.mortbay.jasper:apache-el, which contains the javax.el classes, along with non-javax.el classes, so you
// can't even exclude it. Similarly, org.eclipse.jetty:apache-jsp contains the JSP API javax.servlet.jsp classes,
// yet again along with other classes. Anyway, this mess is temporary, as we will migrate to Jakarta, and only
// support that.
val jettyVersion = "9.4.53.v20231009"
val slf4jVersion = "1.6.1"
val springVersion = "2.5.6.SEC03"
val tagLibsVersion = "1.2.5"

dependencies {
    compileOnly("jaxen:jaxen:1.0-FCS")
    compileOnly("saxpath:saxpath:1.0-FCS")
    compileOnly("xalan:xalan:2.7.0")
    compileOnly("jdom:jdom:1.0b8")
    compileOnly("ant:ant:1.6.5") // FIXME: This could be moved to "jython20CompileOnly"
    compileOnly("rhino:js:1.6R1")
    compileOnly("avalon-logkit:avalon-logkit:2.0")
    compileOnly("org.slf4j:slf4j-api:${slf4jVersion}")
    compileOnly("org.slf4j:log4j-over-slf4j:${slf4jVersion}")
    compileOnly("org.slf4j:jcl-over-slf4j:${slf4jVersion}") // FIXME: This seems to be unused
    compileOnly("commons-logging:commons-logging:1.1.1") // FIXME: This seems to be unused
    compileOnly("org.zeroturnaround:javarebel-sdk:1.2.2")
    compileOnly("com.google.code.findbugs:annotations:3.0.0")
    compileOnly("org.dom4j:dom4j:2.1.3")

    "jsp20CompileOnly"("javax.servlet.jsp:jsp-api:2.0")
    "jsp20CompileOnly"("javax.servlet:servlet-api:2.4")

    "jsp21CompileOnly"(sourceSets["jsp20"].output)
    "jsp21CompileOnly"("javax.servlet.jsp:jsp-api:2.1")
    "jsp21CompileOnly"("javax.servlet:servlet-api:2.5")

    "jython20CompileOnly"("jython:jython:2.1")

    "jython22CompileOnly"(sourceSets["jython20"].output)
    "jython22CompileOnly"("org.python:jython:2.2.1")

    "jython25CompileOnly"(sourceSets["jython20"].output)
    "jython25CompileOnly"("org.python:jython:2.5.0")

    testImplementation("junit:junit:4.12")
    testImplementation("org.hamcrest:hamcrest-library:1.3")
    testImplementation("ch.qos.logback:logback-classic:1.1.2")
    testImplementation("commons-io:commons-io:2.7")
    testImplementation("com.google.guava:guava:29.0-jre")
    testImplementation("org.eclipse.jetty:jetty-server:${jettyVersion}")
    testImplementation("org.eclipse.jetty:jetty-webapp:${jettyVersion}")
    testImplementation("org.eclipse.jetty:jetty-util:${jettyVersion}")
    testImplementation("org.eclipse.jetty:apache-jsp:${jettyVersion}")
    // Jetty also contains the servlet-api and jsp-api classes

    testImplementation("displaytag:displaytag:1.2") {
        exclude(group = "com.lowagie", module = "itext")
        // We manage logging centrally:
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "rg.slf4j", module = "jcl104-over-slf4j")
        exclude(group = "log4j", module = "log4j")
    }

    // JSP JSTL (not included in Jetty):
    testImplementation("org.apache.taglibs:taglibs-standard-impl:${tagLibsVersion}")
    testImplementation("org.apache.taglibs:taglibs-standard-spec:${tagLibsVersion}")

    // Override Java 9 incompatible version (coming from displaytag):
    testImplementation("commons-lang:commons-lang:2.6")

    testImplementation("org.springframework:spring-core:${springVersion}") {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    testImplementation("org.springframework:spring-test:${springVersion}") {
        exclude(group = "commons-logging", module = "commons-logging")
    }
}
