package freemarker.ext.beans;

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Version;

public class DefaultObjectWrapperWithSortedMethods extends DefaultObjectWrapper {
    
    public DefaultObjectWrapperWithSortedMethods(boolean desc) {
        setMethodSorter(this, desc);
    }

    public DefaultObjectWrapperWithSortedMethods(Version incompatibleImprovements, boolean desc) {
        super(incompatibleImprovements);
        setMethodSorter(this, desc);
    }
    
    static void setMethodSorter(BeansWrapper bw, boolean desc) {
        bw.setMethodSorter(new AlphabeticalMethodSorter(desc));
    }
    
}
