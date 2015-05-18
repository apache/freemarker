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
import java.util.Map;

import freemarker.core.Environment;
import freemarker.template.utility.DeepUnwrap;

/**
 * "directive" template language data type: used as user-defined directives 
 * (much like macros) in templates. They can do arbitrary actions, write arbitrary
 * text to the template output, and trigger rendering of their nested content for
 * any number of times.
 * 
 * <p>They are used in templates like {@code <@myDirective foo=1 bar="wombat">...</@myDirective>} (or as
 * {@code <@myDirective foo=1 bar="wombat" />} - the nested content is optional).
 *
 * @since 2.3.11
 */
public interface TemplateDirectiveModel extends TemplateModel
{
    /**
     * Executes this user-defined directive; called by FreeMarker when the user-defined
     * directive is called in the template.
     *
     * @param env the current processing environment. Note that you can access
     * the output {@link java.io.Writer Writer} by {@link Environment#getOut()}.
     * @param params the parameters (if any) passed to the directive as a 
     * map of key/value pairs where the keys are {@link String}-s and the 
     * values are {@link TemplateModel} instances. This is never 
     * <code>null</code>. If you need to convert the template models to POJOs,
     * you can use the utility methods in the {@link DeepUnwrap} class.
     * @param loopVars an array that corresponds to the "loop variables", in
     * the order as they appear in the directive call. ("Loop variables" are out-parameters
     * that are available to the nested body of the directive; see in the Manual.)
     * You set the loop variables by writing this array. The length of the array gives the
     * number of loop-variables that the caller has specified.
     * Never <code>null</code>, but can be a zero-length array.
     * @param body an object that can be used to render the nested content (body) of
     * the directive call. If the directive call has no nested content (i.e., it's like
     * &lt;@myDirective /&gt; or &lt;@myDirective&gt;&lt;/@myDirective&gt;), then this will be
     * <code>null</code>.
     *
     * @throws TemplateException If any problem occurs that's not an {@link IOException} during writing the template
     *          output.
     * @throws IOException When writing the template output fails.
     */
   public void execute(Environment env, Map params, TemplateModel[] loopVars, 
            TemplateDirectiveBody body) throws TemplateException, IOException;
}