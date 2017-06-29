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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.freemarker.converter.ConverterException;
import org.apache.freemarker.converter.ConverterUtils;
import org.apache.freemarker.core.NamingConvention;
import org.apache.freemarker.core.util.FTLUtil;
import org.apache.freemarker.core.util._ClassUtil;
import org.apache.freemarker.core.util._NullArgumentException;
import org.apache.freemarker.core.util._StringUtil;

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
 * ensure that no parameter remains unhandled. If you know you don't need some of the parameters, still at least call
 * {@link #assertParamRole(TemplateObject, int, ParameterRole)} for them. These ensure that if new parameters are
 * added in FreeMarker 2.x, no information will be silently lost during conversion.
 * <li>At many places you will see that we meticulously extract parts from the source, piece by piece print it to the
 * output, but at the end we just end up with the same text that could have been copied from the source. The idea is
 * that as the FM3 template language evolves, we will have to change the output for some pieces.
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
            //!!T new org.apache.freemarker.core.Template(null, s, fm3Config);
        } catch (Exception e) {
            throw new ConverterException(
                    "The result of the conversion wasn't valid FreeMarker 3 template; see cause exception", e);
        }
        return s;
    }

    private void printNode(TemplateObject node) throws ConverterException {
        if (node instanceof TemplateElement) {
            printTemplateElement((TemplateElement) node);
        } else if (node instanceof Expression) {
            printExp((Expression) node);
        } else {
            throw new UnexpectedNodeContentException(node, "Unhandled node class",
                    _ClassUtil.getShortClassNameOfObject(node));
        }
    }

    private void printTemplateElement(TemplateElement node) throws ConverterException {
        if (node instanceof MixedContent) {
            printChildrenElements(node);
        } else if (node instanceof TextBlock) {
            print(getOnlyParam(node, ParameterRole.CONTENT, String.class));
        } else if (node instanceof DollarVariable) {
            printDollarInterpolation((DollarVariable) node);
        } else if (node instanceof NumericalOutput) {
            printNumericalInterpolation((NumericalOutput) node);
        } else if (node instanceof Comment) {
            print(tagBeginChar);
            print("#--");
            print(getOnlyParam(node, ParameterRole.CONTENT, String.class));
            print("--");
            print(tagEndChar);
        } else {
            printDir(node);
        }
    }

    private void printNumericalInterpolation(NumericalOutput node) throws ConverterException {
        printWithParamsLeadingSkippedTokens("${", node);
        Expression content = getParam(node, 0, ParameterRole.CONTENT, Expression.class);

        int pos = getPositionAfterWSAndExpComments(getEndPositionExclusive(content));
        assertNodeContent(pos < src.length(), node, "Unexpected EOF", null);
        char c = src.charAt(pos);
        assertNodeContent(c == ';' || c == '}', node, "Expected ';' or '}', found '{}'", c);
        if (c == ';') { // #{exp; m1M2} -> ${exp?string('0.0#')}
            int minDecimals = getParam(node, 1, ParameterRole.MINIMUM_DECIMALS, Integer.class);
            int maxDecimals = getParam(node, 2, ParameterRole.MAXIMUM_DECIMALS, Integer.class);

            boolean needsParen = !needsNoParenthesisAsBuiltInLHO(content);
            if (needsParen) {
                print('(');
            }
            printNode(content);
            if (needsParen) {
                print(')');
            }
            print("?string('0.");
            for (int i = 0; i < minDecimals; i++) {
                print('0');
            }
            for (int i = minDecimals; i < maxDecimals; i++) {
                print('#');
            }
            print("')}");
        } else { // #{exp} -> ${exp}
            printNode(content);
            printWithParamsTrailingSkippedTokens("}", node, 0);
        }
    }

    private boolean needsNoParenthesisAsBuiltInLHO(Expression exp) {
        return
                exp instanceof Identifier
                || exp instanceof NumberLiteral
                || exp instanceof BooleanLiteral
                || exp instanceof StringLiteral
                || exp instanceof ListLiteral
                || exp instanceof HashLiteral
                || exp instanceof ParentheticalExpression
                || exp instanceof MethodCall
                || exp instanceof DynamicKeyName
                || exp instanceof BuiltIn
                || exp instanceof BuiltinVariable
                || exp instanceof Dot;
    }

    private void printDollarInterpolation(DollarVariable node) throws ConverterException {
        printWithParamsLeadingSkippedTokens("${", node);
        printNode(getOnlyParam(node, ParameterRole.CONTENT, Expression.class));
        printWithParamsTrailingSkippedTokens("}", node, 0);
    }

    /**
     * Prints a directive
     */
    private void printDir(TemplateElement node) throws ConverterException {
        if (node instanceof IfBlock) {
            printDirIfElseElseIfContainer((IfBlock) node);
        } else if (node instanceof ConditionalBlock) {
            printDirIfOrElseOrElseIf((ConditionalBlock) node);
        } else if (node instanceof UnifiedCall) {
            printDirCustom((UnifiedCall) node);
        } else {
            throw new ConverterException("Unhandled AST TemplateElement class: " + node.getClass().getName());
        }
    }

    private void printDirCustom(UnifiedCall node) throws ConverterException {
        print(tagBeginChar);
        print('@');

        Expression callee = getParam(node, 0, ParameterRole.CALLEE, Expression.class);
        printExp(callee);

        Expression lastPrintedExp = callee;
        int paramIdx = 1;
        int paramCount = node.getParameterCount();

        // Print positional arguments:
        while (paramIdx < paramCount && node.getParameterRole(paramIdx) == ParameterRole.ARGUMENT_VALUE) {
            Expression argValue = getParam(node, paramIdx, ParameterRole.ARGUMENT_VALUE, Expression.class);

            printParameterSeparatorSource(lastPrintedExp, argValue);
            printExp(argValue);

            lastPrintedExp = argValue;
            paramIdx++;
        }

        // Print named arguments:
        while (paramIdx < paramCount
                && node.getParameterRole(paramIdx) == ParameterRole.ARGUMENT_NAME) {
            Expression argValue = getParam(node, paramIdx + 1, ParameterRole.ARGUMENT_VALUE, Expression.class);

            printParameterSeparatorSource(lastPrintedExp, argValue); // Prints something like " someArgName="
            printExp(argValue);

            lastPrintedExp = argValue;
            paramIdx += 2;
        }

        // Print loop variables:
        int pos = getEndPositionExclusive(lastPrintedExp);
        boolean beforeFirstLoopVar = true;
        while (paramIdx < paramCount) {
            String sep = readExpWSAndSeparator(pos, beforeFirstLoopVar ? ';' : ',', false);
            assertNodeContent(sep.length() != 0, node,
                    "Can't find loop variable separator", null);
            printWithConvertedExpComments(sep);
            pos += sep.length();

            String loopVarName = getParam(node, paramIdx, ParameterRole.TARGET_LOOP_VARIABLE, String.class);
            print(_StringUtil.toFTLTopLevelIdentifierReference(loopVarName));
            String identifierInSrc = readIdentifier(pos);
            assertNodeContent(identifierInSrc.length() != 0, node,
                    "Can't find loop variable identifier in source", null);
            pos += identifierInSrc.length(); // skip loop var name

            beforeFirstLoopVar = false;
            paramIdx++;
        }

        int startTagEndPos = printStartTagSkippedTokens(node, pos, false);
        print(tagEndChar);

        int elementEndPos = getEndPositionInclusive(node);
        {
            char c = src.charAt(elementEndPos);
            assertNodeContent(c == tagEndChar, node,
                    "tagEndChar expected, found '{}'", c);
        }
        if (startTagEndPos != elementEndPos) { // We have an end-tag
            assertNodeContent(src.charAt(startTagEndPos - 1) != '/', node,
                    "Not expected \"/\" at the end of the start tag", null);
            printChildrenElements(node);

            print(tagBeginChar);
            print("/@");
            int nameStartPos = elementEndPos; // Not 1 less; consider the case of </@>
            while (nameStartPos >= 2 && !src.startsWith("/@", nameStartPos - 2)) {
                nameStartPos--;
            }
            assertNodeContent(nameStartPos >= 2, node,
                    "Couldn't extract name from end-tag.", null);
            // Also prints ignored WS after name, for now:
            printWithConvertedExpComments(src.substring(nameStartPos, elementEndPos));
            print(tagEndChar);
        } else { // We don't have end-tag
            assertNodeContent(src.charAt(startTagEndPos - 1) == '/', node,
                    "Expected \"/\" at the end of the start tag", null);
            assertNodeContent(node.getChildCount() == 0, node,
                    "Expected no children", null);
        }
    }

    private void printDirIfOrElseOrElseIf(ConditionalBlock node) throws ConverterException {
        assertParamCount(node, 2);
        Expression conditionExp = getParam(node, 0, ParameterRole.CONDITION, Expression.class);
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
    }

    private void printDirIfElseElseIfContainer(IfBlock node) throws ConverterException {
        printChildrenElements(node);
        print(tagBeginChar);
        print("/#if");
        printEndTagSkippedTokens(node);
        print(tagEndChar);
    }

    /**
     * Prints an expression
     */
    private void printExp(Expression node) throws ConverterException {
        if (node instanceof Identifier) {
            printExpIdentifier((Identifier) node);
        } else if (node instanceof NumberLiteral) {
            printExpNumericalLiteral((NumberLiteral) node);
        } else if (node instanceof BooleanLiteral) {
            printExpBooleanLiteral((BooleanLiteral) node);
        } else if (node instanceof StringLiteral) {
            printExpStringLiteral((StringLiteral) node);
        } else if (node instanceof ListLiteral) {
            printExpListLiteral((ListLiteral) node);
        } else if (node instanceof HashLiteral) {
            printExpHashLiteral((HashLiteral) node);
        } else if (node instanceof Range) {
            printExpRange((Range) node);
        } else if (node instanceof AddConcatExpression) {
            printExpAddConcat((AddConcatExpression) node);
        } else if (node instanceof ArithmeticExpression) {
            printExpArithmetic((ArithmeticExpression) node);
        } else if (node instanceof UnaryPlusMinusExpression) {
            printExpUnaryPlusMinus((UnaryPlusMinusExpression) node);
        } else if (node instanceof ParentheticalExpression) {
            printExpParenthetical((ParentheticalExpression) node);
        } else if (node instanceof MethodCall) {
            printExpMethodCall((MethodCall) node);
        } else if (node instanceof DynamicKeyName) {
            printExpDynamicKeyName((DynamicKeyName) node);
        } else if (node instanceof BuiltIn) {
            printExpBuiltIn((BuiltIn) node);
        } else if (node instanceof BuiltinVariable) {
            printExpBuiltinVariable((BuiltinVariable) node);
        } else if (node instanceof Dot) {
            printExpDot((Dot) node);
        } else if (node instanceof ComparisonExpression) {
            printExpComparison((ComparisonExpression) node);
        } else if (node instanceof AndExpression) {
            printExpAnd((AndExpression) node);
        } else if (node instanceof OrExpression) {
            printExpOr((OrExpression) node);
        } else if (node instanceof NotExpression) {
            printExpNot((NotExpression) node);
        } else {
            throw new ConverterException("Unhandled AST node expression class: " + node.getClass().getName());
        }
    }

    private void printExpNot(NotExpression node) throws ConverterException {
        printWithParamsLeadingSkippedTokens("!", node);
        printNode(getOnlyParam(node, ParameterRole.RIGHT_HAND_OPERAND, Expression.class));
    }

    private static final Map<String, String> COMPARATOR_OP_MAP;
    static {
        COMPARATOR_OP_MAP = new HashMap<String, String>();
        // For now we leave FM2 ops as is, but later in many cases they will be replaced.
        COMPARATOR_OP_MAP.put("==", "==");
        COMPARATOR_OP_MAP.put("=", "=");
        COMPARATOR_OP_MAP.put("!=", "!=");
        COMPARATOR_OP_MAP.put("<", "<");
        COMPARATOR_OP_MAP.put("lt", "lt");
        COMPARATOR_OP_MAP.put("\\lt", "\\lt");
        COMPARATOR_OP_MAP.put("&lt;", "&lt;");
        COMPARATOR_OP_MAP.put("<=", "<=");
        COMPARATOR_OP_MAP.put("lte", "lte");
        COMPARATOR_OP_MAP.put("\\lte", "\\lte");
        COMPARATOR_OP_MAP.put("&lt;=", "&lt;=");
        COMPARATOR_OP_MAP.put(">", ">");
        COMPARATOR_OP_MAP.put("gt", "gt");
        COMPARATOR_OP_MAP.put("\\gt", "\\gt");
        COMPARATOR_OP_MAP.put("&gt;", "&gt;");
        COMPARATOR_OP_MAP.put(">=", ">=");
        COMPARATOR_OP_MAP.put("gte", "gte");
        COMPARATOR_OP_MAP.put("\\gte", "\\gte");
        COMPARATOR_OP_MAP.put("&gt;=", "&gt;=");
    }

    private void printExpComparison(ComparisonExpression node) throws ConverterException {
        printExpBinaryWithMappedOperator(node, COMPARATOR_OP_MAP);
    }

    private static final Map<String, String> AND_OP_MAP;
    static {
        AND_OP_MAP = new HashMap<String, String>();
        // For now we leave FM2 ops as is, but later in many cases they will be replaced.
        AND_OP_MAP.put("&&", "&&");
        AND_OP_MAP.put("&", "&");
        AND_OP_MAP.put("\\and", "\\and");
        AND_OP_MAP.put("&amp;&amp;", "&amp;&amp;");
    }

    private void printExpAnd(AndExpression node) throws ConverterException {
        printExpBinaryWithMappedOperator(node, AND_OP_MAP);
    }

    private static final Map<String, String> OR_OP_MAP;
    static {
        OR_OP_MAP = new HashMap<String, String>();
        // For now we leave FM2 ops as is, but later in many cases they will be replaced.
        OR_OP_MAP.put("||", "||");
        OR_OP_MAP.put("|", "|");
    }

    private void printExpOr(OrExpression node) throws ConverterException {
        printExpBinaryWithMappedOperator(node, OR_OP_MAP);
    }

    private void printExpBinaryWithMappedOperator(Expression node, Map<String, String> operatorMapper) throws
            ConverterException {
        assertParamCount(node, 2);
        Expression lho = getParam(node, 0, ParameterRole.LEFT_HAND_OPERAND, Expression.class);
        Expression rho = getParam(node, 1, ParameterRole.RIGHT_HAND_OPERAND, Expression.class);

        printExp(lho);

        int lhoEndExcl = getEndPositionExclusive(lho);
        int opStart = getPositionAfterWSAndExpComments(lhoEndExcl);
        printWithConvertedExpComments(src.substring(lhoEndExcl, opStart));
        final String fm2Op = readUntilWSOrComment(opStart);
        String fm3Op = operatorMapper.get(fm2Op);
        if (fm3Op == null) {
            throw new UnexpectedNodeContentException(node, "Unhandled operator: {}", fm2Op);
        }
        print(fm3Op);
        printWithConvertedExpComments(src.substring(opStart + fm2Op.length(), getStartPosition(rho)));

        printExp(rho);
    }

    private void printExpBuiltinVariable(BuiltinVariable node) throws ConverterException {
        int startPos = getStartPosition(node);
        String sep = readExpWSAndSeparator(startPos, '.', false);
        printWithConvertedExpComments(sep);
        String name = src.substring(startPos + sep.length(), getEndPositionExclusive(node));
        print(convertBuiltInVariableName(name));
    }

    private String convertBuiltInVariableName(String name) throws ConverterException {
        String converted = name.indexOf('_') == -1 ? name : ConverterUtils.snakeCaseToCamelCase(name);

        // Will replace removed names here

        return converted;
    }

    private void printExpDot(Dot node) throws ConverterException {
        assertParamCount(node, 2);
        Expression lho = getParam(node, 0, ParameterRole.LEFT_HAND_OPERAND, Expression.class);
        String rho = getParam(node, 1, ParameterRole.RIGHT_HAND_OPERAND, String.class);
        printNode(lho);
        printWithConvertedExpComments(
                readExpWSAndSeparator(getEndPositionExclusive(lho), '.', false));
        print(FTLUtil.escapeIdentifier(rho));
    }

    private void printExpDynamicKeyName(DynamicKeyName node) throws ConverterException {
        assertParamCount(node, 2);

        Expression collExp = getParam(node, 0, ParameterRole.LEFT_HAND_OPERAND, Expression.class);
        printExp(collExp);

        Expression keyExp = getParam(node, 1, ParameterRole.ENCLOSED_OPERAND, Expression.class);
        printParameterSeparatorSource(collExp, keyExp);
        printExp(keyExp);

        printWithParamsTrailingSkippedTokens("]", node, 1);
    }

    private void printExpRange(Range node) throws ConverterException {
        assertParamCount(node, 2);

        Expression lho = getParam(node, 0, ParameterRole.LEFT_HAND_OPERAND, Expression.class);
        Expression rho = getParam(node, 1, ParameterRole.RIGHT_HAND_OPERAND, Expression.class);

        printExp(lho);

        printWithConvertedExpComments(src.substring(
                getEndPositionExclusive(lho),
                rho != null ? getStartPosition(rho) : getEndPositionExclusive(node)));

        if (rho != null) {
            printExp(rho);
        }
    }

    private void printExpHashLiteral(HashLiteral node) throws ConverterException {
        int openCharPos = getStartPosition(node);
        int closeCharPos = getEndPositionInclusive(node);
        assertNodeContent(src.charAt(openCharPos) == '{', node,
                "Expected '{'", null);
        assertNodeContent(src.charAt(closeCharPos) == '}', node,
                "Expected '}'", null);

        int paramCnt = node.getParameterCount();
        if (paramCnt == 0) {
            print('{');
            printWithConvertedExpComments(src.substring(openCharPos + 1, closeCharPos));
            print('}');
        } else {
            printWithParamsLeadingSkippedTokens("{", node);
            Expression prevValue = null;
            for (int paramIdx = 0; paramIdx < paramCnt; paramIdx += 2) {
                Expression key = getParam(node, paramIdx, ParameterRole.ITEM_KEY, Expression.class);
                Expression value = getParam(node, paramIdx + 1, ParameterRole.ITEM_VALUE, Expression.class);

                if (prevValue != null) {
                    printParameterSeparatorSource(prevValue, key);
                }
                printExp(key);
                printParameterSeparatorSource(key, value);
                printExp(value);

                prevValue = value;
            }
            printWithParamsTrailingSkippedTokens("}", node, node.getParameterCount() - 1);
        }
    }

    private void printExpListLiteral(ListLiteral node) throws ConverterException {
        int openCharPos = getStartPosition(node);
        int closeCharPos = getEndPositionInclusive(node);
        assertNodeContent(src.charAt(openCharPos) == '[', node,
                "Expected '['", null);
        assertNodeContent(src.charAt(closeCharPos) == ']', node,
                "Expected ']'", null);

        int paramCnt = node.getParameterCount();
        if (paramCnt == 0) {
            print('[');
            printWithConvertedExpComments(src.substring(openCharPos + 1, closeCharPos));
            print(']');
        } else {
            printWithParamsLeadingSkippedTokens("[", node);
            Expression prevItem = null;
            for (int paramIdx = 0; paramIdx < paramCnt; paramIdx++) {
                Expression item = getParam(node, paramIdx, ParameterRole.ITEM_VALUE, Expression.class);

                if (prevItem != null) {
                    printParameterSeparatorSource(prevItem, item);
                }
                printExp(item);

                prevItem = item;
            }
            printWithParamsTrailingSkippedTokens("]", node, node.getParameterCount() - 1);
        }
    }

    private void printExpParenthetical(ParentheticalExpression node) throws ConverterException {
        printWithParamsLeadingSkippedTokens("(", node);
        printNode(getOnlyParam(node, ParameterRole.ENCLOSED_OPERAND, Expression.class));
        printWithParamsTrailingSkippedTokens(")", node, 0);
    }

    private void printExpUnaryPlusMinus(UnaryPlusMinusExpression node) throws ConverterException {
        assertParamCount(node, 2);
        assertParamRole(node, 1, ParameterRole.AST_NODE_SUBTYPE);
        printWithParamsLeadingSkippedTokens(node.getNodeTypeSymbol().substring(0, 1), node);
        printNode(getParam(node, 0, ParameterRole.RIGHT_HAND_OPERAND, Expression.class));
    }

    private void printExpArithmetic(ArithmeticExpression node) throws ConverterException {
        assertParamCount(node, 3);
        assertParamRole(node, 2, ParameterRole.AST_NODE_SUBTYPE);
        Expression lho = getParam(node, 0, ParameterRole.LEFT_HAND_OPERAND, Expression.class);
        Expression rho = getParam(node, 1, ParameterRole.RIGHT_HAND_OPERAND, Expression.class);
        printNode(lho);
        printParameterSeparatorSource(lho, rho);
        printNode(rho);
    }

    private void printExpAddConcat(AddConcatExpression node) throws ConverterException {
        assertParamCount(node, 2);
        Expression lho = getParam(node, 0, ParameterRole.LEFT_HAND_OPERAND, Expression.class);
        Expression rho = getParam(node, 1, ParameterRole.RIGHT_HAND_OPERAND, Expression.class);
        printNode(lho);
        printParameterSeparatorSource(lho, rho);
        printNode(rho);
    }

    private void printExpBooleanLiteral(BooleanLiteral node) {
        print(node.getCanonicalForm());
    }

    private void printExpNumericalLiteral(NumberLiteral node) {
        print(getSrcSectionInclEnd(
                node.getBeginColumn(), node.getBeginLine(),
                node.getEndColumn(), node.getEndLine()));
    }

    private void printExpIdentifier(Identifier node) {
        print(FTLUtil.escapeIdentifier(node.getName()));
    }

    private void printExpMethodCall(MethodCall node) throws ConverterException {
        Expression callee = getParam(node, 0, ParameterRole.CALLEE, Expression.class);
        printExp(callee);

        Expression prevParam = callee;
        int argCnt = node.getParameterCount() - 1;
        for (int argIdx = 0; argIdx < argCnt; argIdx++) {
            Expression argExp = getParam(node, argIdx + 1, ParameterRole.ARGUMENT_VALUE, Expression.class);
            printParameterSeparatorSource(prevParam, argExp);
            printExp(argExp);
            prevParam = argExp;
        }
        printWithParamsTrailingSkippedTokens(")", node, argCnt);
    }

    private void printExpBuiltIn(BuiltIn node) throws ConverterException {
        assertParamCount(node, 2);
        Expression lho = getParam(node, 0, ParameterRole.LEFT_HAND_OPERAND, Expression.class);
        String rho = getParam(node, 1, ParameterRole.RIGHT_HAND_OPERAND, String.class);

        printExp(lho); // [lho]?biName

        int postLHOPos = getEndPositionExclusive(lho);
        int endPos = getEndPositionInclusive(node);
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
    }

    private void printExpStringLiteral(StringLiteral node) throws ConverterException {
        boolean rawString = false;
        char quote;
        {
            int pos = getStartPosition(node);
            quote = src.charAt(pos);
            while ((quote == '\\' || quote == '{' /* 2.3.26 bug workaround */ || quote == 'r')
                    && pos < src.length()) {
                pos++;
                if (quote == 'r') {
                    rawString = true;
                }
                quote = src.charAt(pos);
            }
            if (quote != '\'' && quote != '"') {
                throw new UnexpectedNodeContentException(node, "Unexpected string quote character: {}", quote);
            }
        }
        if (rawString) {
            print('r');
        }
        print(quote);

        int parameterCount = node.getParameterCount();
        if (parameterCount == 0) {
            if (!rawString) {
                print(FTLUtil.escapeStringLiteralPart(node.getAsString(), quote));
            } else {
                print(node.getAsString());
            }
        } else {
            // Not really a literal; contains interpolations
            for (int paramIdx = 0; paramIdx < parameterCount; paramIdx++) {
                Object param = getParam(node, paramIdx, ParameterRole.VALUE_PART, Object.class);
                if (param instanceof String) {
                    print(FTLUtil.escapeStringLiteralPart((String) param));
                } else {
                    assertNodeContent(param instanceof Interpolation, node,
                            "Unexpected parameter type: {}", param.getClass().getName());

                    // We print the interpolation, the cut it out from the output, then put it back escaped:
                    int interpStartPos = out.length();
                    printNode((TemplateElement) param);
                    int interpEndPos = out.length();
                    String interp = out.substring(interpStartPos, interpEndPos);
                    out.setLength(interpStartPos + 2); // +2 to keep the "${"
                    String inerpInside = interp.substring(2, interp.length() - 1);
                    print(FTLUtil.escapeStringLiteralPart(inerpInside, quote)); // For now we escape as FTL2
                    print(interp.charAt(interp.length() - 1)); // "}"
                }
            }
        }

        print(quote);
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

    private void printParameterSeparatorSource(Expression lho, Expression rho) {
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
        Expression param = getParam(node, 0, null, Expression.class);
        printWithConvertedExpComments(getSrcSectionExclEnd(
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
        printWithConvertedExpComments(skippedTokens);
        print(afterSkippedTokens);
    }

    private void printWithParamsTrailingSkippedTokens(
            String afterParams, TemplateObject node, int lastVisualParamIdx) throws
            ConverterException {
        int parameterCount = node.getParameterCount();
        assertNodeContent(lastVisualParamIdx < parameterCount, node,
                "Parameter count too low: {}", parameterCount);
        Expression param = getParam(node, lastVisualParamIdx, null, Expression.class);
        String skippedTokens = getSrcSectionExclEnd(
                param.getEndColumn() + 1, param.getEndLine(),
                node.getEndColumn() - afterParams.length() + 1, node.getEndLine());
        printWithConvertedExpComments(skippedTokens);
        print(afterParams);
    }

    /**
     * Prints the part between the last parameter (or the directive name if there are no parameters) and the tag closer
     * character, for a start tag. That part may contains whitespace or comments, which aren't visible in the AST.
     *
     * @return The position of the last character of the start tag. Note that the printed string never includes this
     *         character.
     */
    private int printStartTagSkippedTokens(TemplateElement node, Expression lastParam, boolean trimSlash)
            throws ConverterException {
        int pos;
        if (lastParam == null) {
            // No parameter; must skip the tag name
            pos = getStartPosition(node);
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
        return printStartTagSkippedTokens(node, pos, trimSlash);
    }

    /**
     * Similar to {@link #printStartTagSkippedTokens(TemplateElement, Expression, boolean)}, but with explicitly
     * specified scan start position.
     *
     * @param pos The position where the first skipped character can occur (or the tag end character).
     */
    private int printStartTagSkippedTokens(TemplateElement node, int pos, boolean trimSlash)
            throws ConverterException {
        final int startPos = pos;

        pos = getPositionAfterWSAndExpComments(pos);
        if (pos == src.length()) {
            throw new UnexpectedNodeContentException(node,
                    "End of source reached when scanning for tag end", null);
        }

        char c = src.charAt(pos);
        if (c == '/' && pos + 1 < src.length() && src.charAt(pos + 1) == tagEndChar) {
            printWithConvertedExpComments(src.substring(startPos, trimSlash ? pos : pos + 1));
            return pos + 1;
        } else if (c == tagEndChar) {
            printWithConvertedExpComments(src.substring(startPos, pos));
            return pos;
        } else {
            throw new UnexpectedNodeContentException(node,
                    "Unexpected character when scanning for tag end: '{}'", c);
        }
    }

    private void printEndTagSkippedTokens(TemplateElement node) throws UnexpectedNodeContentException {
        int tagEndPos = getEndPositionInclusive(node);
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

        printWithConvertedExpComments(src.substring(pos + 1, tagEndPos));
    }

    private void printWithConvertedExpComments(String s) {
        // Later we might want to convert comment syntax here
        print(s);
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

    private int getStartPosition(TemplateObject node) {
        return getPosition(node.getBeginColumn(), node.getBeginLine());
    }

    private int getEndPositionInclusive(TemplateObject node) {
        return getPosition(node.getEndColumn(), node.getEndLine());
    }

    private int getEndPositionExclusive(TemplateObject node) {
        return getEndPositionInclusive(node) + 1;
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

    private String getSrcSectionInclEnd(int startColumn, int startRow, int exclEndColumn, int endRow) {
        return src.substring(getPosition(startColumn, startRow), getPosition(exclEndColumn, endRow) + 1);
    }

    private String getSrcSectionExclEnd(int startColumn, int startRow, int exclEndColumn, int endRow) {
        return src.substring(getPosition(startColumn, startRow), getPosition(exclEndColumn, endRow));
    }

    private boolean isCoreNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /**
     * @return Position after the whitespace and comments, or the argument position if there were node
     */
    private int getPositionAfterWSAndExpComments(int pos) throws ConverterException {
        scanForNoWSNoComment: while (pos < src.length()) {
            char c = src.charAt(pos);
            if (isExpCommentStart(pos)) {
                pos += 4; // length of "<#--"
                scanForCommentEnd:
                while (pos < src.length()) {
                    if (src.startsWith("-->", pos) || src.startsWith("--]", pos)) {
                        pos += 3;
                        break scanForCommentEnd;
                    }
                    pos++;
                }
                if (pos == src.length()) {
                    throw new ConverterException("Can't find comment end at " + pos, null);
                }
            } else if (Character.isWhitespace(c)) {
                pos++;
            } else {
                break scanForNoWSNoComment;
            }
        }
        return pos;
    }


    private String readExpWSAndSeparator(int startPos, char separator, boolean separatorOptional)
            throws ConverterException {
        int pos = getPositionAfterWSAndExpComments(startPos);

        if (pos == src.length() || src.charAt(pos) != separator) {
            // No separator
            return separatorOptional ? src.substring(startPos, pos) : "";
        }
        pos++; // Skip separator

        pos = getPositionAfterWSAndExpComments(pos);

        return src.substring(startPos, pos);
    }

    private String readIdentifier(int startPos) throws ConverterException {
        int pos = startPos;
        scanUntilIdentifierEnd: while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '\\') {
                if (pos + 1 == src.length()) {
                    throw new ConverterException("Misplaced \"\\\" at position " + pos);
                }
                if (!FTLUtil.isEscapedIdentifierCharacter(src.charAt(pos + 1))) {
                    throw new ConverterException("Invalid escape at position " + pos);
                }
                pos += 2; // to skip escaped character
            } else if (pos == startPos && FTLUtil.isNonEscapedIdentifierStart(c)
                    || FTLUtil.isNonEscapedIdentifierPart(c)) {
                pos++;
            } else {
                break scanUntilIdentifierEnd;
            }
        }
        return src.substring(startPos, pos);
    }

    private String readUntilWSOrComment(int startPos) throws ConverterException {
        int pos = startPos;
        while (pos < src.length() && !Character.isWhitespace(src.charAt(pos)) && !isExpCommentStart(pos)) {
            pos++;
        }
        return src.substring(startPos, pos);
    }

    private boolean isExpCommentStart(int pos) {
        char c = src.charAt(pos);
        return (c == '<' || c == '[')
                && (pos + 1 < src.length()
                && src.startsWith("!--", pos + 1) || src.startsWith("#--", pos + 1));

    }

}
