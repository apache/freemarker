package freemarker.ext.beans;

final class EmptyMemberAndArguments extends MaybeEmptyMemberAndArguments {
    
    static final EmptyMemberAndArguments NO_SUCH_METHOD = new EmptyMemberAndArguments();
    static final EmptyMemberAndArguments AMBIGUOUS_METHOD = new EmptyMemberAndArguments();
    
    private EmptyMemberAndArguments() { }

    public static EmptyMemberAndArguments from(EmptyCallableMemberDescriptor emtpyMemberDesc) {
        if (emtpyMemberDesc == EmptyCallableMemberDescriptor.AMBIGUOUS_METHOD) return AMBIGUOUS_METHOD;
        else if (emtpyMemberDesc == EmptyCallableMemberDescriptor.NO_SUCH_METHOD) return NO_SUCH_METHOD;
        else throw new IllegalArgumentException("Unrecognized constant: " + emtpyMemberDesc);
    }

}
