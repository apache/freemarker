package freemarker.template;

import java.util.Collection;

/**
 * <b>Experimental:</b> "extended collection" template language data type: Adds size/emptiness querybility and
 * "contains" test to {@link TemplateCollectionModel}. The added extra operations is provided by all Java
 * {@link Collection}-s, and this interface was added to make that accessible for templates too.
 * 
 * <p>
 * <b>Experimental status warning:</b> This interface is subject to change (probably until 2.4.0). There's little chance
 * that changes will be needed though.
 * 
 * @since 2.3.22
 */
public interface TemplateCollectionModelEx extends TemplateCollectionModel {

    /**
     * Returns the number items in this collection, or {@link Integer#MAX_VALUE}, if there are more than
     * {@link Integer#MAX_VALUE} items.
     */
    int size() throws TemplateModelException;

    /**
     * Returns if the collection contains any elements. This differs from {@code size() != 0} only in that the exact
     * number of items need not be calculated.
     */
    boolean isEmpty() throws TemplateModelException;

    /**
     * Tells if a given value occurs in the collection. As of 2.3.22, this interface is not yet utilized by FTL, and
     * certainly it won't be until 2.4.0. 
     */
    boolean contains(TemplateModel item) throws TemplateModelException;

}
