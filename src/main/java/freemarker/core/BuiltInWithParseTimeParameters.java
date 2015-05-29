package freemarker.core;

import java.util.List;


abstract class BuiltInWithParseTimeParameters extends SpecialBuiltIn {

    abstract void bindToParameters(List/*<Expression>*/ parameters) throws ParseException;

    public String getCanonicalForm() {
        StringBuffer buf = new StringBuffer();
        
        buf.append(super.getCanonicalForm());
        
        buf.append("(");
        List/*<Expression>*/args = getArgumentsAsList();
        int size = args.size();
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                buf.append(", ");
            }
            Expression arg = (Expression) args.get(i);
            buf.append(arg.getCanonicalForm());
        }
        buf.append(")");
        
        return buf.toString();
    }
    
    String getNodeTypeSymbol() {
        return super.getNodeTypeSymbol() + "(...)";
    }        
    
    int getParameterCount() {
        return super.getParameterCount() + getArgumentsCount();
    }

    Object getParameterValue(int idx) {
        final int superParamCnt = super.getParameterCount();
        if (idx < superParamCnt) {
            return super.getParameterValue(idx); 
        }
        
        final int argIdx = idx - superParamCnt;
        return getArgumentParameterValue(argIdx);
    }
    
    ParameterRole getParameterRole(int idx) {
        final int superParamCnt = super.getParameterCount();
        if (idx < superParamCnt) {
            return super.getParameterRole(idx); 
        }
        
        if (idx - superParamCnt < getArgumentsCount()) {
            return ParameterRole.ARGUMENT_VALUE;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    protected abstract List getArgumentsAsList();
    
    protected abstract int getArgumentsCount();

    protected abstract Expression getArgumentParameterValue(int argIdx);
    
}
