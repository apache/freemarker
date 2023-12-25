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
import java.nio.file.Files
import java.util.stream.Collectors

plugins {
    `freemarker-root`
    `maven-publish`
    signing
    id("biz.aQute.bnd.builder") version "6.1.0"
    id("eclipse")
}

group = "org.freemarker"

val fmExt = freemarkerRoot

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

freemarkerRoot {
    configureSourceSet(SourceSet.MAIN_SOURCE_SET_NAME) { enableTests() }
    configureSourceSet("javaxServlet") { enableTests() }
    configureSourceSet("jython20")
    configureSourceSet("jython22")
    configureSourceSet("jython25") { enableTests() }
    configureSourceSet("core16", "16")
}

val compileJavacc = tasks.register<freemarker.build.CompileJavaccTask>("compileJavacc") {
    sourceDirectory.set(file("freemarker-core/src/main/javacc"))
    destinationDirectory.set(project.layout.buildDirectory.map { it.dir("generated").dir("javacc") })
    javaccVersion.set("7.0.12")

    fileNameOverrides.addAll(
        "ParseException.java",
        "TokenMgrError.java"
    )

    val basePath = "freemarker/core"

    replacePattern(
        "${basePath}/FMParser.java",
        "enum",
        "ENUM"
    )
    replacePattern(
        "${basePath}/FMParserConstants.java",
        "public interface FMParserConstants",
        "interface FMParserConstants"
    )
    replacePattern(
        "${basePath}/Token.java",
        "public class Token",
        "class Token"
    )
    // FIXME: This does nothing at the moment.
    replacePattern(
        "${basePath}/SimpleCharStream.java",
        "public final class SimpleCharStream",
        "final class SimpleCharStream"
    )
}
sourceSets.main.get().java.srcDir(compileJavacc)

tasks.sourcesJar.configure {
    from(compileJavacc.flatMap { it.sourceDirectory })

    from(files("LICENSE", "NOTICE")) {
        into("META-INF")
    }
}

tasks.javadocJar.configure {
    from(files("src/dist/javadoc"))
    from(files("NOTICE")) {
        into("META-INF")
    }
}

tasks.jar.configure {
    from(files("src/dist/jar"))
}

configurations {
    register("combinedClasspath") {
        extendsFrom(named("jython25CompileClasspath").get())
        extendsFrom(named("javaxServletCompileClasspath").get())
    }
}

// This source set is only needed, because the OSGI plugin supports only a single sourceSet.
// We are deleting it, because otherwise it would fool IDEs that a source root has multiple owners.
val otherSourceSetsRef = fmExt
    .allConfiguredSourceSetNames
    .map { names -> names.map { name -> sourceSets.named(name).get() } }
val osgiSourceSet = sourceSets
    .create("osgi") {
        val otherSourceSets = otherSourceSetsRef.get()
        java.setSrcDirs(otherSourceSets.flatMap { s -> s.java.srcDirs })
        resources.setSrcDirs(otherSourceSets.flatMap { s -> s.resources.srcDirs })
    }
    .apply {
        val osgiClasspath = configurations.named("combinedClasspath").get()
        compileClasspath = osgiClasspath
        runtimeClasspath = osgiClasspath
    }
    .also {
        sourceSets.remove(it)
        tasks.named(it.classesTaskName).configure {
            description = "Do not run this task! This task exists only as a work around for the OSGI plugin."
            enabled = false
        }
    }

tasks.named<Jar>(JavaPlugin.JAR_TASK_NAME) {
    configure<aQute.bnd.gradle.BundleTaskExtension> {
        bndfile.set(file("osgi.bnd"))

        setSourceSet(osgiSourceSet)
        val otherSourceSets = otherSourceSetsRef
            .get()
            .flatMap { sourceSet -> sourceSet.output.files }
            .toSet()

        classpath(osgiSourceSet.compileClasspath.filter { file -> file !in otherSourceSets })
        properties.putAll(fmExt.versionDef.versionProperties)
        properties.put("moduleOrg", project.group.toString())
        properties.put("moduleName", project.name)
    }
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

    javadocTool.set(javaToolchains.javadocToolFor {
        languageVersion.set(JavaLanguageVersion.of(fmExt.javadocJavaVersion))
    })

    (options as StandardJavadocDocletOptions).apply {
        val displayVersion = fmExt.versionDef.displayVersion
        val javadocEncoding = StandardCharsets.UTF_8

        locale = "en_US"
        encoding = javadocEncoding.name()
        windowTitle = "FreeMarker ${displayVersion} API"

        links("https://docs.oracle.com/en/java/javase/16/docs/api/")

        author(true)
        version(true)
        docEncoding = javadocEncoding.name()
        charSet = javadocEncoding.name()
        docTitle = "FreeMarker ${displayVersion}"

        // There are too many to check
        addStringOption("Xdoclint:-missing", "-quiet")
    }

    classpath = files(configurations.named("combinedClasspath"))
}

