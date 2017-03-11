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
package freemarker.ext.beans;

public interface Java8DefaultMethodsBeanBase {
    
    static final String DEFAULT_METHOD_PROP = "defaultMethodProp";
    static final String DEFAULT_METHOD_PROP_VALUE = "defaultMethodPropValue";
    static final String DEFAULT_METHOD_PROP_2 = "defaultMethodProp2";
    static final String DEFAULT_METHOD_INDEXED_PROP = "defaultMethodIndexedProp";
    static final String DEFAULT_METHOD_INDEXED_PROP_VALUE = "defaultMethodIndexedPropValue";
    static final String DEFAULT_METHOD_INDEXED_PROP_2 = "defaultMethodIndexedProp2";
    static final String DEFAULT_METHOD_INDEXED_PROP_2_VALUE_0 = "defaultMethodIndexedProp2(0).value";
    static final String DEFAULT_METHOD_INDEXED_PROP_3 = "defaultMethodIndexedProp3";
    static final String DEFAULT_METHOD_NOT_AN_INDEXED_PROP = "defaultMethodNotAnIndexedProp";
    static final String DEFAULT_METHOD_NOT_AN_INDEXED_PROP_VALUE = "defaultMethodNotAnIndexedPropValue";
    static final String DEFAULT_METHOD_NOT_AN_INDEXED_PROP_2 = "defaultMethodNotAnIndexedProp2";
    static final String DEFAULT_METHOD_NOT_AN_INDEXED_PROP_2_VALUE = "defaultMethodNotAnIndexedProp2Value";
    static final String DEFAULT_METHOD_NOT_AN_INDEXED_PROP_3 = "defaultMethodNotAnIndexedProp3";
    static final String DEFAULT_METHOD_NOT_AN_INDEXED_PROP_3_VALUE_0 = "defaultMethodNotAnIndexedProp3Value[0]";
    static final String DEFAULT_METHOD_ACTION = "defaultMethodAction";
    static final String DEFAULT_METHOD_ACTION_RETURN_VALUE = "defaultMethodActionReturnValue";
    static final String OVERRIDDEN_DEFAULT_METHOD_ACTION = "overriddenDefaultMethodAction";

    default String getDefaultMethodProp() {
        return DEFAULT_METHOD_PROP_VALUE;
    }

    default String getDefaultMethodProp2() {
        return "";
    }
    
    default String getDefaultMethodIndexedProp(int i) {
        return DEFAULT_METHOD_INDEXED_PROP_VALUE;
    }

    /**
     * Will be kept as there will be a matching non-indexed read method in the subclass. 
     */
    default String getDefaultMethodIndexedProp2(int i) {
        return DEFAULT_METHOD_INDEXED_PROP_2_VALUE_0;
    }

    /**
     * This is not an indexed reader method, but a matching indexed reader method will be added in the subclass. 
     */
    default String[] getDefaultMethodIndexedProp3() {
        return new String[] {""};
    }
    
    /** Will be discarded because of a non-matching non-indexed read method in a subclass */
    default String getDefaultMethodNotAnIndexedProp(int i) {
        return "";
    }
    
    /** The subclass will try to override this with a non-matching indexed reader, but this will be stronger. */
    default String getDefaultMethodNotAnIndexedProp2() {
        return DEFAULT_METHOD_NOT_AN_INDEXED_PROP_2_VALUE;
    }

    /** The subclass will try to override this with a non-matching indexed reader, but this will be stronger. */
    default String[] getDefaultMethodNotAnIndexedProp3() {
        return new String[] { DEFAULT_METHOD_NOT_AN_INDEXED_PROP_3_VALUE_0 };
    }
    
    default String defaultMethodAction() {
        return DEFAULT_METHOD_ACTION_RETURN_VALUE;
    }

    default Object overriddenDefaultMethodAction() {
        return null;
    }
    
}
