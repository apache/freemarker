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
 * An instruction representing a switch-case or switch-on structure.
 */
final class SwitchBlock extends TemplateElement {

    private Case defaultCase;
    private final Expression searched;
    private int firstCaseOrOnIndex;

    /**
     * @param searched the expression to be tested.
     */
    SwitchBlock(Expression searched, MixedContent ignoredSectionBeforeFirstCase) {
        this.searched = searched;
        
        int ignoredCnt = ignoredSectionBeforeFirstCase != null ? ignoredSectionBeforeFirstCase.getChildCount() : 0;
        setChildBufferCapacity(ignoredCnt + 4);
        for (int i = 0; i < ignoredCnt; i++) {
            addChild(ignoredSectionBeforeFirstCase.getChild(i));
        }
        firstCaseOrOnIndex = ignoredCnt; // Note that normally postParseCleanup will overwrite this
    }

    /**
     * @param cas a Case element.
     */
    void addCase(Case cas) {
        if (cas.condition == null) {
            defaultCase = cas;
        }
        addChild(cas);
    }

    /**
     * @param on an On element.
     */
    void addOn(On on) {
        addChild(on);
    }

    @Override
    TemplateElement[] accept(Environment env)
        throws TemplateException, IOException {
        boolean processedCaseOrOn = false;
        boolean usingOn = false;
        int ln = getChildCount();
        try {
            for (int i = firstCaseOrOnIndex; i < ln; i++) {
                TemplateElement tel = getChild(i);

                if (tel instanceof On) {
                    usingOn = true;

                    for (Expression condition : ((On) tel).conditions) {
                        boolean processOn = EvalUtil.compare(
                                searched,
                                EvalUtil.CMP_OP_EQUALS, "on==", condition, condition, env);
                        if (processOn) {
                            env.visit(tel);
                            processedCaseOrOn = true;
                            break;
                        }
                    }
                    if (processedCaseOrOn) {
                        break;
                    }
                } else { // Case
                    Expression condition = ((Case) tel).condition;
                    boolean processCase = false;

                    // Fall through if a previous case tested true.
                    if (processedCaseOrOn) {
                        processCase = true;
                    } else if (condition != null) {
                        // Otherwise, if this case isn't the default, test it.
                        processCase = EvalUtil.compare(
                                searched,
                                EvalUtil.CMP_OP_EQUALS, "case==", condition, condition, env);
                    }
                    if (processCase) {
                        env.visit(tel);
                        processedCaseOrOn = true;
                    }
                }
            }

            // If we didn't process any nestedElements, and we have a default,
            // process it.
            if (!processedCaseOrOn && defaultCase != null) {
                env.visit(defaultCase);
            }
        } catch (BreakOrContinueException br) {
            // This catches both break and continue,
            // hence continue is incorrectly treated as a break inside a case.
            // Unless using On, do backwards compatible behavior.
            if (usingOn) {
                throw br; // On supports neither break nor continue.
            }
        }
        return null;
    }

    @Override
    protected String dump(boolean canonical) {
        StringBuilder buf = new StringBuilder();
        if (canonical) buf.append('<');
        buf.append(getNodeTypeSymbol());
        buf.append(' ');
        buf.append(searched.getCanonicalForm());
        if (canonical) {
            buf.append('>');
            int ln = getChildCount();
            for (int i = 0; i < ln; i++) {
                buf.append(getChild(i).getCanonicalForm());
            }
            buf.append("</").append(getNodeTypeSymbol()).append('>');
        }
        return buf.toString();
    }

    @Override
    String getNodeTypeSymbol() {
        return "#switch";
    }

    @Override
    int getParameterCount() {
        return 1;
    }

    @Override
    Object getParameterValue(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return searched;
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return ParameterRole.VALUE;
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }

    @Override
    TemplateElement postParseCleanup(boolean stripWhitespace) throws ParseException {
        TemplateElement result = super.postParseCleanup(stripWhitespace);
        
        // The first #case might have shifted in the child array, so we have to find it again:
        int ln = getChildCount();
        int i = 0;
        while (i < ln
                && !(getChild(i) instanceof Case)
                && !(getChild(i) instanceof On)) {
            i++;
        }
        firstCaseOrOnIndex = i;
        
        return result;
    }
    
}
