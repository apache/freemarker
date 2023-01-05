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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import freemarker.core.BuiltInsForDates.iso_BI;
import freemarker.core.BuiltInsForDates.iso_utc_or_local_BI;
import freemarker.core.BuiltInsForMarkupOutputs.markup_stringBI;
import freemarker.core.BuiltInsForMultipleTypes.is_dateLikeBI;
import freemarker.core.BuiltInsForNodes.ancestorsBI;
import freemarker.core.BuiltInsForNodes.childrenBI;
import freemarker.core.BuiltInsForNodes.nextSiblingBI;
import freemarker.core.BuiltInsForNodes.node_nameBI;
import freemarker.core.BuiltInsForNodes.node_namespaceBI;
import freemarker.core.BuiltInsForNodes.node_typeBI;
import freemarker.core.BuiltInsForNodes.parentBI;
import freemarker.core.BuiltInsForNodes.previousSiblingBI;
import freemarker.core.BuiltInsForNodes.rootBI;
import freemarker.core.BuiltInsForNumbers.absBI;
import freemarker.core.BuiltInsForNumbers.byteBI;
import freemarker.core.BuiltInsForNumbers.ceilingBI;
import freemarker.core.BuiltInsForNumbers.doubleBI;
import freemarker.core.BuiltInsForNumbers.floatBI;
import freemarker.core.BuiltInsForNumbers.floorBI;
import freemarker.core.BuiltInsForNumbers.intBI;
import freemarker.core.BuiltInsForNumbers.is_infiniteBI;
import freemarker.core.BuiltInsForNumbers.is_nanBI;
import freemarker.core.BuiltInsForNumbers.longBI;
import freemarker.core.BuiltInsForNumbers.number_to_dateBI;
import freemarker.core.BuiltInsForNumbers.roundBI;
import freemarker.core.BuiltInsForNumbers.shortBI;
import freemarker.core.BuiltInsForOutputFormatRelated.escBI;
import freemarker.core.BuiltInsForOutputFormatRelated.no_escBI;
import freemarker.core.BuiltInsForSequences.chunkBI;
import freemarker.core.BuiltInsForSequences.firstBI;
import freemarker.core.BuiltInsForSequences.lastBI;
import freemarker.core.BuiltInsForSequences.reverseBI;
import freemarker.core.BuiltInsForSequences.seq_containsBI;
import freemarker.core.BuiltInsForSequences.seq_index_ofBI;
import freemarker.core.BuiltInsForSequences.sequenceBI;
import freemarker.core.BuiltInsForSequences.sortBI;
import freemarker.core.BuiltInsForSequences.sort_byBI;
import freemarker.core.BuiltInsForStringsMisc.evalBI;
import freemarker.core.BuiltInsForStringsMisc.evalJsonBI;
import freemarker.template.Configuration;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.utility.DateUtil;
import freemarker.template.utility.StringUtil;

/**
 * The {@code ?} operator used for things like {@code foo?upper_case}.
 */
abstract class BuiltIn extends Expression implements Cloneable {
    
    protected Expression target;
    protected String key;

    static final Set<String> CAMEL_CASE_NAMES = new TreeSet<>();
    static final Set<String> SNAKE_CASE_NAMES = new TreeSet<>();
    static final int NUMBER_OF_BIS = 296;
    static final HashMap<String, BuiltIn> BUILT_INS_BY_NAME = new HashMap<>(NUMBER_OF_BIS * 3 / 2 + 1, 1f);

    static final String BI_NAME_SNAKE_CASE_WITH_ARGS = "with_args";
    static final String BI_NAME_CAMEL_CASE_WITH_ARGS = "withArgs";
    static final String BI_NAME_SNAKE_CASE_WITH_ARGS_LAST = "with_args_last";
    static final String BI_NAME_CAMEL_CASE_WITH_ARGS_LAST = "withArgsLast";

