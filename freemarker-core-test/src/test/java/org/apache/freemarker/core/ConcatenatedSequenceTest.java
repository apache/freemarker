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

import org.apache.freemarker.core.ASTExpAddOrConcat.ConcatenatedSequence;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.impl.DefaultListAdapter;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.core.model.impl.SimpleString;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class ConcatenatedSequenceTest {
    interface SeqFactory {
        TemplateSequenceModel create(String... items);
        boolean isUnrepeatable();
    }

    @Test
    public void testForNativeSequences() throws TemplateException {
        testWithSegmentFactory(new SeqFactory() {
            @Override
            public TemplateSequenceModel create(String... items) {
                return new NativeSequence(Arrays.stream(items).map(it -> it == null ? null : (TemplateModel) new SimpleString(it)).toList());
            }

            @Override
            public boolean isUnrepeatable() {
                return false;
            }
        });
    }

    @Test
    public void testForListAdapter() throws TemplateException {
        DefaultObjectWrapper objectWrapper = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
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

    public void testWithSegmentFactory(SeqFactory segmentFactory) throws TemplateException {
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
            throws TemplateException {
        ConcatenatedSequence seq = seqSupplier.get();

        {
            List<String> actualItems = new ArrayList<>();
            for (TemplateModelIterator iter = seq.iterator(); iter.hasNext(); ) {
                actualItems.add(asNullableString((TemplateStringModel) iter.next()));
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
                actualItems.add(asNullableString((TemplateStringModel) iter.next()));
            }
            assertEquals(Arrays.asList(expectedItems), actualItems);
        }

        if (repeatable) {
            seq = seqSupplier.get();
        }

        {
            List<String> actualItems = new ArrayList<>();
            int size = seq.getCollectionSize();
            for (int i = 0; i < size; i++) {
                actualItems.add(asNullableString((TemplateStringModel) seq.get(i)));
            }
            assertEquals(Arrays.asList(expectedItems), actualItems);
            assertNull(seq.get(-1));
            assertNull(seq.get(size));
            assertNull(seq.get(size + 1));
        }

        if (repeatable) {
            seq = seqSupplier.get();
        }

        assertEquals(expectedItems.length, seq.getCollectionSize());

        if (repeatable) {
            seq = seqSupplier.get();
        }

        assertEquals(expectedItems.length == 0, seq.isEmptyCollection());
    }

    private String asNullableString(TemplateStringModel model) throws TemplateException {
        return model != null ? model.getAsString() : null;
    }

}
