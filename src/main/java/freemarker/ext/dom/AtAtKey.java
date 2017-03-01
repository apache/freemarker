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
package freemarker.ext.dom;

/**
 * The special hash keys that start with "@@".
 */
enum AtAtKey {
    
    MARKUP("@@markup"),
    NESTED_MARKUP("@@nested_markup"),
    ATTRIBUTES_MARKUP("@@attributes_markup"),
    TEXT("@@text"),
    START_TAG("@@start_tag"),
    END_TAG("@@end_tag"),
    QNAME("@@qname"),
    NAMESPACE("@@namespace"),
    LOCAL_NAME("@@local_name"),
    ATTRIBUTES("@@"),
    PREVIOUS_SIBLING_ELEMENT("@@previous_sibling_element"),
    NEXT_SIBLING_ELEMENT("@@next_sibling_element");

    private final String key;

    public String getKey() {
        return key;
    }

    private AtAtKey(String key) {
        this.key = key;
    }
    
    public static boolean containsKey(String key) {
        for (AtAtKey item : AtAtKey.values()) {
            if (item.getKey().equals(key)) {
                return true;
            }
        }
        return false;
    }
    
}
