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

package freemarker.cache;

import freemarker.template.Configuration;

/**
 * Interface that can be implemented by template loaders that maintain some 
 * sort of internal state (i.e. caches of earlier lookups for performance 
 * optimization purposes etc.) and support resetting of their state. 
 */
public interface StatefulTemplateLoader extends TemplateLoader
{
    /**
     * Invoked by {@link Configuration#clearTemplateCache()} to instruct this
     * template loader to throw away its current state and start afresh. 
     */
    public void resetState();
}
