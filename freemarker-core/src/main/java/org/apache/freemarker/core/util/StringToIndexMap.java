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

package org.apache.freemarker.core.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps string keys to non-negative int-s. This isn't a {@link Map}, but a more specialized class. It's immutable.
 * It's slower to create than {@link HashMap}, but usually is a bit faster to read, and when {@link HashMap} gets
 * unlucky with clashing hash keys, then it can be significantly faster.
 * <p>
 * Instances of this class are immutable, thread-safe objects.
 */
public final class StringToIndexMap {

    public static final StringToIndexMap EMPTY = new StringToIndexMap();

    private static final int MAX_VARIATIONS_TRIED = 4;

    private final Entry[] buckets;
    private final int bucketIndexMask;
    private final int bucketIndexOverlap;
    /** Attention: null when you have exactly 1 key, as then there's no key order to remember. */
    private final List<String> keys;

    private static class BucketsSetup {
        private final Entry[] buckets;
        private final int bucketIndexMask;
        private final int bucketIndexOverlap;

        public BucketsSetup(Entry[] buckets, int bucketIndexMask, int bucketIndexOverlap) {
            this.buckets = buckets;
            this.bucketIndexMask = bucketIndexMask;
            this.bucketIndexOverlap = bucketIndexOverlap;
        }
    }

    /**
     * Convenience method for calling {@link #of(Entry[])} with 1 entry.
     */
    public static StringToIndexMap of(String key, int value) {
        // Call special case constructor
        return new StringToIndexMap(new Entry(key, value));
    }

    /**
     * Convenience method for calling {@link #of(Entry[])} with 2 entries.
     */
    public static StringToIndexMap of(String k1, int v1, String k2, int v2) {
        return of(new Entry(k1, v1), new Entry(k2, v2));
    }

    /**
     * Convenience method for calling {@link #of(Entry[])} with 3 entries.
     */
    public static StringToIndexMap of(String k1, int v1, String k2, int v2, String k3, int v3) {
        return of(new Entry(k1, v1), new Entry(k2, v2), new Entry(k3, v3));
    }

    /**
     * Convenience method for calling {@link #of(Entry[])} with 4 entries.
     */
    public static StringToIndexMap of(String k1, int v1, String k2, int v2, String k3, int v3, String k4, int v4) {
        return of(new Entry(k1, v1), new Entry(k2, v2), new Entry(k3, v3), new Entry(k4, v4));
    }

    /**
     * Convenience method for calling {@link #of(Entry[])} with 5 entries.
     */
    public static StringToIndexMap of(String k1, int v1, String k2, int v2, String k3, int v3, String k4, int v4,
            String k5, int v5) {
        return of(new Entry(k1, v1), new Entry(k2, v2), new Entry(k3, v3), new Entry(k4, v4), new Entry(k5, v5));
    }

    /**
     * Convenience method for calling {@link #of(Entry[])} with 6 entries.
     */
    public static StringToIndexMap of(String k1, int v1, String k2, int v2, String k3, int v3, String k4, int v4,
            String k5, int v5, String k6, int v6) {
        return of(new Entry(k1, v1), new Entry(k2, v2), new Entry(k3, v3), new Entry(k4, v4), new Entry(k5, v5),
                new Entry(k6, v6));
    }

    /**
     * @param entries
     *         Contains the entries that will be copied into the created object.
     * @param entriesLength
     *         The method assumes that we only have this many elements; the {@code entries} parameter array might be
     *         longer than this.
     *
     * @throws DuplicateStringKeyException
     *         if the same key occurs twice
     */
    public static StringToIndexMap of(Entry[] entries, int entriesLength) {
        return entriesLength == 0 ? EMPTY
                : entriesLength == 1 ? new StringToIndexMap(entries[0])
                : new StringToIndexMap(entries, entriesLength);
    }

    /**
     * Same as {@link #of(Entry[], int)}, with the last parameter set to {@code entries.length}.
     */
    public static StringToIndexMap of(Entry... entries) {
        return of(entries, entries.length);
    }

    // The 0 argument case is weird, but because of {@link #of(Entry... entries)} it's possible even without this.
    public static StringToIndexMap of() {
        return EMPTY;
    }

