package freemarker.core;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    
    private static final String GET_RESULT = "getResult";

    private static final String BUILDER_CLASS_POSTFIX = "Builder";

    private static final Object NULL = new Object();
    
    private final String src;
    private final Class expectedClass;
    private final _SettingEvaluationEnvironment env;

    // Parser state:
    private int pos;
    
    // Parsing results:
    private String className;
    private List positionalParamValues = new ArrayList();
    private List/*<String>*/ namedParamNames = new ArrayList();
    private List/*<Object>*/ namedParamValues = new ArrayList();
    boolean v2321Mode = false;
    
    private _ObjectBuilderSettingEvaluator(String src, Class expectedClass, _SettingEvaluationEnvironment env) {
        this.src = src;
        this.expectedClass = expectedClass;
        this.env = env;
    }
    
    private static Map/*<String,String>*/ SHORTHANDS;
    
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

    static Object eval(String src, Class expectedClass, _SettingEvaluationEnvironment env) throws _ObjectBuilderSettingEvaluationException,
            ClassNotFoundException, InstantiationException, IllegalAccessException {
        return new _ObjectBuilderSettingEvaluator(src, expectedClass, env).eval();
    }
    
    private Object eval() throws _ObjectBuilderSettingEvaluationException,
            ClassNotFoundException, InstantiationException, IllegalAccessException {
        parse();
        return execute();
    }

    private Object execute() throws _ObjectBuilderSettingEvaluationException,
    // Don't pack these into {@link ObjectFactorySettingEvaluationException} for backward compatibility:
    ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (!v2321Mode) {
            return ClassUtil.forName(className).newInstance();
        } else {
            return execute2321();
        }
    }
    
    private Object execute2321() throws _ObjectBuilderSettingEvaluationException {
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
        
        // Create the object to return or its builder:
        Object constructorResult = callConstructor(cl);
        
        // Named parameters will set JavaBeans properties:
        setJavaBeanProperties(constructorResult);

        final Object result;
        if (clIsBuilderClass) {
            result = callGetResult(constructorResult);
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
        
        /*
        // !!T
        return "class=" + className + ", v2321Mode=" + v2321Mode
                + ", positional=" + positionalParamValues + ", named=" + namedParamNames + namedParamValues;
        */
    }

    private Object callGetResult(Object constructorResult)
            throws _ObjectBuilderSettingEvaluationException {
        final Class cl = constructorResult.getClass();
        Method getResultMethod; 
        try {
            getResultMethod = constructorResult.getClass().getMethod(GET_RESULT, (Class[]) null);
        } catch (NoSuchMethodException e) {
            throw new _ObjectBuilderSettingEvaluationException("The " + cl.getName()
                    + " builder class must have a public " + GET_RESULT + "() method", e);
        } catch (Exception e) {
            throw new _ObjectBuilderSettingEvaluationException("Failed to get the " + GET_RESULT
                    + "() method of the " + cl.getName() + " builder class", e);
        }
        
        try {
            return getResultMethod.invoke(constructorResult, (Object[]) null);
        } catch (Exception e) {
            Throwable cause;
            if (e instanceof InvocationTargetException) {
                cause = ((InvocationTargetException) e).getTargetException();
            } else {
                cause = e;
            }
            throw new _ObjectBuilderSettingEvaluationException("Failed to call " + GET_RESULT + "() method on "
                    + cl.getName() + " instance", cause);
        }
    }

    private void setJavaBeanProperties(Object bean)
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

    private Object callConstructor(Class cl)
            throws _ObjectBuilderSettingEvaluationException {
        Object constructorResult;
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
            constructorResult = ow.newInstance(cl, tmArgs);
        } catch (Exception e) {
            throw new _ObjectBuilderSettingEvaluationException("Failed to call " + cl.getName() + " constructor", e);
        }
        return constructorResult;
    }

    private void parse() throws _ObjectBuilderSettingEvaluationException {
        skipWS();
        
        final String fetchedClassName = fetchClassName();
        className = shorthandToFullQualified(fetchedClassName);
        if (fetchedClassName != className) {
            // Before 2.3.21 only full-qualified class names were allowed
            v2321Mode = true;
        }
        
        skipWS();

        if (fetchCharOrEOS("(") != 0) {
            // Before 2.3.21 there was no parameter list
            v2321Mode = true;
            
            skipWS();
            if (fetchOptionalChar(")") != ')') { 
                do {
                    skipWS();
                    
                    Object paramNameOrValue = fetchValueOrName(false);
                    if (paramNameOrValue != null) {
                        skipWS();
                        if (paramNameOrValue instanceof Name) {
                            namedParamNames.add(((Name) paramNameOrValue).name);
                            
                            skipWS();
                            fetchChar("=");
                            skipWS();
                            
                            int paramValPos = pos;
                            Object paramValue = fetchValueOrName(false);
                            if (paramValue instanceof Name) {
                                throw new _ObjectBuilderSettingEvaluationException("concrete value", src, paramValPos);
                            }
                            namedParamValues.add(paramValue);
                        } else {
                            if (!namedParamNames.isEmpty()) {
                                throw new _ObjectBuilderSettingEvaluationException(
                                        "Positional parameters must precede named parameters");
                            }
                            positionalParamValues.add(paramNameOrValue);
                        }
                        
                        skipWS();
                    }
                } while (fetchChar(",)") == ',');
            }
            
            skipWS();
            if (pos != src.length()) {
                throw new _ObjectBuilderSettingEvaluationException("end-of-expression", src, pos);
            }
        }
    }

    private char fetchOptionalChar(String expectedChars) {
        if (pos < src.length()) {
            char c = src.charAt(pos);
            if (expectedChars.indexOf(c) != -1) {
                pos++;
                return c;
            }
        }
        return 0;
    }
    
    private char fetchChar(String expectedChars) throws _ObjectBuilderSettingEvaluationException {
        return fetchChar(expectedChars, false);
    }
    
    private char fetchChar(String expectedChars, boolean allowEOS) throws _ObjectBuilderSettingEvaluationException {
        char c = pos < src.length() ? src.charAt(pos) : 0;
        if (c == 0 && allowEOS) {
            return c;
        }
        if (expectedChars.indexOf(c) != -1) {
            pos++;
            return c;
        }
        
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < expectedChars.length(); i++) {
            if (i != 0) {
                sb.append(" or ");
            }
            sb.append(StringUtil.jQuote(expectedChars.substring(i, i + 1)));
        }
        if (allowEOS) {
            sb.append(" or end-of-string");
        }
        throw new _ObjectBuilderSettingEvaluationException(
                sb.toString(),
                src, pos);
    }
    
    private char fetchCharOrEOS(String expectedChars) throws _ObjectBuilderSettingEvaluationException {
        return fetchChar(expectedChars, true);
    }

    private String fetchClassName() throws _ObjectBuilderSettingEvaluationException {
        StringBuffer sb = new StringBuffer();
        do {
            sb.append(fetchSimpleName(false));
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
    
    private Object fetchValueOrName(boolean optional) throws _ObjectBuilderSettingEvaluationException {
        if (pos < src.length()) {
            Object val = fetchNumberLike(true);
            if (val != null) {
                return val;
            }
            
            String name = fetchSimpleName(true);
            if (name != null) {
                if (name.equals("true")) return Boolean.TRUE;
                if (name.equals("false")) return Boolean.FALSE;
                if (name.equals("null")) return NULL;
                return new Name(name);
            }
        }
        
        if (optional) {
            return null;
        } else {
            throw new _ObjectBuilderSettingEvaluationException("value or name", src, pos);
        }
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

    private boolean isIdentifierMiddle(char c) {
        return isIdentifierStart(c) || isASCIIDigit(c);
    }

    private boolean isASCIIDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
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
    
    private static class Name {
        
        public Name(String name) {
            this.name = name;
        }

        private final String name;
    }

}
