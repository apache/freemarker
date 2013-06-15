package freemarker.ext.beans;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */ 
public class _BeansAPI {

    private _BeansAPI() { }
    
    public static String getAsClassicCompatibleString(BeanModel bm) {
        return bm.getAsClassicCompatibleString();
    }
    
}
