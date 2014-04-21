package freemarker.ext.beans;

/**
 * Represents that no member was chosen. Why it wasn't is represented by the two singleton instances,
 * {@link #NO_SUCH_METHOD} and {@link #AMBIGUOUS_METHOD}.
 */
final class EmptyCallableMemberDescriptor extends MaybeEmptyCallableMemberDescriptor {
    
    static final EmptyCallableMemberDescriptor NO_SUCH_METHOD = new EmptyCallableMemberDescriptor();
    static final EmptyCallableMemberDescriptor AMBIGUOUS_METHOD = new EmptyCallableMemberDescriptor();
    
    private EmptyCallableMemberDescriptor() { };
    
}