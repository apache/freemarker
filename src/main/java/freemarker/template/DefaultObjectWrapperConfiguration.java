package freemarker.template;

import freemarker.ext.beans.BeansWrapperConfiguration;

/**
 * Holds {@link DefaultObjectWrapper} configuration settings and defines their defaults.
 * You will not use this abstract class directly, but concrete subclasses like {@link DefaultObjectWrapperBuilder}.
 * Unless, you are developing a builder for a custom {@link DefaultObjectWrapper} subclass. In that case, note that
 * overriding the {@link #equals} and {@link #hashCode} is important, as these object are used as {@link ObjectWrapper}
 * singleton lookup keys.
 * 
 * @since 2.3.22
 */
public abstract class DefaultObjectWrapperConfiguration extends BeansWrapperConfiguration {
    
    private boolean useAdaptersForContainers;
    private boolean useAdaptersForNonListCollections;

    protected DefaultObjectWrapperConfiguration(Version incompatibleImprovements) {
        super(DefaultObjectWrapper.normalizeIncompatibleImprovementsVersion(incompatibleImprovements), true);
        useAdaptersForContainers = getIncompatibleImprovements().intValue() >= _TemplateAPI.VERSION_INT_2_3_22;
        useAdaptersForNonListCollections = false; // [2.4]: = IcI >= _TemplateAPI.VERSION_INT_2_4_0;
    }

    /** See {@link DefaultObjectWrapper#getUseAdaptersForContainers()}. */
    public boolean getUseAdaptersForContainers() {
        return useAdaptersForContainers;
    }

    /** See {@link DefaultObjectWrapper#setUseAdaptersForContainers(boolean)}. */
    public void setUseAdaptersForContainers(boolean useAdaptersForContainers) {
        this.useAdaptersForContainers = useAdaptersForContainers;
    }
    
    /** See {@link DefaultObjectWrapper#getUseAdaptersForNonListCollections()}. */
    public boolean getUseAdaptersForNonListCollections() {
        return useAdaptersForNonListCollections;
    }

    /** See {@link DefaultObjectWrapper#setUseAdaptersForNonListCollections(boolean)}. */
    public void setUseAdaptersForNonListCollections(boolean legacyNonListCollectionWrapping) {
        this.useAdaptersForNonListCollections = legacyNonListCollectionWrapping;
    }

    public int hashCode() {
        int result = super.hashCode();
        final int prime = 31;
        result = result * prime + (useAdaptersForContainers ? 1231 : 1237);
        result = result * prime + (useAdaptersForNonListCollections ? 1231 : 1237);
        return result;
    }

    public boolean equals(Object obj) {
        if (!super.equals(obj)) return false;
        final DefaultObjectWrapperConfiguration dowCfg = (DefaultObjectWrapperConfiguration) obj;
        return useAdaptersForContainers == dowCfg.getUseAdaptersForContainers()
                && useAdaptersForNonListCollections == dowCfg.getUseAdaptersForNonListCollections();
    }

}
