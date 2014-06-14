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

import java.io.IOException;
import java.io.Writer;

/**
 * Represents the nested content of a directive ({@link TemplateDirectiveModel}) invocation. An implementation of this 
 * class is passed to {@link TemplateDirectiveModel#execute(freemarker.core.Environment, 
 * java.util.Map, TemplateModel[], TemplateDirectiveBody)}. The implementation of the method is 
 * free to invoke it for any number of times, with any writer.
 *
 * @since 2.3.11
 */
public interface TemplateDirectiveBody
{
    /**
     * Renders the body of the directive body to the specified writer. The 
     * writer is not flushed after the rendering. If you pass the environment's
     * writer, there is no need to flush it. If you supply your own writer, you
     * are responsible to flush/close it when you're done with using it (which
     * might be after multiple renderings).
     * @param out the writer to write the output to.
     */
    public void render(Writer out) throws TemplateException, IOException;
}
