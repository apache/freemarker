package freemarker.ext.beans;

/**
 * Exposes the Java API (and properties) of an object.
 * 
 * <p>
 * Notes:
 * <ul>
 * <li>The exposing level is inherited from the {@link BeansWrapper}</li>
 * <li>But methods will always shadow properties and fields with identical name, regardless of {@link BeansWrapper}
 * settings</li>
 * </ul>
 * 
 * @since 2.3.22
 */
final class APIModel extends BeanModel {

    APIModel(Object object, BeansWrapper wrapper) {
        super(object, wrapper, false);
    }

    protected boolean isMethodsShadowItems() {
        return true;
    }
    
}
