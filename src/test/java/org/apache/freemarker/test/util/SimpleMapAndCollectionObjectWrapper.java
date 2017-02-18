package org.apache.freemarker.test.util;

import java.util.Collection;
import java.util.Map;

import org.apache.freemarker.core.Version;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.core.model.impl.SimpleHash;
import org.apache.freemarker.core.model.impl.SimpleSequence;

/**
 * Forces using "simple" models for {@link Map}-s, {@link Collection}-s and arrays. This is mostly useful for template
 * test cases that wish to test with these models, but otherwise need to able to wrap beans and such. 
 */
public class SimpleMapAndCollectionObjectWrapper extends DefaultObjectWrapper {

    public SimpleMapAndCollectionObjectWrapper(Version incompatibleImprovements) {
        super(incompatibleImprovements);
    }

    @Override
    public TemplateModel wrap(Object obj) throws TemplateModelException {
        if (obj == null) {
            return super.wrap(null);
        }        
        if (obj.getClass().isArray()) {
            obj = convertArray(obj);
        }
        if (obj instanceof Collection) {
            return new SimpleSequence((Collection<?>) obj, this);
        }
        if (obj instanceof Map) {
            return new SimpleHash((Map<?, ?>) obj, this);
        }
        
        return super.wrap(obj);
    }

}
