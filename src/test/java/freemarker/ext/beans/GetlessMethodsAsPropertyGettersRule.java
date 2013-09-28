package freemarker.ext.beans;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import freemarker.ext.beans.BeansWrapper.MethodAppearanceDecision;

class GetlessMethodsAsPropertyGettersRule implements MethodAppearanceFineTuner, SingletonCustomizer {
    
    static final GetlessMethodsAsPropertyGettersRule INSTANCE = new GetlessMethodsAsPropertyGettersRule();
    
    // Can't be constructed from outside
    private GetlessMethodsAsPropertyGettersRule() { }

    public void fineTuneMethodAppearance(
            Class clazz, Method m, MethodAppearanceDecision decision) {
        if (m.getDeclaringClass() != Object.class
                && m.getReturnType() != void.class
                && m.getParameterTypes().length == 0) {
            String mName = m.getName();
            if (!looksLikePropertyReadMethod(mName)) {
                decision.setExposeMethodAs(null);
                try {
                    decision.setExposeAsProperty(new PropertyDescriptor(
                            mName, clazz, mName, null));
                } catch (IntrospectionException e) {  // Won't happen...
                    throw new RuntimeException(e); 
                }
            }
        }
    }
    
    private static boolean looksLikePropertyReadMethod(String name) {
        final int verbEnd;
        if (name.startsWith("get")) verbEnd = 3;
        else if (name.startsWith("is")) verbEnd = 2;
        else return false;
        
        return name.length() == verbEnd || Character.isUpperCase(name.charAt(verbEnd));
    }

}