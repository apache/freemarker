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

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;

/**
 * Testcase to see how FreeMarker deals with multiple Template models.
 */
public class MultiModel5 implements TemplateSequenceModel, TemplateHashModel {

    private LegacyList  m_cList = new LegacyList();

    /** Creates new MultiModel5 */
    public MultiModel5() {
        m_cList.add( new SimpleScalar( "Dummy to make list non-empty" ));
    }

    /**
     * @return the specified index in the list
     */
    public TemplateModel get(int i) throws TemplateModelException {
        return m_cList.get( i );
    }

    /**
     * @return true if this object is empty.
     */
    public boolean isEmpty() {
        return false;
    }

    public int size() {
        return m_cList.size();
    }

    /**
     * Gets a <tt>TemplateModel</tt> from the hash.
     *
     * @param key the name by which the <tt>TemplateModel</tt>
     * is identified in the template.
     * @return the <tt>TemplateModel</tt> referred to by the key,
     * or null if not found.
     */
    public TemplateModel get(String key) {
        if( key.equals( "empty" )) {
            return new SimpleScalar( "Dummy hash value, for test purposes." );
        } else {
            return null;
        }
    }

}
