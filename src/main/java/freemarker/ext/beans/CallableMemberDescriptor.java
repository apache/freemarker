package freemarker.ext.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import freemarker.template.TemplateModelException;

/**
 * Packs a {@link Method} or {@link Constructor} together with its parameter types.
 */
abstract class CallableMemberDescriptor extends MaybeEmptyCallableMemberDescriptor {

    abstract Object invokeMethod(BeansWrapper bw, Object obj, Object[] args)
            throws TemplateModelException, InvocationTargetException, IllegalAccessException;

    abstract Object invokeConstructor(BeansWrapper bw, Object[] args)
            throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException,
            TemplateModelException;
    
    abstract String getDeclaration();
    
    abstract boolean isConstructor();
    
    abstract boolean isStatic();

    abstract boolean isVarargs();

    abstract Class[] getParamTypes();

    abstract String getName();
    
}
