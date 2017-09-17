package org.apache.freemarker.core;

/**
 * Used for implementing #break and #continue. 
 */
// TODO [FM3] This is not a good mechanism (like what if we have <#list ...><@m><#break><@></#list>, and inside `m`
// there's <#list ...><#nested></#list>)
class BreakOrContinueException extends RuntimeException {
    static final BreakOrContinueException BREAK_INSTANCE = new BreakOrContinueException();
    static final BreakOrContinueException CONTINUE_INSTANCE = new BreakOrContinueException();
    
    private BreakOrContinueException() { }
}