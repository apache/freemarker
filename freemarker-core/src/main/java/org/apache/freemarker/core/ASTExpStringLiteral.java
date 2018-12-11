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

import java.io.StringReader;
import java.util.List;

import org.apache.freemarker.core.model.TemplateMarkupOutputModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.impl.SimpleString;
import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.util.TemplateLanguageUtils;

/**
 * AST expression node: string literal
 */
final class ASTExpStringLiteral extends ASTExpression implements TemplateStringModel {
    
    private final String value;
    
    /** {@link List} of {@link String}-s and {@link ASTInterpolation}-s. */
    private List<Object> dynamicValue;
    
    ASTExpStringLiteral(String value) {
        this.value = value;
    }
    
    /**
     * @param parentTkMan
     *            The token source of the template that contains this string literal. This is possibly needed to
     *            inherit tokenizer-level auto-detected settings.
     */
    void parseValue(FMParserTokenManager parentTkMan, OutputFormat outputFormat) throws ParseException {
        // TODO [FM3]
        // The way this works is incorrect (the literal should be parsed without un-escaping),
        // but we can't fix this backward compatibly.
        Template parentTemplate = getTemplate();
        ParsingConfiguration pCfg = parentTemplate.getParsingConfiguration();
        // TODO [FM3] This shouldn't assume DefaultTemplateLanguage.
        InterpolationSyntax intSyn = ((DefaultTemplateLanguage) pCfg.getTemplateLanguage()).getInterpolationSyntax();
        if (value.length() > 3 && (
                // Find related: [interpolation prefixes]
                intSyn == InterpolationSyntax.DOLLAR && value.indexOf("${") != -1
                || intSyn == InterpolationSyntax.SQUARE_BRACKET && value.indexOf("[=") != -1)) {        
            try {
                SimpleCharStream simpleCharacterStream = new SimpleCharStream(
                        new StringReader(value),
                        beginLine, beginColumn + 1,
                        value.length());
                simpleCharacterStream.setTabSize(pCfg.getTabSize());
                
                FMParserTokenManager tkMan = new FMParserTokenManager(
                        simpleCharacterStream);
                
                FMParser parser = new FMParser(parentTemplate, false,
                        tkMan, pCfg,
                        null);
                // We continue from the parent parser's current state:
                parser.setupStringLiteralMode(parentTkMan, outputFormat);
                try {
                    dynamicValue = parser.StaticTextAndInterpolations();
                } finally {
                    // The parent parser continues from this parser's current state:
                    parser.tearDownStringLiteralMode(parentTkMan);
                }
            } catch (ParseException e) {
                e.setTemplate(parentTemplate);
                throw e;
            }
            constantValue = null;
        }
    }
    
    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        if (dynamicValue == null) {
            return new SimpleString(value);
        } else {
            // This should behave like concatenating the values with `+`. Thus, an interpolated expression that
            // returns markup promotes the result of the whole expression to markup.
            
            // Exactly one of these is non-null, depending on if the result will be plain text or markup, which can
            // change during evaluation, depending on the result of the interpolations:
            StringBuilder plainTextResult = null;
            TemplateMarkupOutputModel<?> markupResult = null;
            
            for (Object part : dynamicValue) {
                Object calcedPart =
                        part instanceof String ? part
                        : ((ASTInterpolation) part).calculateInterpolatedStringOrMarkup(env);
                if (markupResult != null) {
                    TemplateMarkupOutputModel<?> partMO = calcedPart instanceof String
                            ? markupResult.getOutputFormat().fromPlainTextByEscaping((String) calcedPart)
                            : (TemplateMarkupOutputModel<?>) calcedPart;
                    markupResult = _EvalUtils.concatMarkupOutputs(this, markupResult, partMO);
                } else { // We are using `plainTextOutput` (or nothing yet)
                    if (calcedPart instanceof String) {
                        String partStr = (String) calcedPart;
                        if (plainTextResult == null) {
                            plainTextResult = new StringBuilder(partStr);
                        } else {
                            plainTextResult.append(partStr);
                        }
                    } else { // `calcedPart` is TemplateMarkupOutputModel
                        TemplateMarkupOutputModel<?> moPart = (TemplateMarkupOutputModel<?>) calcedPart;
                        if (plainTextResult != null) {
                            TemplateMarkupOutputModel<?> leftHandMO = moPart.getOutputFormat()
                                    .fromPlainTextByEscaping(plainTextResult.toString());
                            markupResult = _EvalUtils.concatMarkupOutputs(this, leftHandMO, moPart);
                            plainTextResult = null;
                        } else {
                            markupResult = moPart;
                        }
                    }
                }
            } // for each part
            return markupResult != null ? markupResult
                    : plainTextResult != null ? new SimpleString(plainTextResult.toString())
                    : SimpleString.EMPTY_STRING;
        }
    }

    @Override
    public String getAsString() {
        return value;
    }
    
    /**
     * Tells if this is something like <tt>"${foo}"</tt>, which is usually a user mistake.
     */
    boolean isSingleInterpolationLiteral() {
        return dynamicValue != null && dynamicValue.size() == 1
                && dynamicValue.get(0) instanceof ASTInterpolation;
    }
    
    @Override
    public String getCanonicalForm() {
        if (dynamicValue == null) {
            return TemplateLanguageUtils.toStringLiteral(value);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append('"');
            for (Object child : dynamicValue) {
                if (child instanceof ASTInterpolation) {
                    sb.append(((ASTInterpolation) child).getCanonicalFormInStringLiteral());
                } else {
                    sb.append(TemplateLanguageUtils.escapeStringLiteralPart((String) child, '"'));
                }
            }
            sb.append('"');
            return sb.toString();
        }
    }
    
    @Override
    public String getLabelWithoutParameters() {
        return dynamicValue == null ? getCanonicalForm() : "dynamic \"...\"";
    }
    
    @Override
    boolean isLiteral() {
        return dynamicValue == null;
    }

    @Override
    ASTExpression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, ASTExpression replacement, ReplacemenetState replacementState) {
        ASTExpStringLiteral cloned = new ASTExpStringLiteral(value);
        // FIXME: replacedIdentifier should be searched inside interpolatedOutput too:
        cloned.dynamicValue = dynamicValue;
        return cloned;
    }

    @Override
    int getParameterCount() {
        return dynamicValue == null ? 0 : dynamicValue.size();
    }

    @Override
    Object getParameterValue(int idx) {
        checkIndex(idx);
        return dynamicValue.get(idx);
    }

    private void checkIndex(int idx) {
        if (dynamicValue == null || idx >= dynamicValue.size()) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        checkIndex(idx);
        return ParameterRole.VALUE_PART;
    }
    
}
