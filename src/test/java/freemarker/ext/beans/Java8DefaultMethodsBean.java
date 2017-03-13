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

public class Java8DefaultMethodsBean implements Java8DefaultMethodsBeanBase {
    
    static final String NORMAL_PROP = "normalProp";
    static final String NORMAL_PROP_VALUE = "normalPropValue";
    static final String PROP_2_OVERRIDE_VALUE = "prop2OverrideValue";
    static final String INDEXED_PROP_3_VALUE = "indexedProp3Value";
    static final int NOT_AN_INDEXED_PROP_VALUE = 1;
    static final String ARRAY_PROP_2_VALUE_0 = "arrayProp2[0].value";
    static final int NOT_AN_INDEXED_PROP_3_VALUE = 3;
    static final String NOT_AN_INDEXED_PROP_2_VALUE = "notAnIndecedProp2Value";
    static final String INDEXED_PROP_4 = "indexedProp4";
    static final String INDEXED_PROP_4_VALUE = "indexedProp4Value[0]";
    static final String NORMAL_ACTION = "normalAction";
    static final String NORMAL_ACTION_RETURN_VALUE = "normalActionReturnValue";
    static final String OVERRIDDEN_DEFAULT_METHOD_ACTION_RETURN_VALUE = "overriddenValue";
    
    public String getNormalProp() {
        return NORMAL_PROP_VALUE;
    }
    
    /** Override */
    public String getDefaultMethodProp2() {
        return PROP_2_OVERRIDE_VALUE;
    }
    
    public String[] getDefaultMethodIndexedProp2() {
        return new String[] { ARRAY_PROP_2_VALUE_0 };
    }

    /**
     * There's a matching non-indexed reader method in the base class, but as this is indexed, it takes over. 
     */
    public String getDefaultMethodIndexedProp3(int index) {
        return INDEXED_PROP_3_VALUE;
    }
    
    public int getDefaultMethodNotAnIndexedProp() {
        return NOT_AN_INDEXED_PROP_VALUE;
    }

    /** Actually, this will be indexed if the default method support is off. */
    public String getDefaultMethodNotAnIndexedProp2(int index) {
        return NOT_AN_INDEXED_PROP_2_VALUE;
    }
    
    /** Actually, this will be indexed if the default method support is off. */
    public int getDefaultMethodNotAnIndexedProp3(int index) {
        return NOT_AN_INDEXED_PROP_3_VALUE;
    }
    
    public String getIndexedProp4(int index) {
        return INDEXED_PROP_4_VALUE;
    }
    
    public String normalAction() {
        return NORMAL_ACTION_RETURN_VALUE;
    }
    
    public String overriddenDefaultMethodAction() {
        return OVERRIDDEN_DEFAULT_METHOD_ACTION_RETURN_VALUE;
    }

}
