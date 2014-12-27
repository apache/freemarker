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
 * "collection" template language data type: a collection of values that can be enumerated, but can't be or not meant to
 * be accessed by index or key. As such, this is not a super-interface of {@link TemplateSequenceModel}, and
 * implementations of that interface needn't also implement this interface just because they can. They should though, if
 * enumeration with this interface is significantly faster than enumeration by index. The {@code #list} directive will
 * enumerate using this interface if it's available.
 * 
 * <p>
 * The enumeration should be repeatable if that's possible with reasonable effort, otherwise a second enumeration
 * attempt is allowed to throw an {@link TemplateModelException}. Generally, the interface user Java code need not
 * handle that kind of exception, as in practice only the template author can handle it, by not listing such collections
 * twice.
 * 
 * <p>
 * Note that to wrap Java's {@link Collection}, you should implement {@link TemplateCollectionModelEx}, not just this
 * interface.
 */
public interface TemplateCollectionModel extends TemplateModel {

    /**
     * Retrieves a template model iterator that is used to iterate over the elements in this collection.
     */
    public TemplateModelIterator iterator() throws TemplateModelException;

}
