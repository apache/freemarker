package freemarker.ext.beans;

final class EmptyCallableMemberDescriptor extends MaybeEmptyCallableMemberDescriptor {
    
    static final EmptyCallableMemberDescriptor NO_SUCH_METHOD = new EmptyCallableMemberDescriptor();
    static final EmptyCallableMemberDescriptor AMBIGUOUS_METHOD = new EmptyCallableMemberDescriptor();
    
    private EmptyCallableMemberDescriptor() { };
    
}