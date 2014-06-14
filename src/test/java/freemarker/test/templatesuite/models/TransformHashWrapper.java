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

import freemarker.template.SimpleHash;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.utility.HtmlEscape;
import freemarker.template.utility.StandardCompress;

/**
 * Part of the TestTransform testcase suite.
 */
public class TransformHashWrapper implements TemplateHashModel,
        TemplateScalarModel {

    private SimpleHash m_cHashModel = new SimpleHash();

    /** Creates new TransformHashWrapper */
    public TransformHashWrapper() {
        m_cHashModel.put( "htmlEscape", new HtmlEscape() );
        m_cHashModel.put( "compress", new StandardCompress() );
        m_cHashModel.put( "escape", new TransformMethodWrapper1() );
        m_cHashModel.put( "special", new TransformMethodWrapper2() );
    }

    /**
     * Gets a <tt>TemplateModel</tt> from the hash.
     *
     * @param key the name by which the <tt>TemplateModel</tt>
     * is identified in the template.
     * @return the <tt>TemplateModel</tt> referred to by the key,
     * or null if not found.
     */
    public TemplateModel get(String key) throws TemplateModelException {
        return m_cHashModel.get( key );
    }

    /**
     * @return true if this object is empty.
     */
    public boolean isEmpty() {
        return false;
    }

    /**
     * Returns the scalar's value as a String.
     * @return the String value of this scalar.
     */
    public String getAsString() {
        return "Utility transformations";
    }
}
