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

import freemarker.template.TemplateException;
import freemarker.template.utility.StandardCompress;

/**
 * An instruction that reduces all sequences of whitespace to a single
 * space or newline. In addition, leading and trailing whitespace is removed.
 * @see freemarker.template.utility.StandardCompress
 */
final class CompressedBlock extends TemplateElement {

    CompressedBlock(TemplateElement nestedBlock) { 
        setNestedBlock(nestedBlock);
    }

    void accept(Environment env) throws TemplateException, IOException {
        if (getNestedBlock() != null) {
            env.visitAndTransform(getNestedBlock(), StandardCompress.INSTANCE, null);
        }
    }

    protected String dump(boolean canonical) {
        if (canonical) {
            String nested = getNestedBlock() != null ? getNestedBlock().getCanonicalForm() : "";
            return "<" + getNodeTypeSymbol() + ">" + nested + "</" + getNodeTypeSymbol() + ">";
        } else {
            return getNodeTypeSymbol();
        }
    }
    
    String getNodeTypeSymbol() {
        return "#compress";
    }
    
    int getParameterCount() {
        return 0;
    }

    Object getParameterValue(int idx) {
        throw new IndexOutOfBoundsException();
    }

    ParameterRole getParameterRole(int idx) {
        throw new IndexOutOfBoundsException();
    }

    boolean isIgnorable() {
        return getNestedBlock() == null || getNestedBlock().isIgnorable();
    }

    boolean isNestedBlockRepeater() {
        return false;
    }
    
}
