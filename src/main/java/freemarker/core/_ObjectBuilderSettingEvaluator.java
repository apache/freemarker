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

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import freemarker.ext.beans.BeansWrapper;
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
 * Don't use this; used internally by FreeMarker, might changes without notice.
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

    private static Map/*<String,String>*/ SHORTHANDS;

    private final String src;
    private final Class expectedClass;
    private final _SettingEvaluationEnvironment env;

    // Parser state:
    private int pos;
    
    // Parsing results:
    private boolean v2321Mode = false;
    
    private _ObjectBuilderSettingEvaluator(String src, int pos, Class expectedClass, _SettingEvaluationEnvironment env) {
        this.src = src;
        this.pos = pos;
        this.expectedClass = expectedClass;
        this.env = env;
    }

    public static Object eval(String src, Class expectedClass, _SettingEvaluationEnvironment env) throws _ObjectBuilderSettingEvaluationException,
            ClassNotFoundException, InstantiationException, IllegalAccessException {
        return new _ObjectBuilderSettingEvaluator(src, 0, expectedClass, env).eval();
    }

    /**
     * Used for getting a list of setting assignments (like {@code (x=1, y=2)}) from an existing string, and apply it on
     * an existing bean.
     * 
     * @return The location of the next character to process.
     */
    public static int configureBean(String argumentListSrc, int posAfterOpenParen, Object bean, _SettingEvaluationEnvironment env)
            throws _ObjectBuilderSettingEvaluationException,
            ClassNotFoundException, InstantiationException, IllegalAccessException {
        return new _ObjectBuilderSettingEvaluator(argumentListSrc, posAfterOpenParen, bean.getClass(), env).configureBean(bean);
    }
    
    private Object eval() throws _ObjectBuilderSettingEvaluationException,
            ClassNotFoundException, InstantiationException, IllegalAccessException {
        return execute(parse());
    }
    
    private int configureBean(Object bean) throws _ObjectBuilderSettingEvaluationException,
            ClassNotFoundException, InstantiationException, IllegalAccessException {
        final PropertyAssignmentsExpression propAssignments = new PropertyAssignmentsExpression(bean);
        fetchParameterListInto(propAssignments);
        skipWS();
        propAssignments.eval();
        return pos;
    }

    private BuilderExpression parse() throws _ObjectBuilderSettingEvaluationException {
        skipWS();
        BuilderExpression exp = fetchBuilderCall(true, false);
        skipWS();
        if (pos != src.length()) {
            throw new _ObjectBuilderSettingEvaluationException("end-of-expression", src, pos);
        }
        return exp;
    }

    private Object execute(BuilderExpression exp) throws _ObjectBuilderSettingEvaluationException,
    // Don't pack these into {@link ObjectFactorySettingEvaluationException} for backward compatibility:
    ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (!v2321Mode) {
            return ClassUtil.forName(exp.className).newInstance();
        } else {
            return exp.eval();
        }
    }

    private Object eval(Object value) throws _ObjectBuilderSettingEvaluationException {
        return value instanceof SettingExpression ? ((SettingExpression) value).eval() : value;
    }

    private BuilderExpression fetchBuilderCall(boolean topLevel, boolean optional)
            throws _ObjectBuilderSettingEvaluationException {
        int startPos = pos;
        
        BuilderExpression exp = new BuilderExpression();
        
        {
            final String fetchedClassName = fetchClassName(optional);
            if (fetchedClassName == null) {
                return null;
            }
            exp.className = shorthandToFullQualified(fetchedClassName);
            if (!fetchedClassName.equals(exp.className)) {
                // Before 2.3.21 only full-qualified class names were allowed
                v2321Mode = true;
            }
        }
        
        skipWS();
        
        char openParen = fetchOptionalChar("(");
        // Only the top-level expression can omit the "(...)"
        if (openParen == 0 && !topLevel) {
            if (!optional) {
                throw new _ObjectBuilderSettingEvaluationException("(", src, pos);
            }
            pos = startPos;
            return null;
        }
    
        if (openParen != 0) {
            fetchParameterListInto(exp);
        }
        
        return exp;
    }

    private void fetchParameterListInto(ExpressionWithParameters exp) throws _ObjectBuilderSettingEvaluationException {
        // Before 2.3.21 there was no parameter list
        v2321Mode = true;
        
        skipWS();
        if (fetchOptionalChar(")") != ')') { 
            do {
                skipWS();
                
                Object paramNameOrValue = fetchValueOrName(false);
                if (paramNameOrValue != null) {
                    skipWS();
                    if (paramNameOrValue instanceof ParameterName) {
                        exp.namedParamNames.add(((ParameterName) paramNameOrValue).name);
                        
                        skipWS();
                        fetchRequiredChar("=");
                        skipWS();
                        
                        int paramValPos = pos;
                        Object paramValue = fetchValueOrName(false);
                        if (paramValue instanceof ParameterName) {
                            throw new _ObjectBuilderSettingEvaluationException("concrete value", src, paramValPos);
                        }
                        exp.namedParamValues.add(eval(paramValue));
                    } else {
                        if (!exp.namedParamNames.isEmpty()) {
                            throw new _ObjectBuilderSettingEvaluationException(
                                    "Positional parameters must precede named parameters");
                        }
                        if (!exp.getAllowPositionalParameters()) {
                            throw new _ObjectBuilderSettingEvaluationException(
                                    "Positional parameters not supported here");
                        }
                        exp.positionalParamValues.add(eval(paramNameOrValue));
                    }
                    
                    skipWS();
                }
            } while (fetchRequiredChar(",)") == ',');
        }
    }

    private Object fetchValueOrName(boolean optional) throws _ObjectBuilderSettingEvaluationException {
        if (pos < src.length()) {
            Object val = fetchNumberLike(true);
            if (val != null) {
                return val;
            }
    
            val = fetchStringLiteral(true);
            if (val != null) {
                return val;
            }
            
            val = fetchBuilderCall(false, true);
            if (val != null) {
                return val;
            }
            
            String name = fetchSimpleName(true);
            if (name != null) {
                if (name.equals("true")) return Boolean.TRUE;
                if (name.equals("false")) return Boolean.FALSE;
                if (name.equals("null")) return NullExpression.INSTANCE;
                return new ParameterName(name);
            }
        }
        
        if (optional) {
            return null;
        } else {
            throw new _ObjectBuilderSettingEvaluationException("value or name", src, pos);
        }
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
        StringBuffer sb = new StringBuffer();
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
        
        return sb.toString();
    }

    private Object fetchNumberLike(boolean optional) throws _ObjectBuilderSettingEvaluationException {
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
                return null;
            } else {
                throw new _ObjectBuilderSettingEvaluationException("number-like", src, pos);
            }
        }
        
        String tk = src.substring(startPos, pos);
        if (isVersion) {
            try {
                return new Version(tk);
            } catch (IllegalArgumentException e) {
                throw new _ObjectBuilderSettingEvaluationException("Malformed version number: " + tk, e);
            }
        } else {
            try {
                if (tk.endsWith(".")) {
                    throw new NumberFormatException("A number can't end with a dot");
                }
                if (tk.startsWith(".") || tk.startsWith("-.")  || tk.startsWith("+.")) {
                    throw new NumberFormatException("A number can't start with a dot");
                }
                return new BigDecimal(tk);
            } catch (NumberFormatException e) {
                throw new _ObjectBuilderSettingEvaluationException("Malformed number: " + tk, e);
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
                return null;
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
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < expectedChars.length(); i++) {
                if (i != 0) {
                    sb.append(" or ");
                }
                sb.append(StringUtil.jQuote(expectedChars.substring(i, i + 1)));
            }
            if (optional) {
                sb.append(" or end-of-string");
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
            SHORTHANDS.put("DefaultObjectWrapper", DefaultObjectWrapper.class.getName());
            SHORTHANDS.put("BeansWrapper", BeansWrapper.class.getName());
            SHORTHANDS.put("SimpleObjectWrapper", SimpleObjectWrapper.class.getName());
        }
        String fullClassName = (String) SHORTHANDS.get(className);
        return fullClassName == null ? className : fullClassName;
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

    private static class ParameterName {
        
        public ParameterName(String name) {
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
    
    private class BuilderExpression extends ExpressionWithParameters {
        private String className;
        
        Object eval() throws _ObjectBuilderSettingEvaluationException {
            Class cl;
            try {
                cl = ClassUtil.forName(className);
            } catch (Exception e) {
                throw new _ObjectBuilderSettingEvaluationException(
                        "Failed to get class " + StringUtil.jQuote(className) + ".", e);
            }
            
            boolean clIsBuilderClass;
            try {
                cl = ClassUtil.forName(cl.getName() + BUILDER_CLASS_POSTFIX);
                clIsBuilderClass = true;
            } catch (ClassNotFoundException e) {
                clIsBuilderClass = false;
            }
            
            if (!clIsBuilderClass && hasNoParameters()) {
                try {
                    Field f = cl.getField(INSTANCE_FIELD_NAME);
                    if ((f.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC)) == (Modifier.PUBLIC | Modifier.STATIC)) {
                        return f.get(null);
                    }
                } catch (NoSuchFieldException e) {
                    // Expected
                } catch (Exception e) {
                    throw new _ObjectBuilderSettingEvaluationException(
                            "Error when trying to access " + StringUtil.jQuote(className) + "." + INSTANCE_FIELD_NAME, e);
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
            
            if (!expectedClass.isInstance(result)) {
                throw new _ObjectBuilderSettingEvaluationException("The resulting object (of class "
                        + result.getClass() + ") is not a(n) " + expectedClass.getName() + ".");
            }
            
            return result;
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
                throw new _ObjectBuilderSettingEvaluationException("Failed to call " + BUILD_METHOD_NAME + "() method on "
                        + cl.getName() + " instance", cause);
            }
        }

        private boolean hasNoParameters() {
            return positionalParamValues.isEmpty() && namedParamValues.isEmpty();
        }

        protected boolean getAllowPositionalParameters() {
            return true;
        }
        
    }
    
    private class PropertyAssignmentsExpression extends ExpressionWithParameters {
        
        private final Object bean;
        
        public PropertyAssignmentsExpression(Object bean) {
            this.bean = bean;
        }

        Object eval() throws _ObjectBuilderSettingEvaluationException {
            setJavaBeanProperties(bean, namedParamNames, namedParamValues);
            return bean;
        }

        protected boolean getAllowPositionalParameters() {
            return false;
        }
        
    }
    
    private static class NullExpression extends SettingExpression {
        
        static final NullExpression INSTANCE = new NullExpression();

        Object eval() throws _ObjectBuilderSettingEvaluationException {
            return null;
        }
        
    };
    

}
