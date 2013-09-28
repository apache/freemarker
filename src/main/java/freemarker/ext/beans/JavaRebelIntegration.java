package freemarker.ext.beans;

import java.lang.ref.WeakReference;

import org.zeroturnaround.javarebel.ClassEventListener;
import org.zeroturnaround.javarebel.ReloaderFactory;

/**
 * @author Attila Szegedi
 */
class JavaRebelIntegration
{
    static void testAvailability() {
        ReloaderFactory.getInstance();
    }
    
    /**
     * Adds a JavaRebel class reloading listener for a that will invalidate 
     * cached information for that class in the specified BeansWrapper when the
     * class is reloaded. The beans wrapper is weakly referenced and the 
     * listener is unregistered if the wrapper is garbage collected.
     * @param w the beans wrapper to register.
     */
    static void register(ClassIntrospector w) {
        ReloaderFactory.getInstance().addClassReloadListener(
                new ClassIntrospectorCacheInvalidator(w));
    }
    
    private static class ClassIntrospectorCacheInvalidator 
    implements ClassEventListener
    {
        private final WeakReference ref;
        
        ClassIntrospectorCacheInvalidator(ClassIntrospector w) {
            ref = new WeakReference(w);
        }
        
        public void onClassEvent(int eventType, Class klass) {
            ClassIntrospector ci = (ClassIntrospector)ref.get();
            if(ci == null) {
                ReloaderFactory.getInstance().removeClassReloadListener(this);
            }
            else if(eventType == ClassEventListener.EVENT_RELOADED) {
                ci.remove(klass);
            }
        }
    }
}
