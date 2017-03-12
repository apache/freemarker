package freemarker.ext.beans;

public interface BridgeMethodsWithDefaultMethodBeanBase2 extends BridgeMethodsWithDefaultMethodBeanBase<String> {

    @Override
    default String m1() {
        return BridgeMethodsWithDefaultMethodBean.M1_RETURN_VALUE;
    }
    
}
