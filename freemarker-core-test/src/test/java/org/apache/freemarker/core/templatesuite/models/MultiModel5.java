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

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.impl.SimpleString;
import org.apache.freemarker.core.model.impl.SimpleSequence;

/**
 * Testcase to see how FreeMarker deals with multiple Template models.
 */
public class MultiModel5 implements TemplateSequenceModel, TemplateHashModel {

    private final SimpleSequence m_cList;

    public MultiModel5(ObjectWrapper ow) {
        this.m_cList = new SimpleSequence(ow);
        m_cList.add( new SimpleString( "Dummy to make list non-empty" ));
    }

    @Override
    public TemplateModel get(int i) throws TemplateException {
        return m_cList.get( i );
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int size() {
        return m_cList.size();
    }

    @Override
    public TemplateModel get(String key) {
        if ( key.equals( "empty" )) {
            return new SimpleString( "Dummy hash value, for test purposes." );
        } else {
            return null;
        }
    }

}
