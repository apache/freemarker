package freemarker.ext.beans;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import freemarker.core._ConcurrentMapFactory;
import freemarker.ext.util.ModelCache;
import freemarker.ext.util.ModelFactory;
import freemarker.template.TemplateModel;

public class BeansModelCache extends ModelCache
{
    private final Map classToFactory = _ConcurrentMapFactory.newMaybeConcurrentHashMap();
    private final boolean classToFactoryIsConcurrent
            = _ConcurrentMapFactory.isConcurrent(classToFactory);
    private final Set mappedClassNames = new HashSet();

    private final BeansWrapper wrapper;
    
    BeansModelCache(BeansWrapper wrapper) {
        this.wrapper = wrapper;
    }
    
    protected boolean isCacheable(Object object) {
        return object.getClass() != Boolean.class; 
    }
    
    protected TemplateModel create(Object object) {
        Class clazz = object.getClass();
        
        ModelFactory factory = null;

        if (classToFactoryIsConcurrent) {
            factory = (ModelFactory) classToFactory.get(clazz);
        }
        
        if (factory == null) {
            synchronized(classToFactory) {
                factory = (ModelFactory)classToFactory.get(clazz);
                if(factory == null) {
                    String className = clazz.getName();
                    // clear mappings when class reloading is detected
                    if(!mappedClassNames.add(className)) {
                        classToFactory.clear();
                        mappedClassNames.clear();
                        mappedClassNames.add(className);
                    }
                    factory = wrapper.getModelFactory(clazz);
                    classToFactory.put(clazz, factory);
                }
            }
        }
        
        return factory.create(object, wrapper);
    }
}
