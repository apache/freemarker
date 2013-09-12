package freemarker.ext.beans;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import freemarker.template.utility.Collections12;
import freemarker.template.utility._MethodUtil;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */ 
public class _BeansAPI {

    private _BeansAPI() { }
    
    public static String getAsClassicCompatibleString(BeanModel bm) {
        return bm.getAsClassicCompatibleString();
    }
    
    /**
     * Convenience method that combines {@link #getConstructor(Class, Object[])} and
     * {@link #newInstance(Constructor, Object[])}.
     */
    public static Object newInstance(Class pClass, Object[] args)
            throws NoSuchMethodException, IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        return newInstance(getConstructor(pClass, args), args);
    }
    
    /**
     * Gets the constructor that matches the types of the arguments the best. So this is more
     * than what the Java reflection API provides in that it can handle overloaded constructors. This re-uses the
     * overloaded method selection logic of {@link BeansWrapper}.
     */
    public static Constructor getConstructor(Class pClass, Object[] args) throws NoSuchMethodException {
        if (args == null) args = Collections12.EMPTY_OBJECT_ARRAY;
        
        final ArgumentTypes argTypes = new ArgumentTypes(args, true);
        final List fixedArgMemberDescs = new ArrayList();
        final List varArgsMemberDescs = new ArrayList();
        final Constructor[] constrs = pClass.getConstructors();
        for (int i = 0; i < constrs.length; i++) {
            Constructor constr = constrs[i];
            CallableMemberDescriptor memberDesc = new CallableMemberDescriptor(constr, constr.getParameterTypes());
            if (!_MethodUtil.isVarArgs(constr)) {
                fixedArgMemberDescs.add(memberDesc);
            } else {
                varArgsMemberDescs.add(memberDesc);
            }
        }
        
        MaybeEmptyCallableMemberDescriptor contrDesc = argTypes.getMostSpecific(fixedArgMemberDescs, false);
        if (contrDesc == EmptyCallableMemberDescriptor.NO_SUCH_METHOD) {
            contrDesc = argTypes.getMostSpecific(varArgsMemberDescs, true);
        }
        
        if (contrDesc instanceof EmptyCallableMemberDescriptor) {
            if (contrDesc == EmptyCallableMemberDescriptor.NO_SUCH_METHOD) {
                throw new NoSuchMethodException(
                        "There's no public " + pClass.getName()
                        + " constructor with compatible parameter list.");
            } else if (contrDesc == EmptyCallableMemberDescriptor.AMBIGUOUS_METHOD) {
                throw new NoSuchMethodException(
                        "There are multiple public " + pClass.getName()
                        + " constructors that match the compatible parameter list with the same preferability.");
            } else {
                throw new NoSuchMethodException();
            }
        } else {
            return (Constructor) ((CallableMemberDescriptor) contrDesc).member;
        }
    }
    
    /**
     * Creates a new instance using a flat argument list (no varargs array parameter). 
     */
    public static Object newInstance(Constructor constr, Object[] args)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        if (args == null) args = Collections12.EMPTY_OBJECT_ARRAY;
        
        final Object[] packedArgs;
        if (_MethodUtil.isVarArgs(constr)) {
            // We have to put all the varargs arguments into a single array argument.

            final Class[] paramTypes = constr.getParameterTypes();
            final int fixedArgCnt = paramTypes.length - 1;
            
            packedArgs = new Object[fixedArgCnt + 1]; 
            for (int i = 0; i < fixedArgCnt; i++) {
                packedArgs[i] = args[i];
            }
            
            final Class compType = paramTypes[fixedArgCnt].getComponentType();
            final int varArgCnt = args.length - fixedArgCnt;
            final Object varArgsArray = Array.newInstance(compType, varArgCnt);
            for (int i = 0; i < varArgCnt; i++) {
                Array.set(varArgsArray, i, args[fixedArgCnt + i]);
            }
            packedArgs[fixedArgCnt] = varArgsArray;
        } else {
            packedArgs = args;
        }
        
        return constr.newInstance(packedArgs);
    }
    
}
