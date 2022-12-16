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

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import freemarker.cache.AndMatcher;
import freemarker.cache.ConditionalTemplateConfigurationFactory;
import freemarker.cache.FileExtensionMatcher;
import freemarker.cache.FileNameGlobMatcher;
import freemarker.cache.FirstMatchTemplateConfigurationFactory;
import freemarker.cache.MergingTemplateConfigurationFactory;
import freemarker.cache.NotMatcher;
import freemarker.cache.OrMatcher;
import freemarker.cache.PathGlobMatcher;
import freemarker.cache.PathRegexMatcher;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.SimpleObjectWrapper;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.StringUtil;
import freemarker.template.utility.WriteProtectable;

/**
 * Don't use this; used internally by FreeMarker, might change without notice.
 * 
 * Evaluates object builder expressions used in configuration {@link Properties}.
 * It should be replaced with FTL later (when it was improved to be practical for this), so the syntax should be
 * a subset of the future FTL syntax. This is also why this syntax is restrictive; it shouldn't accept anything that
 * FTL will not.
 */
// Java 5: use generics for expectedClass
// Java 5: Introduce ObjectBuilder interface
public class _ObjectBuilderSettingEvaluator {
    
    private static final String INSTANCE_FIELD_NAME = "INSTANCE";

    private static final String BUILD_METHOD_NAME = "build";

    private static final String BUILDER_CLASS_POSTFIX = "Builder";

    private static Map<String,String> SHORTHANDS;
    
    private static final Object VOID = new Object();

    private final String src;
    private final Class expectedClass;
    private final boolean allowNull;
    private final _SettingEvaluationEnvironment env;

    // Parser state:
    private int pos;
    
    // Parsing results:
    private boolean modernMode = false;
    
    private _ObjectBuilderSettingEvaluator(
            String src, int pos, Class expectedClass, boolean allowNull, _SettingEvaluationEnvironment env) {
        this.src = src;
        this.pos = pos;
        this.expectedClass = expectedClass;
        this.allowNull = allowNull;
        this.env = env;
    }

    public static Object eval(String src, Class expectedClass, boolean allowNull, _SettingEvaluationEnvironment env)
            throws _ObjectBuilderSettingEvaluationException,
            ClassNotFoundException, InstantiationException, IllegalAccessException {
        return new _ObjectBuilderSettingEvaluator(src, 0, expectedClass, allowNull, env).eval();
    }

    /**
     * Used for getting a list of setting assignments (like {@code (x=1, y=2)}) from an existing string, and apply it on
     * an existing bean.
     * 
     * @return The location of the next character to process.
     */
    public static int configureBean(
            String argumentListSrc, int posAfterOpenParen, Object bean, _SettingEvaluationEnvironment env)
            throws _ObjectBuilderSettingEvaluationException,
            ClassNotFoundException, InstantiationException, IllegalAccessException {
        return new _ObjectBuilderSettingEvaluator(
                argumentListSrc, posAfterOpenParen, bean.getClass(), true, env).configureBean(bean);
    }
    
    private Object eval() throws _ObjectBuilderSettingEvaluationException,
            ClassNotFoundException, InstantiationException, IllegalAccessException {
        Object value;
        
        skipWS();
        try {
            value = ensureEvaled(fetchValue(false, true, false, true));
        } catch (LegacyExceptionWrapperSettingEvaluationExpression e) {
            e.rethrowLegacy();
            value = null; // newer reached
        }
        skipWS();
        
        if (pos != src.length()) {
            throw new _ObjectBuilderSettingEvaluationException("end-of-expression", src, pos);
        }
        
        if (value == null && !allowNull) {
            throw new _ObjectBuilderSettingEvaluationException("Value can't be null.");
        }
        if (value != null && !expectedClass.isInstance(value)) {
            throw new _ObjectBuilderSettingEvaluationException("The resulting object (of class "
                    + value.getClass() + ") is not a(n) " + expectedClass.getName() + ".");
        }
        
        return value;
    }
    
    private int configureBean(Object bean) throws _ObjectBuilderSettingEvaluationException,
            ClassNotFoundException, InstantiationException, IllegalAccessException {
        final PropertyAssignmentsExpression propAssignments = new PropertyAssignmentsExpression(bean);
        fetchParameterListInto(propAssignments);
        skipWS();
        propAssignments.eval();
        return pos;
    }

