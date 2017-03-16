/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.test.templatesuite.models;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.core.model.impl.SimpleHash;
import org.apache.freemarker.core.model.impl.SimpleScalar;
import org.apache.freemarker.core.model.impl.SimpleSequence;

/**
 * Testcase to see how FreeMarker deals with multiple Template models.
 */
public class MultiModel1 implements TemplateHashModel,
        TemplateSequenceModel, TemplateScalarModel {

    private ObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();

    private TemplateModel m_cSubModel = new MultiModel2();
    private TemplateModel m_cListHashModel1 = new MultiModel4(ow);
    private TemplateModel m_cListHashModel2 = new MultiModel5(ow);
    private TemplateSequenceModel m_cListModel = new SimpleSequence(ow);
    private TemplateHashModel m_cHashModel = new SimpleHash(ow);

    /** Creates new MultiModel1 */
    public MultiModel1() {
        for ( int i = 0; i < 10; i++ ) {
            ((SimpleSequence) m_cListModel).add( "Model1 value: " + Integer.toString( i ));
        }
        ((SimpleSequence) m_cListModel).add( new MultiModel3() );
        ((SimpleHash) m_cHashModel).put( "nested", new MultiModel3() );
    }

    /**
     * Gets a <tt>TemplateModel</tt> from the hash.
     *
     * @param key the name by which the <tt>TemplateModel</tt>
     * is identified in the template.
     * @return the <tt>TemplateModel</tt> referred to by the key,
     * or null if not found.
     */
    @Override
    public TemplateModel get(String key) {
        if ( key.equals( "model2" )) {
            return m_cSubModel;
        } else if ( key.equals( "modellist" )) {
            return m_cListModel;
        } else if ( key.equals( "selftest" )) {
            return new SimpleScalar( "Selftest of a hash from MultiModel1" );
        } else if ( key.equals( "one" )) {
            return m_cListHashModel1;
        } else if ( key.equals( "two" )) {
            return m_cListHashModel2;
        } else if ( key.equals( "size" )) {
            return new SimpleScalar( "Nasty!" );
        } else if ( key.equals( "nesting1" )) {
            return m_cHashModel;
        } else {
            return null;
        }
    }

    /**
     * @return true if this object is empty.
     */
    @Override
    public boolean isEmpty() {
        return false;
    }

    /**
     * @return the specified index in the list
     */
    @Override
    public TemplateModel get(int i) throws TemplateModelException {
        return m_cListModel.get( i );
    }

    /**
     * Returns the scalar's value as a String.
     *
     * @return the String value of this scalar.
     */
    @Override
    public String getAsString() {
        return "MultiModel1 as a string!";
    }

    @Override
    public int size() throws TemplateModelException {
        return m_cListModel.size();
    }
}
