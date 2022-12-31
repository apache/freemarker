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

package org.apache.freemarker.build.module.common

import aQute.bnd.gradle.BundleTaskExtension
import java.io.File
import org.apache.freemarker.build.core.FREEMARKER_JAVADOC_CLASSPATH_CONFIG_NAME
import org.apache.freemarker.build.core.configureRequirePublished
import org.apache.freemarker.build.core.getFreemarkerVersionService
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.signing.SigningExtension

class FreemarkerPublishedJavaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply {
            apply(FreemarkerJavaPlugin::class)
            apply("maven-publish")
            apply("signing")
            apply("biz.aQute.bnd.builder")
        }

        project.configure<JavaPluginExtension> {
            withSourcesJar()
            withJavadocJar()
        }

        project.configurations.create(FREEMARKER_JAVADOC_CLASSPATH_CONFIG_NAME) {
            extendsFrom(project.configurations.named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME).get())
        }

        configureRequirePublished(project)

        Configurations(project).apply {
            configureTasks()
            configurePublications()
        }
    }

    private class Configurations(private val project: Project) {
        private val providers = project.providers

        private val versionService = project.getFreemarkerVersionService()

        fun configureTasks() {
            project.tasks.apply {
                val rootProject = project.rootProject

                val osgiResourceCopy = register("osgiResourceCopy", Copy::class) {
                    from(rootProject.file("src/dist/jar"))
                    into(project.layout.buildDirectory.map { it.dir("osgi") })
                }

                named<Jar>(JavaPlugin.JAR_TASK_NAME) {
                    inputs.dir(osgiResourceCopy.map { it.destinationDir })

                    configure<BundleTaskExtension> {
                        bndfile.set(rootProject.file("osgi.bnd"))

                        val includeResourcePath = osgiResourceCopy
                                .map { it.destinationDir }
                                .get()
                                .relativeTo(project.projectDir)
                                .toString()
                                .replace(File.separator, "/")

                        properties.putAll(versionService.versionDef.versionProperties)
                        properties.put("osgiIncludeResourcePath", includeResourcePath)
                        properties.put("moduleOrg", project.group.toString())
                        properties.put("moduleName", project.name)
                    }
                }

                named<Javadoc>(JavaPlugin.JAVADOC_TASK_NAME) {
                    configureJavadocDefaults(this)

                    val freemarkerJava = project.the<FreemarkerJavaExtension>()
                    title = "${freemarkerJava.projectTitle.get()} ${versionService.versionDef.displayVersion} API"
                }
            }
        }

        fun configurePublications() {
            project.configure<PublishingExtension> {
                repositories {
                    maven {
                        val snapshot = versionService.versionDef.version.endsWith("-SNAPSHOT")
                        val defaultDeployUrl = if (snapshot) "https://repository.apache.org/content/repositories/snapshots" else "https://repository.apache.org/service/local/staging/deploy/maven2"
                        setUrl(providers.gradleProperty("freemarkerDeployUrl").getOrElse(defaultDeployUrl))
                        name = providers.gradleProperty("freemarkerDeployServerId").getOrElse("apache.releases.https")
                    }
                }

                publications.apply {
                    val mainPublication = register<MavenPublication>("main") {
                        from(project.components.getByName("java"))
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
                                tag.set("v${versionService.versionDef.version}")
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
                    if (versionService.doSignPackages.get()) {
                        project.the<SigningExtension>().sign(mainPublication.get())
                    }
                }
            }
        }
    }
}
