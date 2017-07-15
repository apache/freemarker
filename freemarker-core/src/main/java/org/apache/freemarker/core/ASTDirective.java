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

    static final Set<String> BUILT_IN_DIRECTIVE_NAMES;
    static {
        TreeSet<String> names = new TreeSet<>();
        
        names.add("assign");
        names.add("attempt");
        names.add("autoEsc");
        names.add("break");
        names.add("case");
        names.add("compress");
        names.add("default");
        names.add("else");
        names.add("elseIf");
        names.add("escape");
        names.add("fallback");
        names.add("flush");
        names.add("ftl");
        names.add("function");
        names.add("global");
        names.add("if");
        names.add("import");
        names.add("include");
        names.add("items");
        names.add("list");
        names.add("local");
        names.add("lt");
        names.add("macro");
        names.add("nested");
        names.add("noAutoEsc");
        names.add("noEscape");
        names.add("noParse");
        names.add("nt");
        names.add("outputFormat");
        names.add("recover");
        names.add("recurse");
        names.add("return");
        names.add("rt");
        names.add("sep");
        names.add("setting");
        names.add("stop");
        names.add("switch");
        names.add("t");
        names.add("visit");

        BUILT_IN_DIRECTIVE_NAMES = Collections.unmodifiableSet(names);

    }

}
