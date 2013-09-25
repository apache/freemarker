package freemarker.ext.beans;

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Version;

public class DefaultObjectWrapperWithShortedMethods extends DefaultObjectWrapper {
    
    public DefaultObjectWrapperWithShortedMethods(boolean desc) {
        setMethodShorter(this, desc);
    }

    public DefaultObjectWrapperWithShortedMethods(Version incompatibleImprovements, boolean desc) {
        super(incompatibleImprovements);
        setMethodShorter(this, desc);
    }
    
    static void setMethodShorter(BeansWrapper bw, boolean desc) {
        bw.setMethodShorter(new AlphabeticalMethodShorter(desc));
    }
    
}
