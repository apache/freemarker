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
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.SimpleNumber;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateCollectionModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateSequenceModel;
import freemarker.test.TemplateTest;

/**
 * Tests operators and built-ins that are support getting and/or returning {@link LazilyGeneratedCollectionModel}.
 * @see MapBiTest
 * @see FilterBiTest
 */
public class LazilyGeneratedCollectionTest extends TemplateTest {

    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration cfg = super.createConfiguration();
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_29);
        cfg.setBooleanFormat("c");
        cfg.setSharedVariable("seq", new MonitoredTemplateSequenceModel(1, 2, 3));
        cfg.setSharedVariable("seqLong", new MonitoredTemplateSequenceModel(1, 2, 3, 4, 5, 6));
        cfg.setSharedVariable("coll", new MonitoredTemplateCollectionModel(1, 2, 3));
        cfg.setSharedVariable("collLong", new MonitoredTemplateCollectionModel(1, 2, 3, 4, 5, 6));
        cfg.setSharedVariable("collEx", new MonitoredTemplateCollectionModelEx(1, 2, 3));

        DefaultObjectWrapper objectWrapper = new DefaultObjectWrapper(Configuration.VERSION_2_3_28);
        objectWrapper.setForceLegacyNonListCollections(false);
        cfg.setObjectWrapper(objectWrapper);

        return cfg;
    }

    @Test
    public void dynamicIndexTest() throws Exception {
        assertErrorContains("${coll?sequence?map(it -> it)['x']}",
                "hash", "evaluated to a sequence");

        assertOutput("${coll?sequence?map(it -> it)[0]}",
                "[iterator][hasNext][next]1");
        assertOutput("${coll?sequence?map(it -> it)[1]}",
                "[iterator][hasNext][next][hasNext][next]2");
        assertOutput("${coll?map(it -> it)?sequence[1]}",
                "[iterator][hasNext][next][hasNext][next]2");
        assertOutput("${coll?sequence?map(it -> it)[2]}",
                "[iterator][hasNext][next][hasNext][next][hasNext][next]3");
        assertOutput("${coll?sequence?map(it -> it)[3]!'missing'}",
                "[iterator][hasNext][next][hasNext][next][hasNext][next][hasNext]missing");
        assertOutput("${coll?sequence?filter(it -> it % 2 == 0)[0]}",
                "[iterator][hasNext][next][hasNext][next]2");
        assertOutput("${coll?sequence?filter(it -> it > 3)[0]!'missing'}",
                "[iterator][hasNext][next][hasNext][next][hasNext][next][hasNext]missing");

        assertOutput("${collLong?sequence?map(it -> it)[1 .. 2]?join(', ')}",
                "[iterator][hasNext][next][hasNext][next][hasNext][next]2, 3");
    }

    @Test
    public void dynamicIndexNonSequenceInput() throws Exception {
        assertErrorContains("${coll[1]}", "sequence", "evaluated to a collection");
        assertOutput("${coll?sequence[1]}", "[iterator][hasNext][next][hasNext][next]2");

        assertErrorContains("<#assign t = coll[1..2]>", "sequence", "evaluated to a collection");
        assertOutput("<#assign t = coll?sequence[1..2]>${t?join('')}",
                "[iterator][hasNext][next][hasNext][next][hasNext][next]23");
        assertOutput("<#list coll?sequence[1..2] as it>${it}</#list>",
                "[iterator][hasNext][next][hasNext][next]2[hasNext][next]3");
    }

    @Test
    public void sizeBasicsTest() throws Exception {
        assertOutput("${seq?size}",
                "[size]3");
        assertOutput("${collEx?size}",
                "[size]3");
        assertErrorContains("${coll?size}",
                "sequence", "extended collection");

        assertOutput("${seq?map(x -> x * 10)?size}",
                "[size]3");
        assertOutput("${collEx?sequence?map(x -> x * 10)?size}",
                "[size]3");
        assertOutput("${collEx?map(x -> x * 10)?sequence?size}",
                "[size]3");

        assertOutput("${seq?filter(x -> x != 1)?size}",
                "[size][get 0][get 1][get 2]2");
        assertOutput("${collEx?sequence?filter(x -> x != 1)?size}",
                "[iterator][hasNext][next][hasNext][next][hasNext][next][hasNext]2");
        assertOutput("${collEx?filter(x -> x != 1)?sequence?size}",
                "[iterator][hasNext][next][hasNext][next][hasNext][next][hasNext]2");
    }

    @Test
    public void sizeComparisonTest() throws Exception {
        // These actually aren't related to lazy generation...
        assertOutput("${collEx?size}",
                "[size]3");
        assertOutput("${collEx?size != 0}",
                "[isEmpty]true");
        assertOutput("${0 != collEx?size}",
                "[isEmpty]true");
        assertOutput("${collEx?size == 0}",
                "[isEmpty]false");
        assertOutput("${0 == collEx?size}",
                "[isEmpty]false");
        assertOutput("${(collEx?size >= 1)}",
                "[isEmpty]true");
        assertOutput("${1 <= collEx?size}",
                "[isEmpty]true");
        assertOutput("${collEx?size <= 0}",
                "[isEmpty]false");
        assertOutput("${(0 >= collEx?size)}",
                "[isEmpty]false");
        assertOutput("${collEx?size > 0}",
                "[isEmpty]true");
        assertOutput("${0 < collEx?size}",
                "[isEmpty]true");
        assertOutput("${collEx?size < 1}",
                "[isEmpty]false");
        assertOutput("${1 > collEx?size}",
                "[isEmpty]false");
        assertOutput("${collEx?size == 1}",
                "[size]false");
        assertOutput("${1 == collEx?size}",
                "[size]false");

        // Now the lazy generation things:
        assertOutput("${collLong?sequence?filter(x -> true)?size}",
                "[iterator]" +
                        "[hasNext][next][hasNext][next][hasNext][next]" +
                        "[hasNext][next][hasNext][next][hasNext][next][hasNext]6");
        // Note: "[next]" is added by ?filter, as it has to know if the element matches the predicate.
        assertOutput("${collLong?sequence?filter(x -> true)?size != 0}",
                "[iterator][hasNext][next]true");
        assertOutput("${collLong?sequence?filter(x -> true)?size != 1}",
                "[iterator][hasNext][next][hasNext][next]true");
        assertOutput("${collLong?sequence?filter(x -> true)?size == 1}",
                "[iterator][hasNext][next][hasNext][next]false");
        assertOutput("${collLong?filter(x -> true)?sequence?size == 1}",
                "[iterator][hasNext][next][hasNext][next]false");
        assertOutput("${collLong?sequence?filter(x -> true)?size < 3}",
                "[iterator][hasNext][next][hasNext][next][hasNext][next]false");
    }

    @Test
    public void sizeNonSequenceInput() throws Exception {
        assertErrorContains("${coll?size}", "sequence", "evaluated to a collection");
        assertOutput("${coll?sequence?size}", "[iterator][hasNext][next][hasNext][next][hasNext][next][hasNext]3");
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
        String commonSourceExp = "collLong?filter(x -> x % 2 == 0)";
        for (String sourceExp : new String[] {commonSourceExp, "(" + commonSourceExp + ")"}) {
            assertOutput("<#list " + sourceExp + " as it>${it} ${it?hasNext}<#break></#list>",
                    "[iterator][hasNext][next][hasNext][next][hasNext][next][hasNext][next]2 true");
        }
    }

    @Test
    public void biTargetParenthesisTest() throws Exception {
        assertOutput("${(coll?filter(x -> x % 2 == 0))?first}",
                "[iterator][hasNext][next][hasNext][next]2");
    }

    @Test
    public void rangeOperatorTest() throws Exception {
        assertErrorContains("${coll[1..2]?join(', ')}", "sequence", "collection");

        assertOutput("${seq[1..2]?first}", "[size][get 1][get 2]2");
        assertOutput("${seq[1..]?first}",  "[size][get 1][get 2]2");
        assertOutput("${seq[2..1]?first}",  "[size][get 2][get 1]3");

        assertOutput("${seqLong[1..3]?first}", "[size][get 1][get 2][get 3]2");
        assertOutput("${seqLong[1..]?first}",  "[size][get 1][get 2][get 3][get 4][get 5]2");
        assertOutput("${seqLong[3..1]?first}",  "[size][get 3][get 2][get 1]4");

        assertOutput("${seq?map(x->x)[1..2]?first}", "[size][size][get 0][get 1]2");
        assertOutput("${seq?map(x->x)[1..]?first}",  "[size][size][get 0][get 1]2");
        // Why 2 [size]-s above: 1st to validate range. 2nd for the 1st hasNext call on the iterator.
        assertOutput("${seq?map(x->x)[2..1]?first}",  "[size][size][get 0][get 1][get 2]3");

        assertOutput("${seqLong?map(x->x)[1..3]?first}", "[size][size][get 0][get 1]2");
        assertOutput("${seqLong?map(x->x)[1..]?first}",  "[size][size][get 0][get 1]2");
        assertOutput("${seqLong?map(x->x)[3..1]?first}",  "[size][size][get 0][get 1][get 2][get 3]4");

        assertOutput("${seq?filter(x->true)[1..2]?first}", "[size][get 0][get 1]2");
        assertOutput("${seq?filter(x->true)[1..]?first}",  "[size][get 0][get 1]2");
        assertOutput("${seq?filter(x->true)[2..1]?first}",  "[size][get 0][get 1][get 2]3");

        assertOutput("${seqLong?filter(x->true)[1..3]?first}", "[size][get 0][get 1]2");
        assertOutput("${seqLong?filter(x->true)[1..]?first}",  "[size][get 0][get 1]2");
        assertOutput("${seqLong?filter(x->true)[3..1]?first}",  "[size][get 0][get 1][get 2][get 3]4");

        assertOutput("${seq[1..2][0..1]?first}", "[size][get 1][get 2]2");
        assertOutput("${seq?map(x->x)[1..2][0..1]?first}", "[size][size][get 0][get 1]2");
        assertOutput("${seq?filter(x->true)[1..2][0..1]?first}", "[size][get 0][get 1]2");

        assertOutput("<#list seqLong?filter(x->true)[1..3] as it>${it}</#list>",
                "[size][get 0][get 1]2[get 2]3[get 3]4");
        assertOutput("<#list seqLong[1..3] as it>${it}</#list>",
                "[size][get 1][get 2][get 3]234");

        assertOutput("${seq?map(x->x)[1..2]?size}", "[size]2");
        assertOutput("${seq?filter(x->true)[1..2]?size}", "[size][get 0][get 1][get 2]2");
        assertOutput("${seqLong?map(x->x)[2..]?size}", "[size]4");
        assertOutput("${seqLong?filter(x->true)[2..]?size}", "[size][get 0][get 1][get 2][get 3][get 4][get 5]4");
        assertOutput("${seqLong?map(x->x)[2..*3]?size}", "[size]3");
        assertOutput("${seqLong?filter(x->true)[2..*3]?size}", "[size][get 0][get 1][get 2][get 3][get 4]3");
    }

    @Test
    public void testNonDirectCalledBuiltInsAreNotLazy() throws Exception {
        assertOutput("" +
                "<#assign changing = 1>" +
                "<#assign method = [1, 2]?filter(it -> it != changing)?join>" +
                "<#assign changing = 2>" +
                "${method(', ')}",
                "2");
        assertOutput("" +
                "<#assign changing = 1>" +
                "<#assign method = [1, 2]?filter(it -> it != changing)?seq_contains>" +
                "<#assign changing = 2>" +
                "${method(2)?c}",
                "true");
        assertOutput("" +
                "<#assign changing = 1>" +
                "<#assign method = [1, 2]?filter(it -> it != changing)?seq_index_of>" +
                "<#assign changing = 2>" +
                "${method(2)}",
                "0");
    }

    public static abstract class ListContainingTemplateModel {
        protected final List<Number> elements;

        public ListContainingTemplateModel(Number... elements) {
            this.elements = new ArrayList<>(Arrays.asList(elements));
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
