package freemarker.template.utility;

import freemarker.template.ObjectWrapper;
import freemarker.template.ObjectWrapperAndUnwrapper;

/**
 * <b>Experimental - subject to change:</b> Union of the interfaces that a typical feature rich {@link ObjectWrapper} is
 * expected to implement.
 * 
 * <p>
 * <b>Experimental status warning:</b> This interface is subject to change on non-backward compatible ways, hence, it
 * shouldn't be implemented outside FreeMarker yet.
 * 
 * @since 2.3.22
 */
public interface RichObjectWrapper extends ObjectWrapperAndUnwrapper, APIObjectWrapper {

}
