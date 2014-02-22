package freemarker.ext.beans;

import freemarker.template.ObjectWrapper;

/**
 * Marker interface useful when used together with {@like MethodAppearanceFineTuner} and such customizer objects, to
 * indicate that it <b>doesn't contain reference to the {@link ObjectWrapper}</b> (so beware with non-static inner
 * classes) and can be and should be used in call introspection cache keys. This also implies that you won't
 * create many instances of the class, rather just reuse the same (or same few) instances over and over. Furhtermore,
 * the instances must be thread-safe. The typically pattern in which this instance should be used is like this
 * (hence the name):
 * 
 * <pre>static class MyMethodAppearanceFineTuner implements MethodAppearanceFineTuner, SingletonCustomizer {
 *      
 *    // This is the singleton:
 *    static final MyMethodAppearanceFineTuner INSTANCE = new MyMethodAppearanceFineTuner();
 *     
 *    // Private, so it can't be constructed from outside this class.
 *    private MyMethodAppearanceFineTuner() { }
 *
 *    @<!-- -->Override
 *    public void fineTuneMethodAppearance(...) {
 *       // Do something here, only using the parameters and maybe some other singletons. 
 *       ...
 *    }
 *     
 * }</pre>
 *
 * @since 2.3.21
 */
public interface SingletonCustomizer {

}
