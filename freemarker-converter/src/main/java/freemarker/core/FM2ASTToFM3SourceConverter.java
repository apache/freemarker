/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package freemarker.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.freemarker.core.NamingConvention;
import org.apache.freemarker.core.util._NullArgumentException;
import org.apache.freemarker.converter.ConverterException;
import org.apache.freemarker.converter.ConverterUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Takes a FreeMarker 2 AST, and converts it to an FreeMarker 3 source code.
 * This has to be inside the "freemarker.core" package to access package visible members/classes.
 * <p>
 * Notes for implementators:
 * <ul>
 * <li>The tricky part is that the AST contains no nodes for insignificant white-space and comments inside expressions.
 * Furthermore, directive calls are AST nodes, but the individual tags (the start- and end-tag) aren't, so restoring
 * the insignificant white-space and comments inside the tags is even trickier.
 * This information has to be restored from the source code string ({@link #src}), based on the positions (begin/end
 * column/row number) of the AST nodes, and sometimes that has to be combined with some simple manual parsing.
 * <li>Do not hard-code "&lt;" and "&gt;" into the code where you should use {@link #tagBeginChar} and
 * {@link #tagEndChar}.
 * <li>Stopping with error is always better than risking incorrect output. Use assertions. Don't be permissive with
 * unexpected input.
 * <li>Generally, try to use node parameters (via {@link #getOnlyParam(TemplateObject, ParameterRole, Class)},
 * {@link #getParam(TemplateObject, int, ParameterRole, Class)}, etc.) instead of directly calling methods specific
 * to the node subclass. Always process all parameters; where you don't use
 * {@link #getOnlyParam(TemplateObject, ParameterRole, Class)}, use {@link #assertParamCount(TemplateObject, int)} to
 * ensure that. If you know you don't need some of the paramters, still at least call
 * {@link #assertParamRole(TemplateObject, int, ParameterRole)} for them. These ensure that if new parameters are
 * added in FreeMarker 2.x, no information will be silently lost during conversion.
 * </ul>
 */
@SuppressWarnings("deprecation")
public class FM2ASTToFM3SourceConverter {

    private final String src;
    private final StringBuilder out;
    private List<Integer> rowStartPositions;
    private final char tagBeginChar;
    private final char tagEndChar;
    private final org.apache.freemarker.core.Configuration fm3Config = new org.apache.freemarker.core.Configuration
            .Builder(org.apache.freemarker.core.Configuration.getVersion() /* highest possible by design */)
            .namingConvention(NamingConvention.CAMEL_CASE)
            .build();
    private final Set<String> fm3BuiltInNames = fm3Config.getSupportedBuiltInNames();

    /**
     * @param template Must have been parsed with {@link Configuration#getWhitespaceStripping()} {@code false}.
     */
    public static String convert(Template template, String src) throws ConverterException {
        FM2ASTToFM3SourceConverter instance = new FM2ASTToFM3SourceConverter(template, src);
        instance.printNode(template.getRootTreeNode());
        return instance.getOutput();
    }

    private FM2ASTToFM3SourceConverter(Template template, String src) {
        _NullArgumentException.check("template", template);
        if (template.getParserConfiguration().getWhitespaceStripping()) {
            throw new IllegalArgumentException("The Template must have been parsed with whitespaceStripping false.");
        }

        _NullArgumentException.check("src", src);

        this.src = src;
        this.out = new StringBuilder();
        if (template.getActualTagSyntax() == Configuration.SQUARE_BRACKET_TAG_SYNTAX) {
            tagBeginChar = '[';
            tagEndChar = ']';
        } else {
            tagBeginChar = '<';
            tagEndChar = '>';
        }
    }

    private String getOutput() throws ConverterException {
        String s = out.toString();
        try {
            new org.apache.freemarker.core.Template(null, s, fm3Config);
        } catch (Exception e) {
            throw new ConverterException(
                    "The result of the conversion wasn't valid FreeMarker 3 template; see cause exception", e);
        }
        return s;
    }

    private void printNode(TemplateObject node) throws ConverterException {
        if (node instanceof TemplateElement) {
            printTemplateElement((TemplateElement) node);
        } else {
            printExpressionNode(node);
        }
    }

    private void printTemplateElement(TemplateElement node) throws ConverterException {
        if (node instanceof MixedContent) {
            printChildrenElements(node);
        } else if (node instanceof TextBlock) {
            print(getOnlyParam(node, ParameterRole.CONTENT, String.class));
        } else if (node instanceof DollarVariable) {
            printWithParamsLeadingSkippedTokens("${", node);
            printNode(getOnlyParam(node, ParameterRole.CONTENT, TemplateObject.class));
            printWithParamsTrailingSkippedTokens("}", node, 0);
        } else if (node instanceof IfBlock) {
            printChildrenElements(node);
            print(tagBeginChar);
            print("/#if");
            printEndTagSkippedTokens(node);
            print(tagEndChar);
        } else if (node instanceof ConditionalBlock) {
            assertParamCount(node,2);
            TemplateObject conditionExp = getParam(node, 0, ParameterRole.CONDITION, TemplateObject.class);
            int nodeSubtype = getParam(node, 1, ParameterRole.AST_NODE_SUBTYPE, Integer.class);

            print(tagBeginChar);
            String tagStart;
            if (nodeSubtype == ConditionalBlock.TYPE_IF) {
                tagStart = "#if";
            } else if (nodeSubtype == ConditionalBlock.TYPE_ELSE) {
                tagStart = "#else";
            } else if (nodeSubtype == ConditionalBlock.TYPE_ELSE_IF) {
                tagStart = "#elseIf";
            } else {
                throw new UnexpectedNodeContentException(node, "Unhandled subtype, {}.", nodeSubtype);
            }
            print(tagStart);
            if (conditionExp != null) {
                printWithParamsLeadingSkippedTokens(tagStart.length() + 1, node);
                printNode(conditionExp);
            }
            printStartTagSkippedTokens(node, conditionExp, true);
            print(tagEndChar);

            printChildrenElements(node);

            if (!(node.getParentElement() instanceof IfBlock)) {
                print(tagBeginChar);
                print("/#if");
                printEndTagSkippedTokens(node);
                print(tagEndChar);
            }
        } else if (node instanceof Comment) {
            print(tagBeginChar);
            print("#--");
            print(getOnlyParam(node, ParameterRole.CONTENT, String.class));
            print("--");
            print(tagEndChar);
        } else {
            throw new ConverterException("Unhandled AST TemplateElement class: " + node.getClass().getName());
        }
    }

    private void printExpressionNode(TemplateObject node) throws ConverterException {
        if (node instanceof Identifier || node instanceof NumberLiteral || node instanceof BooleanLiteral) {
            print(node.getCanonicalForm());
        } else if (node instanceof AddConcatExpression) {
            assertParamCount(node, 2);
            TemplateObject lho = getParam(node, 0, ParameterRole.LEFT_HAND_OPERAND, TemplateObject.class);
            TemplateObject rho = getParam(node, 1, ParameterRole.RIGHT_HAND_OPERAND, TemplateObject.class);
            printNode(lho);
            printParameterSeparatorSource(lho, rho);
            printNode(rho);
        } else if (node instanceof ArithmeticExpression) {
            assertParamCount(node, 3);
            assertParamRole(node, 2, ParameterRole.AST_NODE_SUBTYPE);
            TemplateObject lho = getParam(node, 0, ParameterRole.LEFT_HAND_OPERAND, TemplateObject.class);
            TemplateObject rho = getParam(node, 1, ParameterRole.RIGHT_HAND_OPERAND, TemplateObject.class);
            printNode(lho);
            printParameterSeparatorSource(lho, rho);
            printNode(rho);
        } else if (node instanceof UnaryPlusMinusExpression) {
            assertParamCount(node, 2);
            assertParamRole(node, 1, ParameterRole.AST_NODE_SUBTYPE);
            printWithParamsLeadingSkippedTokens(node.getNodeTypeSymbol().substring(0, 1), node);
            printNode(getParam(node, 0, ParameterRole.RIGHT_HAND_OPERAND, TemplateObject.class));
        } else if (node instanceof ParentheticalExpression) {
            printWithParamsLeadingSkippedTokens("(", node);
            printNode(getOnlyParam(node, ParameterRole.ENCLOSED_OPERAND, TemplateObject.class));
            printWithParamsTrailingSkippedTokens(")", node, 0);
        } else if (node instanceof MethodCall) {
            TemplateObject callee = getParam(node, 0, ParameterRole.CALLEE, TemplateObject.class);
            printExpressionNode(callee);

            TemplateObject prevParam = callee;
            int argCnt = node.getParameterCount() - 1;
            for (int argIdx = 0; argIdx < argCnt; argIdx++) {
                TemplateObject argExp = getParam(node, argIdx + 1, ParameterRole.ARGUMENT_VALUE, TemplateObject.class);
                printParameterSeparatorSource(prevParam, argExp);
                printExpressionNode(argExp);
                prevParam = argExp;
            }
            printWithParamsTrailingSkippedTokens(")", node, argCnt);
        } else if (node instanceof BuiltIn) {
            assertParamCount(node, 2);
            TemplateObject lho = getParam(node, 0, ParameterRole.LEFT_HAND_OPERAND, TemplateObject.class);
            String rho = getParam(node, 1, ParameterRole.RIGHT_HAND_OPERAND, String.class);

            printExpressionNode(lho); // [lho]?biName

            int postLHOPos = getPosition(lho.getEndColumn(), lho.getEndLine()) + 1;
            int endPos = getPosition(node.getEndColumn(), node.getEndLine());
            boolean foundQuestionMark = false;
            int pos = postLHOPos;
            scanForRHO: while (pos < endPos) {
                char c = src.charAt(pos);
                if (c == '?') {
                    foundQuestionMark = true;
                    pos++;
                } else if (Character.isWhitespace(c)) {
                    pos++;
                } else if (isCoreNameChar(c)) {
                    break scanForRHO;
                } else {
                    throw new UnexpectedNodeContentException(node,
                            "Unexpected character when scanning for for built-in key: '{}'", c);
                }
            }
            if (pos == endPos || !foundQuestionMark) {
                throw new UnexpectedNodeContentException(node, "Couldn't find built-in key in source", null);
            }
            print(src.substring(postLHOPos, pos)); // lho[?]biName

            print(convertBuiltInName(rho));
        } else {
            throw new ConverterException("Unhandled AST node class: " + node.getClass().getName());
        }
    }

    private String convertBuiltInName(String name) throws ConverterException {
        String converted = name.indexOf('_') == -1 ? name : ConverterUtils.snakeCaseToCamelCase(name);

        if (converted.equals("webSafe")) {
            return "html";
        }

        if (!fm3BuiltInNames.contains(converted)) {
            throw new ConverterException("Couldn't map \"" + name + "\" to a valid FreeMarker 3 built-in name "
                    + "(tried: " + converted + ")");
        }
        return converted;
    }

    private void printParameterSeparatorSource(TemplateObject lho, TemplateObject rho) {
        print(getSrcSectionExclEnd(
                lho.getEndColumn() + 1, lho.getEndLine(),
                rho.getBeginColumn(), rho.getBeginLine()));
    }

    private void printChildrenElements(TemplateElement node) throws ConverterException {
        int ln = node.getChildCount();
        for (int i = 0; i < ln; i++) {
            printNode(node.getChild(i));
        }
    }

    private void printWithParamsLeadingSkippedTokens(String beforeParams, TemplateObject node)
            throws ConverterException {
        print(beforeParams);
        printWithParamsLeadingSkippedTokens(beforeParams.length(), node);
    }

    private void printWithParamsLeadingSkippedTokens(int beforeParamsLength, TemplateObject node)
            throws UnexpectedNodeContentException {
        if (node.getParameterCount() == 0) {
            return;
        }
        TemplateObject param = getParam(node, 0, null, TemplateObject.class);
        print(getSrcSectionExclEnd(
                node.getBeginColumn() + beforeParamsLength, node.getBeginLine(),
                param.getBeginColumn(), param.getBeginLine()));
    }

    private void printWithEnclosedSkippedTokens(
            String beforeSkippedTokens, String afterSkippedTokens, TemplateObject node)
            throws ConverterException {
        print(beforeSkippedTokens);
        String skippedTokens = getSrcSectionExclEnd(
                node.getBeginColumn() + beforeSkippedTokens.length(), node.getBeginLine(),
                node.getEndColumn() - afterSkippedTokens.length() + 1, node.getEndLine());
        print(skippedTokens);
        print(afterSkippedTokens);
    }

    private void printWithParamsTrailingSkippedTokens(
            String afterParams, TemplateObject node, int lastVisualParamIdx) throws
            ConverterException {
        int parameterCount = node.getParameterCount();
        assertNodeContent(lastVisualParamIdx < parameterCount, node,
                "Parameter count too low: {}", parameterCount);
        TemplateObject param = getParam(node, lastVisualParamIdx, null, TemplateObject.class);
        String skippedTokens = getSrcSectionExclEnd(
                param.getEndColumn() + 1, param.getEndLine(),
                node.getEndColumn() - afterParams.length() + 1, node.getEndLine());
        print(skippedTokens);
        print(afterParams);
    }

    /**
     * Prints the part between the last parameter (or the directive name if there are no parameters) and the tag closer
     * character, for a start tag. That part may contains whitespace or comments, which aren't visible in the AST.
     */
    private void printStartTagSkippedTokens(TemplateElement node, TemplateObject lastParam, boolean trimSlash)
            throws UnexpectedNodeContentException {
        int pos;
        if (lastParam == null) {
            // No parameter; must skip the tag name
            pos = getPosition(node.getBeginColumn(), node.getBeginLine());
            {
                char c = src.charAt(pos++);
                assertNodeContent(c == tagBeginChar, node,
                        "tagBeginChar expected, found '{}'", c);
            }
            {
                char c = src.charAt(pos++);
                assertNodeContent(c == '#', node,
                        "'#' expected, found '{}'", c);
            }
            findNameEnd: while (pos < src.length()) {
                char c = src.charAt(pos);
                if (!isCoreNameChar(c)) {
                    break findNameEnd;
                }
                pos++;
            }
        } else {
            pos = getPosition(lastParam.getEndColumn() + 1, lastParam.getEndLine());
        }
        final int startPos = pos;

        while (pos < src.length()) {
            char c = src.charAt(pos);
            if ((c == '<' || c == '[')
                    && (src.startsWith("!--", pos + 1) || src.startsWith("#--", pos + 1))) {
                pos += 4;
                scanForCommentEnd: while (pos < src.length()) {
                    if (src.startsWith("-->", pos) || src.startsWith("--]", pos)) {
                        pos += 3;
                        break scanForCommentEnd;
                    }
                    pos++;
                }
                if (pos == src.length()) {
                    throw new UnexpectedNodeContentException(node, "Can't find comment end in the start tag", null);
                }
            } else if (c == '/' && pos + 1 < src.length() && src.charAt(pos + 1) == tagEndChar) {
                print(src.substring(startPos, trimSlash ? pos : pos + 1));
                return;
            } else if (c == tagEndChar) {
                print(src.substring(startPos, pos));
                return;
            } else if (Character.isWhitespace(c)) {
                pos++;
            } else {
                throw new UnexpectedNodeContentException(node,
                        "Unexpected character when scanning for tag end: '{}'", c);
            }
        }
        throw new UnexpectedNodeContentException(node, "Can't find start tag end", null);
    }

    private void printEndTagSkippedTokens(TemplateElement node) throws UnexpectedNodeContentException {
        int tagEndPos = getPosition(node.getEndColumn(), node.getEndLine());
        {
            char c = src.charAt(tagEndPos);
            assertNodeContent(c == tagEndChar, node,
                    "tagEndChar expected, found '{}'", c);
        }

        int pos = tagEndPos - 1;
        while (pos > 0 && Character.isWhitespace(src.charAt(pos))) {
            pos--;
        }

        assertNodeContent(pos > 0 && isCoreNameChar(src.charAt(pos)), node,
                "Can't find end tag name", null);

        print(src.substring(pos + 1, tagEndPos));
    }

    private void print(String s) {
        _NullArgumentException.check("s", s);
        out.append(s);
    }

    private void print(char c) {
        out.append(c);
    }

    @SuppressWarnings("unchecked")
    private <T> T getOnlyParam(TemplateObject node, ParameterRole role, Class<T> valueClass)
            throws UnexpectedNodeContentException {
        int parameterCount = node.getParameterCount();
        assertNodeContent(parameterCount == 1, node,
                "Node expected to have exactly 1 parameter, but had {}.", parameterCount);
        return (T) getParam(node, 0, role, valueClass);
    }

    @SuppressWarnings("unchecked")
    private <T> T getParam(TemplateObject node, int index, ParameterRole expectedParamRole, Class<T>
            valueClass) throws UnexpectedNodeContentException {
        int paramCount = node.getParameterCount();
        assertNodeContent(paramCount > index, node,
                "Node only have {} parameters.", paramCount);

        if (expectedParamRole != null) {
            assertParamRole(node, index, expectedParamRole);
        }

        Object paramValue = node.getParameterValue(index);
        if (paramValue != null) {
            assertNodeContent(valueClass.isInstance(paramValue), node,
                    "Unexpected node parameter value class: {}",
                    paramValue == null ? "null" : paramValue.getClass());
        }
        return (T) paramValue;
    }

    private TemplateElement getOnlyChild(TemplateElement node) throws ConverterException {
        int childCount = node.getChildCount();
        assertNodeContent(childCount == 1, node,
                "Node should have exactly 1 child, but had {}.", childCount);
        return node.getChild(0);
    }

    private void assertParamCount(TemplateObject node, int expectedParamCount)
            throws UnexpectedNodeContentException {
        int paramCount = node.getParameterCount();
        assertNodeContent(paramCount == expectedParamCount, node,
                "Unexpected parameter count, {}.", paramCount);
    }

    private void assertParamRole(TemplateObject node, int index, ParameterRole expectedParamRole)
            throws UnexpectedNodeContentException {
        ParameterRole paramRole = node.getParameterRole(index);
        assertNodeContent(paramRole == expectedParamRole, node,
                "Unexpected node parameter role \"{}\".", paramRole);
    }

    private void assertNodeContent(boolean good, TemplateObject node, String
            errorMessage, Object msgParam) throws UnexpectedNodeContentException {
        if (!good) {
            throw new UnexpectedNodeContentException(node, errorMessage, msgParam);
        }
    }

    /**
     * Returns the position of a character in the {@link #src} string.
     *
     * @param column 1-based column
     * @param row 1-based row
     */
    private int getPosition(int column, int row) {
        if (rowStartPositions == null) {
            rowStartPositions = new ArrayList<>();
            rowStartPositions.add(0);
            for (int i = 0; i < src.length(); i++) {
                char c = src.charAt(i);
                if (c == '\n' || c == '\r') {
                    if (c == '\r' && i + 1 < src.length() && src.charAt(i + 1) == '\n') {
                        i++; // Skip +1 character as this is a CRLF
                    }
                    rowStartPositions.add(i + 1);
                }
            }
        }
        return rowStartPositions.get(row - 1) + column - 1;
    }

    private String getSrcSectionExclEnd(int startColumn, int startRow, int exclEndColumn, int endRow) {
        return src.substring(getPosition(startColumn, startRow), getPosition(exclEndColumn, endRow));
    }

    private boolean isCoreNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

}
