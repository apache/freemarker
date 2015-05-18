/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.template;

import java.util.Collection;

/**
 * <b>Experimental - subject to change:</b> "extended collection" template language data type: Adds size/emptiness
 * querybility and "contains" test to {@link TemplateCollectionModel}. The added extra operations is provided by all
 * Java {@link Collection}-s, and this interface was added to make that accessible for templates too.
 * 
 * <p>
 * <b>Experimental status warning:</b> This interface is subject to change on non-backward compatible ways, hence, it
 * shouldn't be implemented outside FreeMarker yet.
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
     * Tells if a given value occurs in the collection, accodring the rules of the wrapped collection. As of 2.3.22,
     * this interface is not yet utilized by FTL, and certainly it won't be earlier than 2.4.0. The usefulness of this
     * method is questionable, as the equality rules of Java differs from that of FTL, hence, calling this won't be
     * equivalent with {@code ?seq_contains(e)}.
     */
    boolean contains(TemplateModel item) throws TemplateModelException;

}
