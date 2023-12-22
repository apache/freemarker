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
 * Holder for the attempted section of the #attempt element and of the nested #recover element ({@link RecoveryBlock}).
 */
final class AttemptBlock extends TemplateElement {
    
    private TemplateElement attemptedSection;
    private RecoveryBlock recoverySection;
    
    AttemptBlock(TemplateElements attemptedSectionChildren, RecoveryBlock recoverySection) {
        TemplateElement attemptedSection = attemptedSectionChildren.asSingleElement();
        this.attemptedSection = attemptedSection;
        this.recoverySection = recoverySection;
        setChildBufferCapacity(2);
        addChild(attemptedSection); // for backward compatibility
        addChild(recoverySection);
    }

    @Override
    TemplateElement[] accept(Environment env) throws TemplateException, IOException {
        env.visitAttemptRecover(this, attemptedSection, recoverySection);
        return null;
    }

    @Override
    protected String dump(boolean canonical) {
        if (!canonical) {
            return getNodeTypeSymbol();
        } else {
            StringBuilder buf = new StringBuilder();
            buf.append("<").append(getNodeTypeSymbol()).append(">");
            buf.append(getChildrenCanonicalForm());            
            buf.append("</").append(getNodeTypeSymbol()).append(">");
            return buf.toString();
        }
    }
    
    @Override
    int getParameterCount() {
        return 1;
    }

    @Override
    Object getParameterValue(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return recoverySection;
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return ParameterRole.ERROR_HANDLER;
    }
    
    @Override
    String getNodeTypeSymbol() {
        return "#attempt";
    }
    
    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }
    
}
