package freemarker.ext.beans;

import java.util.Set;

import freemarker.template.TemplateCollectionModel;

/**
 * @author Attila Szegedi
 * @version $Id: SetAdapter.java,v 1.1.2.1 2006/12/22 13:47:36 szegedia Exp $
 */
class SetAdapter extends CollectionAdapter implements Set {
    SetAdapter(TemplateCollectionModel model, BeansWrapper wrapper) {
        super(model, wrapper);
    }
}
