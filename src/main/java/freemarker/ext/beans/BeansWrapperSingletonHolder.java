package freemarker.ext.beans;

/** Hack to prevent creating the default ObjectWrapper before BeansWrapper is fully initialized. */
class BeansWrapperSingletonHolder {
    
    /**
     * Used in {@link BeansWrapper#getDefaultInstance()}.
     * 
     * @deprecated
     */
    static final BeansWrapper INSTANCE = new BeansWrapper();

}