    static {
        // Note that you must update NUMBER_OF_BIS if you add new items here!
        
        putBI("abs", new absBI());
        putBI("absolute_template_name", "absoluteTemplateName", new BuiltInsForStringsMisc.absolute_template_nameBI());
        putBI("ancestors", new ancestorsBI());
        putBI("api", new BuiltInsForMultipleTypes.apiBI());
        putBI("boolean", new BuiltInsForStringsMisc.booleanBI());
        putBI("byte", new byteBI());
        putBI("c", new BuiltInsForMultipleTypes.cBI());
        putBI("cn", new BuiltInsForMultipleTypes.cnBI());
        putBI("cap_first", "capFirst", new BuiltInsForStringsBasic.cap_firstBI());
        putBI("capitalize", new BuiltInsForStringsBasic.capitalizeBI());
        putBI("ceiling", new ceilingBI());
        putBI("children", new childrenBI());
        putBI("chop_linebreak", "chopLinebreak", new BuiltInsForStringsBasic.chop_linebreakBI());
        putBI("contains", new BuiltInsForStringsBasic.containsBI());        
        putBI("date", new BuiltInsForMultipleTypes.dateBI(TemplateDateModel.DATE));
        putBI("date_if_unknown", "dateIfUnknown", new BuiltInsForDates.dateType_if_unknownBI(TemplateDateModel.DATE));
        putBI("datetime", new BuiltInsForMultipleTypes.dateBI(TemplateDateModel.DATETIME));
        putBI("datetime_if_unknown", "datetimeIfUnknown", new BuiltInsForDates.dateType_if_unknownBI(TemplateDateModel.DATETIME));
        putBI("default", new BuiltInsForExistenceHandling.defaultBI());
        putBI("double", new doubleBI());
        putBI("drop_while", "dropWhile", new BuiltInsForSequences.drop_whileBI());
        putBI("ends_with", "endsWith", new BuiltInsForStringsBasic.ends_withBI());
        putBI("ensure_ends_with", "ensureEndsWith", new BuiltInsForStringsBasic.ensure_ends_withBI());
        putBI("ensure_starts_with", "ensureStartsWith", new BuiltInsForStringsBasic.ensure_starts_withBI());
        putBI("esc", new escBI());
        putBI("eval", new evalBI());
        putBI("eval_json", "evalJson", new evalJsonBI());
        putBI("exists", new BuiltInsForExistenceHandling.existsBI());
        putBI("filter", new BuiltInsForSequences.filterBI());
        putBI("first", new firstBI());
        putBI("float", new floatBI());
        putBI("floor", new floorBI());
        putBI("chunk", new chunkBI());
        putBI("counter", new BuiltInsForLoopVariables.counterBI());
        putBI("item_cycle", "itemCycle", new BuiltInsForLoopVariables.item_cycleBI());
        putBI("has_api", "hasApi", new BuiltInsForMultipleTypes.has_apiBI());
        putBI("has_content", "hasContent", new BuiltInsForExistenceHandling.has_contentBI());
        putBI("has_next", "hasNext", new BuiltInsForLoopVariables.has_nextBI());
        putBI("html", new BuiltInsForStringsEncoding.htmlBI());
        putBI("if_exists", "ifExists", new BuiltInsForExistenceHandling.if_existsBI());
        putBI("index", new BuiltInsForLoopVariables.indexBI());
        putBI("index_of", "indexOf", new BuiltInsForStringsBasic.index_ofBI(false));
        putBI("int", new intBI());
        putBI("interpret", new Interpret());
        putBI("is_boolean", "isBoolean", new BuiltInsForMultipleTypes.is_booleanBI());
        putBI("is_collection", "isCollection", new BuiltInsForMultipleTypes.is_collectionBI());
        putBI("is_collection_ex", "isCollectionEx", new BuiltInsForMultipleTypes.is_collection_exBI());
        is_dateLikeBI bi = new BuiltInsForMultipleTypes.is_dateLikeBI();
        putBI("is_date", "isDate", bi);  // misnomer
        putBI("is_date_like", "isDateLike", bi);
        putBI("is_date_only", "isDateOnly", new BuiltInsForMultipleTypes.is_dateOfTypeBI(TemplateDateModel.DATE));
        putBI("is_even_item", "isEvenItem", new BuiltInsForLoopVariables.is_even_itemBI());
        putBI("is_first", "isFirst", new BuiltInsForLoopVariables.is_firstBI());
        putBI("is_last", "isLast", new BuiltInsForLoopVariables.is_lastBI());
        putBI("is_unknown_date_like", "isUnknownDateLike", new BuiltInsForMultipleTypes.is_dateOfTypeBI(TemplateDateModel.UNKNOWN));
        putBI("is_datetime", "isDatetime", new BuiltInsForMultipleTypes.is_dateOfTypeBI(TemplateDateModel.DATETIME));
        putBI("is_directive", "isDirective", new BuiltInsForMultipleTypes.is_directiveBI());
        putBI("is_enumerable", "isEnumerable", new BuiltInsForMultipleTypes.is_enumerableBI());
        putBI("is_hash_ex", "isHashEx", new BuiltInsForMultipleTypes.is_hash_exBI());
        putBI("is_hash", "isHash", new BuiltInsForMultipleTypes.is_hashBI());
        putBI("is_infinite", "isInfinite", new is_infiniteBI());
        putBI("is_indexable", "isIndexable", new BuiltInsForMultipleTypes.is_indexableBI());
        putBI("is_macro", "isMacro", new BuiltInsForMultipleTypes.is_macroBI());
        putBI("is_markup_output", "isMarkupOutput", new BuiltInsForMultipleTypes.is_markup_outputBI());
        putBI("is_method", "isMethod", new BuiltInsForMultipleTypes.is_methodBI());
        putBI("is_nan", "isNan", new is_nanBI());
        putBI("is_node", "isNode", new BuiltInsForMultipleTypes.is_nodeBI());
        putBI("is_number", "isNumber", new BuiltInsForMultipleTypes.is_numberBI());
        putBI("is_odd_item", "isOddItem", new BuiltInsForLoopVariables.is_odd_itemBI());
        putBI("is_sequence", "isSequence", new BuiltInsForMultipleTypes.is_sequenceBI());
        putBI("is_string", "isString", new BuiltInsForMultipleTypes.is_stringBI());
        putBI("is_time", "isTime", new BuiltInsForMultipleTypes.is_dateOfTypeBI(TemplateDateModel.TIME));
        putBI("is_transform", "isTransform", new BuiltInsForMultipleTypes.is_transformBI());
        
        putBI("iso_utc", "isoUtc", new iso_utc_or_local_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_SECONDS, /* useUTC = */ true));
        putBI("iso_utc_fz", "isoUtcFZ", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.TRUE, DateUtil.ACCURACY_SECONDS, /* useUTC = */ true));
        putBI("iso_utc_nz", "isoUtcNZ", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_SECONDS, /* useUTC = */ true));
        
        putBI("iso_utc_ms", "isoUtcMs", new iso_utc_or_local_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_MILLISECONDS, /* useUTC = */ true));
        putBI("iso_utc_ms_nz", "isoUtcMsNZ", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_MILLISECONDS, /* useUTC = */ true));
        
        putBI("iso_utc_m", "isoUtcM", new iso_utc_or_local_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_MINUTES, /* useUTC = */ true));
        putBI("iso_utc_m_nz", "isoUtcMNZ", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_MINUTES, /* useUTC = */ true));
        
        putBI("iso_utc_h", "isoUtcH", new iso_utc_or_local_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_HOURS, /* useUTC = */ true));
        putBI("iso_utc_h_nz", "isoUtcHNZ", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_HOURS, /* useUTC = */ true));
        
        putBI("iso_local", "isoLocal", new iso_utc_or_local_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_SECONDS, /* useUTC = */ false));
        putBI("iso_local_nz", "isoLocalNZ", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_SECONDS, /* useUTC = */ false));
        
        putBI("iso_local_ms", "isoLocalMs", new iso_utc_or_local_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_MILLISECONDS, /* useUTC = */ false));
        putBI("iso_local_ms_nz", "isoLocalMsNZ", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_MILLISECONDS, /* useUTC = */ false));
        
        putBI("iso_local_m", "isoLocalM", new iso_utc_or_local_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_MINUTES, /* useUTC = */ false));
        putBI("iso_local_m_nz", "isoLocalMNZ", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_MINUTES, /* useUTC = */ false));
        
        putBI("iso_local_h", "isoLocalH", new iso_utc_or_local_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_HOURS, /* useUTC = */ false));
        putBI("iso_local_h_nz", "isoLocalHNZ", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_HOURS, /* useUTC = */ false));
        
        putBI("iso", new iso_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_SECONDS));
        putBI("iso_nz", "isoNZ", new iso_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_SECONDS));
        
        putBI("iso_ms", "isoMs", new iso_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_MILLISECONDS));
        putBI("iso_ms_nz", "isoMsNZ", new iso_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_MILLISECONDS));
        
        putBI("iso_m", "isoM", new iso_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_MINUTES));
        putBI("iso_m_nz", "isoMNZ", new iso_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_MINUTES));
        
        putBI("iso_h", "isoH", new iso_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_HOURS));
        putBI("iso_h_nz", "isoHNZ", new iso_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_HOURS));
        
        putBI("j_string", "jString", new BuiltInsForStringsEncoding.j_stringBI());
        putBI("join", new BuiltInsForSequences.joinBI());
        putBI("js_string", "jsString", new BuiltInsForStringsEncoding.js_stringBI());
        putBI("json_string", "jsonString", new BuiltInsForStringsEncoding.json_stringBI());
        putBI("keep_after", "keepAfter", new BuiltInsForStringsBasic.keep_afterBI());
        putBI("keep_before", "keepBefore", new BuiltInsForStringsBasic.keep_beforeBI());
        putBI("keep_after_last", "keepAfterLast", new BuiltInsForStringsBasic.keep_after_lastBI());
        putBI("keep_before_last", "keepBeforeLast", new BuiltInsForStringsBasic.keep_before_lastBI());
        putBI("keys", new BuiltInsForHashes.keysBI());
        putBI("last_index_of", "lastIndexOf", new BuiltInsForStringsBasic.index_ofBI(true));
        putBI("last", new lastBI());
        putBI("left_pad", "leftPad", new BuiltInsForStringsBasic.padBI(true));
        putBI("length", new BuiltInsForStringsBasic.lengthBI());
        putBI("long", new longBI());
        putBI("lower_abc", "lowerAbc", new BuiltInsForNumbers.lower_abcBI());
        putBI("lower_case", "lowerCase", new BuiltInsForStringsBasic.lower_caseBI());
        putBI("c_lower_case", "cLowerCase", new BuiltInsForStringsBasic.c_lower_caseBI());
        putBI("map", new BuiltInsForSequences.mapBI());
        putBI("namespace", new BuiltInsForMultipleTypes.namespaceBI());
        putBI("new", new NewBI());
        putBI("markup_string", "markupString", new markup_stringBI());
        putBI("node_name", "nodeName", new node_nameBI());
        putBI("node_namespace", "nodeNamespace", new node_namespaceBI());
        putBI("node_type", "nodeType", new node_typeBI());
        putBI("no_esc", "noEsc", new no_escBI());
        putBI("max", new BuiltInsForSequences.maxBI());
        putBI("min", new BuiltInsForSequences.minBI());
        putBI("number", new BuiltInsForStringsMisc.numberBI());
        putBI("number_to_date", "numberToDate", new number_to_dateBI(TemplateDateModel.DATE));
        putBI("number_to_time", "numberToTime", new number_to_dateBI(TemplateDateModel.TIME));
        putBI("number_to_datetime", "numberToDatetime", new number_to_dateBI(TemplateDateModel.DATETIME));
        putBI("parent", new parentBI());
        putBI("previous_sibling", "previousSibling", new previousSiblingBI());
        putBI("next_sibling", "nextSibling", new nextSiblingBI());
        putBI("item_parity", "itemParity", new BuiltInsForLoopVariables.item_parityBI());
        putBI("item_parity_cap", "itemParityCap", new BuiltInsForLoopVariables.item_parity_capBI());
        putBI("reverse", new reverseBI());
        putBI("right_pad", "rightPad", new BuiltInsForStringsBasic.padBI(false));
        putBI("root", new rootBI());
        putBI("round", new roundBI());
        putBI("remove_ending", "removeEnding", new BuiltInsForStringsBasic.remove_endingBI());
        putBI("remove_beginning", "removeBeginning", new BuiltInsForStringsBasic.remove_beginningBI());
        putBI("rtf", new BuiltInsForStringsEncoding.rtfBI());
        putBI("seq_contains", "seqContains", new seq_containsBI());
        putBI("seq_index_of", "seqIndexOf", new seq_index_ofBI(true));
        putBI("seq_last_index_of", "seqLastIndexOf", new seq_index_ofBI(false));
        putBI("sequence", new sequenceBI());
        putBI("short", new shortBI());
        putBI("size", new BuiltInsForMultipleTypes.sizeBI());
        putBI("sort_by", "sortBy", new sort_byBI());
        putBI("sort", new sortBI());
        putBI("split", new BuiltInsForStringsBasic.split_BI());
        putBI("switch", new BuiltInsWithLazyConditionals.switch_BI());
        putBI("starts_with", "startsWith", new BuiltInsForStringsBasic.starts_withBI());
        putBI("string", new BuiltInsForMultipleTypes.stringBI());
        putBI("substring", new BuiltInsForStringsBasic.substringBI());
        putBI("take_while", "takeWhile", new BuiltInsForSequences.take_whileBI());
        putBI("then", new BuiltInsWithLazyConditionals.then_BI());
        putBI("time", new BuiltInsForMultipleTypes.dateBI(TemplateDateModel.TIME));
        putBI("time_if_unknown", "timeIfUnknown", new BuiltInsForDates.dateType_if_unknownBI(TemplateDateModel.TIME));
        putBI("trim", new BuiltInsForStringsBasic.trimBI());
        putBI("truncate", new BuiltInsForStringsBasic.truncateBI());
        putBI("truncate_w", "truncateW", new BuiltInsForStringsBasic.truncate_wBI());
        putBI("truncate_c", "truncateC", new BuiltInsForStringsBasic.truncate_cBI());
        putBI("truncate_m", "truncateM", new BuiltInsForStringsBasic.truncate_mBI());
        putBI("truncate_w_m", "truncateWM", new BuiltInsForStringsBasic.truncate_w_mBI());
        putBI("truncate_c_m", "truncateCM", new BuiltInsForStringsBasic.truncate_c_mBI());
        putBI("uncap_first", "uncapFirst", new BuiltInsForStringsBasic.uncap_firstBI());
        putBI("upper_abc", "upperAbc", new BuiltInsForNumbers.upper_abcBI());
        putBI("upper_case", "upperCase", new BuiltInsForStringsBasic.upper_caseBI());
        putBI("c_upper_case", "cUpperCase", new BuiltInsForStringsBasic.c_upper_caseBI());
        putBI("url", new BuiltInsForStringsEncoding.urlBI());
        putBI("url_path", "urlPath", new BuiltInsForStringsEncoding.urlPathBI());
        putBI("values", new BuiltInsForHashes.valuesBI());
        putBI("web_safe", "webSafe", BUILT_INS_BY_NAME.get("html"));  // deprecated; use ?html instead
        putBI(BI_NAME_SNAKE_CASE_WITH_ARGS, BI_NAME_CAMEL_CASE_WITH_ARGS,
                new BuiltInsForCallables.with_argsBI());
        putBI(BI_NAME_SNAKE_CASE_WITH_ARGS_LAST, BI_NAME_CAMEL_CASE_WITH_ARGS_LAST,
                new BuiltInsForCallables.with_args_lastBI());
        putBI("word_list", "wordList", new BuiltInsForStringsBasic.word_listBI());
        putBI("xhtml", new BuiltInsForStringsEncoding.xhtmlBI());
        putBI("xml", new BuiltInsForStringsEncoding.xmlBI());
        putBI("matches", new BuiltInsForStringsRegexp.matchesBI());
        putBI("groups", new BuiltInsForStringsRegexp.groupsBI());
        putBI("replace", new BuiltInsForStringsRegexp.replace_reBI());

        
        if (NUMBER_OF_BIS < BUILT_INS_BY_NAME.size()) {
            throw new AssertionError("Update NUMBER_OF_BIS! Should be: " + BUILT_INS_BY_NAME.size());
        }
    }
    
    private static void putBI(String name, BuiltIn bi) {
        BUILT_INS_BY_NAME.put(name, bi);
        SNAKE_CASE_NAMES.add(name);
        CAMEL_CASE_NAMES.add(name);
    }

    private static void putBI(String nameSnakeCase, String nameCamelCase, BuiltIn bi) {
        BUILT_INS_BY_NAME.put(nameSnakeCase, bi);
        BUILT_INS_BY_NAME.put(nameCamelCase, bi);
        SNAKE_CASE_NAMES.add(nameSnakeCase);
        CAMEL_CASE_NAMES.add(nameCamelCase);
    }
    
    /**
     * @param target
     *            Left-hand-operand expression
     * @param keyTk
     *            Built-in name token
     */
    static BuiltIn newBuiltIn(int incompatibleImprovements, Expression target, Token keyTk,
            FMParserTokenManager tokenManager) throws ParseException {
        String key = keyTk.image;
        BuiltIn bi = BUILT_INS_BY_NAME.get(key);
        if (bi == null) {
            StringBuilder buf = new StringBuilder("Unknown built-in: ").append(StringUtil.jQuote(key)).append(". ");
            
            buf.append(
                    "Help (latest version): https://freemarker.apache.org/docs/ref_builtins.html; "
                    + "you're using FreeMarker ").append(Configuration.getVersion()).append(".\n" 
                    + "The alphabetical list of built-ins:");
            List names = new ArrayList(BUILT_INS_BY_NAME.keySet().size());
            names.addAll(BUILT_INS_BY_NAME.keySet());
            Collections.sort(names);
            char lastLetter = 0;
            
            int shownNamingConvention;
            {
                int namingConvention = tokenManager.namingConvention;
                shownNamingConvention = namingConvention != Configuration.AUTO_DETECT_NAMING_CONVENTION
                        ? namingConvention : Configuration.LEGACY_NAMING_CONVENTION /* [2.4] CAMEL_CASE */; 
            }
            
            boolean first = true;
            for (Iterator it = names.iterator(); it.hasNext(); ) {
                String correctName = (String) it.next();
                int correctNameNamingConvetion = _CoreStringUtils.getIdentifierNamingConvention(correctName);
                if (shownNamingConvention == Configuration.CAMEL_CASE_NAMING_CONVENTION 
                        ? correctNameNamingConvetion != Configuration.LEGACY_NAMING_CONVENTION
                        : correctNameNamingConvetion != Configuration.CAMEL_CASE_NAMING_CONVENTION) {
                    if (first) {
                        first = false;
                    } else {
                        buf.append(", ");
                    }
                    
                    char firstChar = correctName.charAt(0);
                    if (firstChar != lastLetter) {
                        lastLetter = firstChar;
                        buf.append('\n');
                    }
                    buf.append(correctName);
                }
            }
                
            throw new ParseException(buf.toString(), null, keyTk);
        }
        
        while (bi instanceof ICIChainMember
                && incompatibleImprovements < ((ICIChainMember) bi).getMinimumICIVersion()) {
            bi = (BuiltIn) ((ICIChainMember) bi).getPreviousICIChainMember();
        }
        
        try {
            bi = (BuiltIn) bi.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
        bi.key = key;
        bi.setTarget(target);
        return bi;
    }

    protected void setTarget(Expression target) {
        this.target = target;
    }

    @Override
    public String getCanonicalForm() {
        return target.getCanonicalForm() + "?" + key;
    }
    
    @Override
    String getNodeTypeSymbol() {
        return "?" + key;
    }

    @Override
    boolean isLiteral() {
        return false; // be on the safe side.
    }
    
    protected final void checkMethodArgCount(List args, int expectedCnt) throws TemplateModelException {
        checkMethodArgCount(args.size(), expectedCnt);
    }
    
    protected final void checkMethodArgCount(int argCnt, int expectedCnt) throws TemplateModelException {
        if (argCnt != expectedCnt) {
            throw _MessageUtil.newArgCntError("?" + key, argCnt, expectedCnt);
        }
    }

    protected final void checkMethodArgCount(List args, int minCnt, int maxCnt) throws TemplateModelException {
        checkMethodArgCount(args.size(), minCnt, maxCnt);
    }
    
    protected final void checkMethodArgCount(int argCnt, int minCnt, int maxCnt) throws TemplateModelException {
        if (argCnt < minCnt || argCnt > maxCnt) {
            throw _MessageUtil.newArgCntError("?" + key, argCnt, minCnt, maxCnt);
        }
    }

    /**
     * Same as {@link #getStringMethodArg}, but checks if {@code args} is big enough, and returns {@code null} if it
     * isn't.
     */
    protected final String getOptStringMethodArg(List args, int argIdx)
            throws TemplateModelException {
        return args.size() > argIdx ? getStringMethodArg(args, argIdx) : null;
    }
    
    /**
     * Gets a method argument and checks if it's a string; it does NOT check if {@code args} is big enough.
     */
    protected final String getStringMethodArg(List args, int argIdx)
            throws TemplateModelException {
        TemplateModel arg = (TemplateModel) args.get(argIdx);
        if (!(arg instanceof TemplateScalarModel)) {
            throw _MessageUtil.newMethodArgMustBeStringException("?" + key, argIdx, arg);
        } else {
            return EvalUtil.modelToString((TemplateScalarModel) arg, null, null);
        }
    }

    /**
     * Same as {@link #getNumberMethodArg}, but checks if {@code args} is big enough, and returns {@code null} if it
     * isn't.
     */
    protected final Number getOptNumberMethodArg(List args, int argIdx) throws TemplateModelException {
        return args.size() > argIdx ? getNumberMethodArg(args, argIdx) : null;
    }

    /**
     * Gets a method argument and checks if it's a number; it does NOT check if {@code args} is big enough.
     */
    protected final Number getNumberMethodArg(List args, int argIdx)
            throws TemplateModelException {
        TemplateModel arg = (TemplateModel) args.get(argIdx);
        if (!(arg instanceof TemplateNumberModel)) {
            throw _MessageUtil.newMethodArgMustBeNumberException("?" + key, argIdx, arg);
        } else {
            return EvalUtil.modelToNumber((TemplateNumberModel) arg, null);
        }
    }
    
    protected final TemplateModelException newMethodArgInvalidValueException(int argIdx, Object[] details) {
        return _MessageUtil.newMethodArgInvalidValueException("?" + key, argIdx, details);
    }

    protected final TemplateModelException newMethodArgsInvalidValueException(Object[] details) {
        return _MessageUtil.newMethodArgsInvalidValueException("?" + key, details);
    }
    
    @Override
    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
    	try {
	    	BuiltIn clone = (BuiltIn) clone();
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
    
}
