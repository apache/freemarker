package freemarker.template;

import freemarker.ext.util.WrapperTemplateModel;

/**
 * <b>Experimental:</b> Adds functionality to {@link ObjectWrapper} that creates a plain Java object from a
 * {@link TemplateModel}. This is usually implemented by {@link ObjectWrapper}-s and reverses
 * {@link ObjectWrapper#wrap(Object)}. However, an implementation of this interface should make a reasonable effort to
 * "unwrap" {@link TemplateModel}-s that wasn't the result of object wrapping (such as those created directly in FTL),
 * or which was created by another {@link ObjectWrapper}. The author of an {@link ObjectWrapperAndUnwrapper} should be
 * aware of the {@link TemplateModelAdapter} and {@link WrapperTemplateModel} interfaces, which should be used for
 * unwrapping if the {@link TemplateModel} implements them.
 * 
 * <p>
 * <b>Experimental status warning:</b> This interface is subject to change (probably until 2.4.0).
 * 
 * @since 2.3.22
 */
public interface ObjectWrapperAndUnwrapper extends ObjectWrapper {

    /**
     * Unwraps the given {@link TemplateModel} to a plain Java object. See class description for more.
     * 
     * @param hintClass
     *            The caller uses this to indicate what class the result should be instance of. Can't be
     *            {@code null}; if the caller can't give a hint, it should use {@code Object.class}.
     */
    Object unwrap(TemplateModel tm, Class hintClass) throws TemplateModelException;

}
