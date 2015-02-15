/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import java.io.IOException;
import java.util.ArrayList;

import freemarker.template.TemplateException;

/**
 * #attempt element; might has a nested {@link RecoveryBlock}.
 */
final class AttemptBlock extends TemplateElement {
    
    private TemplateElement attemptBlock;
    private RecoveryBlock recoveryBlock;
    
    AttemptBlock(TemplateElement attemptBlock, RecoveryBlock recoveryBlock) {
        this.attemptBlock = attemptBlock;
        this.recoveryBlock = recoveryBlock;
        nestedElements = new ArrayList();
        nestedElements.add(attemptBlock);
        nestedElements.add(recoveryBlock);
    }

    void accept(Environment env) throws TemplateException, IOException 
    {
        env.visitAttemptRecover(attemptBlock, recoveryBlock);
    }

    protected String dump(boolean canonical) {
        if (!canonical) {
            return getNodeTypeSymbol();
        } else {
            StringBuffer buf = new StringBuffer();
            buf.append("<");
            buf.append(getNodeTypeSymbol());
            buf.append(">");
            if (attemptBlock != null) {
                buf.append(attemptBlock.getCanonicalForm());            
            }
            if (recoveryBlock != null) {
                buf.append(recoveryBlock.getCanonicalForm());
            }
            return buf.toString();
        }
    }
    
    int getParameterCount() {
        return 1;
    }

    Object getParameterValue(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return recoveryBlock;
    }

    ParameterRole getParameterRole(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return ParameterRole.ERROR_HANDLER;
    }
    
    String getNodeTypeSymbol() {
        return "#attempt";
    }
    
    boolean isShownInStackTrace() {
        return false;
    }

    boolean isNestedBlockRepeater() {
        return false;
    }
    
}
