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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.junit.Test;

import freemarker.core.AddConcatExpression.ConcatenatedSequence;
import freemarker.template.Configuration;
import freemarker.template.DefaultListAdapter;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.SimpleCollection;
import freemarker.template.SimpleScalar;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateCollectionModelEx;
import freemarker.template.TemplateModel;
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
    public void testForSimpleSequences() throws TemplateModelException {
        testWithSegmentFactory(new SeqFactory() {
            @Override
            public TemplateSequenceModel create(String... items) {
                return new SimpleSequence(Arrays.asList(items));
            }

            @Override
            public boolean isUnrepeatable() {
                return false;
            }
        });
    }

    @Test
    public void testForListAdapter() throws TemplateModelException {
        DefaultObjectWrapper objectWrapper = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_32).build();
        testWithSegmentFactory(new SeqFactory() {
            @Override
            public TemplateSequenceModel create(String... items) {
                return DefaultListAdapter.adapt(Arrays.asList(items), objectWrapper);
            }

            @Override
            public boolean isUnrepeatable() {
                return false;
            }
        });
    }

    @Test
    public void testForSequenceAndCollectionModelEx() throws TemplateModelException {
        testWithSegmentFactory(new SeqFactory() {
            @Override
            public TemplateSequenceModel create(String... items) {
                return new SequenceAndCollectionModelEx(Arrays.asList(items));
            }

            @Override
            public boolean isUnrepeatable() {
                return false;
            }
        });
    }

    @Test
    public void testForCollectionsWrappingIterable() throws TemplateModelException {
        testWithSegmentFactory(new SeqFactory() {
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
        testWithSegmentFactory(new SeqFactory() {
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

    public void testWithSegmentFactory(SeqFactory segmentFactory) throws TemplateModelException {
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
                                segmentFactory.create(),
                                segmentFactory.create()),
                        new ConcatenatedSequence(
                                segmentFactory.create(),
                                segmentFactory.create())
                )
        );

        assertConcatenationResult(segmentFactory.isUnrepeatable(), () ->
                new ConcatenatedSequence(
                        new ConcatenatedSequence(
                                segmentFactory.create("a", "b"),
                                segmentFactory.create()),
                        new ConcatenatedSequence(
                                segmentFactory.create(),
                                segmentFactory.create())
                ),
                "a", "b"
        );
        assertConcatenationResult(segmentFactory.isUnrepeatable(), () ->
                        new ConcatenatedSequence(
                                new ConcatenatedSequence(
                                        segmentFactory.create(),
                                        segmentFactory.create("a", "b")),
                                new ConcatenatedSequence(
                                        segmentFactory.create(),
                                        segmentFactory.create())
                        ),
                "a", "b"
        );
        assertConcatenationResult(segmentFactory.isUnrepeatable(), () ->
                        new ConcatenatedSequence(
                                new ConcatenatedSequence(
                                        segmentFactory.create(),
                                        segmentFactory.create()),
                                new ConcatenatedSequence(
                                        segmentFactory.create("a", "b"),
                                        segmentFactory.create())
                        ),
                "a", "b"
        );
        assertConcatenationResult(segmentFactory.isUnrepeatable(), () ->
                        new ConcatenatedSequence(
                                new ConcatenatedSequence(
                                        segmentFactory.create(),
                                        segmentFactory.create()),
                                new ConcatenatedSequence(
                                        segmentFactory.create(),
                                        segmentFactory.create("a", "b"))
                        ),
                "a", "b"
        );
        assertConcatenationResult(segmentFactory.isUnrepeatable(), () ->
                new ConcatenatedSequence(
                        new ConcatenatedSequence(
                                segmentFactory.create("a"),
                                segmentFactory.create("b")),
                        new ConcatenatedSequence(
                                segmentFactory.create(),
                                segmentFactory.create())
                ),
                "a", "b"
        );
        assertConcatenationResult(segmentFactory.isUnrepeatable(), () ->
                        new ConcatenatedSequence(
                                new ConcatenatedSequence(
                                        segmentFactory.create(),
                                        segmentFactory.create("a")),
                                new ConcatenatedSequence(
                                        segmentFactory.create("b"),
                                        segmentFactory.create())
                        ),
                "a", "b"
        );
        assertConcatenationResult(segmentFactory.isUnrepeatable(), () ->
                        new ConcatenatedSequence(
                                new ConcatenatedSequence(
                                        segmentFactory.create(),
                                        segmentFactory.create()),
                                new ConcatenatedSequence(
                                        segmentFactory.create("a"),
                                        segmentFactory.create("b"))
                        ),
                "a", "b"
        );

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

        assertConcatenationResult(segmentFactory.isUnrepeatable(), () ->
                        new ConcatenatedSequence(
                                new ConcatenatedSequence(
                                        segmentFactory.create(null, "a"),
                                        segmentFactory.create("b", null)),
                                segmentFactory.create((String) null)
                        ),
                null, "a", "b", null, null);
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
                actualItems.add(asNullableString((TemplateScalarModel) iter.next()));
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
                actualItems.add(asNullableString((TemplateScalarModel) iter.next()));
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
                actualItems.add(asNullableString((TemplateScalarModel) seq.get(i)));
            }
            assertEquals(Arrays.asList(expectedItems), actualItems);
            assertNull(seq.get(-1));
            assertNull(seq.get(size));
            assertNull(seq.get(size + 1));
        }

        if (repeatable) {
            seq = seqSupplier.get();
        }

        assertEquals(expectedItems.length, seq.size());

        if (repeatable) {
            seq = seqSupplier.get();
        }

        assertEquals(expectedItems.length == 0, seq.isEmpty());
    }

    private String asNullableString(TemplateScalarModel model) throws TemplateModelException {
        return model != null ? model.getAsString() : null;
    }

    /**
     * This is to test {@link TemplateSequenceModel} that's also a {@link TemplateCollectionModelEx}.
     */
    private static class SequenceAndCollectionModelEx implements TemplateSequenceModel, TemplateCollectionModelEx {
        private final List<String> items;

        public SequenceAndCollectionModelEx(List<String> items) {
            this.items = items;
        }

        @Override
        public TemplateModelIterator iterator() throws TemplateModelException {
            return new TemplateModelIterator() {
                    private final Iterator<String> it = items.iterator();

                    @Override
                    public TemplateModel next() throws TemplateModelException {
                        try {
                            String value = it.next();
                            return value != null ? new SimpleScalar(value) : null;
                        } catch (NoSuchElementException e) {
                            throw new TemplateModelException("The collection has no more items.", e);
                        }
                    }

                    @Override
                    public boolean hasNext() throws TemplateModelException {
                        return it.hasNext();
                    }
            };
        }

        @Override
        public boolean isEmpty() throws TemplateModelException {
            return items.isEmpty();
        }

        @Override
        public TemplateModel get(int index) throws TemplateModelException {
            String value = items.get(index);
            return value != null ? new SimpleScalar(value) : null;
        }

        @Override
        public int size() throws TemplateModelException {
            return items.size();
        }
    }

}