    private Object ensureEvaled(Object value) throws _ObjectBuilderSettingEvaluationException {
        return value instanceof SettingExpression ? ((SettingExpression) value).eval() : value;
    }

    private Object fetchBuilderCall(boolean optional, boolean topLevel)
            throws _ObjectBuilderSettingEvaluationException {
        int startPos = pos;
        
        BuilderCallExpression exp = new BuilderCallExpression();
        // We need the canBeStaticField/mustBeStaticFiled complication to deal with legacy syntax where parentheses
        // weren't required for constructor calls.
        exp.canBeStaticField = true;
        
        final String fetchedClassName = fetchClassName(optional);
        {
            if (fetchedClassName == null) {
                if (!optional) {
                    throw new _ObjectBuilderSettingEvaluationException("class name", src, pos);
                }
                return VOID;
            }
            exp.className = shorthandToFullQualified(fetchedClassName);
            if (!fetchedClassName.equals(exp.className)) {
                // Before 2.3.21 only full-qualified class names were allowed
                modernMode = true;
                exp.canBeStaticField = false;
            }
        }
        
        skipWS();
        
        char openParen = fetchOptionalChar("(");
        // Only the top-level expression can omit the "(...)"
        if (openParen == 0 && !topLevel) {
            if (fetchedClassName.indexOf('.') != -1) {
                exp.mustBeStaticField = true;
            } else {
                pos = startPos;
                return VOID;
            }
        }
    
        if (openParen != 0) {
            fetchParameterListInto(exp);
            exp.canBeStaticField = false;
        }
        
        return exp;
    }

    private void fetchParameterListInto(ExpressionWithParameters exp) throws _ObjectBuilderSettingEvaluationException {
        // Before 2.3.21 there was no parameter list
        modernMode = true;
        
        skipWS();
        if (fetchOptionalChar(")") != ')') { 
            do {
                skipWS();
                
                Object paramNameOrValue = fetchValue(false, false, true, false);
                if (paramNameOrValue != VOID) {
                    skipWS();
                    if (paramNameOrValue instanceof Name) {
                        exp.namedParamNames.add(((Name) paramNameOrValue).name);
                        
                        skipWS();
                        fetchRequiredChar("=");
                        skipWS();
                        
                        Object paramValue = fetchValue(false, false, true, true);
                        exp.namedParamValues.add(ensureEvaled(paramValue));
                    } else {
                        if (!exp.namedParamNames.isEmpty()) {
                            throw new _ObjectBuilderSettingEvaluationException(
                                    "Positional parameters must precede named parameters");
                        }
                        if (!exp.getAllowPositionalParameters()) {
                            throw new _ObjectBuilderSettingEvaluationException(
                                    "Positional parameters not supported here");
                        }
                        exp.positionalParamValues.add(ensureEvaled(paramNameOrValue));
                    }
                    
                    skipWS();
                }
            } while (fetchRequiredChar(",)") == ',');
        }
    }

    private Object fetchValue(boolean optional, boolean topLevel, boolean resultCoerced, boolean resolveVariables)
            throws _ObjectBuilderSettingEvaluationException {
        if (pos < src.length()) {
            Object val = fetchNumberLike(true, resultCoerced);
            if (val != VOID) {
                return val;
            }
    
            val = fetchStringLiteral(true);
            if (val != VOID) {
                return val;
            }

            val = fetchListLiteral(true);
            if (val != VOID) {
                return val;
            }

            val = fetchMapLiteral(true);
            if (val != VOID) {
                return val;
            }
            
            val = fetchBuilderCall(true, topLevel);
            if (val != VOID) {
                return val;
            }
            
            String name = fetchSimpleName(true);
            if (name != null) {
                val = keywordToValueOrVoid(name);
                if (val != VOID) {
                    return val;
                }
                
                if (resolveVariables) {
                    // Not supported currently...
                    throw new _ObjectBuilderSettingEvaluationException("Can't resolve variable reference: " + name);
                } else {
                    return new Name(name);
                }
            }
        }
        
        if (optional) {
            return VOID;
        } else {
            throw new _ObjectBuilderSettingEvaluationException("value or name", src, pos);
        }
    }

    private boolean isKeyword(String name) {
        return keywordToValueOrVoid(name) != VOID;
    }
    
    private Object keywordToValueOrVoid(String name) {
        if (name.equals("true")) return Boolean.TRUE;
        if (name.equals("false")) return Boolean.FALSE;
        if (name.equals("null")) return null;
        return VOID;
    }