    /**
     * Creates a new {@link StringToIndexMap} by inheriting all the entries in {@code baseMap} and appending all the
     * entries in {@code additionalEntries}.
     * 
     * @param baseMap
     *            {@link StringToIndexMap} to inherit entries from
     * @param additionalEntries
     *            additional entries; it must not contain a name that's already in {@code baseMap}, or else
     *            {@link DuplicateStringKeyException} will be thrown.
     * 
     * @return a new {@link StringToIndexMap} created by adding the entries in {@code baseMap} and
     *         {@code additionalEntries}
     *
     * @throws DuplicateStringKeyException
     *             if the same key occurs twice
     */
    public static StringToIndexMap of(StringToIndexMap baseMap, Entry... additionalEntries) {
        final int additionalEntriesLength = (additionalEntries != null) ? additionalEntries.length : 0;
        Entry[] newEntries = new Entry[baseMap.size() + additionalEntriesLength];
        int index = 0;
        if (baseMap.buckets != null) {
            for (Entry entry : baseMap.buckets) {
                while (entry != null) {
                    newEntries[index++] = entry;
                    entry = entry.nextInSameBucket;
                }
            }
        }
        if (additionalEntriesLength > 0) {
            System.arraycopy(additionalEntries, 0, newEntries, index, additionalEntriesLength);
        }
        return of(newEntries);
    }

    // This is a very frequent case, so we optimize for it a bit.
    private StringToIndexMap(Entry entry) {
        buckets = new Entry[] { entry };
        bucketIndexMask = 0;
        bucketIndexOverlap = 0;
        keys = null; // As there's only one key, we can extract the key list from the buckets.
    }

    private StringToIndexMap(Entry... entries) {
        this(entries, entries.length);
    }

    private StringToIndexMap(Entry[] entries, int entriesLength) {
        if (entriesLength == 0) {
            buckets = null;
            bucketIndexMask = 0;
            bucketIndexOverlap = 0;
            keys = Collections.emptyList();
        } else {
            String[] keyArray = new String[entriesLength];
            for (int i = 0; i < entriesLength; i++) {
                keyArray[i] = entries[i].key;
            }
            keys = _ArrayAdapterList.adapt(keyArray);

            // We try to find the best hash algorithm parameter (variation) for the known key set:
            int variation = 0;
            BucketsSetup bestSetup = null;
            int bestSetupGoodness = 0;
            do {
                variation++;
                BucketsSetup setup = getBucketsSetup(entries, entriesLength, variation);

                int filledBucketCnt = 0;
                for (Entry bucket : setup.buckets) {
                    if (bucket != null) {
                        filledBucketCnt++;
                    }
                }
                // Ideally, filledBucketCnt == entriesLength. If less, we have buckets with more than 1 element.
                int setupGoodness = filledBucketCnt - entriesLength;

                if (bestSetup == null || bestSetupGoodness < setupGoodness) {
                    bestSetup = setup;
                    bestSetupGoodness = setupGoodness;
                }
            } while (bestSetupGoodness != 0 && variation < MAX_VARIATIONS_TRIED);

            this.buckets = bestSetup.buckets;
            this.bucketIndexMask = bestSetup.bucketIndexMask;
            this.bucketIndexOverlap = bestSetup.bucketIndexOverlap;
        }
    }

    private static BucketsSetup getBucketsSetup(Entry[] entries, int entriesLength, int variation) {
        // The 2.5 multipier was chosen experimentally.
        int bucketCnt = getPowerOf2GreaterThanOrEqualTo(entriesLength * 2 + entriesLength / 2);

        Entry[] buckets = new Entry[bucketCnt];
        int bucketIndexMask = bucketCnt - 1;
        int bucketIndexOverlap = Integer.numberOfTrailingZeros(bucketCnt) + (variation - 1);
        for (int i = 0; i < entriesLength; i++) {
            Entry entry = entries[i];
            int bucketIdx = getBucketIndex(entry.key, bucketIndexMask, bucketIndexOverlap);
            Entry nextInSameBucket = buckets[bucketIdx];
            checkNameUnique(nextInSameBucket, entry.key);
            buckets[bucketIdx] = nextInSameBucket != entry.nextInSameBucket
                    ? new Entry(entry.key, entry.value, nextInSameBucket)
                    : entry;
        }
        return new BucketsSetup(buckets, bucketIndexMask, bucketIndexOverlap);
    }

