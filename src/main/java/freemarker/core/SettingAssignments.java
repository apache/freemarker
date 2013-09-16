package freemarker.core;

import java.util.Properties;

/**
 * Marker interface for classes that are used to submit configuration settings to a constructors with JavaBean
 * properties. Such objects meant to be used as the parameter of factory methods that return a read-only object that
 * thus can't be configured after it was constructed. Marking them is necessary so when FreeMarker is set up based on
 * {@link Properties}, the "eval:" expressions will know that the factory method parameter can be used instead
 * of setting the JavaBean properties of the object post-construction.  
 * 
 * @since 2.3.21
 */
public interface SettingAssignments {

}
