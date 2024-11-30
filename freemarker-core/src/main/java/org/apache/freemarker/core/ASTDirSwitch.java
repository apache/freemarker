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

/**
 * AST directive node: {@code #switch}.
 */
final class ASTDirSwitch extends ASTDirective {

    private ASTDirCase defaultCase;
    private boolean usesOnDirective;
    private final ASTExpression searched;
    private int firstCaseOrOnIndex;

    /**
     * @param searched the expression to be tested.
     */
    ASTDirSwitch(ASTExpression searched, ASTImplicitParent ignoredSectionBeforeFirstCase) {
        this.searched = searched;

        int ignoredCnt = ignoredSectionBeforeFirstCase != null ? ignoredSectionBeforeFirstCase.getChildCount() : 0;
        setChildBufferCapacity(ignoredCnt + 4);
        for (int i = 0; i < ignoredCnt; i++) {
            addChild(ignoredSectionBeforeFirstCase.fastGetChild(i));
        }
        firstCaseOrOnIndex = ignoredCnt; // Note that normally postParseCleanup will overwrite this
    }

    void addCase(ASTDirCase cas) {
        if (cas.condition == null) {
            defaultCase = cas;
        }
        addChild(cas);
    }

    void addOn(ASTDirOn on) {
        addChild(on);
        usesOnDirective = true;
    }

    @Override
    ASTElement[] execute(Environment env)
        throws TemplateException, IOException {
        boolean processedCase = false;
        int ln = getChildCount();
        if (usesOnDirective) {
            processOnDirectives: for (int i = firstCaseOrOnIndex; i < ln; i++) {
                ASTElement tel = getChild(i);

                // "default" is always the last; the parser ensures this
                if (tel == defaultCase) {
                    env.executeElement(defaultCase);
                    break;
                }

                for (ASTExpression condition : ((ASTDirOn) tel).conditions) {
                    boolean processOn = _EvalUtils.compare(
                            searched,
                            _EvalUtils.CMP_OP_EQUALS, "on==", condition, condition, env);
                    if (processOn) {
                        env.executeElement(tel);
                        break processOnDirectives;
                    }
                }
            }
        } else { // case-s
            try {
                for (int i = firstCaseOrOnIndex; i < ln; i++) {
                    ASTDirCase cas = (ASTDirCase) fastGetChild(i);
                    boolean processCase = false;

                    // Fall through if a previous case tested true.
                    if (processedCase) {
                        processCase = true;
                    } else if (cas.condition != null) {
                        // Otherwise, if this case isn't the default, test it.
                        processCase = _EvalUtils.compare(
                                searched,
                                _EvalUtils.CMP_OP_EQUALS, "case==", cas.condition, cas.condition, env);
                    }
                    if (processCase) {
                        env.executeElement(cas);
                        processedCase = true;
                    }
                }

                // If we didn't process any nestedElements, and we have a default,
                // process it.
                if (!processedCase && defaultCase != null) {
                    env.executeElement(defaultCase);
                }
            } catch (BreakOrContinueException br) {
                // Catch #break, but not #continue
                if (br == BreakOrContinueException.CONTINUE_INSTANCE) {
                    throw br;
                }
            }
        }

        return null;
    }

    @Override
    String dump(boolean canonical) {
        StringBuilder buf = new StringBuilder();
        if (canonical) buf.append('<');
        buf.append(getLabelWithoutParameters());
        buf.append(' ');
        buf.append(searched.getCanonicalForm());
        if (canonical) {
            buf.append('>');
            int ln = getChildCount();
            for (int i = 0; i < ln; i++) {
                buf.append(fastGetChild(i).getCanonicalForm());
            }
            buf.append("</").append(getLabelWithoutParameters()).append('>');
        }
        return buf.toString();
    }

    @Override
    public String getLabelWithoutParameters() {
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
    ASTElement postParseCleanup(boolean stripWhitespace) throws ParseException {
        ASTElement result = super.postParseCleanup(stripWhitespace);

        // The first #case might have shifted in the child array, so we have to find it again:
        int ln = getChildCount();
        int i = 0;
        while (i < ln && !(fastGetChild(i) instanceof ASTDirCase || fastGetChild(i) instanceof ASTDirOn)) {
            i++;
        }
        firstCaseOrOnIndex = i;

        return result;
    }

}
