package freemarker.template;

import freemarker.template.utility.APIObjectWrapper;

/**
 * <b>Experimental - subject to change:</b> A {@link TemplateModel} on which the {@code ?api} operation can be applied.
 * 
 * <p>
 * <b>Experimental status warning:</b> This interface is subject to change on non-backward compatible ways, hence, it
 * shouldn't be implemented outside FreeMarker yet.
 * 
 * @since 2.3.22
 */
public interface TemplateModelWithAPISupport extends TemplateModel {

    /**
     * Returns the model that exposes the (Java) API of the value. This is usually implemented by delegating to
     * {@link APIObjectWrapper#wrapAsAPI(Object)}.
     */
    TemplateModel getAPI() throws TemplateModelException;

}
