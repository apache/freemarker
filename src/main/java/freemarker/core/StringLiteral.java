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
import java.io.StringReader;
import java.util.Enumeration;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.utility.StringUtil;

final class StringLiteral extends Expression implements TemplateScalarModel {
    
    private final String value;
    private TemplateElement dynamicValue;
    
    StringLiteral(String value) {
        this.value = value;
    }
    
    /**
     * @param parentTkMan
     *            The token source of the template that contains this string literal. As of this writing, we only need
     *            this to share the {@code namingConvetion} with that.
     */
    // TODO This should be the part of the "parent" parsing; now it contains hacks like those with namingConvention.  
    void parseValue(FMParserTokenManager parentTkMan) throws ParseException {
        if (value.length() > 3 && (value.indexOf("${") >= 0 || value.indexOf("#{") >= 0)) {
            
            UnboundTemplate parentTemplate = getUnboundTemplate();

            try {
                FMParserTokenManager tkMan = new FMParserTokenManager(
                        new SimpleCharStream(
                                new StringReader(value),
                                beginLine, beginColumn + 1,
                                value.length()));
                
                FMParser parser = new FMParser(parentTemplate, false, tkMan, null, parentTemplate.getParserConfiguration());
                // We continue from the parent parser's current state:
                parser.setupStringLiteralMode(parentTkMan);
                try {
                    dynamicValue = parser.FreeMarkerText();
                } finally {
                    // The parent parser continues from this parser's current state:
                    parser.tearDownStringLiteralMode(parentTkMan);
                }
            } catch (ParseException e) {
                e.setTemplateName(parentTemplate.getSourceName());
                throw e;
            }
            this.constantValue = null;
        }
    }
    
    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        return new SimpleScalar(evalAndCoerceToString(env));
    }

    public String getAsString() {
        return value;
    }
    
    /**
     * Tells if this is something like <tt>"${foo}"</tt>, which is usually a user mistake.
     */
    boolean isSingleInterpolationLiteral() {
        return dynamicValue != null && dynamicValue.getChildCount() == 1
                && dynamicValue.getChildAt(0) instanceof DollarVariable;
    }
    
    @Override
    String evalAndCoerceToString(Environment env) throws TemplateException {
        if (dynamicValue == null) {
            return value;
        } else {
            TemplateExceptionHandler teh = env.getTemplateExceptionHandler();
            env.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            try {
               return env.renderElementToString(dynamicValue);
            } catch (IOException ioe) {
                throw new _MiscTemplateException(ioe, env);
            } finally {
                env.setTemplateExceptionHandler(teh);
            }
        }
    }

    @Override
    public String getCanonicalForm() {
        if (dynamicValue == null) {
            return StringUtil.ftlQuote(value);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append('"');
            for (Enumeration childrenEnum = dynamicValue.children(); childrenEnum.hasMoreElements(); ) {
                TemplateElement child = (TemplateElement) childrenEnum.nextElement();
                if (child instanceof Interpolation) {
                    sb.append(((Interpolation) child).getCanonicalFormInStringLiteral());
                } else {
                    sb.append(StringUtil.FTLStringLiteralEnc(child.getCanonicalForm(), '"'));
                }
            }
            sb.append('"');
            return sb.toString();
        }
    }
    
    @Override
    String getNodeTypeSymbol() {
        return dynamicValue == null ? getCanonicalForm() : "dynamic \"...\"";
    }
    
    @Override
    boolean isLiteral() {
        return dynamicValue == null;
    }

    @Override
    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
        StringLiteral cloned = new StringLiteral(value);
        // FIXME: replacedIdentifier should be searched inside interpolatedOutput too:
        cloned.dynamicValue = this.dynamicValue;
        return cloned;
    }

    @Override
    int getParameterCount() {
        return 1;
    }

    @Override
    Object getParameterValue(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return dynamicValue;
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return ParameterRole.EMBEDDED_TEMPLATE;
    }
    
}
