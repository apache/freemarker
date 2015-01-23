package freemarker.cache;

import freemarker.template.MalformedTemplateNameException;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */ 
public class _CacheAPI {

    public _CacheAPI() {
        // TODO Auto-generated constructor stub
    }
    
    public static String toAbsoluteName(TemplateNameFormat templateNameFormat, String baseName, String targetName)
            throws MalformedTemplateNameException {
        return templateNameFormat.toAbsoluteName(baseName, targetName);
    }

}
