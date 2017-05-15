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

package org.apache.freemarker.core;

import static org.apache.freemarker.test.hamcerst.Matchers.*;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import javax.annotation.PostConstruct;

import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.impl.DefaultArrayAdapter;
import org.apache.freemarker.core.model.impl.DefaultListAdapter;
import org.apache.freemarker.core.model.impl.DefaultMapAdapter;
import org.apache.freemarker.core.model.impl.DefaultNonListCollectionAdapter;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapperTest.TestBean;
import org.apache.freemarker.core.model.impl.RestrictedObjectWrapper;
import org.apache.freemarker.core.model.impl.SimpleDate;
import org.apache.freemarker.core.model.impl.SimpleNumber;
import org.apache.freemarker.core.model.impl.SimpleScalar;
import org.junit.Test;

public class RestrictedObjectWrapperTest {

    @Test
    public void testBasics() throws TemplateModelException {
        PostConstruct.class.toString();
        RestrictedObjectWrapper ow = new RestrictedObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
        testCustomizationCommonPart(ow);
        assertTrue(ow.wrap(Collections.emptyMap()) instanceof DefaultMapAdapter);
        assertTrue(ow.wrap(Collections.emptyList()) instanceof DefaultListAdapter);
        assertTrue(ow.wrap(new boolean[] { }) instanceof DefaultArrayAdapter);
        assertTrue(ow.wrap(new HashSet()) instanceof DefaultNonListCollectionAdapter);
    }

    @SuppressWarnings("boxing")
    private void testCustomizationCommonPart(RestrictedObjectWrapper ow) throws TemplateModelException {
        assertTrue(ow.wrap("x") instanceof SimpleScalar);
        assertTrue(ow.wrap(1.5) instanceof SimpleNumber);
        assertTrue(ow.wrap(new Date()) instanceof SimpleDate);
        assertEquals(TemplateBooleanModel.TRUE, ow.wrap(true));
        
        try {
            ow.wrap(new TestBean());
            fail();
        } catch (TemplateModelException e) {
            assertThat(e.getMessage(), containsStringIgnoringCase("type"));
        }
    }
    
}