    private String fetchSimpleName(boolean optional) throws _ObjectBuilderSettingEvaluationException {
        char c = pos < src.length() ? src.charAt(pos) : 0;
        if (!isIdentifierStart(c)) {
            if (optional) {
                return null;
            } else {
                throw new _ObjectBuilderSettingEvaluationException("class name", src, pos);
            }
        }
        int startPos = pos;
        pos++;
        
        seekClassNameEnd: while (true) {
            if (pos == src.length()) {
                break seekClassNameEnd;
            }
            c = src.charAt(pos);
            if (!isIdentifierMiddle(c)) {
                break seekClassNameEnd;
            }
            pos++;
        }
        
        return src.substring(startPos, pos);
    }

    private String fetchClassName(boolean optional) throws _ObjectBuilderSettingEvaluationException {
        int startPos = pos;
        StringBuilder sb = new StringBuilder();
        do {
            String name = fetchSimpleName(true);
            if (name == null) {
                if (!optional) {
                    throw new _ObjectBuilderSettingEvaluationException("name", src, pos);
                } else {
                    pos = startPos;
                    return null;
                }
            }
            sb.append(name);
            
            skipWS();
            
            if (pos >= src.length() || src.charAt(pos) != '.') {
                break;
            }
            sb.append('.');
            pos++;
            
            skipWS();
        } while (true);
        
        String className = sb.toString();
        if (isKeyword(className)) {
            pos = startPos;
            return null;
        }
        return className;
    }

    private Object fetchNumberLike(boolean optional, boolean resultCoerced)
            throws _ObjectBuilderSettingEvaluationException {
        int startPos = pos;
        boolean isVersion = false;
        boolean hasDot = false;
        seekTokenEnd: while (true) {
            if (pos == src.length()) {
                break seekTokenEnd;
            }
            char c = src.charAt(pos);
            if (c == '.') {
                if (hasDot) {
                    // More than one dot
                    isVersion = true;
                } else {
                    hasDot = true;
                }
            } else if (!(isASCIIDigit(c) || c == '-')) {
                break seekTokenEnd;
            }
            pos++;
        }
        
        if (startPos == pos) {
            if (optional) {
                return VOID;
            } else {
                throw new _ObjectBuilderSettingEvaluationException("number-like", src, pos);
            }
        }
        
        String numStr = src.substring(startPos, pos);
        if (isVersion) {
            try {
                return new Version(numStr);
            } catch (IllegalArgumentException e) {
                throw new _ObjectBuilderSettingEvaluationException("Malformed version number: " + numStr, e);
            }
        } else {
            // For example, in 1.0f, numStr is "1.0", and typePostfix is "f".
            String typePostfix = null;
            seekTypePostfixEnd: while (true) {
                if (pos == src.length()) {
                    break seekTypePostfixEnd;
                }
                char c = src.charAt(pos);
                if (Character.isLetter(c)) {
                    if (typePostfix == null) {
                        typePostfix = String.valueOf(c);
                    } else {
                        typePostfix += c; 
                    }
                } else {
                    break seekTypePostfixEnd;
                }
                pos++;
            }
            
            try {
                if (numStr.endsWith(".")) {
                    throw new NumberFormatException("A number can't end with a dot");
                }
                if (numStr.startsWith(".") || numStr.startsWith("-.")  || numStr.startsWith("+.")) {
                    throw new NumberFormatException("A number can't start with a dot");
                }

                if (typePostfix == null) {
                    // Auto-detect type
                    if (numStr.indexOf('.') == -1) {
                        BigInteger biNum = new BigInteger(numStr);
                        final int bitLength = biNum.bitLength();  // Doesn't include sign bit
                        if (bitLength <= 31) {
                            return Integer.valueOf(biNum.intValue());
                        } else if (bitLength <= 63) {
                            return Long.valueOf(biNum.longValue());
                        } else {
                            return biNum;
                        }
                    } else {
                        if (resultCoerced) {
                            // The FTL way (BigDecimal is lossless, and it will be coerced to the target type later):
                            return new BigDecimal(numStr);
                        } else {
                            // The Java way (lossy but familiar):
                            return Double.valueOf(numStr);
                        }
                    }
                } else { // Has explicitly specified type
                    if (typePostfix.equalsIgnoreCase("l")) {
                        return Long.valueOf(numStr);
                    } else if (typePostfix.equalsIgnoreCase("bi")) {
                        return new BigInteger(numStr);
                    } else if (typePostfix.equalsIgnoreCase("bd")) {
                        return new BigDecimal(numStr);
                    } else if (typePostfix.equalsIgnoreCase("d")) {
                        return Double.valueOf(numStr);
                    } else if (typePostfix.equalsIgnoreCase("f")) {
                        return Float.valueOf(numStr);
                    } else {
                        throw new _ObjectBuilderSettingEvaluationException(
                                "Unrecognized number type postfix: " + typePostfix);
                    }
                }
                
            } catch (NumberFormatException e) {
                throw new _ObjectBuilderSettingEvaluationException("Malformed number: " + numStr, e);
            }
        }
    }

