package freemarker.template.utility;

/**
 * Used for the trivial cases of the factory pattern. Will have generic type argument as soon as we switch to Java 5.
 * 
 * @since 2.3.22
 */
public interface ObjectFactory/*<T>*/ {
    
    /*T*/ Object createObject() throws Exception;

}
