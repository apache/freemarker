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
package freemarker.core;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;

import freemarker.template.Configuration;
import freemarker.template.DefaultIterableAdapter;
import freemarker.template.DefaultNonListCollectionAdapter;
import freemarker.template.TemplateCollectionModelEx;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.utility.ObjectWrapperWithAPISupport;
import freemarker.test.TemplateTest;

public class SequenceBuiltInTest extends TemplateTest {

    @Test
    public void testWithCollection() throws TemplateException, IOException {
        ObjectWrapperWithAPISupport ow = (ObjectWrapperWithAPISupport) getConfiguration().getObjectWrapper();
        
        TemplateModel xs = DefaultIterableAdapter.adapt(ImmutableSet.of("a", "b"), ow);
        assertThat(xs, not(instanceOf(TemplateCollectionModelEx.class)));
        assertThat(xs, not(instanceOf(TemplateSequenceModel.class)));
        addToDataModel("xs", xs);

        try {
            assertOutput("${xs[1]}", "b");
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString("?sequence")); // Contains tip to use ?sequence
        }
        assertOutput("${xs?sequence[1]}", "b");
        
        try {
            assertOutput("${xs?size}", "2");
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString("?sequence")); // Contains tip to use ?sequence
        }
        assertOutput("${xs?sequence?size}", "2");
    }

    @Test
    public void testWithCollectionEx() throws TemplateException, IOException {
        ObjectWrapperWithAPISupport ow = (ObjectWrapperWithAPISupport) getConfiguration().getObjectWrapper();
        
        TemplateModel xs = DefaultNonListCollectionAdapter.adapt(ImmutableSet.of("a", "b"), ow);
        assertThat(xs, not(instanceOf(TemplateSequenceModel.class)));
        assertThat(xs, instanceOf(TemplateCollectionModelEx.class));
        addToDataModel("xs", xs);

        try {
            assertOutput("${xs[1]}", "b");
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString("?sequence")); // Contains tip to use ?sequence
        }
        assertOutput("${xs?sequence[1]}", "b");

        assertOutput("${xs?size}", "2"); // No need for ?sequence
    }

    @Test
    public void testWithSequence() throws TemplateException, IOException {
        assertOutput("${[11, 12]?sequence[1]}", "12");

        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_23);
        // As it returns the sequence as is, it works with an infinite sequence:
        assertOutput("${(11..)?sequence[1]}", "12");
    }

}
