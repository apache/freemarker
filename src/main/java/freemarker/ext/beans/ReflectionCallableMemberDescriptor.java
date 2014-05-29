package freemarker.ext.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * The most commonly used {@link CallableMemberDescriptor} implementation. 
 */
final class ReflectionCallableMemberDescriptor extends CallableMemberDescriptor {

    private final Member/*Method|Constructor*/ member;
    
    /**
     * Don't modify this array!
     */
    final Class[] paramTypes;
    
    ReflectionCallableMemberDescriptor(Method member, Class[] paramTypes) {
        this.member = member;
        this.paramTypes = paramTypes;
    }

    ReflectionCallableMemberDescriptor(Constructor member, Class[] paramTypes) {
        this.member = member;
        this.paramTypes = paramTypes;
    }

    TemplateModel invokeMethod(BeansWrapper bw, Object obj, Object[] args)
            throws TemplateModelException, InvocationTargetException, IllegalAccessException {
        return bw.invokeMethod(obj, (Method) member, args);
    }

    Object invokeConstructor(BeansWrapper bw, Object[] args)
            throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        return ((Constructor) member).newInstance(args);
    }

    String getDeclaration() {
        return _MethodUtil.toString(member);
    }
    
    boolean isConstructor() {
        return member instanceof Constructor;
    }
    
    boolean isStatic() {
        return (member.getModifiers() & Modifier.STATIC) != 0;
    }

    boolean isVarargs() {
        return _MethodUtil.isVarargs(member);
    }

    Class[] getParamTypes() {
        return paramTypes;
    }

    String getName() {
        return member.getName();
    }
    
}