fun registerManualTask(taskName: String, localeValue: String, offlineValue: Boolean) {
    val manualTaskRef = tasks.register<freemarker.build.ManualBuildTask>(taskName) {
        inputDirectory.set(file("freemarker-manual/src/main/docgen/${localeValue}"))

        offline.set(offlineValue)
        locale.set(localeValue)
    }
    tasks.named(LifecycleBasePlugin.BUILD_TASK_NAME) { dependsOn(manualTaskRef) }
}

registerManualTask("manualOffline", "en_US", true)
registerManualTask("manualOnline", "en_US", false)

publishing {
    repositories {
        maven {
            val snapshot = fmExt.versionDef.version.endsWith("-SNAPSHOT")
            val defaultDeployUrl = if (snapshot) "https://repository.apache.org/content/repositories/snapshots" else "https://repository.apache.org/service/local/staging/deploy/maven2"
            setUrl(providers.gradleProperty("freemarkerDeployUrl").getOrElse(defaultDeployUrl))
            name = providers.gradleProperty("freemarkerDeployServerId").getOrElse("apache.releases.https")

            val apacheUser = providers.gradleProperty("freemarker.deploy.apache.user")
                .getOrElse("")

            if (apacheUser.isNotEmpty()) {
                credentials {
                    username = apacheUser
                    password = providers.gradleProperty("freemarker.deploy.apache.password")
                        .getOrElse("")
                }
            }
        }
        maven {
            name = "local"
            setUrl(layout.buildDirectory.map { it.dir("local-deployment") })
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
                    tag.set("v${fmExt.versionDef.version}")
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
        if (fmExt.signMethod.needSignature()) {
            fmExt.signMethod.configure(signing)
            signing.sign(mainPublication)
        }
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    if (repository.name != "local") {
        doFirst {
            if (fmExt.versionService.developmentBuild) {
                throw IllegalStateException("Cannot deploy to ${repository.name} in a development build." +
                        " Start the build with -PdevelopmentBuild=false")
            }
        }
    }
}

val distArchiveBaseName = "apache-${name}"
val distDir = layout.buildDirectory.map { it.dir("distributions") }

fun registerDistSupportTasks(archiveTask: TaskProvider<Tar>) {
    val signTask = tasks.register<freemarker.build.SignatureTask>("${archiveTask.name}Signature") {
        signatureConfiguration.set(fmExt.signMethod)
        inputFile.set(archiveTask.flatMap { task -> task.archiveFile })
    }

    val checksumTask = tasks.register<freemarker.build.ChecksumFileTask>("${archiveTask.name}Checksum") {
        inputFile.set(signTask.flatMap(freemarker.build.SignatureTask::inputFile))
    }

    tasks.named(LifecycleBasePlugin.BUILD_TASK_NAME) {
        dependsOn(archiveTask)
        dependsOn(checksumTask)

        if (fmExt.signMethod.needSignature()) {
            dependsOn(signTask)
        }
    }
}

fun registerCommonFiles(tar: Tar) {
    tar.from("README.md") {
        filter { content -> content.replace("{version}", fmExt.versionDef.displayVersion) }
    }

    tar.from(files("NOTICE", "RELEASE-NOTES"))
}

val distBin = tasks.register<Tar>("distBin") {
    compression = Compression.GZIP
    archiveBaseName.set(distArchiveBaseName)
    destinationDirectory.set(distDir)
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
registerDistSupportTasks(distBin)

val distSrc = tasks.register<Tar>("distSrc") {
    compression = Compression.GZIP
    archiveBaseName.set(distArchiveBaseName)
    destinationDirectory.set(distDir)
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
                "rat-excludes",
                "gradlew*",
                "gradle/**"
        )
        exclude(
                "/build",
                "/*/build",
                "/gradle/wrapper/gradle-wrapper.jar",
                "**/*.bak",
                "**/*.~*",
                "*/*.*~"
        )
    }
}
registerDistSupportTasks(distSrc)

fun readExcludeFile(excludeFile: File): List<String> {
    Files.lines(excludeFile.toPath()).use { lines ->
        return lines
            .map { it.trim() }
            .filter { !it.startsWith("#") && !it.isEmpty() }
            .collect(Collectors.toList())
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

        destinationDir = layout.buildDirectory.get().asFile.resolve("rat-prep").resolve(taskName)
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

eclipse {
    classpath {
        // Eclipse sees only a single classpath,
        // so make a best effort for a combined classpath.
        plusConfigurations = listOf(
            configurations["combinedClasspath"],
            configurations["core16CompileClasspath"],
            configurations["testUtilsCompileClasspath"],
            configurations["javaxServletTestCompileClasspath"]
        )
    }
}

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

configurations {
    compileOnly {
        exclude(group = "xml-apis", module = "xml-apis")
    }

    "javaxServletTestImplementation" {
        extendsFrom(compileClasspath.get())
        exclude(group = "javax.servlet.jsp")
        exclude(group = "javax.servlet", module = "servlet-api")
    }
}

dependencies {
    val xalan = "xalan:xalan:2.7.0"

    compileOnly("jaxen:jaxen:1.0-FCS")
    compileOnly("saxpath:saxpath:1.0-FCS")
    compileOnly(xalan)
    compileOnly("jdom:jdom:1.0b8")
    compileOnly("ant:ant:1.6.5") // FIXME: This could be moved to "jython20CompileOnly"
    compileOnly("rhino:js:1.6R1")
    compileOnly("avalon-logkit:avalon-logkit:2.0")
    compileOnly("org.slf4j:slf4j-api:${slf4jVersion}")
    compileOnly("org.slf4j:log4j-over-slf4j:${slf4jVersion}")
    compileOnly("org.slf4j:jcl-over-slf4j:${slf4jVersion}") // FIXME: This seems to be unused
    compileOnly("commons-logging:commons-logging:1.1.1") // FIXME: This seems to be unused
    compileOnly("org.zeroturnaround:javarebel-sdk:1.2.2")
    compileOnly("org.dom4j:dom4j:2.1.3") {
        // Excluding "pull-parser" to avoid test failures due to SAX parser conflict.
        exclude(group = "pull-parser", module = "pull-parser")
    }

    testImplementation(xalan)

    "javaxServletCompileOnly"("javax.servlet.jsp:jsp-api:2.1")
    "javaxServletCompileOnly"("javax.servlet:servlet-api:2.5")

    "javaxServletTestImplementation"("org.eclipse.jetty:jetty-server:${jettyVersion}")
    "javaxServletTestImplementation"("org.eclipse.jetty:jetty-webapp:${jettyVersion}")
    "javaxServletTestImplementation"("org.eclipse.jetty:jetty-util:${jettyVersion}")
    "javaxServletTestImplementation"("org.eclipse.jetty:apache-jsp:${jettyVersion}")
    // Jetty also contains the servlet-api and jsp-api classes

    // JSP JSTL (not included in Jetty):
    "javaxServletTestImplementation"("org.apache.taglibs:taglibs-standard-impl:${tagLibsVersion}")
    "javaxServletTestImplementation"("org.apache.taglibs:taglibs-standard-spec:${tagLibsVersion}")

    "javaxServletTestImplementation"("org.springframework:spring-core:${springVersion}") {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    "javaxServletTestImplementation"("org.springframework:spring-test:${springVersion}") {
        exclude(group = "commons-logging", module = "commons-logging")
    }

    "jython20CompileOnly"("jython:jython:2.1")

    "jython22CompileOnly"(sourceSets["jython20"].output)
    "jython22CompileOnly"("org.python:jython:2.2.1")

    "jython25CompileOnly"(sourceSets["jython20"].output)
    "jython25CompileOnly"("org.python:jython:2.5.0")

    "testUtilsImplementation"("displaytag:displaytag:1.2") {
        exclude(group = "com.lowagie", module = "itext")
        // We manage logging centrally:
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "rg.slf4j", module = "jcl104-over-slf4j")
        exclude(group = "log4j", module = "log4j")
    }
    "testUtilsImplementation"(sourceSets.main.get().output)
    "testUtilsImplementation"("com.google.code.findbugs:annotations:3.0.0")
    "testUtilsImplementation"(libs.junit)
    "testUtilsImplementation"("org.hamcrest:hamcrest-library:1.3")
    "testUtilsImplementation"("ch.qos.logback:logback-classic:1.1.2")
    "testUtilsImplementation"("commons-io:commons-io:2.7")
    "testUtilsImplementation"("com.google.guava:guava:29.0-jre")
    "testUtilsImplementation"("commons-collections:commons-collections:3.1")

    // Override Java 9 incompatible version (coming from displaytag):
    "testUtilsImplementation"("commons-lang:commons-lang:2.6")
}
