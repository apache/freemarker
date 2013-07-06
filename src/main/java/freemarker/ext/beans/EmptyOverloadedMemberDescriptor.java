package freemarker.ext.beans;

final class EmptyOverloadedMemberDescriptor extends MaybeEmptyOverloadedMemberDescriptor {
    static final EmptyOverloadedMemberDescriptor NO_SUCH_METHOD = new EmptyOverloadedMemberDescriptor();
    static final EmptyOverloadedMemberDescriptor AMBIGUOUS_METHOD = new EmptyOverloadedMemberDescriptor();
    
    private EmptyOverloadedMemberDescriptor() { };
}