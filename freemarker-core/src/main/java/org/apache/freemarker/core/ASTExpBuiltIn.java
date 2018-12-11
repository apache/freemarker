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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.freemarker.core.BuiltInsForDates.iso_BI;
import org.apache.freemarker.core.BuiltInsForDates.iso_utc_or_local_BI;
import org.apache.freemarker.core.BuiltInsForMarkupOutputs.markup_stringBI;
import org.apache.freemarker.core.BuiltInsForMultipleTypes.is_dateLikeBI;
import org.apache.freemarker.core.BuiltInsForNodes.ancestorsBI;
import org.apache.freemarker.core.BuiltInsForNodes.childrenBI;
import org.apache.freemarker.core.BuiltInsForNodes.nextSiblingBI;
import org.apache.freemarker.core.BuiltInsForNodes.node_nameBI;
import org.apache.freemarker.core.BuiltInsForNodes.node_namespaceBI;
import org.apache.freemarker.core.BuiltInsForNodes.node_typeBI;
import org.apache.freemarker.core.BuiltInsForNodes.parentBI;
import org.apache.freemarker.core.BuiltInsForNodes.previousSiblingBI;
import org.apache.freemarker.core.BuiltInsForNodes.rootBI;
import org.apache.freemarker.core.BuiltInsForNumbers.absBI;
import org.apache.freemarker.core.BuiltInsForNumbers.byteBI;
import org.apache.freemarker.core.BuiltInsForNumbers.ceilingBI;
import org.apache.freemarker.core.BuiltInsForNumbers.doubleBI;
import org.apache.freemarker.core.BuiltInsForNumbers.floatBI;
import org.apache.freemarker.core.BuiltInsForNumbers.floorBI;
import org.apache.freemarker.core.BuiltInsForNumbers.intBI;
import org.apache.freemarker.core.BuiltInsForNumbers.is_infiniteBI;
import org.apache.freemarker.core.BuiltInsForNumbers.is_nanBI;
import org.apache.freemarker.core.BuiltInsForNumbers.longBI;
import org.apache.freemarker.core.BuiltInsForNumbers.number_to_dateBI;
import org.apache.freemarker.core.BuiltInsForNumbers.roundBI;
import org.apache.freemarker.core.BuiltInsForNumbers.shortBI;
import org.apache.freemarker.core.BuiltInsForOutputFormatRelated.escBI;
import org.apache.freemarker.core.BuiltInsForOutputFormatRelated.no_escBI;
import org.apache.freemarker.core.BuiltInsForSequences.chunkBI;
import org.apache.freemarker.core.BuiltInsForSequences.firstBI;
import org.apache.freemarker.core.BuiltInsForSequences.lastBI;
import org.apache.freemarker.core.BuiltInsForSequences.reverseBI;
import org.apache.freemarker.core.BuiltInsForSequences.seq_containsBI;
import org.apache.freemarker.core.BuiltInsForSequences.seq_index_ofBI;
import org.apache.freemarker.core.BuiltInsForSequences.sortBI;
import org.apache.freemarker.core.BuiltInsForSequences.sort_byBI;
import org.apache.freemarker.core.BuiltInsForStringsMisc.evalBI;
import org.apache.freemarker.core.model.TemplateCallableModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateModelWithOriginName;
import org.apache.freemarker.core.util._DateUtils;
import org.apache.freemarker.core.util._StringUtils;

/**
 * AST expression node: {@code exp?name}
 */
abstract class ASTExpBuiltIn extends ASTExpression implements Cloneable {
    
    protected ASTExpression target;
    protected String key;

    static final int NUMBER_OF_BIS = 265;
    static final HashMap<String, ASTExpBuiltIn> BUILT_INS_BY_NAME = new HashMap(NUMBER_OF_BIS * 3 / 2 + 1, 1f);

