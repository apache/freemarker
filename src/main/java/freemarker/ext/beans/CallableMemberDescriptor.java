package freemarker.ext.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * Packs a {@link Method} or {@link Constructor} together with its parameter types.
 */
final class CallableMemberDescriptor extends MaybeEmptyCallableMemberDescriptor {

    final Member/*Method|Constructor*/ member;
    
    /**
     * Don't modify this array!
     */
    final Class[] paramTypes;
    
    CallableMemberDescriptor(Method member, Class[] paramTypes) {
        this.member = member;
        this.paramTypes = paramTypes;
    }

    CallableMemberDescriptor(Constructor member, Class[] paramTypes) {
        this.member = member;
        this.paramTypes = paramTypes;
    }
    
}
