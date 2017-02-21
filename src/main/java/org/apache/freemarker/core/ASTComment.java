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

import org.apache.freemarker.core.util._StringUtil;

/**
 * AST comment node
 */
final class ASTComment extends _ASTElement {

    private final String text;

    ASTComment(String text) {
        this.text = text;
    }

    @Override
    _ASTElement[] accept(Environment env) {
        // do nothing, skip the body
        return null;
    }

    @Override
    protected String dump(boolean canonical) {
        if (canonical) {
            return "<#--" + text + "-->";
        } else {
            return "comment " + _StringUtil.jQuote(text.trim());
        }
    }
    
    @Override
    String getNodeTypeSymbol() {
        return "#--...--";
    }
    

    @Override
    int getParameterCount() {
        return 1;
    }

    @Override
    Object getParameterValue(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return text;
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return ParameterRole.CONTENT;
    }

    public String getText() {
        return text;
    }

    @Override
    boolean isOutputCacheable() {
        return true;
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }
    
}
