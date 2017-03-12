package freemarker.ext.beans;

public class BridgeMethodsWithDefaultMethodBean implements BridgeMethodsWithDefaultMethodBeanBase<String> {

    static final String M1_RETURN_VALUE = "m1ReturnValue"; 
    
    public String m1() {
        return M1_RETURN_VALUE;
    }

}
