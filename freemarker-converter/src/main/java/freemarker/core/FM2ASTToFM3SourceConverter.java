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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.freemarker.converter.ConversionMarkers;
import org.apache.freemarker.converter.ConverterException;
import org.apache.freemarker.converter.ConverterUtils;
import org.apache.freemarker.converter.UnconvertableLegacyFeatureException;
import org.apache.freemarker.core.util.FTLUtil;
import org.apache.freemarker.core.util._ClassUtil;
import org.apache.freemarker.core.util._NullArgumentException;
import org.apache.freemarker.core.util._StringUtil;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.ext.dom.AtAtKeyAccessor;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.utility.StringUtil;

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

    private final Template template;
    private final StringTemplateLoader overlayTemplateLoader;
    private final String src;
    private final ConversionMarkers markers;

    private final StringBuilder out;
    private List<Integer> rowStartPositions;
    private final char tagBeginChar;
    private final char tagEndChar;
    private final Set<String> fm3BuiltInNames = new org.apache.freemarker.core.Configuration
            .Builder(org.apache.freemarker.core.Configuration.getVersion() /* highest possible by design */)
            .build().getSupportedBuiltInNames();

    private boolean printNextCustomDirAsFtlDir;

    /**
     * @param fm2Cfg
     *         The {@link Configuration} used for parsing; {@link Configuration#getWhitespaceStripping()} must return
     *         {@code false}.
     */
    public static Result convert(
            Template template, Configuration fm2Cfg, StringTemplateLoader overlayTemplateLoader,
            ConversionMarkers warnReceiver)
            throws ConverterException {
        return new FM2ASTToFM3SourceConverter(template, fm2Cfg, overlayTemplateLoader, warnReceiver).convert();
    }

    private Result convert() throws ConverterException {
        printDirFtl();
        printNode(template.getRootTreeNode());
        return new Result(template, out.toString());
    }

    private FM2ASTToFM3SourceConverter(
            Template template, Configuration fm2Cfg, StringTemplateLoader overlayTemplateLoader,
            ConversionMarkers warnReceiver)
            throws ConverterException {
        this.template = template;
        this.overlayTemplateLoader = overlayTemplateLoader;

        try {
            TemplateLoader templateLoader = fm2Cfg.getTemplateLoader();
            Object templateSource = templateLoader.findTemplateSource(template.getName());
            if (templateSource == null) {
                throw new FileNotFoundException("Template not found: " + template.getName());
            }

            Reader reader = templateLoader.getReader(templateSource, template.getEncoding());
            try {
                this.src = IOUtils.toString(reader);
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new ConverterException("Failed to load template source", e);
        } finally {
            fm2Cfg.clearTemplateCache();
        }

        this.markers = warnReceiver;

        this.out = new StringBuilder();
        if (template.getActualTagSyntax() == Configuration.SQUARE_BRACKET_TAG_SYNTAX) {
            tagBeginChar = '[';
            tagEndChar = ']';
        } else {
            tagBeginChar = '<';
            tagEndChar = '>';
        }
    }

    // The FTL tag is not part of the AST tree, so we have to treat it differently
    private void printDirFtl() throws ConverterException {
        if (printNextCustomDirAsFtlDir) {
            return;
        }

        int pos = getPositionAfterWSAndExpComments(0);
        if (src.length() > pos + 1 && src.charAt(pos) == tagBeginChar && src.startsWith("#ftl", pos + 1)) {
            printWithConvertedExpComments(src.substring(0, pos));

            pos += 5; // "<#ftl".length()

            int tagEnd;
            String postFtlTagSkippedWS;
            {
                int firstNodePos;

                TemplateElement rootNode = template.getRootTreeNode();
                if (rootNode.getBeginLine() != 0) {
                    firstNodePos = getStartPosition(rootNode);
                    assertNodeContent(firstNodePos > pos, rootNode,
                            "Root node position should be after #ftl header");
                } else {
                    // Extreme case where the template only contains the #ftl header.
                    assertNodeContent(rootNode.getEndLine() == 0, rootNode,
                            "Expected 0 end line for root node");
                    firstNodePos = src.length();
                }

                tagEnd = firstNodePos - 1;
                while (tagEnd >= 0 && !isTagEndChar(src.charAt(tagEnd))) {
                    if (!Character.isWhitespace(src.charAt(tagEnd))) {
                        throw new ConverterException("Non-WS character while backtracking to #ftl tag end character.");
                    }
                    tagEnd--;
                }
                if (tagEnd < 0) {
                    throw new ConverterException("Couldn't backtrack to #ftl tag end character.");
                }

                postFtlTagSkippedWS = src.substring(tagEnd + 1, firstNodePos);
            }

            boolean hasSlash = src.charAt(tagEnd - 1) == '/';

            // We need the Expression-s parsed, but they aren't part of the AST. So, we parse a template that contains
            // a similar custom "ftl" directive, and set up the converter to print it as an #ftl directive.
            String fm3Content;
            {
                Configuration fm2Cfg = template.getConfiguration();
                fm2Cfg.clearTemplateCache();
                FM2ASTToFM3SourceConverter customFtlDirSrcConverter;
                overlayTemplateLoader.putTemplate(
                        template.getName(),
                        tagBeginChar + "#ftl" + tagEndChar
                        + tagBeginChar + "@ftl" + src.substring(pos, tagEnd) + (hasSlash ? "" : "/") + tagEndChar);
                try {
                    customFtlDirSrcConverter = new FM2ASTToFM3SourceConverter(
                            fm2Cfg.getTemplate(template.getName()),
                            fm2Cfg, overlayTemplateLoader, markers
                    );
                } catch (IOException e) {
                    throw new ConverterException("Failed load template made for #ftl parsing", e);
                } finally {
                    overlayTemplateLoader.removeTemplate(template.getName());
                    fm2Cfg.clearTemplateCache();
                }
                customFtlDirSrcConverter.printNextCustomDirAsFtlDir = true;
                fm3Content = customFtlDirSrcConverter.convert().fm3Content;
            }
            print(hasSlash
                    ? fm3Content
                    : fm3Content.substring(0, fm3Content.length() - 2) + tagEndChar);

            print(postFtlTagSkippedWS);
        }
    }

    private String convertFtlHeaderParamName(String name) throws ConverterException {
        String converted = name.indexOf('_') == -1 ? name : _StringUtil.snakeCaseToCamelCase(name);
        if (converted.equals("attributes")) {
            converted = "customSettings";
        }
        return converted;
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
            printChildElements(node);
        } else if (node instanceof TextBlock) {
            printText(node);
        } else if (node instanceof DollarVariable) {
            printDollarInterpolation((DollarVariable) node);
        } else if (node instanceof NumericalOutput) {
            printNumericalInterpolation((NumericalOutput) node);
        } else if (node instanceof Comment) {
            printComment((Comment) node);
        } else {
            printDir(node);
        }
    }

    private void printText(TemplateElement node) throws ConverterException {
        int startPos = getStartPosition(node);
        int endPos = getEndPositionExclusive(node);
        if (startPos < 0 || startPos >= endPos) { // empty text
            return;
        }

        boolean isNoParseBlock = src.startsWith(tagBeginChar + "#no", startPos);
        if (isNoParseBlock) {
            printDirStartTagNoParamsHasNested(node, "noParse");
        }
        print(getOnlyParam(node, ParameterRole.CONTENT, String.class));
        if (isNoParseBlock) {
            printDirEndTag(node, NO_PARSE_FM_2_TAG_NAMES, "noParse");
        }
    }

    private static final Set<String> NO_PARSE_FM_2_TAG_NAMES = ImmutableSet.of("noparse", "noParse");

    private void printComment(Comment node) throws UnexpectedNodeContentException, UnconvertableLegacyFeatureException {
        print(tagBeginChar);
        print("#--");
        String content = getOnlyParam(node, ParameterRole.CONTENT, String.class);
        if (content.indexOf("-->") != -1) {
            throw new UnconvertableLegacyFeatureException("You can't have a \"-->\" inside a comment.",
                    node.getBeginLine(), node.getBeginColumn());
        }
        print(content);
        print("--");
        print(tagEndChar);
    }

    private void printNumericalInterpolation(NumericalOutput node) throws ConverterException {
        printWithParamsLeadingSkippedTokens("${", node);
        Expression content = getParam(node, 0, ParameterRole.CONTENT, Expression.class);

        int pos = getPositionAfterWSAndExpComments(getEndPositionExclusive(content));
        assertNodeContent(pos < src.length(), node, "Unexpected EOF");
        char c = src.charAt(pos);
        assertNodeContent(c == ';' || c == '}', node, "Expected ';' or '}', found {}", c);
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
        _NullArgumentException.check("node", node);
        if (node instanceof IfBlock) {
            printDirIfElseElseIfContainer((IfBlock) node);
        } else if (node instanceof ConditionalBlock) {
            printDirIfOrElseOrElseIf((ConditionalBlock) node);
        } else if (node instanceof UnifiedCall) {
            printDirCustom((UnifiedCall) node);
        } else if (node instanceof Macro) {
            printDirMacroOrFunction((Macro) node);
        } else if (node instanceof BodyInstruction) {
            printDirNested((BodyInstruction) node);
        } else if (node instanceof Assignment) {
            printDirAssignmentLonely((Assignment) node);
        } else if (node instanceof BlockAssignment) {
            printDirBlockAssignment((BlockAssignment) node);
        } else if (node instanceof AssignmentInstruction) {
            printDirAssignmentMultiple((AssignmentInstruction) node);
        } else if (node instanceof AttemptBlock) {
            printDirAttemptRecover((AttemptBlock) node);
        } else if (node instanceof AttemptBlock) {
            printDirAttemptRecover((AttemptBlock) node);
        } else if (node instanceof AutoEscBlock) {
            printDirAutoEsc((AutoEscBlock) node);
        } else if (node instanceof NoAutoEscBlock) {
            printDirNoAutoEsc((NoAutoEscBlock) node);
        } else if (node instanceof CompressedBlock) {
            printDirCompress((CompressedBlock) node);
        } else if (node instanceof EscapeBlock) {
            printDirEscape((EscapeBlock) node);
        } else if (node instanceof NoEscapeBlock) {
            printDirNoEscape((NoEscapeBlock) node);
        } else if (node instanceof FlushInstruction) {
            printDirFlush((FlushInstruction) node);
        } else if (node instanceof ReturnInstruction) {
            printDirReturn((ReturnInstruction) node);
        } else if (node instanceof LibraryLoad) {
            printDirImport((LibraryLoad) node);
        } else if (node instanceof Include) {
            printDirInclude((Include) node);
        } else if (node instanceof IteratorBlock) {
            printDirListOrForeach((IteratorBlock) node, true);
        } else if (node instanceof ListElseContainer) {
            printDirListElseContainer((ListElseContainer) node);
        } else if (node instanceof Sep) {
            printDirSep((Sep) node);
        } else if (node instanceof Items) {
            printDirItems((Items) node);
        } else if (node instanceof BreakInstruction) {
            printDirBreak((BreakInstruction) node);
        } else if (node instanceof TrimInstruction) {
            printDirTOrNtOrLtOrRt((TrimInstruction) node);
        } else if (node instanceof PropertySetting) {
            printDirSetting((PropertySetting) node);
        } else if (node instanceof StopInstruction) {
            printDirStop((StopInstruction) node);
        } else if (node instanceof SwitchBlock) {
            printDirSwitch((SwitchBlock) node);
        } else if (node instanceof Case) {
            printDirCase((Case) node);
        } else if (node instanceof VisitNode) {
            printDirVisit((VisitNode) node);
        } else if (node instanceof RecurseNode) {
            printDirRecurse((RecurseNode) node);
        } else if (node instanceof FallbackInstruction) {
            printDirFallback((FallbackInstruction) node);
        } else if (node instanceof TransformBlock) {
            printDirTransform((TransformBlock) node);
        } else if (node instanceof OutputFormatBlock) {
            printDirOutputFormat((OutputFormatBlock) node);
        } else {
            throw new ConverterException("Unhandled AST TemplateElement class: " + node.getClass().getName());
        }
    }

    private void printDirOutputFormat(OutputFormatBlock node) throws ConverterException {
        printDirStartTagPartBeforeParams(node, "outputFormat");
        Expression value = getParam(node, 0, ParameterRole.VALUE, Expression.class);
        printExp(value);
        printDirStartTagEnd(node, value, false);

        printChildElements(node);

        printDirEndTag(node, OUTPUT_FORMAT_FM2_TAG_NAMES, "outputFormat");
    }

    private static final Set<String> OUTPUT_FORMAT_FM2_TAG_NAMES = ImmutableSet.of("outputFormat", "outputformat");

    private void printDirTransform(TransformBlock node) throws ConverterException {
        Expression callee = getParam(node, 0, ParameterRole.CALLEE, Expression.class);

        print(tagBeginChar);
        print('@');
        printExp(callee);

        int pos = printWSAndExpComments(getEndPositionExclusive(callee));
        if (src.charAt(pos) == ';') {
            pos++;
            pos = printWSAndExpComments(pos);
        }

        int paramIdx = 1;
        while (paramIdx < node.getParameterCount()) {
            String paramName = getParam(node, paramIdx++, ParameterRole.ARGUMENT_NAME, String.class);
            print(FTLUtil.escapeIdentifier(paramName));
            pos = getPositionAfterIdentifier(pos);

            pos = printSeparatorAndWSAndExpComments(pos, "=");

            Expression paramValue = getParam(node, paramIdx++, ParameterRole.ARGUMENT_VALUE, Expression.class);
            printExp(paramValue);

            pos = printWSAndExpComments(getEndPositionExclusive(paramValue));
        }

        printDirStartTagEnd(node, pos, false);

        printChildElements(node);

        print(tagBeginChar);
        print("/@");
        if (callee instanceof Identifier || (callee instanceof Dot && ((Dot) callee).onlyHasIdentifiers())) {
            printExp(callee);
        }
        print(tagEndChar);
    }

    private void printDirFallback(FallbackInstruction node) throws ConverterException {
        printDirGenericNoParamsNoNested(node, "fallback");
    }

    private void printDirVisit(VisitNode node) throws ConverterException {
        printDirVisitLike(node, "visit");
    }

    private void printDirRecurse(RecurseNode node) throws ConverterException {
        printDirVisitLike(node, "recurse");
    }

    private void printDirVisitLike(TemplateElement node, String tagName) throws ConverterException {
        assertParamCount(node, 2);

        int pos = printDirStartTagPartBeforeParams(node, tagName);

        Expression nodeExp = getParam(node, 0, ParameterRole.NODE, Expression.class);
        if (nodeExp != null) {
            printExp(nodeExp);
            pos = getEndPositionExclusive(nodeExp);
        }

        Expression ns = getParam(node, 1, ParameterRole.NAMESPACE, Expression.class);
        if (ns != null) {
            printSeparatorAndWSAndExpComments(pos, "using");
            printExp(ns);
            pos = getEndPositionExclusive(ns);
        }

        printDirStartTagEnd(node, pos, false);
    }

    private void printDirCase(Case node) throws ConverterException {
        assertParamCount(node, 2);

        String tagName;
        Integer subtype = getParam(node, 1, ParameterRole.AST_NODE_SUBTYPE, Integer.class);
        if (subtype == Case.TYPE_CASE) {
            tagName = "case";
        } else if (subtype == Case.TYPE_DEFAULT) {
            tagName = "default";
        } else {
            throw new UnexpectedNodeContentException(node, "Unsupported subtype {}", subtype);
        }

        int pos = printDirStartTagPartBeforeParams(node, tagName);

        Expression value = getParam(node, 0, ParameterRole.CONDITION, Expression.class);
        if (value != null) {
            printExp(value);
            pos = getEndPositionExclusive(value);
        }

        printDirStartTagEnd(node, pos, false);

        printChildElements(node);

        // Element end tag is always omitted
    }

    private void printDirSwitch(SwitchBlock node) throws ConverterException {
        assertParamCount(node, 1);

        printDirStartTagPartBeforeParams(node, "switch");

        Expression param = getOnlyParam(node, ParameterRole.VALUE, Expression.class);
        printExp(param);

        printDirStartTagEnd(node, param, false);

        // FM2 have allowed #case after #default, FM3 doesn't:
        boolean pastDefault = false;
        for (int i = 0; i < node.getChildCount(); i++) {
            TemplateElement child = node.getChild(i);
            if (child instanceof Case) {
                if (((Case) child).condition == null) {
                    pastDefault = true;
                } else if (pastDefault) {
                    throw new UnconvertableLegacyFeatureException("The \"case\" directive can't be after "
                            + "the \"default\" directive since FreeMarker 3. You need to rearrange this \"switch\".",
                            child.getBeginLine(), child.getBeginColumn());
                }
            }
        }

        printChildElements(node);

        printDirEndTag(node, "switch");
    }

    private void printDirStop(StopInstruction node) throws ConverterException {
        assertParamCount(node, 1);

        int pos = printDirStartTagPartBeforeParams(node, "stop");
        Expression message = getParam(node, 0, ParameterRole.MESSAGE, Expression.class);
        if (message != null) {
            printExp(message);
            pos = getEndPositionExclusive(message);
        }
        printDirStartTagEnd(node, pos, false);
    }

    private void printDirSetting(PropertySetting node) throws ConverterException {
        assertParamCount(node, 2);

        int pos = printDirStartTagPartBeforeParams(node, "setting");

        print(FTLUtil.escapeIdentifier(convertSettingName(
                getParam(node, 0, ParameterRole.ITEM_KEY, String.class),
                node)));
        pos = getPositionAfterIdentifier(pos);

        pos = printSeparatorAndWSAndExpComments(pos, "=");

        Expression paramValue = getParam(node, 1, ParameterRole.ITEM_VALUE, Expression.class);
        printExp(paramValue);

        printDirStartTagEnd(node, paramValue, false);
    }

    private String convertSettingName(String name, TemplateObject node) throws ConverterException {
        String converted = name.indexOf('_') == -1 ? name : _StringUtil.snakeCaseToCamelCase(name);

        if (converted.equals("classicCompatible")) {
            throw new UnconvertableLegacyFeatureException("There \"classicCompatible\" setting doesn't exist in "
                    + "FreeMarker 3. You have to remove it manually before conversion.",
                    node.getBeginLine(), node.getBeginColumn());
        }

        if (!Arrays.asList(PropertySetting.SETTING_NAMES).contains(converted)) {
            throw new ConverterException("Couldn't map \"" + name + "\" to a valid FreeMarker 3 setting name "
                    + "(tried: " + converted + ")");
        }
        return converted;
    }

    private void printDirTOrNtOrLtOrRt(TrimInstruction node) throws ConverterException {
        int subtype = getOnlyParam(node, ParameterRole.AST_NODE_SUBTYPE, Integer.class);
        String tagName;
        if (subtype == TrimInstruction.TYPE_T) {
            tagName = "t";
        } else if (subtype == TrimInstruction.TYPE_LT) {
            tagName = "lt";
        } else if (subtype == TrimInstruction.TYPE_RT) {
            tagName = "rt";
        } else if (subtype == TrimInstruction.TYPE_NT) {
            tagName = "nt";
        } else {
            throw new UnexpectedNodeContentException(node, "Unhandled subtype {}.", subtype);
        }

        printDirStartTagNoParamsNoNested(node, tagName);
    }

    private void printDirNested(BodyInstruction node) throws ConverterException {
        int pos = printDirStartTagPartBeforeParams(node, "nested");
        int paramCnt = node.getParameterCount();
        for (int paramIdx = 0; paramIdx < paramCnt; paramIdx++) {
            Expression passedValue = getParam(node, paramIdx, ParameterRole.PASSED_VALUE, Expression.class);
            printExp(passedValue);
            pos = getEndPositionExclusive(passedValue);
            if (paramIdx < paramCnt - 1) {
                printOptionalSeparatorAndWSAndExpComments(pos, ",");
            }
        }
        printDirStartTagEnd(node, pos, false);
    }

    private void printDirBreak(BreakInstruction node) throws ConverterException {
        printDirStartTagNoParamsNoNested(node, "break");
    }

    private void printDirItems(Items node) throws ConverterException {
        int pos = printDirStartTagPartBeforeParams(node, "items");
        pos = printSeparatorAndWSAndExpComments(pos, "as");

        int paramCnt = node.getParameterCount();
        assertNodeContent(paramCnt <= 2, node, "Expected at most 2 parameters");
        String loopVar1 = getParam(node, 0, ParameterRole.TARGET_LOOP_VARIABLE, String.class);
        String loopVar2 = paramCnt >= 2 ? getParam(node, 1, ParameterRole.TARGET_LOOP_VARIABLE, String.class) : null;

        print(FTLUtil.escapeIdentifier(loopVar1));
        pos = getPositionAfterIdentifier(pos);
        if (loopVar2 != null) {
            pos = printSeparatorAndWSAndExpComments(pos, ",");
            print(FTLUtil.escapeIdentifier(loopVar2));
            pos = getPositionAfterIdentifier(pos);
        }

        printDirStartTagEnd(node, pos, false);

        printChildElements(node);

        printDirEndTag(node, "items");
    }

    private void printDirListElseContainer(ListElseContainer node) throws ConverterException {
        assertNodeContent(node.getChildCount() == 2, node, "Expected 2 child elements.");

        printDirListOrForeach((IteratorBlock) node.getChild(0), false);
        printDirElseOfList((ElseOfList) node.getChild(1));
        printDirEndTag(node, "list");
    }

    private void printDirElseOfList(ElseOfList node) throws ConverterException {
        printDirStartTagNoParamsHasNested(node, "else");
        printChildElements(node);
    }

    private void printDirSep(Sep node) throws ConverterException {
        printDirStartTagNoParamsHasNested(node, "sep");
        printChildElements(node);
        printDirEndTag(node, Collections.singleton("sep"), "sep", true);
    }

    private void printDirListOrForeach(IteratorBlock node, boolean printEndTag) throws ConverterException {
        int paramCount = node.getParameterCount();
        assertNodeContent(paramCount <= 3, node, "ParameterCount <= 3 was expected");

        int pos = printDirStartTagPartBeforeParams(node, "list");

        Expression listSource = getParam(node, 0, ParameterRole.LIST_SOURCE, Expression.class);
        // To be future proof, we don't assume that the parameter count of list don't include the null parameters.
        String loopVal1 = paramCount >= 2 ? getParam(node, 1, ParameterRole.TARGET_LOOP_VARIABLE, String.class)
                : null;
        String loopVal2 = paramCount >= 3 ? getParam(node, 2, ParameterRole.TARGET_LOOP_VARIABLE, String.class)
                : null;

        String fm2TagName1;
        String fm2TagName2;
        if (node.getNodeTypeSymbol().equals("#list")) {
            fm2TagName1 = "list";
            fm2TagName2 = null;

            printExp(listSource);

            if (loopVal1 != null) { // #list xs as <v1 | v1, v2>
                pos = printSeparatorAndWSAndExpComments(getEndPositionExclusive(listSource), "as");

                print(FTLUtil.escapeIdentifier(loopVal1));
                pos = getPositionAfterAssignmentTargetIdentifier(pos);

                if (loopVal2 != null) { // #list xs as <v1, v2>
                    pos = printSeparatorAndWSAndExpComments(pos, ",");

                    print(FTLUtil.escapeIdentifier(loopVal2));
                    pos = getPositionAfterAssignmentTargetIdentifier(pos);
                }

                printWSAndExpComments(pos);
            }
        } else if (node.getNodeTypeSymbol().equals("#foreach")) {
            fm2TagName1 = "foreach";
            fm2TagName2 = "forEach";

            assertNodeContent(loopVal1 != null && loopVal2 == null,
                    node, "Unsupported #foreach parameter ");

            // We rewrite the #foreach to #list. We assume that comments after around the "in" belong to the loop
            // variable, and comments after the list source belong to the list source.

            // #foreach <x> in xs:
            pos = getPositionAfterIdentifier(pos);

            // #foreach x< >in xs:
            int prevPos = pos;
            pos = getPositionAfterWSAndExpComments(pos);
            String postVar1WSAndComment = src.substring(prevPos, pos);

            // #foreach x <in> xs:
            assertNodeContent(src.startsWith("in", pos), node,
                    "Keyword \"in\" expected at position {}.", pos);
            pos += 2; // skip `in`

            // #foreach x in< >xs:
            prevPos = pos;
            pos = getPositionAfterWSAndExpComments(pos);
            String postInWSAndComment = src.substring(prevPos, pos);

            // #foreach x in xs< >:
            String postVar2WSAndComment = readWSAndExpComments(getEndPositionExclusive(listSource));

            printExp(listSource);
            printWithConvertedExpComments(ConverterUtils.rightTrim(postVar2WSAndComment));
            print(" as ");
            print(FTLUtil.escapeIdentifier(loopVal1));
            printWithConvertedExpComments(ConverterUtils.rightTrim(postVar1WSAndComment));
            printWithConvertedExpComments(ConverterUtils.rightTrim(postInWSAndComment));
        } else {
            throw new UnexpectedNodeContentException(node, "Expected #list or #foreach as node symbol", null);
        }
        print(tagEndChar);

        printChildElements(node);

        if (printEndTag) {
            printDirEndTag(node, LIST_FM_2_TAG_NAMES, "list", false);
        }
    }

    private static final Set<String> LIST_FM_2_TAG_NAMES = ImmutableSet.of("list", "foreach", "forEach");

    private void printDirInclude(Include node) throws ConverterException {
        assertParamCount(node, 4);

        printDirStartTagPartBeforeParams(node, "include");

        Expression templateName = getParam(node, 0, ParameterRole.TEMPLATE_NAME, Expression.class);
        int templateNameEndPos = getEndPositionExclusive(templateName);

        Expression parseParam = getParam(node, 1, ParameterRole.PARSE_PARAMETER, Expression.class);
        if (parseParam != null) {
            markers.markInSource(parseParam.getBeginLine(), parseParam.getBeginColumn(),
                    ConversionMarkers.Type.WARN,
                    "The \"parse\" parameter of #include was removed, as it's not supported anymore. Use the "
                            + "templateConfigurations configuration setting to specify which files are not parsed.");

        }

        Expression encodingParam = getParam(node, 2, ParameterRole.ENCODING_PARAMETER, Expression.class);
        if (encodingParam != null) {
            markers.markInSource(encodingParam.getBeginLine(), encodingParam.getBeginColumn(),
                    ConversionMarkers.Type.WARN,
                    "The \"encoding\" parameter of #include was removed, as it's not supported anymore. Use the "
                            + "templateConfigurations configuration setting to specify which files has a different "
                            + "encoding than the configured default.");
        }

        Expression ignoreMissingParam = getParam(node, 3, ParameterRole.IGNORE_MISSING_PARAMETER, Expression.class);

        List<Expression> sortedExps =
                sortExpressionsByPosition(templateName, parseParam, encodingParam, ignoreMissingParam);

        printExp(templateName);
        String postNameWSOrComment = readWSAndExpComments(templateNameEndPos);
        if (ignoreMissingParam != null || (parseParam == null && encodingParam == null)) {
            // This will separate us from ignoreMissing=exp, or from the tagEndChar
            printWithConvertedExpComments(postNameWSOrComment);
        } else {
            // We only have removed thing after in the src => no need for spacing after us
            int commentPos = postNameWSOrComment.indexOf("--") - 1;
            if (commentPos >= 0) {
                printWithConvertedExpComments(ConverterUtils.rightTrim(postNameWSOrComment));
            }
        }

        for (int i = 1; i < sortedExps.size(); i++) {
            Expression paramExp = sortedExps.get(i);
            if (paramExp == ignoreMissingParam) {
                int identifierStartPos = getPositionAfterWSAndExpComments(
                        getEndPositionExclusive(sortedExps.get(i - 1)));
                print("ignoreMissing");
                printSeparatorAndWSAndExpComments(getPositionAfterIdentifier(identifierStartPos), "=");
                printExp(paramExp);

                String postParamWSOrComment = readWSAndExpComments(getEndPositionExclusive(paramExp));
                if (i == sortedExps.size() - 1) {
                    // We were the last int the source as well
                    printWithConvertedExpComments(postParamWSOrComment);
                } else {
                    int commentPos = postParamWSOrComment.indexOf("--") - 1;
                    if (commentPos >= 0) {
                        printWithConvertedExpComments(ConverterUtils.rightTrim(postParamWSOrComment));
                    }
                }
            }
        }

        print(tagEndChar);
    }

    private List<Expression> sortExpressionsByPosition(Expression... expressions) {
        ArrayList<Expression> list = new ArrayList<>(expressions.length);
        for (Expression expression : expressions) {
            if (expression != null) {
                list.add(expression);
            }
        }
        Collections.sort(list, new Comparator<Expression>() {
            @Override
            public int compare(Expression o1, Expression o2) {
                return Integer.compare(getStartPosition(o1), getStartPosition(o2));
            }
        });
        return list;
    }

    private void printDirImport(LibraryLoad node) throws ConverterException {
        assertParamCount(node, 2);

        printDirStartTagPartBeforeParams(node, "import");

        Expression templateName = getParam(node, 0, ParameterRole.TEMPLATE_NAME, Expression.class);
        printExp(templateName);

        int pos = printSeparatorAndWSAndExpComments(getEndPositionExclusive(templateName), "as");

        print(FTLUtil.escapeIdentifier(getParam(node, 1, ParameterRole.NAMESPACE, String.class)));
        pos = getPositionAfterIdentifier(pos);

        printDirStartTagEnd(node, pos, false);
    }

    private void printDirReturn(ReturnInstruction node) throws ConverterException {
        int pos = printDirStartTagPartBeforeParams(node, "return");

        Expression value = getOnlyParam(node, ParameterRole.VALUE, Expression.class);
        if (value != null) {
            printExp(value);
            pos = getEndPositionExclusive(value);
        }
        printDirStartTagEnd(node, pos, false);
    }

    private void printDirFlush(FlushInstruction node) throws ConverterException {
        printDirGenericNoParamsNoNested(node, "flush");
    }

    private void printDirNoEscape(NoEscapeBlock node) throws ConverterException {
        printDirGenericNoParamsHasNested(node, NO_ESCAPE_FM_2_TAG_NAMES, "noEscape");
    }

    private static final Set<String> NO_ESCAPE_FM_2_TAG_NAMES = ImmutableSet.of("noescape", "noEscape");

    private void printDirEscape(EscapeBlock node) throws ConverterException {
        assertParamCount(node, 2);

        int pos = printDirStartTagPartBeforeParams(node, "escape");

        pos = getPositionAfterIdentifier(pos);
        print(FTLUtil.escapeIdentifier(getParam(node, 0, ParameterRole.PLACEHOLDER_VARIABLE, String.class)));

        pos = printSeparatorAndWSAndExpComments(pos, "as");

        Expression expTemplate = getParam(node, 1, ParameterRole.EXPRESSION_TEMPLATE, Expression.class);
        printExp(expTemplate);
        printDirStartTagEnd(node, expTemplate, false);

        printChildElements(node);

        printDirEndTag(node, "escape");
    }

    private void printDirCompress(CompressedBlock node) throws ConverterException {
        printDirGenericNoParamsHasNested(node, "compress");
    }

    private void printDirAutoEsc(AutoEscBlock node) throws ConverterException {
        printDirGenericNoParamsHasNested(node, AUTO_ESC_FM_2_TAG_NAMES, "autoEsc");
    }

    private static final Set<String> AUTO_ESC_FM_2_TAG_NAMES = ImmutableSet.of("autoesc", "autoEsc");

    private void printDirNoAutoEsc(NoAutoEscBlock node) throws ConverterException {
        printDirGenericNoParamsHasNested(node, NO_AUTO_ESC_FM_2_TAG_NAMES, "noAutoEsc");
    }

    private static final Set<String> NO_AUTO_ESC_FM_2_TAG_NAMES = ImmutableSet.of("noautoesc", "noAutoEsc");

    private void printDirGenericNoParamsHasNested(TemplateElement node, String fm3TagName)
            throws ConverterException {
        printDirGenericNoParamsHasNested(node, Collections.singleton(fm3TagName), fm3TagName);
    }

    private void printDirGenericNoParamsHasNested(TemplateElement node, Set<String> fm2TagNames, String fm3TagName)
            throws ConverterException {
        assertParamCount(node, 0);

        printDirStartTagNoParamsHasNested(node, fm3TagName);
        printChildElements(node);
        printDirEndTag(node, fm2TagNames, fm3TagName);
    }

    private void printDirGenericNoParamsNoNested(TemplateElement node, String name)
            throws ConverterException {
        assertParamCount(node, 0);
        printDirStartTagNoParamsNoNested(node, name);
    }

    private void printDirAttemptRecover(AttemptBlock node) throws ConverterException {
        assertParamCount(node, 1); // 1: The recovery block

        printDirStartTagNoParamsHasNested(node, "attempt");

        printNode(node.getChild(0));
        assertNodeContent(node.getChild(1) instanceof RecoveryBlock, node, "child[1] should be #recover");

        RecoveryBlock recoverDir = getOnlyParam(node, ParameterRole.ERROR_HANDLER, RecoveryBlock.class);
        printDirStartTagNoParamsHasNested(recoverDir, "recover");

        printChildElements(recoverDir);

        // In FM2 this could be </#recover> as well, but we normalize it
        printDirEndTag(node, ATTEMPT_RECOVER_FM_2_TAG_NAMES, "attempt", false);
    }

    private static final Set<String> ATTEMPT_RECOVER_FM_2_TAG_NAMES = ImmutableSet.of("attempt", "recover");

    private void printDirAssignmentMultiple(AssignmentInstruction node) throws ConverterException {
        assertParamCount(node, 2);

        int pos = printDirAssignmentCommonTagTillAssignmentExp(node, 0);

        int childCnt = node.getChildCount();
        for (int childIdx = 0; childIdx < childCnt; childIdx++) {
            Assignment assignment = (Assignment) node.getChild(childIdx);
            pos = printDirAssignmentCommonExp(assignment, pos);
            if (childIdx != childCnt - 1) {
                pos = printOptionalSeparatorAndWSAndExpComments(pos, ",");
            }
        }

        printDirAssignmentCommonTagAfterLastAssignmentExp(node, 1, pos);
    }

    private void printDirAssignmentLonely(Assignment node) throws ConverterException {
        assertParamCount(node, 5);

        int pos = printDirAssignmentCommonTagTillAssignmentExp(node, 3);
        pos = printDirAssignmentCommonExp(node, pos);
        printDirAssignmentCommonTagAfterLastAssignmentExp(node, 4, pos);
    }

    private void printDirBlockAssignment(BlockAssignment node) throws ConverterException {
        assertParamCount(node, 3);

        int pos = printDirAssignmentCommonTagTillAssignmentExp(node, 1);

        print(FTLUtil.escapeIdentifier(getParam(node, 0, ParameterRole.ASSIGNMENT_TARGET, String.class)));
        pos = getPositionAfterAssignmentTargetIdentifier(pos);

        Expression namespace = getParam(node, 2, ParameterRole.NAMESPACE, Expression.class);
        if (namespace != null) {
            printSeparatorAndWSAndExpComments(pos, "in");
            printExp(namespace);
            pos = getEndPositionExclusive(namespace);
        }

        printDirStartTagEnd(node, pos, true);

        printChildElements(node);

        printDirEndTag(node, getAssignmentDirTagName(node, 1));
    }

    private void printDirAssignmentCommonTagAfterLastAssignmentExp(TemplateElement node, int nsParamIdx, int pos)
            throws ConverterException {
        Expression ns = getParam(node, nsParamIdx, ParameterRole.NAMESPACE, Expression.class);
        if (ns != null) {
            printSeparatorAndWSAndExpComments(pos, "in");
            printExp(ns);
            pos = getEndPositionExclusive(ns);
        }

        printDirStartTagEnd(node, pos, false);
    }

    private int printDirAssignmentCommonTagTillAssignmentExp(TemplateElement node, int scopeParamIdx)
            throws ConverterException {
        return printDirStartTagPartBeforeParams(node, getAssignmentDirTagName(node, scopeParamIdx));
    }

    private String getAssignmentDirTagName(TemplateElement node, int scopeParamIdx)
            throws UnexpectedNodeContentException {
        int scope = getParam(node, scopeParamIdx, ParameterRole.VARIABLE_SCOPE, Integer.class);
        String tagName;
        if (scope == Assignment.NAMESPACE) {
            tagName = "assign";
        } else if (scope == Assignment.GLOBAL) {
            tagName = "global";
        } else if (scope == Assignment.LOCAL) {
            tagName = "local";
        } else {
            throw new UnexpectedNodeContentException(node, "Unhandled scope: {}", scope);
        }
        return tagName;
    }

    private int printDirAssignmentCommonExp(Assignment node, int pos) throws ConverterException {
        String target = getParam(node, 0, ParameterRole.ASSIGNMENT_TARGET, String.class);
        print(FTLUtil.escapeIdentifier(target));
        pos = getPositionAfterAssignmentTargetIdentifier(pos);

        pos = printWSAndExpComments(pos);

        {
            String operator = getParam(node, 1, ParameterRole.ASSIGNMENT_OPERATOR, String.class);
            print(operator);
            pos += operator.length();
        }

        pos = printWSAndExpComments(pos);

        Expression source = getParam(node, 2, ParameterRole.ASSIGNMENT_SOURCE, Expression.class);
        if (source != null) {
            printExp(source);
            pos = getEndPositionExclusive(source);
        }

        return pos;
    }

    private void printDirMacroOrFunction(Macro node) throws ConverterException {
        int paramCnt = node.getParameterCount();

        int subtype = getParam(node, paramCnt - 1, ParameterRole.AST_NODE_SUBTYPE, Integer.class);
        String tagName;
        if (subtype == Macro.TYPE_MACRO) {
            tagName = "macro";
        } else if (subtype == Macro.TYPE_FUNCTION) {
            tagName = "function";
        } else {
            throw new UnexpectedNodeContentException(node, "Unhandled node subtype: {}", subtype);
        }

        int pos = printDirStartTagPartBeforeParams(node, tagName);

        String assignedName = getParam(node, 0, ParameterRole.ASSIGNMENT_TARGET, String.class);
        print(FTLUtil.escapeIdentifier(assignedName));
        pos = getPositionAfterAssignmentTargetIdentifier(pos);

        pos = printOptionalSeparatorAndWSAndExpComments(pos, "(");

        int paramIdx = 1;
        while (node.getParameterRole(paramIdx) == ParameterRole.PARAMETER_NAME) {
            String paramName = getParam(node, paramIdx++, ParameterRole.PARAMETER_NAME, String.class);
            print(FTLUtil.escapeIdentifier(paramName));
            pos = getPositionAfterIdentifier(pos);

            Expression paramDefault = getParam(node, paramIdx++, ParameterRole.PARAMETER_DEFAULT, Expression.class);
            if (paramDefault != null) {
                printSeparatorAndWSAndExpComments(pos, "=");
                printExp(paramDefault);
                pos = getEndPositionExclusive(paramDefault);
            }

            pos = printWSAndExpComments(pos);
            {
                char c = src.charAt(pos);
                assertNodeContent(
                        c == ',' || c == ')' || isTagEndChar(c)
                                || c == '\\' || StringUtil.isFTLIdentifierStart(c),
                        node,
                        "Unexpected character: {}", c);
                if (c == ',') {
                    print(c);
                    pos++;

                    pos = printWSAndExpComments(pos);
                }
                if (c == ')') {
                    assertNodeContent(node.getParameterRole(paramIdx) != ParameterRole.PARAMETER_NAME, node,
                            "Expected no parameter after \"(\"");
                }
            }
        }

        {
            ParameterRole parameterRole = node.getParameterRole(paramIdx);
            assertNodeContent(parameterRole == ParameterRole.CATCH_ALL_PARAMETER_NAME, node,
                    "Expected catch-all parameter role, but found {}", parameterRole);
        }
        String paramName = getParam(node, paramIdx++, ParameterRole.CATCH_ALL_PARAMETER_NAME, String.class);
        if (paramName != null) {
            print(FTLUtil.escapeIdentifier(paramName));
            pos = getPositionAfterIdentifier(pos);
            pos = printWSAndExpComments(pos);
            assertNodeContent(src.startsWith("...", pos), node,
                    "Expected \"...\" after catch-all parameter name");
            print("...");
            pos += 3;
        }

        assertNodeContent(paramIdx == paramCnt - 1, node,
                "Expected AST parameter at index {} to be the last one", paramIdx);

        pos = printOptionalSeparatorAndWSAndExpComments(pos, ")");
        assertNodeContent(isTagEndChar(src.charAt(pos)), node, "Tag end not found");
        print(tagEndChar);

        printChildElements(node);

        printDirEndTag(node, tagName);
    }

    private void printDirCustom(UnifiedCall node) throws ConverterException {
        boolean ftlDirMode = printNextCustomDirAsFtlDir;
        printNextCustomDirAsFtlDir = false;

        boolean legacyCallDirMode = src.startsWith("#call" , getStartPosition(node) + 1);

        print(tagBeginChar);
        print(ftlDirMode ? '#' : '@');

        Expression callee = getParam(node, 0, ParameterRole.CALLEE, Expression.class);
        printExp(callee);

        int calleEnd = getEndPositionExclusive(callee);
        int lastParamEnd = printWSAndExpComments(calleEnd);
        boolean legacyCallDirWithParenthesis = false;
        boolean legacyCallDirNeedsSeparatorSpace = false;
        if (legacyCallDirMode) {
            if (src.charAt(lastParamEnd) == '(') {
                if (calleEnd == lastParamEnd) {
                    legacyCallDirNeedsSeparatorSpace = true;
                }
                lastParamEnd++; // skip '('
                legacyCallDirWithParenthesis = true;
            }
        }

        int paramIdx = 1;
        int paramCount = node.getParameterCount();

        // Print positional arguments:
        while (paramIdx < paramCount && node.getParameterRole(paramIdx) == ParameterRole.ARGUMENT_VALUE) {
            Expression argValue = getParam(node, paramIdx, ParameterRole.ARGUMENT_VALUE, Expression.class);

            if (legacyCallDirNeedsSeparatorSpace) {
                print(' ');
                legacyCallDirNeedsSeparatorSpace = false;
            }
            printSeparatorAndWSAndExpComments(lastParamEnd, ",", true);
            printExp(argValue);

            lastParamEnd = getEndPositionExclusive(argValue);
            paramIdx++;
        }

        if (legacyCallDirWithParenthesis) {
            lastParamEnd = printWSAndExpComments(lastParamEnd);
            assertNodeContent(src.charAt(lastParamEnd) == ')', node,
                    "Can't find closing parenthesis of #call(...)");
            lastParamEnd++; // skip ')'
        }

        // Print named arguments:
        while (paramIdx < paramCount
                && node.getParameterRole(paramIdx) == ParameterRole.ARGUMENT_NAME) {
            String paramName = getParam(node, paramIdx, ParameterRole.ARGUMENT_NAME, String.class);
            Expression argValue = getParam(node, paramIdx + 1, ParameterRole.ARGUMENT_VALUE, Expression.class);

            int pos = lastParamEnd;
            pos = printOptionalSeparatorAndWSAndExpComments(pos, ",");
            pos = getPositionAfterIdentifier(pos);
            if (ftlDirMode) {
                paramName = convertFtlHeaderParamName(paramName);
            }
            print(FTLUtil.escapeIdentifier(paramName));
            printSeparatorAndWSAndExpComments(pos, "=");
            printExp(argValue);

            lastParamEnd = getEndPositionExclusive(argValue);
            paramIdx += 2;
        }

        // Print loop variables:
        int pos = lastParamEnd;
        boolean beforeFirstLoopVar = true;
        while (paramIdx < paramCount) {
            pos = printSeparatorAndWSAndExpComments(pos, beforeFirstLoopVar ? ";" : ",");

            String loopVarName = getParam(node, paramIdx, ParameterRole.TARGET_LOOP_VARIABLE, String.class);
            print(_StringUtil.toFTLTopLevelIdentifierReference(loopVarName));
            String identifierInSrc = readIdentifier(pos);
            assertNodeContent(identifierInSrc.length() != 0, node,
                    "Can't find loop variable identifier in source");
            pos += identifierInSrc.length(); // skip loop var name

            beforeFirstLoopVar = false;
            paramIdx++;
        }

        pos = printWSAndExpComments(pos);
        if (legacyCallDirMode) {
            print('/');
        }
        int startTagEndPos = printDirStartTagEnd(node, pos, false);

        int elementEndPos = getEndPositionInclusive(node);
        {
            char c = src.charAt(elementEndPos);
            assertNodeContent(isTagEndChar(c), node,
                    "tagEndChar expected, found {}", c);
        }
        if (startTagEndPos != elementEndPos) { // We have an end-tag
            assertNodeContent(src.charAt(startTagEndPos - 1) != '/', node,
                    "Not expected \"/\" at the end of the start tag");
            printChildElements(node);

            print(tagBeginChar);
            print("/@");
            int nameStartPos = elementEndPos; // Not 1 less; consider the case of </@>
            while (nameStartPos >= 2 && !src.startsWith("/@", nameStartPos - 2)) {
                nameStartPos--;
            }
            assertNodeContent(nameStartPos >= 2, node,
                    "Couldn't extract name from end-tag.");
            // Also prints ignored WS after name, for now:
            printWithConvertedExpComments(src.substring(nameStartPos, elementEndPos));
            print(tagEndChar);
        } else { // We don't have end-tag
            if (!legacyCallDirMode) {
                assertNodeContent(src.charAt(startTagEndPos - 1) == '/', node,
                        "Expected \"/\" at the end of the start tag");
            }
            assertNodeContent(node.getChildCount() == 0, node,
                    "Expected no children");
        }
    }

    private void printDirIfOrElseOrElseIf(ConditionalBlock node) throws ConverterException {
        assertParamCount(node, 2);
        Expression conditionExp = getParam(node, 0, ParameterRole.CONDITION, Expression.class);
        int nodeSubtype = getParam(node, 1, ParameterRole.AST_NODE_SUBTYPE, Integer.class);

        String tagName;
        if (nodeSubtype == ConditionalBlock.TYPE_IF) {
            tagName = "if";
        } else if (nodeSubtype == ConditionalBlock.TYPE_ELSE) {
            tagName = "else";
        } else if (nodeSubtype == ConditionalBlock.TYPE_ELSE_IF) {
            tagName = "elseIf";
        } else {
            throw new UnexpectedNodeContentException(node, "Unhandled subtype, {}.", nodeSubtype);
        }

        if (conditionExp != null) {
            printDirStartTagPartBeforeParams(node, tagName);
            printNode(conditionExp);
            printDirStartTagEnd(node, conditionExp, true);
        } else {
            printDirStartTagNoParamsHasNested(node, tagName);
        }

        printChildElements(node);

        if (!(node.getParentElement() instanceof IfBlock)) {
            printDirEndTag(node, "if");
        }
    }

    private void printDirIfElseElseIfContainer(IfBlock node) throws ConverterException {
        printChildElements(node);

        printDirEndTag(node, "if");
    }

    /**
     * Prints an expression
     */
    private void printExp(Expression node) throws ConverterException {
        _NullArgumentException.check("node", node);
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
        } else if (node instanceof DefaultToExpression) {
            printExpDefault((DefaultToExpression) node);
        } else if (node instanceof ExistsExpression) {
            printExpExists((ExistsExpression) node);
        } else {

            throw new ConverterException("Unhandled AST node expression class: " + node.getClass().getName());
        }
    }

    private void printPostfixOperator(Expression node, String operator) throws ConverterException {
        Expression lho = getParam(node, 0, ParameterRole.LEFT_HAND_OPERAND, Expression.class);
        printExp(lho);

        int wsStartPos = getEndPositionExclusive(lho);
        int opPos = getPositionAfterWSAndExpComments(wsStartPos);
        printWithConvertedExpComments(src.substring(wsStartPos, opPos));
        String operatorInSrc = src.substring(opPos, opPos + operator.length());
        assertNodeContent(operatorInSrc.equals(operator), node,
                "Expected " + _StringUtil.jQuote(operator) + ", found {}", operatorInSrc);
        print(operator);
    }

    private void printExpExists(ExistsExpression node) throws ConverterException {
        assertParamCount(node, 1);
        printPostfixOperator(node, "??");
    }

    private void printExpDefault(DefaultToExpression node) throws ConverterException {
        assertParamCount(node, 2);
        Expression rho = getParam(node, 1, ParameterRole.RIGHT_HAND_OPERAND, Expression.class);
        if (rho != null) {
            Expression lho = getParam(node, 0, ParameterRole.LEFT_HAND_OPERAND, Expression.class);
            printExp(lho);
            printParameterSeparatorSource(lho, rho);
            printNode(rho);
        } else {
            printPostfixOperator(node, "!");
        }
    }

    private void printExpNot(NotExpression node) throws ConverterException {
        printWithParamsLeadingSkippedTokens("!", node);
        printExp(getOnlyParam(node, ParameterRole.RIGHT_HAND_OPERAND, Expression.class));
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
        final String fm2Op = readUntilWSOrComment(opStart, getStartPosition(rho));
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

        int varNameStart = printSeparatorAndWSAndExpComments(startPos, ".");

        String name = src.substring(varNameStart, getEndPositionExclusive(node));
        print(convertBuiltInVariableName(name));
    }

    private String convertBuiltInVariableName(String name) throws ConverterException {
        String converted = name.indexOf('_') == -1 ? name : _StringUtil.snakeCaseToCamelCase(name);

        // Will replace removed names here

        return converted;
    }

    private void printExpDot(Dot node) throws ConverterException {
        assertParamCount(node, 2);
        Expression lho = getParam(node, 0, ParameterRole.LEFT_HAND_OPERAND, Expression.class);
        String rho = getParam(node, 1, ParameterRole.RIGHT_HAND_OPERAND, String.class);
        printNode(lho);

        printSeparatorAndWSAndExpComments(getEndPositionExclusive(lho), ".");

        rho = mapStringHashKey(rho);
        print(rho.startsWith("*") ? rho : FTLUtil.escapeIdentifier(rho));
    }

    private String mapStringHashKey(String key) {
        if (key.startsWith("@@")) {
            String mappedKey = DOM_KEY_MAPPING.get(key);
            if (mappedKey != null) {
                key = mappedKey;
            }
        }
        return key;
    }

    private static final Map<String, String> DOM_KEY_MAPPING;
    static {
        Map<String, String> domKeyMapping = new HashMap<>();
        for (String atAtKey : AtAtKeyAccessor.getAtAtKeys()) {
            String atAtKeyCC = _StringUtil.snakeCaseToCamelCase(atAtKey);
            if (!atAtKeyCC.equals(atAtKey)) {
                domKeyMapping.put(atAtKey, atAtKeyCC);
            }
        }

        DOM_KEY_MAPPING = Collections.unmodifiableMap(domKeyMapping);
    }

    private void printExpDynamicKeyName(DynamicKeyName node) throws ConverterException {
        assertParamCount(node, 2);

        Expression collExp = getParam(node, 0, ParameterRole.LEFT_HAND_OPERAND, Expression.class);
        printExp(collExp);

        Expression keyExp = getParam(node, 1, ParameterRole.ENCLOSED_OPERAND, Expression.class);
        printParameterSeparatorSource(collExp, keyExp);
        if (keyExp instanceof StringLiteral && keyExp.isLiteral()) {
            StringLiteral keyStringLiteral = (StringLiteral) keyExp;
            printExpStringLiteral(keyStringLiteral, mapStringHashKey((keyStringLiteral).getAsString()));
        } else {
            printExp(keyExp);
        }

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
                "Expected '{'");
        assertNodeContent(src.charAt(closeCharPos) == '}', node,
                "Expected '}'");

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
                "Expected '['");
        assertNodeContent(src.charAt(closeCharPos) == ']', node,
                "Expected ']'");

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
        Expression lho = getParam(node, 0, ParameterRole.LEFT_HAND_OPERAND, Expression.class);
        String rho = getParam(node, 1, ParameterRole.RIGHT_HAND_OPERAND, String.class);

        // <lho>?biName
        printExp(lho);
        int pos = getEndPositionExclusive(lho);

        // lho<?>biName
        pos = printSeparatorAndWSAndExpComments(pos, "?");

        // lho?<biName>
        print(convertBuiltInName(rho));

        if (node instanceof BuiltInWithParseTimeParameters) {
            // lho?biName<(>
            pos = getPositionAfterIdentifier(pos);
            pos = printSeparatorAndWSAndExpComments(pos, "(");
            int paramCnt = node.getParameterCount();
            for (int paramIdx = 2; paramIdx < paramCnt; paramIdx++) {
                Expression argValue = getParam(node, paramIdx, ParameterRole.ARGUMENT_VALUE, Expression.class);
                printExp(argValue);
                pos = getEndPositionExclusive(argValue);

                if (paramIdx + 1 < paramCnt) {
                    printSeparatorAndWSAndExpComments(pos, ",");
                }
            }
            pos = printWSAndExpComments(pos);
            boolean endChar = src.charAt(pos) == ')';
            assertNodeContent(pos == getEndPositionInclusive(node), node,
                    "Actual end position doesn't match node end position.");
            assertNodeContent(endChar, node,
                    "Expected ')' but found {}.");
            print(')');
        } else {
            assertParamCount(node, 2);
        }
    }

    private int stringLiteralNestingLevel;

    private void printExpStringLiteral(StringLiteral node) throws ConverterException {
        printExpStringLiteral(node, node.isLiteral()     ? node.getAsString() : null);
    }

    private void printExpStringLiteral(StringLiteral node, String value) throws ConverterException {
        boolean escapeAmp, escapeLT, escapeGT;
        if (stringLiteralNestingLevel == 0) {
            // We check if the source code has avoided '&', '<', and '>'. If it did, we will escape them in the output.

            escapeAmp = true;
            escapeLT = true;
            escapeGT = true;

            int endPos = getEndPositionInclusive(node);
            for (int idx = getStartPosition(node) + 1; idx < endPos; idx++) {
                char c = src.charAt(idx);
                if (c == '&') {
                    escapeAmp = false;
                } else if (c == '<') {
                    escapeLT = false;
                } else if (c == '>') {
                    escapeGT = false;
                }
            }
        } else {
            // Don't escape in nested literals (like ${'${"nested"}'}), as the outer literal takes care of that.
            escapeAmp = false;
            escapeLT = false;
            escapeGT = false;
        }

        boolean rawString = false;
        char quote;
        {
            int pos = getStartPosition(node);
            quote = src.charAt(pos);
            while ((quote == '\\' || quote == '{' /* [broken in 2.3.26] */ || quote == 'r')
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
            _NullArgumentException.check("value", value);
            if (!rawString) {
                print(FTLUtil.escapeStringLiteralPart(value, quote, escapeAmp, escapeLT, escapeGT));
            } else {
                print(value);
            }
        } else {
            if (value != null) {
                throw new IllegalArgumentException("\"value\" argument must be null when the string contains "
                        + "interpolations.");
            }

            // Not really a literal; contains interpolations
            for (int paramIdx = 0; paramIdx < parameterCount; paramIdx++) {
                Object param = getParam(node, paramIdx, ParameterRole.VALUE_PART, Object.class);
                if (param instanceof String) {
                    print(FTLUtil.escapeStringLiteralPart((String) param, quote, escapeAmp, escapeLT, escapeGT));
                } else {
                    assertNodeContent(param instanceof Interpolation, node,
                            "Unexpected parameter type: {}", param.getClass().getName());

                    // We print the interpolation, the cut it out from the output, then put it back escaped:
                    int interpStartPos = out.length();

                    try {
                        stringLiteralNestingLevel++;
                        printNode((TemplateElement) param);
                    } finally {
                        stringLiteralNestingLevel--;
                    }

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
        String converted = IRREGULAR_BUILT_IN_NAME_CONVERSIONS.get(name);
        if (converted == null) {
            converted = name.indexOf('_') == -1 ? name : _StringUtil.snakeCaseToCamelCase(name);
        }

        if (!fm3BuiltInNames.contains(converted)) {
            throw new ConverterException("Couldn't map \"" + name + "\" to a valid FreeMarker 3 built-in name "
                    + "(tried: " + converted + ")");
        }
        return converted;
    }

    private static Map<String, String> IRREGULAR_BUILT_IN_NAME_CONVERSIONS = new ImmutableMap.Builder<String, String>()
            .put("webSafe", "html")
            .put("web_safe", "html")
            .put("iso_utc_fz", "isoUtcFZ")
            .put("iso_utc_nz", "isoUtcNZ")
            .put("iso_utc_ms_nz", "isoUtcMsNZ")
            .put("iso_utc_m_nz", "isoUtcMNZ")
            .put("iso_utc_h_nz", "isoUtcHNZ")
            .put("iso_local_nz", "isoLocalNZ")
            .put("iso_local_ms_nz", "isoLocalMsNZ")
            .put("iso_local_m_nz", "isoLocalMNZ")
            .put("iso_local_h_nz", "isoLocalHNZ")
            .put("iso_nz", "isoNZ")
            .put("iso_ms_nz", "isoMsNZ")
            .put("iso_m_nz", "isoMNZ")
            .put("iso_h_nz", "isoHNZ")
            .build();

    private void printParameterSeparatorSource(Expression lho, Expression rho) {
        print(getSrcSectionExclEnd(
                lho.getEndColumn() + 1, lho.getEndLine(),
                rho.getBeginColumn(), rho.getBeginLine()));
    }

    private void printChildElements(TemplateElement node) throws ConverterException {
        int ln = node.getChildCount();
        for (int i = 0; i < ln; i++) {
            printNode(node.getChild(i));
        }
    }

    /**
     * Prints the start tag until the parameters come; this works even if there are no parameters, in whic case it
     * prints until the tag end character.
     *
     * @return The position in the source after the printed part
     */
    private int printDirStartTagPartBeforeParams(TemplateElement node, String fm3TagName)
            throws ConverterException {
        print(tagBeginChar);
        print('#');
        print(fm3TagName);
        return printWSAndExpComments(getPositionAfterTagName(node));
    }

    private int printDirStartTagNoParamsNoNested(TemplateElement node, String fm3TagName)
            throws ConverterException {
        return printDirStartTagNoParams(node, fm3TagName, false);
    }

    private int printDirStartTagNoParamsHasNested(TemplateElement node, String fm3TagName)
            throws ConverterException {
        return printDirStartTagNoParams(node, fm3TagName, true);
    }

    private int printDirStartTagNoParams(TemplateElement node, String fm3TagName, boolean removeSlash)
            throws ConverterException {
        int pos = printDirStartTagPartBeforeParams(node, fm3TagName);
        printDirStartTagEnd(node, pos, removeSlash);
        return pos + 1;
    }

    private void printDirEndTag(TemplateElement node, String tagName) throws UnexpectedNodeContentException {
        printDirEndTag(node, Collections.singleton(tagName), tagName);
    }

    private void printDirEndTag(TemplateElement node, Set<String> fm2TagNames, String fm3TagName) throws
            UnexpectedNodeContentException {
        if (fm2TagNames.size() == 0) {
            throw new IllegalArgumentException("You must specify at least 1 FM2 tag names");
        }
        if (fm2TagNames.size() == 1 && containsUpperCaseLetter(fm3TagName)) {
            throw new IllegalArgumentException(
                    "You must specify multiple FM2 tag names when the FM3 tag name ("
                    + fm3TagName + ") contains upper case letters");
        }
        printDirEndTag(node, fm2TagNames, fm3TagName, false);
    }

    private void printDirEndTag(TemplateElement node, Set<String> fm2TagNames, String fm3TagName, boolean optional)
            throws UnexpectedNodeContentException {
        int tagEndPos = getEndPositionInclusive(node);
        {
            char c = src.charAt(tagEndPos);
            if (!isTagEndChar(c)) {
                if (optional) {
                    return;
                }
                throw new UnexpectedNodeContentException(node, "tagEndChar expected, found {}", c);
            }
        }

        int pos = tagEndPos - 1;
        while (pos > 0 && Character.isWhitespace(src.charAt(pos))) {
            pos--;
        }
        if (pos < 0 || !isCoreNameChar(src.charAt(pos))) {
            if (optional) {
                return;
            }
            throw new UnexpectedNodeContentException(node, "Can't find end tag name", null);
        }
        int nameEndPos = pos + 1;

        while (pos > 0 && src.charAt(pos) != '#') {
            pos--;
        }
        String srcTagName = src.substring(pos + 1 /* skip '#' */, nameEndPos);

        if (!fm2TagNames.contains(srcTagName)) {
            if (optional) {
                return;
            }
            throw new UnexpectedNodeContentException(node, "Unexpected end tag name: {}", srcTagName);
        }

        char slash = src.charAt(pos - 1);
        if (pos < 1 || slash != '/') {
            if (optional) {
                return;
            }
            throw new UnexpectedNodeContentException(node, "'/' expected, but found {}", slash);
        }

        print(tagBeginChar);
        print("/#");

        print(fm3TagName);

        printWithConvertedExpComments(src.substring(nameEndPos, tagEndPos));

        print(tagEndChar);
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
     * Prints the part of the start tag that's after the last parameter,or after the directive name when there's no
     * parameter. (This will print the whitespace or comments that isn't visible in the AST.)
     *
     * @return The position of the last character of the start tag. Note that the printed string never includes this
     * character.
     */
    private int printDirStartTagEnd(TemplateElement node, Expression lastParam, boolean trimSlash)
            throws ConverterException {
        _NullArgumentException.check("lastParam", lastParam);
        return printDirStartTagEnd(
                node,
                getPosition(lastParam.getEndColumn() + 1, lastParam.getEndLine()),
                trimSlash);
    }

    private int getPositionAfterTagName(TemplateElement node) throws UnexpectedNodeContentException {
        int pos = getStartPosition(node);
        {
            char c = src.charAt(pos++);
            assertNodeContent(c == tagBeginChar, node,
                    "tagBeginChar expected, found {}", c);
        }
        {
            char c = src.charAt(pos++);
            assertNodeContent(c == '#', node,
                    "'#' expected, found {}", c);
        }
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (!isCoreNameChar(c)) {
                return pos;
            }
            pos++;
        }
        throw new UnexpectedNodeContentException(node, "Can't find end of tag name", null);
    }

    /**
     * Similar to {@link #printDirStartTagEnd(TemplateElement, Expression, boolean)}, but with explicitly
     * specified scan start position.
     *
     * @param pos
     *         The position where the first skipped character can occur (or the tag end character).
     */
    private int printDirStartTagEnd(TemplateElement node, int pos, boolean removeSlash)
            throws ConverterException {
        final int startPos = pos;

        pos = getPositionAfterWSAndExpComments(pos);
        if (pos == src.length()) {
            throw new UnexpectedNodeContentException(node,
                    "End of source reached when scanning for tag end", null);
        }

        char c = src.charAt(pos);
        if (c == '/' && pos + 1 < src.length() && isTagEndChar(src.charAt(pos + 1))) {
            printWithConvertedExpComments(src.substring(startPos, pos));

            if (removeSlash) {
                // In <#foo param />, the space before the removed '/' should be removed:
                if (out.length() > 0 && out.charAt(out.length() - 1) == ' ') {
                    out.setLength(out.length() - 1);
                }
            } else {
                print('/');
            }
            print(tagEndChar);
            return pos + 1;
        } else if (isTagEndChar(c)) {
            printWithConvertedExpComments(src.substring(startPos, pos));
            print(tagEndChar);
            return pos;
        } else {
            throw new UnexpectedNodeContentException(node,
                    "Unexpected character when scanning for tag end: {}", c);
        }
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
            errorMessage) throws UnexpectedNodeContentException {
        assertNodeContent(good, node, errorMessage, null);
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
     * @param column
     *         1-based column
     * @param row
     *         1-based row
     */
    private int getPosition(int column, int row) {
        if (row == 0) {
            return  -1;
        }
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
        scanForNoWSNoComment:
        while (pos < src.length()) {
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

    private String readWSAndExpComments(int startPos)
            throws ConverterException {
        return src.substring(startPos, getPositionAfterWSAndExpComments(startPos));
    }

    private String readSeparatorAndWSAndExpComments(int startPos, String separator, boolean separatorOptional)
            throws ConverterException {
        int pos = getPositionAfterWSAndExpComments(startPos);

        if (pos == src.length() || !src.startsWith(separator, pos)) {
            if (!separatorOptional) {
                throw new ConverterException(
                        "Expected separator " + _StringUtil.jQuote(separator) + " at position " + pos + ".");
            }
            return src.substring(startPos, pos);
        }
        pos += separator.length();

        pos = getPositionAfterWSAndExpComments(pos);

        return src.substring(startPos, pos);
    }

    private int printWSAndExpComments(int pos) throws ConverterException {
        String sep = readWSAndExpComments(pos);
        printWithConvertedExpComments(sep);
        pos += sep.length();
        return pos;
    }

    private int printSeparatorAndWSAndExpComments(int pos, String separator) throws ConverterException {
        return printSeparatorAndWSAndExpComments(pos, separator, false);
    }

    private int printOptionalSeparatorAndWSAndExpComments(int pos, String separator) throws ConverterException {
        return printSeparatorAndWSAndExpComments(pos, separator, true);
    }

    private int printSeparatorAndWSAndExpComments(int pos, String separator, boolean sepOptional)
            throws ConverterException {
        String sep = readSeparatorAndWSAndExpComments(pos, separator, sepOptional);
        printWithConvertedExpComments(sep);
        pos += sep.length();
        return pos;
    }

    private int getPositionAfterIdentifier(int startPos) throws ConverterException {
        return getPositionAfterIdentifier(startPos, false);
    }

    private int getPositionAfterAssignmentTargetIdentifier(int startPos) throws ConverterException {
        return getPositionAfterIdentifier(startPos, true);
    }

    private int getPositionAfterIdentifier(int startPos, boolean assignmentTarget) throws ConverterException {
        if (assignmentTarget && looksLikeStringLiteralStart(startPos)) {
            return getPositionAfterStringLiteral(startPos);
        } else {
            int pos = startPos;
            scanUntilIdentifierEnd:
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == '\\') {
                    if (pos + 1 == src.length()) {
                        throw new ConverterException("Misplaced \"\\\" at position " + pos);
                    }
                    pos += 2; // to skip escaped character
                } else if (pos == startPos && StringUtil.isFTLIdentifierStart(c)
                        || StringUtil.isFTLIdentifierPart(c)) {
                    pos++;
                } else {
                    break scanUntilIdentifierEnd;
                }
            }
            if (pos == startPos) {
                throw new ConverterException("Expected an identifier at position " + startPos + ".");
            }
            return pos;
        }
    }

    private int getPositionAfterStringLiteral(int pos) throws ConverterException {
        char c = src.charAt(pos++);
        boolean raw;
        if (c == 'r') {
            c = src.charAt(pos++);
            raw = true;
        } else {
            raw = false;
        }
        char quotationC = c;
        if (!ConverterUtils.isQuotationChar(quotationC)) {
            throw new IllegalArgumentException("The specifies position is not the beginning of a string literal");
        }

        boolean escaped = false;
        while (pos < src.length()) {
            c = src.charAt(pos++);
            if (c == quotationC && !escaped) {
                return pos;
            } if (c == '\\' && !escaped && !raw) {
                escaped = true;
            } else {
                escaped = false;
            }
        }
        throw new ConverterException("Reached end of input before the string literal was closed.");
    }

    private boolean looksLikeStringLiteralStart(int pos) {
        if (pos >= src.length()) {
            return false;
        }
        char c = src.charAt(pos);
        return (ConverterUtils.isQuotationChar(c) || c == 'r' && pos < src.length() + 1 && ConverterUtils
                .isQuotationChar(src.charAt(pos + 1)));
    }

    private String readIdentifier(int startPos) throws ConverterException {
        return src.substring(startPos, getPositionAfterIdentifier(startPos));
    }

    private String readUntilWSOrComment(int startPos, int endPosition) throws ConverterException {
        int pos = startPos;
        while (pos < endPosition && !Character.isWhitespace(src.charAt(pos)) && !isExpCommentStart(pos)) {
            pos++;
        }
        return src.substring(startPos, pos);
    }

    /**
     * Because FM2 has this glitch where tags starting wit {@code <} can be closed with an unparied {@code ]}, we
     * have to do this more complicated check.
     */
    private boolean isTagEndChar(char c) {
        return c == tagEndChar || c == ']';
    }

    private boolean isExpCommentStart(int pos) {
        char c = src.charAt(pos);
        return (c == '<' || c == '[')
                && (pos + 1 < src.length()
                && src.startsWith("!--", pos + 1) || src.startsWith("#--", pos + 1));

    }

    private HashMap<String, Boolean> containsUpperCaseLetterResults = new HashMap<>();

    private boolean containsUpperCaseLetter(String s) {
        Boolean result = containsUpperCaseLetterResults.get(s);
        if (result != null) {
            return result;
        }

        int i = 0;
        while (i < s.length() && !ConverterUtils.isUpperCaseLetter(s.charAt(i))) {
            i++;
        }
        result = i < s.length();

        containsUpperCaseLetterResults.put(s, result);
        return result;
    }

    public static class Result {
        private final Template fm2Template;
        private final String fm3Content;

        public Result(Template fm2Template, String fm3Content) {
            this.fm2Template = fm2Template;
            this.fm3Content = fm3Content;
        }

        public Template getFM2Template() {
            return fm2Template;
        }

        public String getFM3Content() {
            return fm3Content;
        }
    }

}
