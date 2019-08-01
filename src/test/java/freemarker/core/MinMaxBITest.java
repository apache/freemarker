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

import java.sql.Time;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import freemarker.template.Configuration;
import freemarker.template.DefaultIterableAdapter;
import freemarker.template.utility.DateUtil;
import freemarker.template.utility.ObjectWrapperWithAPISupport;
import freemarker.test.TemplateTest;

public class MinMaxBITest extends TemplateTest {

    @Test
    public void basicsTest() throws Exception {
        getConfiguration().setTimeZone(DateUtil.UTC);
        getConfiguration().setTimeFormat("HH:mm:ss");
        
        ObjectWrapperWithAPISupport ow = (ObjectWrapperWithAPISupport) getConfiguration().getObjectWrapper();
        for (boolean exposeAsSeq : new boolean[] { true, false }) { // Expose xs as SequenceTM or as CollectionTM
            for (InputMinMax testParams : ImmutableList.of(
                    // Test parameters:             List (xs), Expected result for `?min`, For `?max`
                    new InputMinMax(ImmutableList.of(1, 2, 3), "1", "3"),
                    new InputMinMax(ImmutableList.of(3, 2, 1), "1", "3"),
                    new InputMinMax(ImmutableList.of(1, 3, 2), "1", "3"),
                    new InputMinMax(ImmutableList.of(2, 1, 3), "1", "3"),
                    new InputMinMax(ImmutableList.of(2), "2", "2"),
                    new InputMinMax(Collections.emptyList(), "-", "-"),
                    new InputMinMax(ImmutableList.of(1.5, -0.5, 1L, 2.25), "-0.5", "2.25"),
                    new InputMinMax(ImmutableList.of(Double.NEGATIVE_INFINITY, 1, Double.POSITIVE_INFINITY),
                            "-\u221E", "\u221E"), // \u221E = ∞
                    new InputMinMax(Arrays.asList(new Object[] { null, 1, null, 2, null }), "1", "2"),
                    new InputMinMax(Arrays.asList(new Object[] { null, null, null }), "-", "-"),
                    new InputMinMax(ImmutableList.of(new Time(2000), new Time(3000), new Time(1000)),
                            "00:00:01", "00:00:03")
                    )) {
                addToDataModel("xs",
                        exposeAsSeq ? testParams.input : DefaultIterableAdapter.adapt(testParams.input, ow));
                assertOutput("${xs?min!'-'}", testParams.minExpected);
                assertOutput("${xs?max!'-'}", testParams.maxExpected);
            }
        }
    }
    
    private class InputMinMax {
        private final List<?> input;
        private final String minExpected;
        private final String maxExpected;
        
        public InputMinMax(List<?> input, String minExpected, String maxExpected) {
            this.input = input;
            this.minExpected = minExpected;
            this.maxExpected = maxExpected;
        }
    }

    @Test
    public void comparisonErrorTest() {
        assertErrorContains("${['a', 'x']?min}", "less-than", "string");
        assertErrorContains("${[0, true]?min}", "number", "boolean");
    }

    @Test
    public void rightUnboundedNumericalRangeTest() throws Exception {
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_21); // So (1..) is listable at all
        assertErrorContains("${(1..)?min}", "right-unbounded", "infinite");
        assertErrorContains("${(1..)?max}", "right-unbounded", "infinite");
        assertOutput("${(1..2)?min}", "1");
        assertOutput("${(1..2)?max}", "2");
    }
    
}
