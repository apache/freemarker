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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    
    
    /**
     * test sequence concatenation before and after 2.3.33 
     * in 2.3.33 a performance improvement was added
     * see ConcatenatedSequence vs. ConcatenatedSequenceFixedSize
     * in {@link AddConcatExpression}
     * But since the optimization is not BC
     * we test both via setIncompatibleImprovements
     * 
     * @throws TemplateException
     * @throws IOException
     */
    @Test
    public void testSequenceConcatenation() throws TemplateException, IOException {
        
       int maxElements = 200;
        String ftl = "<#assign s = []><#list 1.."+ maxElements + " as i><#assign s = s + ['foo' + i]></#list>${s?join(',')}";
        // e.g. foo1,foo2,foo3,etc.
        String expected = IntStream.rangeClosed(1, maxElements).mapToObj(i -> "foo" + i).collect(Collectors.joining(","));
        
        assertOutput(ftl, expected);

        // small performance comparison 
        int numIterations = 100;
        
        // Before 2.3.33
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numIterations ; i++) {
            assertOutput(ftl, expected);
        }
        long endTime = System.currentTimeMillis();
        double duration1 = (endTime - startTime) / 1000.0;
        System.out.println(getConfiguration().getIncompatibleImprovements() + " : Time taken: " + duration1 + " s");
        
        // now test after 2.3.33 which has a performance improvement which should be just a fraction
        // of the duration1 
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_33);
        assertOutput(ftl, expected);
        
        
        long startTime2 = System.currentTimeMillis();
        for (int i = 0; i < numIterations ; i++) {
            assertOutput(ftl, expected);
        }
        long  endTime2 = System.currentTimeMillis();
        
        double duration2 = (endTime2 - startTime2) / 1000.0;
        System.out.println(getConfiguration().getIncompatibleImprovements() + " : Time taken: " + duration2 + " s");
        
        
        // check that new version is at least twice as fast
        assertTrue("Duration2 should be twice as fast, but was: 1. " + duration1 + " vs. 2. " + duration2,
                duration2 < duration1 / 2);
        
    }

}