    private static int getBucketIndex(String key, int bucketIndexMask, int bucketIndexOverlap) {
        int h = key.hashCode();
        // What's the shift+xor trick: When we just drop the higher bits with plain masking, for some inputs the
        // distribution among the buckets weren't uniform at all. For example, "k11", "k22", "k33", "k44", etc. all
        // fell into the same bucket. To make such clashes less likely, we use twice as much bits as the mask size,
        // by xor-ring the next mask-size-number-of-bits on the hash. That's still far from all the bits, but it seems
        // it's good enough, and overlapping with all the bit would be slower.
        // Note: This is all based of the String.hashCode() of Java 8 though... let's hope it won't get worse.
        return (h ^ (h >>> bucketIndexOverlap)) & bucketIndexMask;
    }

    /**
     * Returns the integer mapped to the name, or -1 if nothing is mapped to the int.
     */
    public int get(String key) {
        if (buckets == null) {
            return -1;
        }

        Entry entry = buckets[getBucketIndex(key, bucketIndexMask, bucketIndexOverlap)];
        if (entry == null) {
            return -1;
        }
        while (!entry.key.equals(key)) {
            entry = entry.nextInSameBucket;
            if (entry == null) {
                return -1;
            }
        }
        return entry.value;
    }

    /**
     * Return the keys in the order as they were once specified. This is not necessary a very fast operation (as it's
     * meant to be used for error message generation, to show valid names).
     */
    public List<String> getKeys() {
        return keys != null ? keys : Collections.singletonList(buckets[0].key);
    }

    public int size() {
        return keys != null ? keys.size() : 1;
    }

    /**
     * Checks if all entries are in the {@code start} - {@code start}+{@code size()} (exclusive) index
     * range.
     *
     * @throws IllegalArgumentException
     *         If some entry is not in the specified index range.
     */
    public void checkIndexRange(int start) {
        if (buckets == null) {
            return;
        }

        int end = start + size();
        for (Entry bucket : buckets) {
            Entry entry = bucket;
            while (entry != null) {
                if (entry.value < start || entry.value >= end) {
                    throw new IllegalArgumentException(
                            "Entry " + _StringUtils.jQuote(entry.key) + " -> " + entry.value
                            + " is out of allowed index range: " + start + " ..< " + end);
                }
                entry = entry.nextInSameBucket;
            }
        }

    }

    private static int getPowerOf2GreaterThanOrEqualTo(int n) {
        if (n == 0) {
            return 0;
        }

        int powOf2 = 1;
        while (powOf2 < n) {
            powOf2 = (powOf2 << 1);
        }
        return powOf2;
    }

    private static void checkNameUnique(Entry entry, String key) {
        while (entry != null) {
            if (entry.key.equals(key)) {
                throw new DuplicateStringKeyException(key);
            }
            entry = entry.nextInSameBucket;
        }
    }

    public static class Entry {
        private final String key;
        private final int value;
        private final Entry nextInSameBucket;

        public Entry(String key, int value) {
            this(key, value, null);
        }

        private Entry(String key, int value, Entry nextInSameBucket) {
            this.key = key;
            this.value = value;
            this.nextInSameBucket = nextInSameBucket;
        }

        public String getKey() {
            return key;
        }

        public int getValue() {
            return value;
        }
    }

    /** Returns the key for a value, or {@code null} if the value wasn't found; this is a slow (O(N)) operation! */
    public String getKeyOfValue(int value) {
        if (buckets == null) {
            return null;
        }
        for (Entry entry : buckets) {
            while (entry != null) {
                if (entry.getValue() == value) {
                    return entry.getKey();
                }
                entry = entry.nextInSameBucket;
            }
        }
        return null;
    }

    /*
    // Code used to see how well the elements are spread among the buckets:

    void dumpBucketSizes() {
        Map<Integer, Integer> hist = new TreeMap<>();
        int goodness = -size();
        for (Entry bucket : buckets) {
            int bucketSize = bucketSize(bucket);
            if (bucketSize != 0) {
                goodness++;
            }
            Integer histCnt = hist.get(bucketSize);
            hist.put(bucketSize, histCnt == null ? 1 : histCnt + 1);
            System.out.print(bucketSize);
        }
        System.out.println();
        System.out.println(hist + "; goodness " + goodness);
    }

    private int bucketSize(Entry entry) {
        int size = 0;
        while (entry != null) {
            size++;
            entry = entry.nextInSameBucket;
        }
        return size;
    }
    */

}