    static {
        // Note that you must update NUMBER_OF_BIS if you add new items here!
        
        putBI("abs", new absBI());
        putBI("absoluteTemplateName", new BuiltInsForStringsMisc.absolute_template_nameBI());
        putBI("ancestors", new ancestorsBI());
        putBI("api", new BuiltInsForMultipleTypes.apiBI());
        putBI("boolean", new BuiltInsForStringsMisc.booleanBI());
        putBI("byte", new byteBI());
        putBI("c", new BuiltInsForMultipleTypes.cBI());
        putBI("capFirst", new BuiltInsForStringsBasic.cap_firstBI());
        putBI("capitalize", new BuiltInsForStringsBasic.capitalizeBI());
        putBI("ceiling", new ceilingBI());
        putBI("children", new childrenBI());
        putBI("chopLinebreak", new BuiltInsForStringsBasic.chop_linebreakBI());
        putBI("contains", new BuiltInsForStringsBasic.containsBI());        
        putBI("date", new BuiltInsForMultipleTypes.dateBI(TemplateDateModel.DATE));
        putBI("dateIfUnknown", new BuiltInsForDates.dateType_if_unknownBI(TemplateDateModel.DATE));
        putBI("dateTime", new BuiltInsForMultipleTypes.dateBI(TemplateDateModel.DATE_TIME));
        putBI("dateTimeIfUnknown", new BuiltInsForDates.dateType_if_unknownBI(TemplateDateModel.DATE_TIME));
        putBI("double", new doubleBI());
        putBI("endsWith", new BuiltInsForStringsBasic.ends_withBI());
        putBI("ensureEndsWith", new BuiltInsForStringsBasic.ensure_ends_withBI());
        putBI("ensureStartsWith", new BuiltInsForStringsBasic.ensure_starts_withBI());
        putBI("esc", new escBI());
        putBI("eval", new evalBI());
        putBI("first", new firstBI());
        putBI("float", new floatBI());
        putBI("floor", new floorBI());
        putBI("chunk", new chunkBI());
        putBI("counter", new BuiltInsForNestedContentParameters.counterBI());
        putBI("itemCycle", new BuiltInsForNestedContentParameters.item_cycleBI());
        putBI("hasApi", new BuiltInsForMultipleTypes.has_apiBI());
        putBI("hasContent", new BuiltInsForExistenceHandling.has_contentBI());
        putBI("hasNext", new BuiltInsForNestedContentParameters.has_nextBI());
        putBI("html", new BuiltInsForStringsEncoding.htmlBI());
        putBI("index", new BuiltInsForNestedContentParameters.indexBI());
        putBI("indexOf", new BuiltInsForStringsBasic.index_ofBI(false));
        putBI("int", new intBI());
        putBI("interpret", new BuiltInsForStringsMisc.interpretBI());
        putBI("isBoolean", new BuiltInsForMultipleTypes.is_booleanBI());
        putBI("isIterable", new BuiltInsForMultipleTypes.is_iterableBI());
        putBI("isCollection", new BuiltInsForMultipleTypes.is_collectionBI());
        is_dateLikeBI bi = new BuiltInsForMultipleTypes.is_dateLikeBI();
        putBI("isDate", bi);  // misnomer
        putBI("isDateLike", bi);
        putBI("isDateOnly", new BuiltInsForMultipleTypes.is_dateOfTypeBI(TemplateDateModel.DATE));
        putBI("isEvenItem", new BuiltInsForNestedContentParameters.is_even_itemBI());
        putBI("isFirst", new BuiltInsForNestedContentParameters.is_firstBI());
        putBI("isLast", new BuiltInsForNestedContentParameters.is_lastBI());
        putBI("isUnknownDateLike", new BuiltInsForMultipleTypes.is_dateOfTypeBI(TemplateDateModel.UNKNOWN));
        putBI("isDatetime", new BuiltInsForMultipleTypes.is_dateOfTypeBI(TemplateDateModel.DATE_TIME));
        putBI("isDirective", new BuiltInsForMultipleTypes.is_directiveBI());
        putBI("isHashEx", new BuiltInsForMultipleTypes.is_hash_exBI());
        putBI("isHash", new BuiltInsForMultipleTypes.is_hashBI());
        putBI("isInfinite", new is_infiniteBI());
        putBI("isMarkupOutput", new BuiltInsForMultipleTypes.is_markup_outputBI());
        putBI("isFunction", new BuiltInsForMultipleTypes.is_functionBI());
        putBI("isNan", new is_nanBI());
        putBI("isNode", new BuiltInsForMultipleTypes.is_nodeBI());
        putBI("isNumber", new BuiltInsForMultipleTypes.is_numberBI());
        putBI("isOddItem", new BuiltInsForNestedContentParameters.is_odd_itemBI());
        putBI("isSequence", new BuiltInsForMultipleTypes.is_sequenceBI());
        putBI("isString", new BuiltInsForMultipleTypes.is_stringBI());
        putBI("isTime", new BuiltInsForMultipleTypes.is_dateOfTypeBI(TemplateDateModel.TIME));

        putBI("isoUtc", new iso_utc_or_local_BI(
                /* showOffset = */ null, _DateUtils.ACCURACY_SECONDS, /* useUTC = */ true));
        putBI("isoUtcFZ", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.TRUE, _DateUtils.ACCURACY_SECONDS, /* useUTC = */ true));
        putBI("isoUtcNZ", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, _DateUtils.ACCURACY_SECONDS, /* useUTC = */ true));
        
        putBI("isoUtcMs", new iso_utc_or_local_BI(
                /* showOffset = */ null, _DateUtils.ACCURACY_MILLISECONDS, /* useUTC = */ true));
        putBI("isoUtcMsNZ", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, _DateUtils.ACCURACY_MILLISECONDS, /* useUTC = */ true));
        
        putBI("isoUtcM", new iso_utc_or_local_BI(
                /* showOffset = */ null, _DateUtils.ACCURACY_MINUTES, /* useUTC = */ true));
        putBI("isoUtcMNZ", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, _DateUtils.ACCURACY_MINUTES, /* useUTC = */ true));
        
        putBI("isoUtcH", new iso_utc_or_local_BI(
                /* showOffset = */ null, _DateUtils.ACCURACY_HOURS, /* useUTC = */ true));
        putBI("isoUtcHNZ", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, _DateUtils.ACCURACY_HOURS, /* useUTC = */ true));
        
        putBI("isoLocal", new iso_utc_or_local_BI(
                /* showOffset = */ null, _DateUtils.ACCURACY_SECONDS, /* useUTC = */ false));
        putBI("isoLocalNZ", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, _DateUtils.ACCURACY_SECONDS, /* useUTC = */ false));
        
        putBI("isoLocalMs", new iso_utc_or_local_BI(
                /* showOffset = */ null, _DateUtils.ACCURACY_MILLISECONDS, /* useUTC = */ false));
        putBI("isoLocalMsNZ", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, _DateUtils.ACCURACY_MILLISECONDS, /* useUTC = */ false));
        
        putBI("isoLocalM", new iso_utc_or_local_BI(
                /* showOffset = */ null, _DateUtils.ACCURACY_MINUTES, /* useUTC = */ false));
        putBI("isoLocalMNZ", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, _DateUtils.ACCURACY_MINUTES, /* useUTC = */ false));
        
        putBI("isoLocalH", new iso_utc_or_local_BI(
                /* showOffset = */ null, _DateUtils.ACCURACY_HOURS, /* useUTC = */ false));
        putBI("isoLocalHNZ", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, _DateUtils.ACCURACY_HOURS, /* useUTC = */ false));
        
        putBI("iso", new iso_BI(
                /* showOffset = */ null, _DateUtils.ACCURACY_SECONDS));
        putBI("isoNZ", new iso_BI(
                /* showOffset = */ Boolean.FALSE, _DateUtils.ACCURACY_SECONDS));
        
        putBI("isoMs", new iso_BI(
                /* showOffset = */ null, _DateUtils.ACCURACY_MILLISECONDS));
        putBI("isoMsNZ", new iso_BI(
                /* showOffset = */ Boolean.FALSE, _DateUtils.ACCURACY_MILLISECONDS));
        
        putBI("isoM", new iso_BI(
                /* showOffset = */ null, _DateUtils.ACCURACY_MINUTES));
        putBI("isoMNZ", new iso_BI(
                /* showOffset = */ Boolean.FALSE, _DateUtils.ACCURACY_MINUTES));
        
        putBI("isoH", new iso_BI(
                /* showOffset = */ null, _DateUtils.ACCURACY_HOURS));
        putBI("isoHNZ", new iso_BI(
                /* showOffset = */ Boolean.FALSE, _DateUtils.ACCURACY_HOURS));
        
        putBI("jString", new BuiltInsForStringsEncoding.j_stringBI());
        putBI("join", new BuiltInsForSequences.joinBI());
        putBI("jsString", new BuiltInsForStringsEncoding.js_stringBI());
        putBI("jsonString", new BuiltInsForStringsEncoding.json_stringBI());
        putBI("keepAfter", new BuiltInsForStringsBasic.keep_afterBI());
        putBI("keepBefore", new BuiltInsForStringsBasic.keep_beforeBI());
        putBI("keepAfterLast", new BuiltInsForStringsBasic.keep_after_lastBI());
        putBI("keepBeforeLast", new BuiltInsForStringsBasic.keep_before_lastBI());
        putBI("keys", new BuiltInsForHashes.keysBI());
        putBI("lastIndexOf", new BuiltInsForStringsBasic.index_ofBI(true));
        putBI("last", new lastBI());
        putBI("leftPad", new BuiltInsForStringsBasic.padBI(true));
        putBI("length", new BuiltInsForStringsBasic.lengthBI());
        putBI("long", new longBI());
        putBI("lowerAbc", new BuiltInsForNumbers.lower_abcBI());
        putBI("lowerCase", new BuiltInsForStringsBasic.lower_caseBI());
        putBI("namespace", new BuiltInsForMultipleTypes.namespaceBI());
        putBI("new", new BuiltInsForStringsMisc.newBI());
        putBI("markupString", new markup_stringBI());
        putBI("nodeName", new node_nameBI());
        putBI("nodeNamespace", new node_namespaceBI());
        putBI("nodeType", new node_typeBI());
        putBI("noEsc", new no_escBI());
        putBI("max", new BuiltInsForSequences.maxBI());
        putBI("min", new BuiltInsForSequences.minBI());
        putBI("number", new BuiltInsForStringsMisc.numberBI());
        putBI("numberToDate", new number_to_dateBI(TemplateDateModel.DATE));
        putBI("numberToTime", new number_to_dateBI(TemplateDateModel.TIME));
        putBI("numberToDatetime", new number_to_dateBI(TemplateDateModel.DATE_TIME));
        putBI("parent", new parentBI());
        putBI("previousSibling", new previousSiblingBI());
        putBI("nextSibling", new nextSiblingBI());
        putBI("itemParity", new BuiltInsForNestedContentParameters.item_parityBI());
        putBI("itemParityCap", new BuiltInsForNestedContentParameters.item_parity_capBI());
        putBI("reverse", new reverseBI());
        putBI("rightPad", new BuiltInsForStringsBasic.padBI(false));
        putBI("root", new rootBI());
        putBI("round", new roundBI());
        putBI("removeEnding", new BuiltInsForStringsBasic.remove_endingBI());
        putBI("removeBeginning", new BuiltInsForStringsBasic.remove_beginningBI());
        putBI("rtf", new BuiltInsForStringsEncoding.rtfBI());
        putBI("sequence", new BuiltInsForSequences.sequenceBI());
        putBI("seqContains", new seq_containsBI());
        putBI("seqIndexOf", new seq_index_ofBI(true));
        putBI("seqLastIndexOf", new seq_index_ofBI(false));
        putBI("short", new shortBI());
        putBI("size", new BuiltInsForMultipleTypes.sizeBI());
        putBI("sortBy", new sort_byBI());
        putBI("sort", new sortBI());
        putBI("split", new BuiltInsForStringsBasic.split_BI());
        putBI("switch", new BuiltInsWithParseTimeParameters.switch_BI());
        putBI("startsWith", new BuiltInsForStringsBasic.starts_withBI());
        putBI("string", new BuiltInsForMultipleTypes.stringBI());
        putBI("substring", new BuiltInsForStringsBasic.substringBI());
        putBI("then", new BuiltInsWithParseTimeParameters.then_BI());
        putBI("time", new BuiltInsForMultipleTypes.dateBI(TemplateDateModel.TIME));
        putBI("timeIfUnknown", new BuiltInsForDates.dateType_if_unknownBI(TemplateDateModel.TIME));
        putBI("trim", new BuiltInsForStringsBasic.trimBI());
        putBI("uncapFirst", new BuiltInsForStringsBasic.uncap_firstBI());
        putBI("upperAbc", new BuiltInsForNumbers.upper_abcBI());
        putBI("upperCase", new BuiltInsForStringsBasic.upper_caseBI());
        putBI("url", new BuiltInsForStringsEncoding.urlBI());
        putBI("urlPath", new BuiltInsForStringsEncoding.urlPathBI());
        putBI("values", new BuiltInsForHashes.valuesBI());
        putBI("wordList", new BuiltInsForStringsBasic.word_listBI());
        putBI("xhtml", new BuiltInsForStringsEncoding.xhtmlBI());
        putBI("xml", new BuiltInsForStringsEncoding.xmlBI());
        putBI("matches", new BuiltInsForStringsRegexp.matchesBI());
        putBI("groups", new BuiltInsForStringsRegexp.groupsBI());
        putBI("replace", new BuiltInsForStringsRegexp.replace_reBI());

        
        if (NUMBER_OF_BIS < BUILT_INS_BY_NAME.size()) {
            throw new AssertionError("Update NUMBER_OF_BIS! Should be: " + BUILT_INS_BY_NAME.size());
        }
    }
    
    private static void putBI(String name, ASTExpBuiltIn bi) {
        BUILT_INS_BY_NAME.put(name, bi);
    }

    /**
     * @param target
     *            Left-hand-operand expression
     * @param keyTk
     *            Built-in name token
     */
    static ASTExpBuiltIn newBuiltIn(int incompatibleImprovements, ASTExpression target, Token keyTk,
            FMParserTokenManager tokenManager) throws ParseException {
        String key = keyTk.image;
        ASTExpBuiltIn bi = BUILT_INS_BY_NAME.get(key);
        if (bi == null) {
            StringBuilder sb = new StringBuilder("Unknown built-in: ").append(_StringUtils.jQuote(key)).append(".");

            String correctedKey;
            if (key.indexOf("_") != -1) {
                sb.append(MessageUtils.FM3_SNAKE_CASE);
                correctedKey = _StringUtils.snakeCaseToCamelCase(key);
                if (!BUILT_INS_BY_NAME.containsKey(correctedKey)) {
                    if (correctedKey.length() > 1) {
                        correctedKey = correctedKey.substring(0, correctedKey.length() - 2)
                                + correctedKey.substring(correctedKey.length() - 2).toUpperCase();
                        if (!BUILT_INS_BY_NAME.containsKey(correctedKey)) {
                            if (key.equals("datetime_if_unknown")) {
                                correctedKey = "dateTimeIfUnknown";
                            } else {
                                correctedKey = null;
                            }
                        }
                    } else {
                        correctedKey = null;
                    }
                }
            } else if (key.equals("datetime")) {
                correctedKey = "dateTime";
            } else if (key.equals("datetimeIfUnknown")) {
                correctedKey = "dateTimeIfUnknown";
            } else {
                correctedKey = null;
            }

            if (correctedKey != null) {
                sb.append("\nThe correct name is: ").append(correctedKey);
            } else if (key.equals("exists")) {
                sb.append("\nUse someExpression?? instead of someExpression?exists.");
            } else if (key.equals("ifExists") || key.equals("if_exists")) {
                sb.append("\nUse someExpression! instead of someExpression?" + key + ".");
            } else if (key.equals("default")) {
                sb.append("\nUse someExpression!defaultExpression instead of "
                        + "someExpression?default(defaultExpression), or someExpression!(defaultExpression) if "
                        + "defaultExpression contains operators that have lower precedence than the default value "
                        + "operator (!). Also note that instead of x?default(y, z), you can write x!y!z.");
            } else {
                sb.append(
                        "\nHelp (latest version): http://freemarker.org/docs/ref_builtins.html; "
                                + "you're using FreeMarker ").append(Configuration.getVersion()).append(".\n"
                        + "The alphabetical list of built-ins:");
                List<String> names = new ArrayList<>(BUILT_INS_BY_NAME.keySet().size());
                names.addAll(BUILT_INS_BY_NAME.keySet());
                Collections.sort(names);
                char lastLetter = 0;

                boolean first = true;
                for (String correctName : names) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(", ");
                    }

                    char firstChar = correctName.charAt(0);
                    if (firstChar != lastLetter) {
                        lastLetter = firstChar;
                        sb.append('\n');
                    }
                    sb.append(correctName);
                }
            }
                
            throw new ParseException(sb.toString(), null, keyTk);
        }
        
        try {
            bi = (ASTExpBuiltIn) bi.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
        bi.key = key;
        bi.target = target;
        return bi;
    }

    @Override
    public String getCanonicalForm() {
        return target.getCanonicalForm() + "?" + key;
    }
    
    @Override
    public String getLabelWithoutParameters() {
        return "?" + key;
    }

    @Override
    boolean isLiteral() {
        return false;
    }
    
    @Override
    ASTExpression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, ASTExpression replacement, ReplacemenetState replacementState) {
    	try {
	    	ASTExpBuiltIn clone = (ASTExpBuiltIn) clone();
	    	clone.target = target.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState);
	    	return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Internal error: " + e);
        }
    }

    @Override
    int getParameterCount() {
        return 2;
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return target;
        case 1: return key;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.LEFT_HAND_OPERAND;
        case 1: return ParameterRole.RIGHT_HAND_OPERAND;
        default: throw new IndexOutOfBoundsException();
        }
    }

    /** */
    interface BuiltInCallable extends TemplateModelWithOriginName {
        String getBuiltInName();
    }

    abstract class BuiltInCallableImpl implements TemplateCallableModel, BuiltInCallable {
        @Override
        public String getBuiltInName() {
            return key;
        }

        @Override
        public String getOriginName() {
            return ASTExpBuiltIn.getOriginName(this);
        }
    }

    static String getOriginName(BuiltInCallable lThis) {
        return  "?" + lThis.getBuiltInName();
    }

}
