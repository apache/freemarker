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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.SimpleNumber;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateCollectionModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateSequenceModel;
import freemarker.test.TemplateTest;

/**
 * Tests built-ins that are support getting {@link LazilyGeneratedSequenceModel} as their LHO input.
 */
public class LazilyGeneratedSeqTargetSupportInBuiltinsTest extends TemplateTest {

    @Test
    public void sizeTest() throws Exception {
        assertOutput("${seq?size}",
                "[size]3");
        assertOutput("${collEx?size}",
                "[size]3");
        assertErrorContains("${coll?size}",
                "sequence", "extended collection");

        assertOutput("${seq?map(x -> x * 10)?size}",
                "[size]3");
        assertOutput("${collEx?map(x -> x * 10)?size}",
                "[size]3");

        assertOutput("${seq?filter(x -> x != 1)?size}",
                "[size][get 0][get 1][get 2]2");
        assertOutput("${collEx?filter(x -> x != 1)?size}",
                "[iterator][hasNext][next][hasNext][next][hasNext][next][hasNext]2");
    }

    @Test
    public void firstTest() throws Exception {
        assertOutput("${coll?first}",
                "[iterator][hasNext][next]1");
        assertOutput("${coll?filter(x -> x % 2 == 0)?first}",
                "[iterator][hasNext][next][hasNext][next]2");
    }

    @Test
    public void seqIndexOfTest() throws Exception {
        assertOutput("${coll?seqIndexOf(2)}",
                "[iterator][hasNext][next][hasNext][next]1");
        assertOutput("${coll?filter(x -> x % 2 == 0)?seqIndexOf(2)}",
                "[iterator][hasNext][next][hasNext][next]0");
    }

    @Test
    public void filterTest() throws Exception {
        assertOutput("${coll?filter(x -> x % 2 == 0)?filter(x -> true)?first}",
                "[iterator][hasNext][next][hasNext][next]2");
    }

    @Test
    public void listTest() throws Exception {
        // Note: #list has to fetch elements up to 4 to know if it?hasNext
        assertOutput("<#list collLong?filter(x -> x % 2 == 0) as it>${it} ${it?hasNext?c}<#break></#list>",
                "[iterator][hasNext][next][hasNext][next][hasNext][next][hasNext][next]2 true");
    }

    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration cfg = super.createConfiguration();
        cfg.setSharedVariable("seq", new MonitoredTemplateSequenceModel(1, 2, 3));
        cfg.setSharedVariable("coll", new MonitoredTemplateCollectionModel(1, 2, 3));
        cfg.setSharedVariable("collLong", new MonitoredTemplateCollectionModel(1, 2, 3, 4, 5, 6));
        cfg.setSharedVariable("collEx", new MonitoredTemplateCollectionModelEx(1, 2, 3));
        return cfg;
    }

    public static abstract class ListContainingTemplateModel {
        protected final List<Number> elements;

        public ListContainingTemplateModel(Number... elements) {
            this.elements = new ArrayList<Number>(Arrays.asList(elements));
        }

        public int size() {
            logCall("size");
            return elements.size();
        }

        protected void logCall(String callDesc) {
            try {
                Environment.getCurrentEnvironment().getOut().write("[" + callDesc + "]");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static class MonitoredTemplateSequenceModel extends ListContainingTemplateModel
            implements TemplateSequenceModel {
        public MonitoredTemplateSequenceModel(Number... elements) {
            super(elements);
        }

        public TemplateModel get(int index) throws TemplateModelException {
            logCall("get " + index);
            return new SimpleNumber(elements.get(index));
        }

    }

    public static class MonitoredTemplateCollectionModel extends ListContainingTemplateModel
            implements TemplateCollectionModel {
        public MonitoredTemplateCollectionModel(Number... elements) {
            super(elements);
        }

        public TemplateModelIterator iterator() throws TemplateModelException {
            logCall("iterator");
            final Iterator<Number> iterator = elements.iterator();
            return new TemplateModelIterator() {
                public TemplateModel next() throws TemplateModelException {
                    logCall("next");
                    return new SimpleNumber(iterator.next());
                }

                public boolean hasNext() throws TemplateModelException {
                    logCall("hasNext");
                    return iterator.hasNext();
                }
            };
        }
    }

    public static class MonitoredTemplateCollectionModelEx extends MonitoredTemplateCollectionModel
            implements TemplateCollectionModelEx {
        public MonitoredTemplateCollectionModelEx(Number... elements) {
            super(elements);
        }

        public boolean isEmpty() throws TemplateModelException {
            logCall("isEmpty");
            return elements.isEmpty();
        }
    }

}
