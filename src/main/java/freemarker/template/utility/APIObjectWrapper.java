package freemarker.template.utility;

import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * <b>Experimental - subject to change:</b> Implemented by {@link ObjectWrapper}-s to help {@link TemplateModel}-s to
 * implement the {@code someValue?api} operation.
 * 
 * <p>
 * <b>Experimental status warning:</b> This interface is subject to change on non-backward compatible ways, hence, it
 * shouldn't be implemented outside FreeMarker yet.
 * 
 * @since 2.3.22
 */
public interface APIObjectWrapper extends ObjectWrapper {

    /**
     * Wraps an object to a {@link TemplateModel} that exposes the object's API.
     * 
     * @param obj
     *            The object for which the API model has to be returned. Shouldn't be {@code null}.
     * 
     * @return The {@link TemplateModel} through which the API of the object can be accessed. Can't be {@code null}.
     * 
     * @since 2.3.22
     */
    TemplateHashModel wrapAsAPI(Object obj) throws TemplateModelException;

}
