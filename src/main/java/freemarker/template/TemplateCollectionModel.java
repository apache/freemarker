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
 * "collection" template language data type: a collection of values that can be enumerated, repeatedly (not just once).
 * This is very similar to {@link TemplateSequenceModel}, but it doesn't support indexed (random) access and
 * its size can't be queried.
 *  
 * <p>They are mostly used in template languages like {@code <#list myCollection as i>...</#list>}.  
 *
 * @author Attila Szegedi, szegedia at users dot sourceforge dot net
 */
public interface TemplateCollectionModel extends TemplateModel {

    /**
     * Retrieves a template model iterator that is used to iterate over
     * the elements in this collection.
     */
    public TemplateModelIterator iterator() throws TemplateModelException;
}
