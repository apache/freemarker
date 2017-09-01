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

package org.apache.freemarker.core.templatesuite.models;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.core.model.impl.SimpleHash;
import org.apache.freemarker.core.model.impl.SimpleString;
import org.apache.freemarker.core.model.impl.SimpleSequence;

/**
 * Testcase to see how FreeMarker deals with multiple Template models.
 */
public class MultiModel1 implements TemplateHashModel,
        TemplateSequenceModel, TemplateStringModel {

    private ObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();

    private TemplateModel m_cSubModel = new MultiModel2();
    private TemplateModel m_cListHashModel1 = new MultiModel4(ow);
    private TemplateModel m_cListHashModel2 = new MultiModel5(ow);
    private TemplateSequenceModel m_cListModel = new SimpleSequence(ow);
    private TemplateHashModel m_cHashModel = new SimpleHash(ow);

    public MultiModel1() {
        for ( int i = 0; i < 10; i++ ) {
            ((SimpleSequence) m_cListModel).add( "Model1 value: " + Integer.toString( i ));
        }
        ((SimpleSequence) m_cListModel).add( new MultiModel3() );
        ((SimpleHash) m_cHashModel).put( "nested", new MultiModel3() );
    }

    @Override
    public TemplateModel get(String key) {
        if ( key.equals( "model2" )) {
            return m_cSubModel;
        } else if ( key.equals( "modellist" )) {
            return m_cListModel;
        } else if ( key.equals( "selftest" )) {
            return new SimpleString( "Selftest of a hash from MultiModel1" );
        } else if ( key.equals( "one" )) {
            return m_cListHashModel1;
        } else if ( key.equals( "two" )) {
            return m_cListHashModel2;
        } else if ( key.equals( "size" )) {
            return new SimpleString( "Nasty!" );
        } else if ( key.equals( "nesting1" )) {
            return m_cHashModel;
        } else {
            return null;
        }
    }

    @Override
    public boolean isEmptyHash() {
        return false;
    }

    @Override
    public TemplateModel get(int i) throws TemplateException {
        return m_cListModel.get( i );
    }

    @Override
    public String getAsString() {
        return "MultiModel1 as a string!";
    }

    @Override
    public int getCollectionSize() throws TemplateException {
        return m_cListModel.getCollectionSize();
    }

    @Override
    public boolean isEmptyCollection() throws TemplateException {
        return m_cListModel.isEmptyCollection();
    }

    @Override
    public TemplateModelIterator iterator() throws TemplateException {
        return m_cListModel.iterator();
    }

}
