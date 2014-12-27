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

/**
 * "sequence" template language data type; an object that contains other objects accessible through an integer 0-based
 * index.
 * 
 * <p>
 * Used in templates like: {@code mySeq[index]}, {@code <#list mySeq as i>...</#list>}, {@code mySeq?size}, etc.
 * 
 * @see TemplateCollectionModel
 */
public interface TemplateSequenceModel extends TemplateModel {

    /**
     * Retrieves the i-th template model in this sequence.
     * 
     * @return the item at the specified index, or <code>null</code> if the index is out of bounds. Note that a
     *         <code>null</code> value is interpreted by FreeMarker as "variable does not exist", and accessing a
     *         missing variables is usually considered as an error in the FreeMarker Template Language, so the usage of
     *         a bad index will not remain hidden, unless the default value for that case was also specified in the
     *         template.
     */
    TemplateModel get(int index) throws TemplateModelException;

    /**
     * @return the number of items in the list.
     */
    int size() throws TemplateModelException;
}
