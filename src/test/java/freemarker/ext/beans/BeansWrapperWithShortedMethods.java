package freemarker.ext.beans;


import freemarker.template.Version;

/**
 * Used so that the order in which the methods are added to the introspection cache is deterministic. 
 */
public abstract class BeansWrapperWithShortedMethods extends BeansWrapper {
    
    public BeansWrapperWithShortedMethods(boolean desc) {
        this.setMethodShorter(new AlphabeticalMethodShorter(desc));
    }

    public BeansWrapperWithShortedMethods(Version incompatibleImprovements, boolean desc) {
        super(incompatibleImprovements);
        this.setMethodShorter(new AlphabeticalMethodShorter(desc));
    }

}