    private Object fetchStringLiteral(boolean optional) throws _ObjectBuilderSettingEvaluationException {
        int startPos = pos;
        char q = 0;
        boolean afterEscape = false;
        boolean raw = false;
        seekTokenEnd: while (true) {
            if (pos == src.length()) {
                if (q != 0) {
                    // We had an open quotation
                    throw new _ObjectBuilderSettingEvaluationException(String.valueOf(q), src, pos);
                }
                break seekTokenEnd;
            }
            char c = src.charAt(pos);
            if (q == 0) {
                if (c == 'r' && (pos + 1 < src.length())) {
                    // Maybe it's like r"foo\bar"
                    raw = true;
                    c = src.charAt(pos + 1);
                }
                if (c == '\'') {
                    q = '\'';
                } else if (c == '"') {
                    q = '"';
                } else {
                    break seekTokenEnd;
                }
                if (raw) {
                    // because of the preceding "r"
                    pos++;
                }
            } else {
                if (!afterEscape) {
                    if (c == '\\' && !raw) {
                        afterEscape = true;
                    } else if (c == q) {
                        break seekTokenEnd;
                    } else if (c == '{') {
                        char prevC = src.charAt(pos - 1);
                        if (prevC == '$' || prevC == '#') {
                            throw new _ObjectBuilderSettingEvaluationException(
                                    "${...} and #{...} aren't allowed here.");
                        }
                    }
                } else {
                    afterEscape = false;
                }
            }
            pos++;
        }
        if (startPos == pos) {
            if (optional) {
                return VOID;
            } else {
                throw new _ObjectBuilderSettingEvaluationException("string literal", src, pos);
            }
        }
            
        final String sInside = src.substring(startPos + (raw ? 2 : 1), pos);
        try {
            pos++; // skip closing quotation mark
            return raw ? sInside : StringUtil.FTLStringLiteralDec(sInside);
        } catch (ParseException e) {
            throw new _ObjectBuilderSettingEvaluationException("Malformed string literal: " + sInside, e);
        }
    }

    private Object fetchListLiteral(boolean optional) throws _ObjectBuilderSettingEvaluationException {
        if (pos == src.length() || src.charAt(pos) != '[') {
            if (!optional) {
                throw new _ObjectBuilderSettingEvaluationException("[", src, pos);
            }
            return VOID;
        }
        pos++;
        
        ListExpression listExp = new ListExpression();
        
        while (true) {
            skipWS();
            
            if (fetchOptionalChar("]") != 0) {
                return listExp;
            }
            if (listExp.itemCount() != 0) {
                fetchRequiredChar(",");
                skipWS();
            }
            
            listExp.addItem(fetchValue(false, false, false, true));
            
            skipWS();
        }
    }

    private Object fetchMapLiteral(boolean optional) throws _ObjectBuilderSettingEvaluationException {
        if (pos == src.length() || src.charAt(pos) != '{') {
            if (!optional) {
                throw new _ObjectBuilderSettingEvaluationException("{", src, pos);
            }
            return VOID;
        }
        pos++;
        
        MapExpression mapExp = new MapExpression();
        
        while (true) {
            skipWS();
            
            if (fetchOptionalChar("}") != 0) {
                return mapExp;
            }
            if (mapExp.itemCount() != 0) {
                fetchRequiredChar(",");
                skipWS();
            }
            
            Object key = fetchValue(false, false, false, true);
            skipWS();
            fetchRequiredChar(":");
            skipWS();
            Object value = fetchValue(false, false, false, true);
            mapExp.addItem(new KeyValuePair(key, value));
            
            skipWS();
        }
    }
    
    private void skipWS() {
        while (true) {
            if (pos == src.length()) {
                return;
            }
            char c = src.charAt(pos);
            if (!Character.isWhitespace(c)) {
                return;
            }
            pos++;
        }
    }

