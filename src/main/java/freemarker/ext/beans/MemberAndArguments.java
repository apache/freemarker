package freemarker.ext.beans;

import java.lang.reflect.InvocationTargetException;

import freemarker.template.TemplateModelException;

/**
 * @author Attila Szegedi
 */
class MemberAndArguments extends MaybeEmptyMemberAndArguments {
    
    private final CallableMemberDescriptor callableMemberDesc;
    private final Object[] args;
    
    /**
     * @param args The already unwrapped arguments
     */
    MemberAndArguments(CallableMemberDescriptor memberDesc, Object[] args) {
        this.callableMemberDesc = memberDesc;
        this.args = args;
    }
    
    /**
     * The already unwrapped arguments.
     */
    Object[] getArgs() {
        return args;
    }
    
    Object invokeMethod(BeansWrapper bw, Object obj)
            throws TemplateModelException, InvocationTargetException, IllegalAccessException {
        return callableMemberDesc.invokeMethod(bw, obj, args);
    }

    Object invokeConstructor(BeansWrapper bw)
            throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException,
            TemplateModelException {
        return callableMemberDesc.invokeConstructor(bw, args);
    }
    
    CallableMemberDescriptor getCallableMemberDescriptor() {
        return callableMemberDesc;
    }
    
}
