/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import freemarker.core.DateBuiltins.iso_BI;
import freemarker.core.DateBuiltins.iso_tz_BI;
import freemarker.core.NodeBuiltins.ancestorsBI;
import freemarker.core.NodeBuiltins.childrenBI;
import freemarker.core.NodeBuiltins.node_nameBI;
import freemarker.core.NodeBuiltins.node_namespaceBI;
import freemarker.core.NodeBuiltins.node_typeBI;
import freemarker.core.NodeBuiltins.parentBI;
import freemarker.core.NodeBuiltins.rootBI;
import freemarker.core.NumericalBuiltins.absBI;
import freemarker.core.NumericalBuiltins.byteBI;
import freemarker.core.NumericalBuiltins.ceilingBI;
import freemarker.core.NumericalBuiltins.doubleBI;
import freemarker.core.NumericalBuiltins.floatBI;
import freemarker.core.NumericalBuiltins.floorBI;
import freemarker.core.NumericalBuiltins.intBI;
import freemarker.core.NumericalBuiltins.is_infiniteBI;
import freemarker.core.NumericalBuiltins.is_nanBI;
import freemarker.core.NumericalBuiltins.longBI;
import freemarker.core.NumericalBuiltins.number_to_dateBI;
import freemarker.core.NumericalBuiltins.roundBI;
import freemarker.core.NumericalBuiltins.shortBI;
import freemarker.core.SequenceBuiltins.chunkBI;
import freemarker.core.SequenceBuiltins.firstBI;
import freemarker.core.SequenceBuiltins.lastBI;
import freemarker.core.SequenceBuiltins.reverseBI;
import freemarker.core.SequenceBuiltins.seq_containsBI;
import freemarker.core.SequenceBuiltins.seq_index_ofBI;
import freemarker.core.SequenceBuiltins.sortBI;
import freemarker.core.SequenceBuiltins.sort_byBI;
import freemarker.core.StringBuiltins.cap_firstBI;
import freemarker.core.StringBuiltins.capitalizeBI;
import freemarker.core.StringBuiltins.chop_linebreakBI;
import freemarker.core.StringBuiltins.evalBI;
import freemarker.core.StringBuiltins.j_stringBI;
import freemarker.core.StringBuiltins.js_stringBI;
import freemarker.core.StringBuiltins.json_stringBI;
import freemarker.core.StringBuiltins.lower_caseBI;
import freemarker.core.StringBuiltins.numberBI;
import freemarker.core.StringBuiltins.substringBI;
import freemarker.core.StringBuiltins.uncap_firstBI;
import freemarker.core.StringBuiltins.upper_caseBI;
import freemarker.core.StringBuiltins.word_listBI;
import freemarker.log.Logger;
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
 * @author <a href="mailto:jon@revusky.com">Jonathan Revusky</a>
 */
abstract class BuiltIn extends Expression implements Cloneable {
    
    private static final Logger logger = Logger.getLogger("freemarker.runtime");
    
    protected Expression target;
    protected String key;

