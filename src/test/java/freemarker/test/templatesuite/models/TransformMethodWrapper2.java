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

package freemarker.test.templatesuite.models;

import java.util.Iterator;
import java.util.List;

import freemarker.template.TemplateMethodModel;

/**
 * Another test of the interaction between MethodModels and TransformModels.
 */
public class TransformMethodWrapper2 implements TemplateMethodModel {

    /**
     * Executes a method call.
     *
     * @param arguments a <tt>List</tt> of <tt>String</tt> objects containing
     * the values of the arguments passed to the method.
     * @return the <tt>TemplateModel</tt> produced by the method, or null.
     */
    public Object exec(List arguments) {
        TransformModel1 cTransformer = new TransformModel1();
        Iterator    iArgument = arguments.iterator();

        // Sets up properties of the Transform model based on the arguments
        // passed into this method

        while( iArgument.hasNext() ) {
            String  aArgument = (String)iArgument.next();

            if( aArgument.equals( "quote" )) {
                cTransformer.setQuotes( true );
            } else if( aArgument.equals( "tag" )) {
                cTransformer.setTags( true );
            } else if( aArgument.equals( "ampersand" )) {
                cTransformer.setAmpersands( true );
            } else {
                cTransformer.setComment( aArgument );
            }
        }

        // Now return the transform class.
        return cTransformer;
    }
}
