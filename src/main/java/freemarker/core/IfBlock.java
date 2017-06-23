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

package freemarker.core;

import java.io.IOException;

import freemarker.template.TemplateException;

/**
 * Container for a group of related #if, #elseif and #else elements.
 * Each such block is a nested {@link ConditionalBlock}. Note that if an #if has no #else or #elseif,
 * {@link ConditionalBlock} doesn't need this parent element. 
 */
final class IfBlock extends TemplateElement {

    IfBlock(ConditionalBlock block) {
        setChildBufferCapacity(1);
        addBlock(block);
    }

    void addBlock(ConditionalBlock block) {
        addChild(block);
    }

    @Override
    TemplateElement[] accept(Environment env) throws TemplateException, IOException {
        int ln  = getChildCount();
        for (int i = 0; i < ln; i++) {
            ConditionalBlock cblock = (ConditionalBlock) getChild(i);
            Expression condition = cblock.condition;
            env.replaceElementStackTop(cblock);
            if (condition == null || condition.evalToBoolean(env)) {
                return cblock.getChildBuffer();
            }
        }
        return null;
    }

    @Override
    TemplateElement postParseCleanup(boolean stripWhitespace)
        throws ParseException {
        if (getChildCount() == 1) {
            ConditionalBlock cblock = (ConditionalBlock) getChild(0);
            cblock.setLocation(getTemplate(), cblock, this);
            return cblock.postParseCleanup(stripWhitespace);
        } else {
            return super.postParseCleanup(stripWhitespace);
        }
    }
    
    @Override
    protected String dump(boolean canonical) {
        if (canonical) {
            StringBuilder buf = new StringBuilder();
            int ln = getChildCount();
            for (int i = 0; i < ln; i++) {
                ConditionalBlock cblock = (ConditionalBlock) getChild(i);
                buf.append(cblock.dump(canonical));
            }
            buf.append("</#if>");
            return buf.toString();
        } else {
            return getNodeTypeSymbol();
        }
    }
    
    @Override
    String getNodeTypeSymbol() {
        return "#if-#elseif-#else-container";
    }
    
    @Override
    int getParameterCount() {
        return 0;
    }

    @Override
    Object getParameterValue(int idx) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }
    
}
