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

import java.io.IOException;

import org.apache.freemarker.core.model.CallPlace;
import org.apache.freemarker.core.model.TemplateDateModel;

/**
 * Not yet public; subject to change.
 * 
 * <p>
 * Known compatibility risks when using this post-processor:
 * <ul>
 * <li>
 *   Software that uses {@link CallPlace#isNestedOutputCacheable()} will always get {@code false}, because
 *   interruption checks ({@link ASTThreadInterruptionCheck} elements) are, obviously, not cacheable. This should only
 *   impact the performance.
 * <li>
 *   Software that investigates the AST will see the injected {@link ASTThreadInterruptionCheck} elements. As of this
 *   writing the AST API-s aren't published, also such software need to be able to deal with new kind of elements
 *   anyway, so this shouldn't be a problem. When a {@link TemplateDateModel} checks if it has no nested content, it
 *   should always use {@link CallPlace#hasNestedContent()}, which will ignore nested content that only
 *   contains a {@link ASTThreadInterruptionCheck}.
 * </ul>
 */
class ThreadInterruptionSupportTemplatePostProcessor extends TemplatePostProcessor {

    @Override
    public void postProcess(Template t) throws TemplatePostProcessorException {
        final ASTElement te = t.getRootASTNode();
        addInterruptionChecks(te);
    }

    private void addInterruptionChecks(final ASTElement te) throws TemplatePostProcessorException {
        if (te == null) {
            return;
        }
        
        final int childCount = te.getChildCount();
        for (int i = 0; i < childCount; i++) {
            addInterruptionChecks(te.getChild(i));
        }
        
        if (te.isNestedBlockRepeater()) {
            try {
                te.addChild(0, new ASTThreadInterruptionCheck(te));
            } catch (ParseException e) {
                throw new TemplatePostProcessorException("Unexpected error; see cause", e);
            }
        }
    }

    /**
     * AST directive-like node: Checks if the current thread's "interrupted" flag is set, and throws
     * {@link TemplateProcessingThreadInterruptedException} if it is. We inject this to some points into the AST.
     */
    static class ASTThreadInterruptionCheck extends ASTElement {
        
        private ASTThreadInterruptionCheck(ASTElement te) throws ParseException {
            setLocation(te.getTemplate(), te.beginColumn, te.beginLine, te.beginColumn, te.beginLine);
        }

        @Override
        ASTElement[] accept(Environment env) throws TemplateException, IOException {
            // As the API doesn't allow throwing InterruptedException here (nor anywhere else, most importantly,
            // Template.process can't throw it), we must not clear the "interrupted" flag of the thread.
            if (Thread.currentThread().isInterrupted()) {
                throw new TemplateProcessingThreadInterruptedException();
            }
            return null;
        }

        @Override
        protected String dump(boolean canonical) {
            return canonical ? "" : "<#--" + getASTNodeDescriptor() + "--#>";
        }

        @Override
        String getASTNodeDescriptor() {
            return "##threadInterruptionCheck";
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
    
    /**
     * Indicates that the template processing thread's "interrupted" flag was found to be set.
     * 
     * <p>ATTENTION: This is used by https://github.com/kenshoo/freemarker-online. Don't break backward
     * compatibility without updating that project too! 
     */
    static class TemplateProcessingThreadInterruptedException extends RuntimeException {
        
        TemplateProcessingThreadInterruptedException() {
            super("Template processing thread \"interrupted\" flag was set.");
        }
        
    }

}
