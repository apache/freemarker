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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Test;

import freemarker.core.AddConcatExpression.ConcatenatedSequence;
import freemarker.template.SimpleCollection;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

public class ConcatenatedSequenceTest {
    interface SeqFactory {
        TemplateSequenceModel create(String... items);
        boolean isUnrepeatable();
    }

    @Test
    public void testForSequences() throws TemplateModelException {
        test(new SeqFactory() {
            @Override
            public TemplateSequenceModel create(String... items) {
                return new SimpleSequence(List.of(items));
            }

            @Override
            public boolean isUnrepeatable() {
                return false;
            }
        });
    }

    @Test
    public void testForCollectionsWrappingIterable() throws TemplateModelException {
        test(new SeqFactory() {
            @Override
            public TemplateSequenceModel create(String... items) {
                return new CollectionAndSequence(new SimpleCollection(Arrays.asList(items)));
            }

            @Override
            public boolean isUnrepeatable() {
                return false;
            }
        });
    }

    @Test
    public void testForCollectionsWrappingIterator() throws TemplateModelException {
        test(new SeqFactory() {
            @Override
            public TemplateSequenceModel create(String... items) {
                return new CollectionAndSequence(new SimpleCollection(Arrays.asList(items).iterator()));
            }

            @Override
            public boolean isUnrepeatable() {
                return true;
            }
        });
    }

    public void test(SeqFactory segmentFactory) throws TemplateModelException {
        assertConcatenationResult(segmentFactory.isUnrepeatable(), () ->
                new ConcatenatedSequence(segmentFactory.create(), segmentFactory.create()));
        assertConcatenationResult(segmentFactory.isUnrepeatable(), () ->
                new ConcatenatedSequence(segmentFactory.create(), segmentFactory.create("b")),
                "b");
        assertConcatenationResult(segmentFactory.isUnrepeatable(), () ->
                new ConcatenatedSequence(segmentFactory.create("a"), segmentFactory.create()),
                "a");
        assertConcatenationResult(segmentFactory.isUnrepeatable(), () ->
                new ConcatenatedSequence(segmentFactory.create("a"), segmentFactory.create("b")),
                "a", "b");

        assertConcatenationResult(segmentFactory.isUnrepeatable(), () ->
                new ConcatenatedSequence(
                        new ConcatenatedSequence(
                                new ConcatenatedSequence(
                                        segmentFactory.create("a"),
                                        segmentFactory.create("b")),
                                segmentFactory.create("c")),
                        segmentFactory.create("d")),
                "a", "b", "c", "d");
        assertConcatenationResult(segmentFactory.isUnrepeatable(), () ->
                new ConcatenatedSequence(
                        new ConcatenatedSequence(
                                segmentFactory.create("a"),
                                segmentFactory.create("b")),
                        new ConcatenatedSequence(
                                segmentFactory.create("c"),
                                segmentFactory.create("d"))
                ),
                "a", "b", "c", "d");
        assertConcatenationResult(segmentFactory.isUnrepeatable(), () ->
                new ConcatenatedSequence(
                    segmentFactory.create("a"),
                    new ConcatenatedSequence(
                            segmentFactory.create("b"),
                            new ConcatenatedSequence(
                                    segmentFactory.create("c"),
                                    segmentFactory.create("d")
                            )
                    )
                ),
                "a", "b", "c", "d");

        assertConcatenationResult(segmentFactory.isUnrepeatable(), () ->
                new ConcatenatedSequence(
                        new ConcatenatedSequence(
                                segmentFactory.create("a", "b"),
                                segmentFactory.create("c", "d")),
                        new ConcatenatedSequence(
                                segmentFactory.create("e", "f"),
                                segmentFactory.create("g", "h"))
                ),
                "a", "b", "c", "d", "e", "f", "g", "h");
        assertConcatenationResult(segmentFactory.isUnrepeatable(), () ->
                new ConcatenatedSequence(
                        segmentFactory.create("a", "b", "c"),
                        new ConcatenatedSequence(
                                segmentFactory.create("d", "e"),
                                segmentFactory.create("f", "g", "h"))
                ),
                "a", "b", "c", "d", "e", "f", "g", "h");
        assertConcatenationResult(segmentFactory.isUnrepeatable(), () ->
                new ConcatenatedSequence(
                        new ConcatenatedSequence(
                                segmentFactory.create("a", "b"),
                                segmentFactory.create("c", "d")),
                        segmentFactory.create("e", "f", "g", "h")
                ),
                "a", "b", "c", "d", "e", "f", "g", "h");

        if (!segmentFactory.isUnrepeatable()) {
            // Test when the same segment seq instance is for multiple times in a concatenated seq.
            assertConcatenationResult(segmentFactory.isUnrepeatable(), () ->
                    {
                        TemplateSequenceModel ab = segmentFactory.create("a", "b");
                        ConcatenatedSequence abab = new ConcatenatedSequence(ab, ab);
                        return new ConcatenatedSequence(abab, abab);
                    },
                    "a", "b", "a", "b", "a", "b", "a", "b");
        }
    }

    private void assertConcatenationResult(
            boolean repeatable,
            Supplier<ConcatenatedSequence> seqSupplier,
            String... expectedItems)
            throws TemplateModelException {
        ConcatenatedSequence seq = seqSupplier.get();

        {
            List<String> actualItems = new ArrayList<>();
            for (TemplateModelIterator iter = seq.iterator(); iter.hasNext(); ) {
                actualItems.add(((TemplateScalarModel) iter.next()).getAsString());
            }
            assertEquals(Arrays.asList(expectedItems), actualItems);
        }

        if (repeatable) {
            seq = seqSupplier.get();
        }

        {
            List<String> actualItems = new ArrayList<>();
            for (TemplateModelIterator iter = seq.iterator(); iter.hasNext(); ) {
                assertTrue(iter.hasNext());
                actualItems.add(((TemplateScalarModel) iter.next()).getAsString());
            }
            assertEquals(Arrays.asList(expectedItems), actualItems);
        }

        if (repeatable) {
            seq = seqSupplier.get();
        }

        {
            List<String> actualItems = new ArrayList<>();
            int size = seq.size();
            for (int i = 0; i < size; i++) {
                actualItems.add(((TemplateScalarModel) seq.get(i)).getAsString());
            }
            assertEquals(Arrays.asList(expectedItems), actualItems);
        }

        if (repeatable) {
            seq = seqSupplier.get();
        }

        assertEquals(expectedItems.length, seq.size());
    }

}