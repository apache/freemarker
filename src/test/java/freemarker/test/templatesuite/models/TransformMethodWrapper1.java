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

import java.util.List;

import freemarker.template.TemplateMethodModel;
import freemarker.template.utility.HtmlEscape;
import freemarker.template.utility.XmlEscape;

/**
 * Simple test of the interaction between MethodModels and TransformModels.
 */
public class TransformMethodWrapper1 extends Object implements TemplateMethodModel {

    /**
     * Executes a method call.
     *
     * @param arguments a <tt>List</tt> of <tt>String</tt> objects containing
     * the values of the arguments passed to the method.
     * @return the <tt>TemplateModel</tt> produced by the method, or null.
     */
    public Object exec(List arguments) {

        if(( arguments.size() > 0 ) && ( arguments.get( 0 ).toString().equals( "xml" ))) {
            return new XmlEscape();
        } else {
            return new HtmlEscape();
        }
    }
}
