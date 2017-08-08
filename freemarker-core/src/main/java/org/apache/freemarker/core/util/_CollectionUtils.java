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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Don't use this; used internally by FreeMarker, might changes without notice.
 * {@link Collection} and {@link Map}-related utilities.
 */
public class _CollectionUtils {
    
    private _CollectionUtils() { }

    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[] { };
    public static final Class[] EMPTY_CLASS_ARRAY = new Class[] { };
    public static final String[] EMPTY_STRING_ARRAY = new String[] { };
    public static final char[] EMPTY_CHAR_ARRAY = new char[] { };

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> List<? extends T> safeCastList(
            String argName, List list,
            Class<T> itemClass, boolean allowNullItem) {
        if (list == null) {
            return null;
        }
        for (int i = 0; i < list.size(); i++) {
            Object it = list.get(i);
            if (!itemClass.isInstance(it)) {
                if (it == null) {
                    if (!allowNullItem) {
                        throw new IllegalArgumentException(
                                (argName != null ? "Invalid value for argument \"" + argName + "\"" : "")
                                + "List item at index " + i + " is null");
                    }
                } else {
                    throw new IllegalArgumentException(
                            (argName != null ? "Invalid value for argument \"" + argName + "\"" : "")
                            + "List item at index " + i + " is not instance of " + itemClass.getName() + "; "
                            + "its class is " + it.getClass().getName() + ".");
                }
            }
        }

        return list;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <K, V> Map<? extends K, ? extends V> safeCastMap(
            String argName, Map map,
            Class<K> keyClass, boolean allowNullKey,
            Class<V> valueClass, boolean allowNullValue) {
        if (map == null) {
            return null;
        }
        for (Map.Entry<?, ?> ent : ((Map<?, ?>) map).entrySet()) {
            Object key = ent.getKey();
            if (!keyClass.isInstance(key)) {
                if (key == null) {
                    if (!allowNullKey) {
                        throw new IllegalArgumentException(
                                (argName != null ? "Invalid value for argument \"" + argName + "\": " : "")
                                        + "The Map contains null key");
                    }
                } else {
                    throw new IllegalArgumentException(
                            (argName != null ? "Invalid value for argument \"" + argName + "\": " : "")
                                    + "The Map contains a key that's not instance of " + keyClass.getName() +
                                    "; its class is " + key.getClass().getName() + ".");
                }
            }

            Object value = ent.getValue();
            if (!valueClass.isInstance(value)) {
                if (value == null) {
                    if (!allowNullValue) {
                        throw new IllegalArgumentException(
                                (argName != null ? "Invalid value for argument \"" + argName + "\"" : "")
                                        + "The Map contains null value");
                    }
                } else {
                    throw new IllegalArgumentException(
                            (argName != null ? "Invalid value for argument \"" + argName + "\"" : "")
                                    + "The Map contains a value that's not instance of " + valueClass.getName() +
                                    "; its class is " + value.getClass().getName() + ".");
                }
            }
        }

        return map;
    }

    private static final Class<?> UNMODIFIABLE_MAP_CLASS_1 = Collections.emptyMap().getClass();
    private static final Class<?> UNMODIFIABLE_MAP_CLASS_2 = Collections.unmodifiableMap(
            new HashMap<Object, Object> (1)).getClass();
    private static final Class<?> UNMODIFIABLE_LIST_CLASS_1 = Collections.emptyList().getClass();
    private static final Class<?> UNMODIFIABLE_LIST_CLASS_2 = Collections.unmodifiableList(
            new ArrayList<Object>(1)).getClass();

    public static boolean isMapKnownToBeUnmodifiable(Map<?, ?> map) {
        if (map == null) {
            return true;
        }
        Class<? extends Map> mapClass = map.getClass();
        return mapClass == UNMODIFIABLE_MAP_CLASS_1 || mapClass == UNMODIFIABLE_MAP_CLASS_2;
    }

    public static boolean isListKnownToBeUnmodifiable(List<?> list) {
        if (list == null) {
            return true;
        }
        Class<? extends List> listClass = list.getClass();
        return listClass == UNMODIFIABLE_LIST_CLASS_1 || listClass == UNMODIFIABLE_LIST_CLASS_2;
    }

    /**
     * Optimized version of {@link Collections#unmodifiableMap(Map)} (avoids needless wrapping).
     *
     * @param map The map to return or wrap if not already unmodifiable, or {@code null} which is silently bypassed.
     */
    public static <K, V> Map<K, V> unmodifiableMap(Map<K, V> map) {
        return isMapKnownToBeUnmodifiable(map) ? map : Collections.unmodifiableMap(map);
    }

    /**
     * Adds two {@link Map}-s (keeping the iteration order); assuming the inputs are already unmodifiable and
     * unchanging, it returns an unmodifiable and unchanging {@link Map} itself.
     */
    public static <K,V> Map<K,V> mergeImmutableMaps(Map<K,V> m1, Map<K,V> m2, boolean keepOriginalOrder) {
        if (m1 == null) return m2;
        if (m2 == null) return m1;
        if (m1.isEmpty()) return m2;
        if (m2.isEmpty()) return m1;

        Map<K, V> mergedM = keepOriginalOrder
                ? new LinkedHashMap<K, V>((m1.size() + m2.size()) * 4 / 3 + 1, 0.75f)
                : new HashMap<K, V>((m1.size() + m2.size()) * 4 / 3 + 1, 0.75f);
        mergedM.putAll(m1);
        if (keepOriginalOrder) {
            for (K m2Key : m2.keySet()) {
                mergedM.remove(m2Key); // So that duplicate keys are moved after m1 keys
            }
        }
        mergedM.putAll(m2);
        return Collections.unmodifiableMap(mergedM);
    }

    /**
     * Adds multiple {@link List}-s; assuming the inputs are already unmodifiable and unchanging, it returns an
     * unmodifiable and unchanging {@link List} itself.
     */
    public static <T> List<T> mergeImmutableLists(boolean skipDuplicatesInList1, List<T> ... lists) {
        if (lists == null || lists.length == 0) {
            return null;
        }

        if (lists.length == 1) {
            return mergeImmutableLists(lists[0], null, skipDuplicatesInList1);
        } else if (lists.length == 2) {
            return mergeImmutableLists(lists[0], lists[1], skipDuplicatesInList1);
        } else {
            List<T> [] reducedLists = new List[lists.length - 1];
            reducedLists[0] = mergeImmutableLists(lists[0], lists[1], skipDuplicatesInList1);
            System.arraycopy(lists, 2, reducedLists, 1, lists.length - 2);
            return mergeImmutableLists(skipDuplicatesInList1, reducedLists);
        }
    }

    /**
     * Adds two {@link List}-s; assuming the inputs are already unmodifiable and unchanging, it returns an
     * unmodifiable and unchanging {@link List} itself.
     */
    public static <T> List<T> mergeImmutableLists(List<T> list1, List<T> list2,
            boolean skipDuplicatesInList1) {
        if (list1 == null) return list2;
        if (list2 == null) return list1;
        if (list1.isEmpty()) return list2;
        if (list2.isEmpty()) return list1;

        ArrayList<T> mergedList = new ArrayList<>(list1.size() + list2.size());
        if (skipDuplicatesInList1) {
            Set<T> list2Set = new HashSet<>(list2);
            for (T it : list1) {
                if (!list2Set.contains(it)) {
                    mergedList.add(it);
                }
            }
        } else {
            mergedList.addAll(list1);
        }
        mergedList.addAll(list2);
        return Collections.unmodifiableList(mergedList);
    }

}
