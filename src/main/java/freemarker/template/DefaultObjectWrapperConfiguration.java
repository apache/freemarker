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

    protected DefaultObjectWrapperConfiguration(Version incompatibleImprovements) {
        super(DefaultObjectWrapper.normalizeIncompatibleImprovementsVersion(incompatibleImprovements), true);
        useAdaptersForContainers = getIncompatibleImprovements().intValue() >= _TemplateAPI.VERSION_INT_2_3_22;
    }

    public boolean getUseAdaptersForContainers() {
        return useAdaptersForContainers;
    }

    /** See {@link DefaultObjectWrapper#setUseAdaptersForContainers(boolean)}. */
    public void setUseAdaptersForContainers(boolean useAdaptersForContainers) {
        this.useAdaptersForContainers = useAdaptersForContainers;
    }

    public int hashCode() {
        return super.hashCode() * 31 + (useAdaptersForContainers ? 1231 : 1237);
    }

    public boolean equals(Object obj) {
        if (!super.equals(obj)) return false;
        return useAdaptersForContainers == ((DefaultObjectWrapperConfiguration) obj).getUseAdaptersForContainers();
    }

}
