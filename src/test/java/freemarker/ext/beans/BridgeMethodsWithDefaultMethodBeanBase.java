package freemarker.ext.beans;

public interface BridgeMethodsWithDefaultMethodBeanBase<T> {

    default T m1() {
        return null;
    }
    
    default T m2() {
        return null;
    }
    
}
