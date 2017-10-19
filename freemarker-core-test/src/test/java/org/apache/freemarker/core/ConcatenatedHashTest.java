package org.apache.freemarker.core;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.ObjectWrappingException;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateHashModelEx.KeyValuePair;
import org.apache.freemarker.core.model.TemplateHashModelEx.KeyValuePairIterator;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class ConcatenatedHashTest {

    private static final ImmutableMap<String, Integer> ABCD_MAP = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4);
    private static final ImmutableMap<String, Integer> CD_MAP = ImmutableMap.of("c", 3, "d", 4);
    private static final ImmutableMap<String, Integer> AB_MAP = ImmutableMap.of("a", 1, "b", 2);
    private static final ImmutableMap<String, Integer> ABC33_MAP = ImmutableMap.of("a", 1, "b", 2, "c", 33);
    private static final ImmutableMap<String, Integer> C33DAB_MAP = ImmutableMap.of("c", 33, "d", 4, "a", 1, "b", 2);
    
    private static final Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0).build();
    private static final ObjectWrapperAndUnwrapper ow = (ObjectWrapperAndUnwrapper) cfg.getObjectWrapper();

    @Test
    public void testHashPlusHash() throws Exception {
        TemplateHashModel r = getConcatenatedModel(toHashModelNonEx(AB_MAP), toHashModelNonEx(CD_MAP));
        assertThat(r, not(instanceOf(TemplateHashModelEx.class)));
        assertHashModelContent(ABCD_MAP,
                r);
        assertHashModelContent(ABCD_MAP,
                getConcatenatedModel(toHashModelNonEx(ABC33_MAP), toHashModelNonEx(CD_MAP)));
        assertHashModelContent(C33DAB_MAP,
                getConcatenatedModel(toHashModelNonEx(CD_MAP), toHashModelNonEx(ABC33_MAP)));
        assertHashModelContent(AB_MAP,
                getConcatenatedModel(
                        toHashModelNonEx(AB_MAP), toHashModelNonEx(Collections.<String, Object>emptyMap())));
        assertHashModelContent(AB_MAP,
                getConcatenatedModel(
                        toHashModelNonEx(Collections.<String, Object>emptyMap()), toHashModelNonEx(AB_MAP)));
        assertHashModelContent(Collections.<String, Object>emptyMap(),
                getConcatenatedModel(
                        toHashModelNonEx(Collections.<String, Object>emptyMap()),
                        toHashModelNonEx(Collections.<String, Object>emptyMap())));
    }

    @Test
    public void testHashExPlusHashEx() throws Exception {
        TemplateHashModel r = getConcatenatedModel(toHashModelEx(AB_MAP), toHashModelEx(CD_MAP));
        assertThat(r, instanceOf(TemplateHashModelEx.class));
        assertHashModelContent(ABCD_MAP,
                r);
        assertHashModelContent(ABCD_MAP,
                getConcatenatedModel(toHashModelEx(ABC33_MAP), toHashModelEx(CD_MAP)));
        assertHashModelContent(C33DAB_MAP,
                getConcatenatedModel(toHashModelEx(CD_MAP), toHashModelEx(ABC33_MAP)));
        assertHashModelContent(AB_MAP,
                getConcatenatedModel(
                        toHashModelEx(AB_MAP), toHashModelEx(Collections.<String, Object>emptyMap())));
        assertHashModelContent(AB_MAP,
                getConcatenatedModel(
                        toHashModelEx(Collections.<String, Object>emptyMap()), toHashModelEx(AB_MAP)));
        assertHashModelContent(Collections.<String, Object>emptyMap(),
                getConcatenatedModel(
                        toHashModelEx(Collections.<String, Object>emptyMap()),
                        toHashModelEx(Collections.<String, Object>emptyMap())));
    }

    @Test
    public void testNonStringKeyHashExPlusHashEx() throws Exception {
        TemplateHashModelEx r = (TemplateHashModelEx) getConcatenatedModel(
                toHashModelEx(ImmutableMap.of(1, "one", 2, "two", Locale.GERMAN, "de", Locale.FRANCE, "fr")),
                toHashModelEx(ImmutableMap.of(2, "two v2", 3, "three", Locale.FRANCE, "fr v2")));
        assertEquals(ImmutableList.of(1, 2, Locale.GERMAN, Locale.FRANCE, 3), unwrappedKeysToList(r));
        assertEquals(ImmutableList.of("one", "two v2", "de", "fr v2", "three"), unwrappedValuesToList(r));
    }
    
    private TemplateHashModel getConcatenatedModel(TemplateHashModel h1, TemplateHashModel h2)
            throws IOException, TemplateException {
        StringWriter sw = new StringWriter();
        Environment env = new Template(null, "<#assign r = h1 + h2>", cfg)
                .createProcessingEnvironment(ImmutableMap.of("h1", h1, "h2", h2), sw);
        env.process();
        return (TemplateHashModel) env.getVariable("r");
    }

    private TemplateHashModelEx toHashModelEx(Map<?, ?> map) throws ObjectWrappingException {
        TemplateModel tm = cfg.getObjectWrapper().wrap(map);
        assertThat(tm, instanceOf(TemplateHashModelEx.class));
        return (TemplateHashModelEx) tm;
    }

    private TemplateHashModel toHashModelNonEx(Map<String, ?> map) throws ObjectWrappingException {
        final TemplateHashModelEx tm = toHashModelEx(map);
        return new TemplateHashModel() {

            @Override
            public boolean isEmptyHash() throws TemplateException {
                return tm.isEmptyHash();
            }

            @Override
            public TemplateModel get(String key) throws TemplateException {
                return tm.get(key);
            }
        };
    }

    private void assertHashModelContent(Map<String, ?> expected, TemplateHashModel actual) throws TemplateException {
        for (Entry<String, ?> ent : expected.entrySet()) {
            TemplateModel value = actual.get(ent.getKey());
            assertNotNull(value);
            assertEquals(ent.getValue(), ow.unwrap(value));
        }

        assertEquals(expected.isEmpty(), actual.isEmptyHash());

        if (actual instanceof TemplateHashModelEx) {
            TemplateHashModelEx actualEx = (TemplateHashModelEx) actual;

            assertEquals(expected.size(), actualEx.getHashSize());

            // Keys:
            {
                ArrayList<String> expectedKeys = new ArrayList<>(expected.keySet());

                ArrayList<?> actualKeys = unwrappedKeysToList(actualEx);
                assertEquals(expectedKeys, actualKeys);

                // Without hasNext:
                ArrayList<String> actualKeys2 = new ArrayList<>();
                TemplateModelIterator iter = actualEx.keys().iterator();
                for (int i = 0; i < actualEx.getHashSize(); i++) {
                    actualKeys2.add((String) ow.unwrap(iter.next()));
                }
                assertEquals(actualKeys, actualKeys2);
                
                assertEquals(expectedKeys.size(), actualEx.keys().getCollectionSize());
                assertEquals(expectedKeys.isEmpty(), actualEx.keys().isEmptyCollection());
            }

            // Values:
            {
                ArrayList<?> expectedValues = new ArrayList<>(expected.values());

                ArrayList<Object> actualValues = unwrappedValuesToList(actualEx);
                assertEquals(expectedValues, actualValues);

                // Without hasNext:
                ArrayList<Object> actualValues2 = new ArrayList<>();
                TemplateModelIterator iter = actualEx.values().iterator();
                for (int i = 0; i < actualEx.getHashSize(); i++) {
                    actualValues2.add(ow.unwrap(iter.next()));
                }
                assertEquals(actualValues, actualValues2);
                
                assertEquals(expectedValues.size(), actualEx.values().getCollectionSize());
                assertEquals(expectedValues.isEmpty(), actualEx.values().isEmptyCollection());
            }

            // Key-value pairs:
            {
                ArrayList<Pair<String, ?>> expectedPairs = new ArrayList<>();
                for (Map.Entry<String, ?> ent : expected.entrySet()) {
                    expectedPairs.add(Pair.of(ent.getKey(), ent.getValue()));
                }

                ArrayList<Pair<String, ?>> actualPairs = new ArrayList<>();
                for (KeyValuePairIterator iter = actualEx.keyValuePairIterator(); iter.hasNext();) {
                    KeyValuePair kvp = iter.next();
                    actualPairs.add(Pair.of((String) ow.unwrap(kvp.getKey()), ow.unwrap(kvp.getValue())));
                }
                assertEquals(expectedPairs, actualPairs);

                // Without hasNext:
                ArrayList<Pair<String, ?>> actualPairs2 = new ArrayList<>();
                KeyValuePairIterator iter = actualEx.keyValuePairIterator();
                for (int i = 0; i < actualEx.getHashSize(); i++) {
                    KeyValuePair kvp = iter.next();
                    actualPairs2.add(Pair.of((String) ow.unwrap(kvp.getKey()), ow.unwrap(kvp.getValue())));
                }
                assertEquals(actualPairs, actualPairs2);
            }
        }
    }

    private ArrayList<?> unwrappedKeysToList(TemplateHashModelEx actualEx) throws TemplateException {
        ArrayList<Object> actualKeys = new ArrayList<>();
        for (TemplateModelIterator iter = actualEx.keys().iterator(); iter.hasNext();) {
            actualKeys.add(ow.unwrap(iter.next()));
        }
        return actualKeys;
    }

    private ArrayList<Object> unwrappedValuesToList(TemplateHashModelEx actualEx) throws TemplateException {
        ArrayList<Object> actualValues = new ArrayList<>();
        for (TemplateModelIterator iter = actualEx.values().iterator(); iter.hasNext();) {
            actualValues.add(ow.unwrap(iter.next()));
        }
        return actualValues;
    }

}
