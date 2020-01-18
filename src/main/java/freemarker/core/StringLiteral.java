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

import java.io.StringReader;
import java.util.List;

import freemarker.template.Configuration;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.utility.StringUtil;

final class StringLiteral extends Expression implements TemplateScalarModel {
    
    private final String value;
    
    /** {@link List} of {@link String}-s and {@link Interpolation}-s. */
    private List<Object> dynamicValue;
    
    StringLiteral(String value) {
        this.value = value;
    }
    
    /**
     * @param parentParser
     *            The parser of the template that contains this string literal.
     */
    void parseValue(FMParser parentParser, OutputFormat outputFormat) throws ParseException {
        // The way this works is incorrect (the literal should be parsed without un-escaping),
        // but we can't fix this backward compatibly.
        Template parentTemplate = getTemplate();
        ParserConfiguration pcfg = parentTemplate.getParserConfiguration();
        int intSyn = pcfg.getInterpolationSyntax();
        if (value.length() > 3 && (
                    (intSyn == Configuration.LEGACY_INTERPOLATION_SYNTAX
                        || intSyn == Configuration.DOLLAR_INTERPOLATION_SYNTAX) 
                        && (value.indexOf("${") != -1
                    || intSyn == Configuration.LEGACY_INTERPOLATION_SYNTAX && value.indexOf("#{") != -1)
                    || intSyn == Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX && value.indexOf("[=") != -1)) {
            try {
                SimpleCharStream simpleCharacterStream = new SimpleCharStream(
                        new StringReader(value),
                        beginLine, beginColumn + 1,
                        value.length());
                simpleCharacterStream.setTabSize(pcfg.getTabSize());
                
                FMParserTokenManager tkMan = new FMParserTokenManager(
                        simpleCharacterStream);
                
                FMParser parser = new FMParser(parentTemplate, false, tkMan, pcfg);
                // We continue from the parent parser's current state:
                parser.setupStringLiteralMode(parentParser, outputFormat);
                try {
                    dynamicValue = parser.StaticTextAndInterpolations();
                } finally {
                    // The parent parser continues from this parser's current state:
                    parser.tearDownStringLiteralMode(parentParser);
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
        if (dynamicValue == null) {
            return new SimpleScalar(value);
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
                        : ((Interpolation) part).calculateInterpolatedStringOrMarkup(env);
                if (markupResult != null) {
                    TemplateMarkupOutputModel<?> partMO = calcedPart instanceof String
                            ? markupResult.getOutputFormat().fromPlainTextByEscaping((String) calcedPart)
                            : (TemplateMarkupOutputModel<?>) calcedPart;
                    markupResult = EvalUtil.concatMarkupOutputs(this, markupResult, partMO);
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
                            markupResult = EvalUtil.concatMarkupOutputs(this, leftHandMO, moPart);
                            plainTextResult = null;
                        } else {
                            markupResult = moPart;
                        }
                    }
                }
            } // for each part
            return markupResult != null ? markupResult
                    : plainTextResult != null ? new SimpleScalar(plainTextResult.toString())
                    : SimpleScalar.EMPTY_STRING;
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
                && dynamicValue.get(0) instanceof Interpolation;
    }
    
    @Override
    public String getCanonicalForm() {
        if (dynamicValue == null) {
            return StringUtil.ftlQuote(value);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append('"');
            for (Object child : dynamicValue) {
                if (child instanceof Interpolation) {
                    sb.append(((Interpolation) child).getCanonicalFormInStringLiteral());
                } else {
                    sb.append(StringUtil.FTLStringLiteralEnc((String) child, '"'));
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
