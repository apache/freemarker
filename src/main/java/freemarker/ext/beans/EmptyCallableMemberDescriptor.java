package freemarker.ext.beans;

/**
 * Represents that no member was chosen. Why it wasn't is represented by the two singleton instances,
 * {@link #NO_SUCH_METHOD} and {@link #AMBIGUOUS_METHOD}. (Note that instances of these are cached associated with the
 * argument types, thus it shouldn't store details that are specific to the actual argument values. In fact, it better
 * remains a set of singletons.)     
 */
final class EmptyCallableMemberDescriptor extends MaybeEmptyCallableMemberDescriptor {
    
    static final EmptyCallableMemberDescriptor NO_SUCH_METHOD = new EmptyCallableMemberDescriptor();
    static final EmptyCallableMemberDescriptor AMBIGUOUS_METHOD = new EmptyCallableMemberDescriptor();
    
    private EmptyCallableMemberDescriptor() { };
    
}