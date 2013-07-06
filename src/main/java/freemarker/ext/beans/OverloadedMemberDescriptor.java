package freemarker.ext.beans;

import java.lang.reflect.Member;

final class OverloadedMemberDescriptor extends MaybeEmptyOverloadedMemberDescriptor {

    final Member member;
    final Class[] paramTypes;
    
    public OverloadedMemberDescriptor(Member member, Class[] paramTypes) {
        this.member = member;
        this.paramTypes = paramTypes;
    }
    
}