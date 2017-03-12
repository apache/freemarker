package freemarker.ext.beans;

public class BridgeMethodsBean extends BridgeMethodsBeanBase<String> {

    static final String M1_RETURN_VALUE = "m1ReturnValue"; 
    
    @Override
    public String m1() {
        return M1_RETURN_VALUE;
    }

}