    static final HashMap builtins = new HashMap();
    static {
        builtins.put("abs", new absBI());
        builtins.put("ancestors", new ancestorsBI());
        builtins.put("byte", new byteBI());
        builtins.put("c", new MiscellaneousBuiltins.cBI());
        builtins.put("cap_first", new cap_firstBI());
        builtins.put("capitalize", new capitalizeBI());
        builtins.put("ceiling", new ceilingBI());
        builtins.put("children", new childrenBI());
        builtins.put("chop_linebreak", new chop_linebreakBI());
        builtins.put("contains", new StringBuiltins.containsBI());        
        builtins.put("date", new MiscellaneousBuiltins.dateBI(TemplateDateModel.DATE));
        builtins.put("datetime", new MiscellaneousBuiltins.dateBI(TemplateDateModel.DATETIME));
        builtins.put("default", new ExistenceBuiltins.defaultBI());
        builtins.put("double", new doubleBI());
        builtins.put("ends_with", new StringBuiltins.ends_withBI());
        builtins.put("eval", new evalBI());
        builtins.put("exists", new ExistenceBuiltins.existsBI());
        builtins.put("first", new firstBI());
        builtins.put("float", new floatBI());
        builtins.put("floor", new floorBI());
        builtins.put("chunk", new chunkBI());
        builtins.put("has_content", new ExistenceBuiltins.has_contentBI());
        builtins.put("html", new StringBuiltins.htmlBI());
        builtins.put("if_exists", new ExistenceBuiltins.if_existsBI());
        builtins.put("index_of", new StringBuiltins.index_ofBI(false));
        builtins.put("int", new intBI());
        builtins.put("interpret", new Interpret());
        builtins.put("is_boolean", new MiscellaneousBuiltins.is_booleanBI());
        builtins.put("is_collection", new MiscellaneousBuiltins.is_collectionBI());
        builtins.put("is_date", new MiscellaneousBuiltins.is_dateBI());
        builtins.put("is_directive", new MiscellaneousBuiltins.is_directiveBI());
        builtins.put("is_enumerable", new MiscellaneousBuiltins.is_enumerableBI());
        builtins.put("is_hash_ex", new MiscellaneousBuiltins.is_hash_exBI());
        builtins.put("is_hash", new MiscellaneousBuiltins.is_hashBI());
        builtins.put("is_infinite", new is_infiniteBI());
        builtins.put("is_indexable", new MiscellaneousBuiltins.is_indexableBI());
        builtins.put("is_macro", new MiscellaneousBuiltins.is_macroBI());
        builtins.put("is_method", new MiscellaneousBuiltins.is_methodBI());
        builtins.put("is_nan", new is_nanBI());
        builtins.put("is_node", new MiscellaneousBuiltins.is_nodeBI());
        builtins.put("is_number", new MiscellaneousBuiltins.is_numberBI());
        builtins.put("is_sequence", new MiscellaneousBuiltins.is_sequenceBI());
        builtins.put("is_string", new MiscellaneousBuiltins.is_stringBI());
        builtins.put("is_transform", new MiscellaneousBuiltins.is_transformBI());
        builtins.put("iso_utc", new iso_tz_BI(
                /* showOffset = */ true, DateUtil.ACCURACY_SECONDS, /* useUTC = */ true));
        builtins.put("iso_utc_nz", new iso_tz_BI(
                /* showOffset = */ false, DateUtil.ACCURACY_SECONDS, /* useUTC = */ true));
        builtins.put("iso_utc_ms", new iso_tz_BI(
                /* showOffset = */ true, DateUtil.ACCURACY_MILLISECONDS, /* useUTC = */ true));
        builtins.put("iso_utc_ms_nz", new iso_tz_BI(
                /* showOffset = */ false, DateUtil.ACCURACY_MILLISECONDS, /* useUTC = */ true));
        builtins.put("iso_utc_m", new iso_tz_BI(
                /* showOffset = */ true, DateUtil.ACCURACY_MINUTES, /* useUTC = */ true));
        builtins.put("iso_utc_m_nz", new iso_tz_BI(
                /* showOffset = */ false, DateUtil.ACCURACY_MINUTES, /* useUTC = */ true));
        builtins.put("iso_utc_h", new iso_tz_BI(
                /* showOffset = */ true, DateUtil.ACCURACY_HOURS, /* useUTC = */ true));
        builtins.put("iso_utc_h_nz", new iso_tz_BI(
                /* showOffset = */ false, DateUtil.ACCURACY_HOURS, /* useUTC = */ true));
        builtins.put("iso_local", new iso_tz_BI(
                /* showOffset = */ true, DateUtil.ACCURACY_SECONDS, /* useUTC = */ false));
        builtins.put("iso_local_nz", new iso_tz_BI(
                /* showOffset = */ false, DateUtil.ACCURACY_SECONDS, /* useUTC = */ false));
        builtins.put("iso_local_ms", new iso_tz_BI(
                /* showOffset = */ true, DateUtil.ACCURACY_MILLISECONDS, /* useUTC = */ false));
        builtins.put("iso_local_ms_nz", new iso_tz_BI(
                /* showOffset = */ false, DateUtil.ACCURACY_MILLISECONDS, /* useUTC = */ false));
        builtins.put("iso_local_m", new iso_tz_BI(
                /* showOffset = */ true, DateUtil.ACCURACY_MINUTES, /* useUTC = */ false));
        builtins.put("iso_local_m_nz", new iso_tz_BI(
                /* showOffset = */ false, DateUtil.ACCURACY_MINUTES, /* useUTC = */ false));
        builtins.put("iso_local_h", new iso_tz_BI(
                /* showOffset = */ true, DateUtil.ACCURACY_HOURS, /* useUTC = */ false));
        builtins.put("iso_local_h_nz", new iso_tz_BI(
                /* showOffset = */ false, DateUtil.ACCURACY_HOURS, /* useUTC = */ false));
        builtins.put("iso", new iso_BI(
                /* showOffset = */ true, DateUtil.ACCURACY_SECONDS));
        builtins.put("iso_nz", new iso_BI(
                /* showOffset = */ false, DateUtil.ACCURACY_SECONDS));
        builtins.put("iso_ms", new iso_BI(
                /* showOffset = */ true, DateUtil.ACCURACY_MILLISECONDS));
        builtins.put("iso_ms_nz", new iso_BI(
                /* showOffset = */ false, DateUtil.ACCURACY_MILLISECONDS));
        builtins.put("iso_m", new iso_BI(
                /* showOffset = */ true, DateUtil.ACCURACY_MINUTES));
        builtins.put("iso_m_nz", new iso_BI(
                /* showOffset = */ false, DateUtil.ACCURACY_MINUTES));
        builtins.put("iso_h", new iso_BI(
                /* showOffset = */ true, DateUtil.ACCURACY_HOURS));
        builtins.put("iso_h_nz", new iso_BI(
                /* showOffset = */ false, DateUtil.ACCURACY_HOURS));
        builtins.put("j_string", new j_stringBI());
        builtins.put("join", new SequenceBuiltins.joinBI());
        builtins.put("js_string", new js_stringBI());
        builtins.put("json_string", new json_stringBI());
        builtins.put("keys", new HashBuiltins.keysBI());
        builtins.put("last_index_of", new StringBuiltins.index_ofBI(true));
        builtins.put("last", new lastBI());
        builtins.put("left_pad", new StringBuiltins.padBI(true));
        builtins.put("length", new StringBuiltins.lengthBI());
        builtins.put("long", new longBI());
        builtins.put("lower_case", new lower_caseBI());
        builtins.put("namespace", new MiscellaneousBuiltins.namespaceBI());
        builtins.put("new", new NewBI());
        builtins.put("node_name", new node_nameBI());
        builtins.put("node_namespace", new node_namespaceBI());
        builtins.put("node_type", new node_typeBI());
        builtins.put("number", new numberBI());
        builtins.put("number_to_date", new number_to_dateBI(TemplateDateModel.DATE));
        builtins.put("number_to_time", new number_to_dateBI(TemplateDateModel.TIME));
        builtins.put("number_to_datetime", new number_to_dateBI(TemplateDateModel.DATETIME));
        builtins.put("parent", new parentBI());
        builtins.put("replace", new StringBuiltins.replaceBI());
        builtins.put("reverse", new reverseBI());
        builtins.put("right_pad", new StringBuiltins.padBI(false));
        builtins.put("root", new rootBI());
        builtins.put("round", new roundBI());
        builtins.put("rtf", new StringBuiltins.rtfBI());
        builtins.put("seq_contains", new seq_containsBI());
        builtins.put("seq_index_of", new seq_index_ofBI(1));
        builtins.put("seq_last_index_of", new seq_index_ofBI(-1));
        builtins.put("short", new shortBI());
        builtins.put("size", new MiscellaneousBuiltins.sizeBI());
        builtins.put("sort_by", new sort_byBI());
        builtins.put("sort", new sortBI());
        builtins.put("split", new StringBuiltins.splitBI());
        builtins.put("starts_with", new StringBuiltins.starts_withBI());
        builtins.put("string", new MiscellaneousBuiltins.stringBI());
        builtins.put("substring", new substringBI());
        builtins.put("time", new MiscellaneousBuiltins.dateBI(TemplateDateModel.TIME));
        builtins.put("trim", new StringBuiltins.trimBI());
        builtins.put("uncap_first", new uncap_firstBI());
        builtins.put("upper_case", new upper_caseBI());
        builtins.put("url", new StringBuiltins.urlBI());
        builtins.put("values", new HashBuiltins.valuesBI());
        builtins.put("web_safe", builtins.get("html"));  // deprecated; use ?html instead
        builtins.put("word_list", new word_listBI());
        builtins.put("xhtml", new StringBuiltins.xhtmlBI());
        builtins.put("xml", new StringBuiltins.xmlBI());
        try {
            Class.forName("java.util.regex.Pattern");
            builtins.put("matches", instantiateBI("freemarker.core._RegexBuiltins$matchesBI"));
            builtins.put("groups", instantiateBI("freemarker.core._RegexBuiltins$groupsBI"));
            builtins.put("replace", instantiateBI("freemarker.core._RegexBuiltins$replace_reBI"));
            builtins.put("split", instantiateBI("freemarker.core._RegexBuiltins$split_reBI"));
        } catch (Exception e) {
            logger.debug("Regular expression built-ins won't be avilable", e);
        }
    }

    private static Object instantiateBI(String className) throws Exception
    {
        return Class.forName(className).newInstance();
    }
    
    static BuiltIn newBuiltIn(int incompatibleImprovements, Expression target, String key) throws ParseException {
        BuiltIn bi = (BuiltIn) builtins.get(key);
        if (bi == null) {
            StringBuffer buf = new StringBuffer(
                    "Unknown built-in: " + StringUtil.jQuote(key) + ". "
                    + "Help (latest version): http://freemarker.org/docs/ref_builtins.html; "
                    + "you're using FreeMarker " + Configuration.getVersion() + ".\n" 
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
