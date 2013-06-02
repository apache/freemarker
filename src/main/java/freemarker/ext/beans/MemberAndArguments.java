package freemarker.ext.beans;

import java.lang.reflect.Member;

/**
 * @author Attila Szegedi
 */
class MemberAndArguments {
    private final Member member;
    private final Object[] args;
    
    MemberAndArguments(Member member, Object[] args) {
        this.member = member;
        this.args = args;
    }
    
    Object[] getArgs() {
        return args;
    }
    
    public Member getMember() {
        return member;
    }
}
