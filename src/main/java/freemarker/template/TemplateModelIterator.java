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
 * Used to iterate over a set of template models <em>once</em>; usually returned from
 * {@link TemplateCollectionModel#iterator()}. Note that it's not a {@link TemplateModel}.
 */
public interface TemplateModelIterator {

    /**
     * Returns the next model.
     * @throws TemplateModelException if the next model can not be retrieved
     *   (i.e. because the iterator is exhausted).
     */
    TemplateModel next() throws TemplateModelException;

    /**
     * @return whether there are any more items to iterate over.
     */
    boolean hasNext() throws TemplateModelException;
}
