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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.impl.SimpleCollection;
import org.apache.freemarker.core.model.impl.SimpleIterable;
import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class SequenceBuiltInTest extends TemplateTest {

    @Test
    public void testWithIterable() throws TemplateException, IOException {
        TemplateModel xs = new SimpleIterable(ImmutableList.of("a", "b"), getConfiguration().getObjectWrapper());
        assertThat(xs, not(instanceOf(TemplateCollectionModel.class)));
        addToDataModel("xs", xs);

        try {
            assertOutput("${xs[1]}", "b");
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString("?sequence")); // Contains tip to use ?sequence
        }
        assertOutput("${xs?sequence[1]}", "b");
    }

    @Test
    public void testWithCollection() throws TemplateException, IOException {
        TemplateModel xs = new SimpleCollection(ImmutableList.of("a", "b"), getConfiguration().getObjectWrapper());
        assertThat(xs, not(instanceOf(TemplateSequenceModel.class)));
        addToDataModel("xs", xs);

        try {
            assertOutput("${xs[1]}", "b");
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString("?sequence")); // Contains tip to use ?sequence
        }
        assertOutput("${xs?sequence[1]}", "b");
    }

    @Test
    public void testWithSequence() throws TemplateException, IOException {
        // As it returns the sequence as is, it works with an infinite sequence:
        assertOutput("${(11..)?sequence[1]}", "12");
    }

}