    private char fetchOptionalChar(String expectedChars) throws _ObjectBuilderSettingEvaluationException {
        return fetchChar(expectedChars, true);
    }
    
    private char fetchRequiredChar(String expectedChars) throws _ObjectBuilderSettingEvaluationException {
        return fetchChar(expectedChars, false);
    }
    
    private char fetchChar(String expectedChars, boolean optional) throws _ObjectBuilderSettingEvaluationException {
        char c = pos < src.length() ? src.charAt(pos) : 0;
        if (expectedChars.indexOf(c) != -1) {
            pos++;
            return c;
        } else if (optional) {
            return 0;
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < expectedChars.length(); i++) {
                if (i != 0) {
                    sb.append(" or ");
                }
                sb.append(StringUtil.jQuote(expectedChars.substring(i, i + 1)));
            }
            throw new _ObjectBuilderSettingEvaluationException(
                    sb.toString(),
                    src, pos);
        }
    }
    
    private boolean isASCIIDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private boolean isIdentifierMiddle(char c) {
        return isIdentifierStart(c) || isASCIIDigit(c);
    }

    private static synchronized String shorthandToFullQualified(String className) {
        if (SHORTHANDS == null) {
            SHORTHANDS = new HashMap/*<String,String>*/();
            
            addWithSimpleName(SHORTHANDS, DefaultObjectWrapper.class);
            addWithSimpleName(SHORTHANDS, BeansWrapper.class);
            addWithSimpleName(SHORTHANDS, SimpleObjectWrapper.class);

            addWithSimpleName(SHORTHANDS, TemplateConfiguration.class);
            
            addWithSimpleName(SHORTHANDS, PathGlobMatcher.class);
            addWithSimpleName(SHORTHANDS, FileNameGlobMatcher.class);
            addWithSimpleName(SHORTHANDS, FileExtensionMatcher.class);
            addWithSimpleName(SHORTHANDS, PathRegexMatcher.class);
            addWithSimpleName(SHORTHANDS, AndMatcher.class);
            addWithSimpleName(SHORTHANDS, OrMatcher.class);
            addWithSimpleName(SHORTHANDS, NotMatcher.class);
            
            addWithSimpleName(SHORTHANDS, ConditionalTemplateConfigurationFactory.class);
            addWithSimpleName(SHORTHANDS, MergingTemplateConfigurationFactory.class);
            addWithSimpleName(SHORTHANDS, FirstMatchTemplateConfigurationFactory.class);

            addWithSimpleName(SHORTHANDS, HTMLOutputFormat.class);
            addWithSimpleName(SHORTHANDS, XHTMLOutputFormat.class);
            addWithSimpleName(SHORTHANDS, XMLOutputFormat.class);
            addWithSimpleName(SHORTHANDS, RTFOutputFormat.class);
            addWithSimpleName(SHORTHANDS, PlainTextOutputFormat.class);
            addWithSimpleName(SHORTHANDS, UndefinedOutputFormat.class);

            addWithSimpleName(SHORTHANDS, DefaultTruncateBuiltinAlgorithm.class);

            addWithSimpleName(SHORTHANDS, Locale.class);
            SHORTHANDS.put("TimeZone", "freemarker.core._TimeZone");
            SHORTHANDS.put("markup", "freemarker.core._Markup");

            // For accessing static fields:
            addWithSimpleName(SHORTHANDS, Configuration.class);
        }
        String fullClassName = SHORTHANDS.get(className);
        return fullClassName == null ? className : fullClassName;
    }
    
    private static void addWithSimpleName(Map map, Class<?> pClass) {
        map.put(pClass.getSimpleName(), pClass.getName());
    }

    private void setJavaBeanProperties(Object bean,
            List/*<String>*/ namedParamNames, List/*<Object>*/ namedParamValues)
            throws _ObjectBuilderSettingEvaluationException {
        if (namedParamNames.isEmpty()) {
            return;
        }
        
        final Class cl = bean.getClass();
        Map/*<String,Method>*/ beanPropSetters;
        try {
            PropertyDescriptor[] propDescs = Introspector.getBeanInfo(cl).getPropertyDescriptors();
            beanPropSetters = new HashMap(propDescs.length * 4 / 3, 1.0f);
            for (int i = 0; i < propDescs.length; i++) {
                PropertyDescriptor propDesc = propDescs[i];
                final Method writeMethod = propDesc.getWriteMethod();
                if (writeMethod != null) {
                    beanPropSetters.put(propDesc.getName(), writeMethod);
                }
            }
        } catch (Exception e) {
            throw new _ObjectBuilderSettingEvaluationException("Failed to inspect " + cl.getName() + " class", e);
        }

        TemplateHashModel beanTM = null;
        for (int i = 0; i < namedParamNames.size(); i++) {
            String name = (String) namedParamNames.get(i);
            if (!beanPropSetters.containsKey(name)) {
                throw new _ObjectBuilderSettingEvaluationException(
                        "The " + cl.getName() + " class has no writeable JavaBeans property called "
                        + StringUtil.jQuote(name) + ".");
            }
            
            Method beanPropSetter = (Method) beanPropSetters.put(name, null);
            if (beanPropSetter == null) {
                    throw new _ObjectBuilderSettingEvaluationException(
                            "JavaBeans property " + StringUtil.jQuote(name) + " is set twice.");
            }
            
            try {
                if (beanTM == null) {
                    TemplateModel wrappedObj = env.getObjectWrapper().wrap(bean);
                    if (!(wrappedObj instanceof TemplateHashModel)) {
                        throw new _ObjectBuilderSettingEvaluationException(
                                "The " + cl.getName() + " class is not a wrapped as TemplateHashModel.");
                    }
                    beanTM = (TemplateHashModel) wrappedObj;
                }
                
                TemplateModel m = beanTM.get(beanPropSetter.getName());
                if (m == null) {
                    throw new _ObjectBuilderSettingEvaluationException(
                            "Can't find " + beanPropSetter + " as FreeMarker method.");
                }
                if (!(m instanceof TemplateMethodModelEx)) {
                    throw new _ObjectBuilderSettingEvaluationException(
                            StringUtil.jQuote(beanPropSetter.getName()) + " wasn't a TemplateMethodModelEx.");
                }
                List/*TemplateModel*/ args = new ArrayList();
                args.add(env.getObjectWrapper().wrap(namedParamValues.get(i)));
                ((TemplateMethodModelEx) m).exec(args);
            } catch (Exception e) {
                throw new _ObjectBuilderSettingEvaluationException(
                        "Failed to set " + StringUtil.jQuote(name), e);
            }
        }
    }

    private static class Name {
        
        public Name(String name) {
            this.name = name;
        }

        private final String name;
    }
    
    private abstract static class SettingExpression {
        abstract Object eval() throws _ObjectBuilderSettingEvaluationException;
    }
    
    private abstract class ExpressionWithParameters extends SettingExpression {
        protected List positionalParamValues = new ArrayList();
        protected List/*<String>*/ namedParamNames = new ArrayList();
        protected List/*<Object>*/ namedParamValues = new ArrayList();
        
        protected abstract boolean getAllowPositionalParameters();
    }
    
    private class ListExpression extends SettingExpression {
        
        private List<Object> items = new ArrayList();
        
        void addItem(Object item) {
            items.add(item);
        }

        public int itemCount() {
            return items.size();
        }

        @Override
        Object eval() throws _ObjectBuilderSettingEvaluationException {
            ArrayList res = new ArrayList(items.size());
            for (Object item : items) {
                res.add(ensureEvaled(item));
            }
            return res;
        }
        
    }
    
    private class MapExpression extends SettingExpression {
        
        private List<KeyValuePair> items = new ArrayList();
        
        void addItem(KeyValuePair item) {
            items.add(item);
        }

        public int itemCount() {
            return items.size();
        }

        @Override
        Object eval() throws _ObjectBuilderSettingEvaluationException {
            LinkedHashMap res = new LinkedHashMap(items.size() * 4 / 3, 1f);
            for (KeyValuePair item : items) {
                Object key = ensureEvaled(item.key);
                if (key == null) {
                    throw new _ObjectBuilderSettingEvaluationException("Map can't use null as key.");
                }
                res.put(key, ensureEvaled(item.value));
            }
            return res;
        }
        
    }
    
    private static class KeyValuePair {
        private final Object key;
        private final Object value;
        
        public KeyValuePair(Object key, Object value) {
            this.key = key;
            this.value = value;
        }
    }
    
    private class BuilderCallExpression extends ExpressionWithParameters {
        private String className;
        private boolean canBeStaticField;
        private boolean mustBeStaticField;
        
        @Override
        Object eval() throws _ObjectBuilderSettingEvaluationException {
            if (mustBeStaticField) {
                if (!canBeStaticField) {
                    throw new BugException();
                }
                return getStaticFieldValue(className);
            }
            
            Class cl;
            
            if (!modernMode) {
                try {
                    try {
                        return ClassUtil.forName(className).newInstance();
                    } catch (InstantiationException e) {
                        throw new LegacyExceptionWrapperSettingEvaluationExpression(e);
                    } catch (IllegalAccessException e) {
                        throw new LegacyExceptionWrapperSettingEvaluationExpression(e);
                    } catch (ClassNotFoundException e) {
                        throw new LegacyExceptionWrapperSettingEvaluationExpression(e);
                    }
                } catch (LegacyExceptionWrapperSettingEvaluationExpression e) {
                    if (!canBeStaticField || className.indexOf('.') == -1) {
                        throw e;
                    }
                    // Silently try to interpret className as static filed, throw the original exception if that fails. 
                    try {
                        return getStaticFieldValue(className);
                    } catch (_ObjectBuilderSettingEvaluationException e2) {
                        throw e;
                    }
                }
            }

            boolean clIsBuilderClass;
            try {
                cl = ClassUtil.forName(className + BUILDER_CLASS_POSTFIX);
                clIsBuilderClass = true;
            } catch (ClassNotFoundException e) {
                clIsBuilderClass = false;
                try {
                    cl = ClassUtil.forName(className);
                } catch (Exception e2) {
                    boolean failedToGetAsStaticField;
                    if (canBeStaticField) {
                        // Try to interpret className as static filed: 
                        try {
                            return getStaticFieldValue(className);
                        } catch (_ObjectBuilderSettingEvaluationException e3) {
                            // Suppress it
                            failedToGetAsStaticField = true;
                        }
                    } else {
                        failedToGetAsStaticField = false;
                    }
                    throw new _ObjectBuilderSettingEvaluationException(
                            "Failed to get class " + StringUtil.jQuote(className)
                            + (failedToGetAsStaticField ? " (also failed to resolve name as static field)" : "")
                            + ".",
                            e2);
                }
            }
            
            if (!clIsBuilderClass && hasNoParameters()) {
                try {
                    Field f = cl.getField(INSTANCE_FIELD_NAME);
                    if ((f.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC))
                            == (Modifier.PUBLIC | Modifier.STATIC)) {
                        return f.get(null);
                    }
                } catch (NoSuchFieldException e) {
                    // Expected
                } catch (Exception e) {
                    throw new _ObjectBuilderSettingEvaluationException(
                            "Error when trying to access " + StringUtil.jQuote(className) + "."
                            + INSTANCE_FIELD_NAME, e);
                }
            }
            
            // Create the object to return or its builder:
            Object constructorResult = callConstructor(cl);
            
            // Named parameters will set JavaBeans properties:
            setJavaBeanProperties(constructorResult, namedParamNames, namedParamValues);

            final Object result;
            if (clIsBuilderClass) {
                result = callBuild(constructorResult);
            } else {
                if (constructorResult instanceof WriteProtectable) {
                    ((WriteProtectable) constructorResult).writeProtect();
                }
                result = constructorResult;
            }
            
            return result;
        }
        
        private Object getStaticFieldValue(String dottedName) throws _ObjectBuilderSettingEvaluationException {
            int lastDotIdx = dottedName.lastIndexOf('.');
            if (lastDotIdx == -1) {
                throw new IllegalArgumentException();
            }
            String className = shorthandToFullQualified(dottedName.substring(0, lastDotIdx));
            String fieldName = dottedName.substring(lastDotIdx + 1);

            Class<?> cl;
            try {
                cl = ClassUtil.forName(className);
            } catch (Exception e) {
                throw new _ObjectBuilderSettingEvaluationException(
                        "Failed to get field's parent class, " + StringUtil.jQuote(className) + ".",
                        e);
            }
            
            Field field;
            try {
                field = cl.getField(fieldName);
            } catch (Exception e) {
                throw new _ObjectBuilderSettingEvaluationException(
                        "Failed to get field " + StringUtil.jQuote(fieldName) + " from class "
                        + StringUtil.jQuote(className) + ".",
                        e);
            }
            
            if ((field.getModifiers() & Modifier.STATIC) == 0) {
                throw new _ObjectBuilderSettingEvaluationException("Referred field isn't static: " + field);
            }
            if ((field.getModifiers() & Modifier.PUBLIC) == 0) {
                throw new _ObjectBuilderSettingEvaluationException("Referred field isn't public: " + field);
            }

            if (field.getName().equals(INSTANCE_FIELD_NAME)) {
                throw new _ObjectBuilderSettingEvaluationException(
                        "The " + INSTANCE_FIELD_NAME + " field is only accessible through pseudo-constructor call: "
                        + className + "()");
            }
            
            try {
                return field.get(null);
            } catch (Exception e) {
                throw new _ObjectBuilderSettingEvaluationException("Failed to get field value: " + field, e);
            }
        }

        private Object callConstructor(Class cl)
                throws _ObjectBuilderSettingEvaluationException {
            if (hasNoParameters()) {
                // No need to create ObjectWrapper
                try {
                    return cl.newInstance();
                } catch (Exception e) {
                    throw new _ObjectBuilderSettingEvaluationException(
                            "Failed to call " + cl.getName() + " 0-argument constructor", e);
                }
            } else {
                BeansWrapper ow = env.getObjectWrapper();
                List/*<TemplateModel>*/ tmArgs = new ArrayList(positionalParamValues.size());
                for (int i = 0; i < positionalParamValues.size(); i++) {
                    try {
                        tmArgs.add(ow.wrap(positionalParamValues.get(i)));
                    } catch (TemplateModelException e) {
                        throw new _ObjectBuilderSettingEvaluationException("Failed to wrap arg #" + (i + 1), e);
                    }
                }
                try {
                    return ow.newInstance(cl, tmArgs);
                } catch (Exception e) {
                    throw new _ObjectBuilderSettingEvaluationException(
                            "Failed to call " + cl.getName() + " constructor", e);
                }
            }
        }

        private Object callBuild(Object constructorResult)
                throws _ObjectBuilderSettingEvaluationException {
            final Class cl = constructorResult.getClass();
            Method buildMethod; 
            try {
                buildMethod = constructorResult.getClass().getMethod(BUILD_METHOD_NAME, (Class[]) null);
            } catch (NoSuchMethodException e) {
                throw new _ObjectBuilderSettingEvaluationException("The " + cl.getName()
                        + " builder class must have a public " + BUILD_METHOD_NAME + "() method", e);
            } catch (Exception e) {
                throw new _ObjectBuilderSettingEvaluationException("Failed to get the " + BUILD_METHOD_NAME
                        + "() method of the " + cl.getName() + " builder class", e);
            }
            
            try {
                return buildMethod.invoke(constructorResult, (Object[]) null);
            } catch (Exception e) {
                Throwable cause;
                if (e instanceof InvocationTargetException) {
                    cause = ((InvocationTargetException) e).getTargetException();
                } else {
                    cause = e;
                }
                throw new _ObjectBuilderSettingEvaluationException("Failed to call " + BUILD_METHOD_NAME
                        + "() method on " + cl.getName() + " instance", cause);
            }
        }

        private boolean hasNoParameters() {
            return positionalParamValues.isEmpty() && namedParamValues.isEmpty();
        }

        @Override
        protected boolean getAllowPositionalParameters() {
            return true;
        }
        
    }
    
    private class PropertyAssignmentsExpression extends ExpressionWithParameters {
        
        private final Object bean;
        
        public PropertyAssignmentsExpression(Object bean) {
            this.bean = bean;
        }

        @Override
        Object eval() throws _ObjectBuilderSettingEvaluationException {
            setJavaBeanProperties(bean, namedParamNames, namedParamValues);
            return bean;
        }

        @Override
        protected boolean getAllowPositionalParameters() {
            return false;
        }
        
    }
    
    private static class LegacyExceptionWrapperSettingEvaluationExpression
            extends _ObjectBuilderSettingEvaluationException {

        public LegacyExceptionWrapperSettingEvaluationExpression(Throwable cause) {
            super("Legacy operation failed", cause);
            if (!(
                    (cause instanceof ClassNotFoundException) 
                    || (cause instanceof InstantiationException)
                    || (cause instanceof IllegalAccessException)
                    )) {
                throw new IllegalArgumentException();
            }
        }

        public void rethrowLegacy() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
            Throwable cause = getCause();
            if (cause instanceof ClassNotFoundException) throw (ClassNotFoundException) cause;
            if (cause instanceof InstantiationException) throw (InstantiationException) cause;
            if (cause instanceof IllegalAccessException) throw (IllegalAccessException) cause;
            throw new BugException();
        }
        
    }
    
}
