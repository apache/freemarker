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
package org.apache.freemarker.core;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * AST directive node superclass.
 */
abstract class ASTDirective extends ASTElement {

    private static void addName(Set<String> allNames, Set<String> lcNames, Set<String> ccNames,
                                String commonName) {
        allNames.add(commonName);
        lcNames.add(commonName);
        ccNames.add(commonName);
    }

    private static void addName(Set<String> allNames, Set<String> lcNames, Set<String> ccNames,
                                String lcName, String ccName) {
        allNames.add(lcName);
        allNames.add(ccName);
        lcNames.add(lcName);
        ccNames.add(ccName);
    }

    static final Set<String> ALL_BUILT_IN_DIRECTIVE_NAMES;
    static final Set<String> LEGACY_BUILT_IN_DIRECTIVE_NAMES;
    static final Set<String> CAMEL_CASE_BUILT_IN_DIRECTIVE_NAMES;
    static {
        Set<String> allNames = new TreeSet();
        Set<String> lcNames = new TreeSet();
        Set<String> ccNames = new TreeSet();

        addName(allNames, lcNames, ccNames, "assign");
        addName(allNames, lcNames, ccNames, "attempt");
        addName(allNames, lcNames, ccNames, "autoesc", "autoEsc");
        addName(allNames, lcNames, ccNames, "break");
        addName(allNames, lcNames, ccNames, "call");
        addName(allNames, lcNames, ccNames, "case");
        addName(allNames, lcNames, ccNames, "comment");
        addName(allNames, lcNames, ccNames, "compress");
        addName(allNames, lcNames, ccNames, "default");
        addName(allNames, lcNames, ccNames, "else");
        addName(allNames, lcNames, ccNames, "elseif", "elseIf");
        addName(allNames, lcNames, ccNames, "escape");
        addName(allNames, lcNames, ccNames, "fallback");
        addName(allNames, lcNames, ccNames, "flush");
        addName(allNames, lcNames, ccNames, "foreach", "forEach");
        addName(allNames, lcNames, ccNames, "ftl");
        addName(allNames, lcNames, ccNames, "function");
        addName(allNames, lcNames, ccNames, "global");
        addName(allNames, lcNames, ccNames, "if");
        addName(allNames, lcNames, ccNames, "import");
        addName(allNames, lcNames, ccNames, "include");
        addName(allNames, lcNames, ccNames, "items");
        addName(allNames, lcNames, ccNames, "list");
        addName(allNames, lcNames, ccNames, "local");
        addName(allNames, lcNames, ccNames, "lt");
        addName(allNames, lcNames, ccNames, "macro");
        addName(allNames, lcNames, ccNames, "nested");
        addName(allNames, lcNames, ccNames, "noautoesc", "noAutoEsc");
        addName(allNames, lcNames, ccNames, "noescape", "noEscape");
        addName(allNames, lcNames, ccNames, "noparse", "noParse");
        addName(allNames, lcNames, ccNames, "nt");
        addName(allNames, lcNames, ccNames, "outputformat", "outputFormat");
        addName(allNames, lcNames, ccNames, "recover");
        addName(allNames, lcNames, ccNames, "recurse");
        addName(allNames, lcNames, ccNames, "return");
        addName(allNames, lcNames, ccNames, "rt");
        addName(allNames, lcNames, ccNames, "sep");
        addName(allNames, lcNames, ccNames, "setting");
        addName(allNames, lcNames, ccNames, "stop");
        addName(allNames, lcNames, ccNames, "switch");
        addName(allNames, lcNames, ccNames, "t");
        addName(allNames, lcNames, ccNames, "transform");
        addName(allNames, lcNames, ccNames, "visit");

        ALL_BUILT_IN_DIRECTIVE_NAMES = Collections.unmodifiableSet(allNames);
        LEGACY_BUILT_IN_DIRECTIVE_NAMES = Collections.unmodifiableSet(lcNames);
        CAMEL_CASE_BUILT_IN_DIRECTIVE_NAMES = Collections.unmodifiableSet(ccNames);
    }

}
