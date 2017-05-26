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

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Don't use this; used internally by FreeMarker, might changes without notice.
 * {@link Collection} and {@link Map}-related utilities.
 */
public class _CollectionUtil {
    
    private _CollectionUtil() { }

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

}
