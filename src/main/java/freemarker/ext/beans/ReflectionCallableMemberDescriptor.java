package freemarker.ext.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import freemarker.template.TemplateModelException;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility._MethodUtil;

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

    Object invokeMethod(BeansWrapper bw, Object obj, Object[] args)
            throws TemplateModelException, InvocationTargetException, IllegalAccessException {
        return bw.invokeMethod(obj, (Method) member, args);
    }

    Object invokeConstructor(BeansWrapper bw, Object[] args)
            throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        return ((Constructor) member).newInstance(args);
    }

    String getDeclaration() {
        StringBuffer sb = new StringBuffer();
        
        String className = ClassUtil.getShortClassName(member.getDeclaringClass());
        if (className != null) {
            sb.append(className);
            sb.append('.');
        }
        sb.append(member.getName());

        sb.append('(');
        Class[] paramTypes = _MethodUtil.getParameterTypes(member);
        for (int i = 0; i < paramTypes.length; i++) {
            if (i != 0) sb.append(", ");
            String paramTypeDecl = ClassUtil.getShortClassName(paramTypes[i]);
            if (i == paramTypes.length - 1 && paramTypeDecl.endsWith("[]") && isVarargs()) {
                sb.append(paramTypeDecl.substring(0, paramTypeDecl.length() - 2));
                sb.append("...");
            } else {
                sb.append(paramTypeDecl);
            }
        }
        sb.append(')');
        
        return sb.toString();
    }
    
    boolean isConstructor() {
        return member instanceof Constructor;
    }
    
    boolean isStatic() {
        return (member.getModifiers() & Modifier.STATIC) != 0;
    }

    boolean isVarargs() {
        return _MethodUtil.isVarArgs(member);
    }

    Class[] getParamTypes() {
        return paramTypes;
    }

    String getName() {
        return member.getName();
    }
    
}
