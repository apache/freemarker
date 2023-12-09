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

import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import java.nio.charset.StandardCharsets

fun configureJavadocDefaults(javadoc: Javadoc) {
    javadoc.group = JavaBasePlugin.DOCUMENTATION_GROUP
    javadoc.modularity.inferModulePath.set(false)

    (javadoc.options as StandardJavadocDocletOptions).apply {
        val javadocEncoding = StandardCharsets.UTF_8

        locale = "en_US"
        encoding = javadocEncoding.name()

        links("https://docs.oracle.com/en/java/javase/17/docs/api/")

        author(true)
        version(true)
        docEncoding = javadocEncoding.name()
        charSet = javadocEncoding.name()

        // There are too many to check
        addStringOption("Xdoclint:all,-missing", "-quiet")
    }
}
