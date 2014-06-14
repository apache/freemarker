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
import java.util.Map;

import freemarker.template.utility.DeepUnwrap;

/**
 * "transform" template language data type: user-defined directives 
 * (much like macros) specialized on filtering output; you should rather use the newer {@link TemplateDirectiveModel}
 * instead. This certainly will be deprecated in FreeMarker 2.4.
 */
public interface TemplateTransformModel extends TemplateModel {

     /**
      * Returns a writer that will be used by the engine to feed the
      * transformation input to the transform. Each call to this method
      * must return a new instance of the writer so that the transformation
      * is thread-safe.
      * @param out the character stream to which to write the transformed output
      * @param args the arguments (if any) passed to the transformation as a 
      * map of key/value pairs where the keys are strings and the arguments are
      * TemplateModel instances. This is never null. If you need to convert the
      * template models to POJOs, you can use the utility methods in the 
      * {@link DeepUnwrap} class.
      * @return a writer to which the engine will feed the transformation 
      * input, or null if the transform does not support nested content (body).
      * The returned writer can implement the {@link TransformControl}
      * interface if it needs advanced control over the evaluation of the 
      * transformation body.
      */
     Writer getWriter(Writer out, Map args) 
         throws TemplateModelException, IOException;
}
