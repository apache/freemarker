/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import freemarker.core.BuiltInsForDates.iso_BI;
import freemarker.core.BuiltInsForDates.iso_utc_or_local_BI;
import freemarker.core.BuiltInsForNodes.ancestorsBI;
import freemarker.core.BuiltInsForNodes.childrenBI;
import freemarker.core.BuiltInsForNodes.node_nameBI;
import freemarker.core.BuiltInsForNodes.node_namespaceBI;
import freemarker.core.BuiltInsForNodes.node_typeBI;
import freemarker.core.BuiltInsForNodes.parentBI;
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
import freemarker.core.BuiltInsForSequences.chunkBI;
import freemarker.core.BuiltInsForSequences.firstBI;
import freemarker.core.BuiltInsForSequences.lastBI;
import freemarker.core.BuiltInsForSequences.reverseBI;
import freemarker.core.BuiltInsForSequences.seq_containsBI;
import freemarker.core.BuiltInsForSequences.seq_index_ofBI;
import freemarker.core.BuiltInsForSequences.sortBI;
import freemarker.core.BuiltInsForSequences.sort_byBI;
import freemarker.core.BuiltInsForStringsMisc.evalBI;
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

    static final HashMap builtins = new HashMap();
    static {
        BuiltIn bi;
        
        builtins.put("abs", new absBI());
        builtins.put("ancestors", new ancestorsBI());
        builtins.put("api", new BuiltInsForMultipleTypes.apiBI());
        builtins.put("boolean", new BuiltInsForStringsMisc.booleanBI());
        builtins.put("byte", new byteBI());
        builtins.put("c", new BuiltInsForMultipleTypes.cBI());
        builtins.put("cap_first", new BuiltInsForStringsBasic.cap_firstBI());
        builtins.put("capitalize", new BuiltInsForStringsBasic.capitalizeBI());
        builtins.put("ceiling", new ceilingBI());
        builtins.put("children", new childrenBI());
        builtins.put("chop_linebreak", new BuiltInsForStringsBasic.chop_linebreakBI());
        builtins.put("contains", new BuiltInsForStringsBasic.containsBI());        
        builtins.put("date", new BuiltInsForMultipleTypes.dateBI(TemplateDateModel.DATE));
        builtins.put("date_if_unknown", new BuiltInsForDates.dateType_if_unknownBI(TemplateDateModel.DATE));
        builtins.put("datetime", new BuiltInsForMultipleTypes.dateBI(TemplateDateModel.DATETIME));
        builtins.put("datetime_if_unknown", new BuiltInsForDates.dateType_if_unknownBI(TemplateDateModel.DATETIME));
        builtins.put("default", new ExistenceBuiltins.defaultBI());
        builtins.put("double", new doubleBI());
        builtins.put("ends_with", new BuiltInsForStringsBasic.ends_withBI());
        builtins.put("ensure_ends_with", new BuiltInsForStringsBasic.ensure_ends_withBI());
        builtins.put("ensure_starts_with", new BuiltInsForStringsBasic.ensure_starts_withBI());
        builtins.put("eval", new evalBI());
        builtins.put("exists", new ExistenceBuiltins.existsBI());
        builtins.put("first", new firstBI());
        builtins.put("float", new floatBI());
        builtins.put("floor", new floorBI());
        builtins.put("chunk", new chunkBI());
        builtins.put("has_api", new BuiltInsForMultipleTypes.has_apiBI());
        builtins.put("has_content", new ExistenceBuiltins.has_contentBI());
        builtins.put("html", new BuiltInsForStringsEncoding.htmlBI());
        builtins.put("if_exists", new ExistenceBuiltins.if_existsBI());
        builtins.put("index_of", new BuiltInsForStringsBasic.index_ofBI(false));
        builtins.put("int", new intBI());
        builtins.put("interpret", new Interpret());
        builtins.put("is_boolean", new BuiltInsForMultipleTypes.is_booleanBI());
        builtins.put("is_collection", new BuiltInsForMultipleTypes.is_collectionBI());
        builtins.put("is_collection_ex", new BuiltInsForMultipleTypes.is_collection_exBI());
        bi = new BuiltInsForMultipleTypes.is_dateLikeBI();
        builtins.put("is_date", bi);  // misnomer
        builtins.put("is_date_like", bi);
        builtins.put("is_date_only", new BuiltInsForMultipleTypes.is_dateOfTypeBI(TemplateDateModel.DATE));
        builtins.put("is_unknown_date_like", new BuiltInsForMultipleTypes.is_dateOfTypeBI(TemplateDateModel.UNKNOWN));
        builtins.put("is_datetime", new BuiltInsForMultipleTypes.is_dateOfTypeBI(TemplateDateModel.DATETIME));
        builtins.put("is_directive", new BuiltInsForMultipleTypes.is_directiveBI());
        builtins.put("is_enumerable", new BuiltInsForMultipleTypes.is_enumerableBI());
        builtins.put("is_hash_ex", new BuiltInsForMultipleTypes.is_hash_exBI());
        builtins.put("is_hash", new BuiltInsForMultipleTypes.is_hashBI());
        builtins.put("is_infinite", new is_infiniteBI());
        builtins.put("is_indexable", new BuiltInsForMultipleTypes.is_indexableBI());
        builtins.put("is_macro", new BuiltInsForMultipleTypes.is_macroBI());
        builtins.put("is_method", new BuiltInsForMultipleTypes.is_methodBI());
        builtins.put("is_nan", new is_nanBI());
        builtins.put("is_node", new BuiltInsForMultipleTypes.is_nodeBI());
        builtins.put("is_number", new BuiltInsForMultipleTypes.is_numberBI());
        builtins.put("is_sequence", new BuiltInsForMultipleTypes.is_sequenceBI());
        builtins.put("is_string", new BuiltInsForMultipleTypes.is_stringBI());
        builtins.put("is_time", new BuiltInsForMultipleTypes.is_dateOfTypeBI(TemplateDateModel.TIME));
        builtins.put("is_transform", new BuiltInsForMultipleTypes.is_transformBI());
        
        builtins.put("iso_utc", new iso_utc_or_local_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_SECONDS, /* useUTC = */ true));
        builtins.put("iso_utc_fz", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.TRUE, DateUtil.ACCURACY_SECONDS, /* useUTC = */ true));
        builtins.put("iso_utc_nz", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_SECONDS, /* useUTC = */ true));
        
        builtins.put("iso_utc_ms", new iso_utc_or_local_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_MILLISECONDS, /* useUTC = */ true));
        builtins.put("iso_utc_ms_nz", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_MILLISECONDS, /* useUTC = */ true));
        
        builtins.put("iso_utc_m", new iso_utc_or_local_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_MINUTES, /* useUTC = */ true));
        builtins.put("iso_utc_m_nz", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_MINUTES, /* useUTC = */ true));
        
        builtins.put("iso_utc_h", new iso_utc_or_local_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_HOURS, /* useUTC = */ true));
        builtins.put("iso_utc_h_nz", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_HOURS, /* useUTC = */ true));
        
        builtins.put("iso_local", new iso_utc_or_local_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_SECONDS, /* useUTC = */ false));
        builtins.put("iso_local_nz", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_SECONDS, /* useUTC = */ false));
        
        builtins.put("iso_local_ms", new iso_utc_or_local_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_MILLISECONDS, /* useUTC = */ false));
        builtins.put("iso_local_ms_nz", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_MILLISECONDS, /* useUTC = */ false));
        
        builtins.put("iso_local_m", new iso_utc_or_local_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_MINUTES, /* useUTC = */ false));
        builtins.put("iso_local_m_nz", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_MINUTES, /* useUTC = */ false));
        
        builtins.put("iso_local_h", new iso_utc_or_local_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_HOURS, /* useUTC = */ false));
        builtins.put("iso_local_h_nz", new iso_utc_or_local_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_HOURS, /* useUTC = */ false));
        
        builtins.put("iso", new iso_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_SECONDS));
        builtins.put("iso_nz", new iso_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_SECONDS));
        
        builtins.put("iso_ms", new iso_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_MILLISECONDS));
        builtins.put("iso_ms_nz", new iso_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_MILLISECONDS));
        
        builtins.put("iso_m", new iso_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_MINUTES));
        builtins.put("iso_m_nz", new iso_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_MINUTES));
        
        builtins.put("iso_h", new iso_BI(
                /* showOffset = */ null, DateUtil.ACCURACY_HOURS));
        builtins.put("iso_h_nz", new iso_BI(
                /* showOffset = */ Boolean.FALSE, DateUtil.ACCURACY_HOURS));
        
        builtins.put("j_string", new BuiltInsForStringsEncoding.j_stringBI());
        builtins.put("join", new BuiltInsForSequences.joinBI());
        builtins.put("js_string", new BuiltInsForStringsEncoding.js_stringBI());
        builtins.put("json_string", new BuiltInsForStringsEncoding.json_stringBI());
        builtins.put("keep_after", new BuiltInsForStringsBasic.keep_afterBI());
        builtins.put("keep_before", new BuiltInsForStringsBasic.keep_beforeBI());
        builtins.put("keep_after_last", new BuiltInsForStringsBasic.keep_after_lastBI());
        builtins.put("keep_before_last", new BuiltInsForStringsBasic.keep_before_lastBI());
        builtins.put("keys", new BuiltInsForHashes.keysBI());
        builtins.put("last_index_of", new BuiltInsForStringsBasic.index_ofBI(true));
        builtins.put("last", new lastBI());
        builtins.put("left_pad", new BuiltInsForStringsBasic.padBI(true));
        builtins.put("length", new BuiltInsForStringsBasic.lengthBI());
        builtins.put("long", new longBI());
        builtins.put("lower_abc", new BuiltInsForNumbers.lower_abcBI());
        builtins.put("lower_case", new BuiltInsForStringsBasic.lower_caseBI());
        builtins.put("namespace", new BuiltInsForMultipleTypes.namespaceBI());
        builtins.put("new", new NewBI());
        builtins.put("node_name", new node_nameBI());
        builtins.put("node_namespace", new node_namespaceBI());
        builtins.put("node_type", new node_typeBI());
        builtins.put("number", new BuiltInsForStringsMisc.numberBI());
        builtins.put("number_to_date", new number_to_dateBI(TemplateDateModel.DATE));
        builtins.put("number_to_time", new number_to_dateBI(TemplateDateModel.TIME));
        builtins.put("number_to_datetime", new number_to_dateBI(TemplateDateModel.DATETIME));
        builtins.put("parent", new parentBI());
        builtins.put("reverse", new reverseBI());
        builtins.put("right_pad", new BuiltInsForStringsBasic.padBI(false));
        builtins.put("root", new rootBI());
        builtins.put("round", new roundBI());
        builtins.put("remove_ending", new BuiltInsForStringsBasic.remove_endingBI());
        builtins.put("remove_beginning", new BuiltInsForStringsBasic.remove_beginningBI());
        builtins.put("rtf", new BuiltInsForStringsEncoding.rtfBI());
        builtins.put("seq_contains", new seq_containsBI());
        builtins.put("seq_index_of", new seq_index_ofBI(1));
        builtins.put("seq_last_index_of", new seq_index_ofBI(-1));
        builtins.put("short", new shortBI());
        builtins.put("size", new BuiltInsForMultipleTypes.sizeBI());
        builtins.put("sort_by", new sort_byBI());
        builtins.put("sort", new sortBI());
        builtins.put("split", new BuiltInsForStringsBasic.split_BI());
        builtins.put("starts_with", new BuiltInsForStringsBasic.starts_withBI());
        builtins.put("string", new BuiltInsForMultipleTypes.stringBI());
        builtins.put("substring", new BuiltInsForStringsBasic.substringBI());
        builtins.put("time", new BuiltInsForMultipleTypes.dateBI(TemplateDateModel.TIME));
        builtins.put("time_if_unknown", new BuiltInsForDates.dateType_if_unknownBI(TemplateDateModel.TIME));
        builtins.put("trim", new BuiltInsForStringsBasic.trimBI());
        builtins.put("uncap_first", new BuiltInsForStringsBasic.uncap_firstBI());
        builtins.put("upper_abc", new BuiltInsForNumbers.upper_abcBI());
        builtins.put("upper_case", new BuiltInsForStringsBasic.upper_caseBI());
        builtins.put("url", new BuiltInsForStringsEncoding.urlBI());
        builtins.put("url_path", new BuiltInsForStringsEncoding.urlPathBI());
        builtins.put("values", new BuiltInsForHashes.valuesBI());
        builtins.put("web_safe", builtins.get("html"));  // deprecated; use ?html instead
        builtins.put("word_list", new BuiltInsForStringsBasic.word_listBI());
        builtins.put("xhtml", new BuiltInsForStringsEncoding.xhtmlBI());
        builtins.put("xml", new BuiltInsForStringsEncoding.xmlBI());
        builtins.put("matches", new BuiltInsForStringsRegexp.matchesBI());
        builtins.put("groups", new BuiltInsForStringsRegexp.groupsBI());
        builtins.put("replace", new BuiltInsForStringsRegexp.replace_reBI());
    }

    static BuiltIn newBuiltIn(int incompatibleImprovements, Expression target, String key) throws ParseException {
        BuiltIn bi = (BuiltIn) builtins.get(key);
        if (bi == null) {
            StringBuffer buf = new StringBuffer("Unknown built-in: ").append(StringUtil.jQuote(key)).append(". ");
            
            final String underscoredName = _CoreStringUtils.camelCaseToUnderscored(key);
            if (!underscoredName.equals(key) && builtins.containsKey(underscoredName)) {
                buf.append("Supporting camelCase built-in names is planned for FreeMarker 2.4.0; check if an update is "
                        + "available, and if it indeed supports camel case. "
                        + "Until that, use \"").append(underscoredName).append("\".");
            } else {
                buf.append(
                        "Help (latest version): http://freemarker.org/docs/ref_builtins.html; "
                        + "you're using FreeMarker ").append(Configuration.getVersion()).append(".\n" 
                        + "The alphabetical list of built-ins:");
                List names = new ArrayList(builtins.keySet().size());
                names.addAll(builtins.keySet());
                Collections.sort(names);
                char lastLetter = 0;
                for (Iterator it = names.iterator(); it.hasNext();) {
                    String name = (String) it.next();
                    char firstChar = name.charAt(0);
                    if (firstChar != lastLetter) {
                        lastLetter = firstChar;
                        buf.append('\n');
                    }
                    buf.append(name);
                    
                    if (it.hasNext()) {
                        buf.append(", ");
                    }
                }
            }
            throw new ParseException(buf.toString(), target);
        }
        
        while (bi instanceof ICIChainMember
                && incompatibleImprovements < ((ICIChainMember) bi).getMinimumICIVersion()) {
            bi = (BuiltIn) ((ICIChainMember) bi).getPreviousICIChainMember();
        }
        
        try {
            bi = (BuiltIn) bi.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
        bi.target = target;
        bi.key = key;
        return bi;
    }

    public String getCanonicalForm() {
        return target.getCanonicalForm() + getNodeTypeSymbol();
    }
    
    String getNodeTypeSymbol() {
        return "?" + key;
    }

    boolean isLiteral() {
        return false; // be on the safe side.
    }
    
    protected final void checkMethodArgCount(List args, int expectedCnt) throws TemplateModelException {
        checkMethodArgCount(args.size(), expectedCnt);
    }
    
    protected final void checkMethodArgCount(int argCnt, int expectedCnt) throws TemplateModelException {
        if (argCnt != expectedCnt) {
            throw MessageUtil.newArgCntError("?" + key, argCnt, expectedCnt);
        }
    }

    protected final void checkMethodArgCount(List args, int minCnt, int maxCnt) throws TemplateModelException {
        checkMethodArgCount(args.size(), minCnt, maxCnt);
    }
    
    protected final void checkMethodArgCount(int argCnt, int minCnt, int maxCnt) throws TemplateModelException {
        if (argCnt < minCnt || argCnt > maxCnt) {
            throw MessageUtil.newArgCntError("?" + key, argCnt, minCnt, maxCnt);
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
            throw MessageUtil.newMethodArgMustBeStringException("?" + key, argIdx, arg);
        } else {
            return EvalUtil.modelToString((TemplateScalarModel) arg, null, null);
        }
    }

    /**
     * Gets a method argument and checks if it's a number; it does NOT check if {@code args} is big enough.
     */
    protected final Number getNumberMethodArg(List args, int argIdx)
            throws TemplateModelException {
        TemplateModel arg = (TemplateModel) args.get(argIdx);
        if (!(arg instanceof TemplateNumberModel)) {
            throw MessageUtil.newMethodArgMustBeNumberException("?" + key, argIdx, arg);
        } else {
            return EvalUtil.modelToNumber((TemplateNumberModel) arg, null);
        }
    }
    
    protected final TemplateModelException newMethodArgInvalidValueException(int argIdx, Object[] details) {
        return MessageUtil.newMethodArgInvalidValueException("?" + key, argIdx, details);
    }

    protected final TemplateModelException newMethodArgsInvalidValueException(Object[] details) {
        return MessageUtil.newMethodArgsInvalidValueException("?" + key, details);
    }
    
    protected final Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
    	try {
	    	BuiltIn clone = (BuiltIn)clone();
	    	clone.target = target.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState);
	    	return clone;
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException("Internal error: " + e);
        }
    }

    int getParameterCount() {
        return 2;
    }

    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return target;
        case 1: return key;
        default: throw new IndexOutOfBoundsException();
        }
    }

    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.LEFT_HAND_OPERAND;
        case 1: return ParameterRole.RIGHT_HAND_OPERAND;
        default: throw new IndexOutOfBoundsException();
        }
    }
    
}
