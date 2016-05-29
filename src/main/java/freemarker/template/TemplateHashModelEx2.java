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
package freemarker.template;

/**
 * Adds key-value pair listing capability to {@link TemplateHashModelEx}. While in many cases that can also be achieved
 * with {@link #keys()} and then {@link #get(String)}, that has some problems. One is that {@link #get(String)} only
 * accepts string keys, while {@link #keys()} can return non-string keys too. The other is that {@link #keys()} and then
 * {@link #get(String)} for each key can be slower than listing the key-value pairs in one go.
 * 
 * @since 2.3.25 
 */
public interface TemplateHashModelEx2 extends TemplateHashModelEx {

    KeyValuePairIterator keyValuePairIterator();
    
    interface KeyValuePair {
        TemplateModel getKey() throws TemplateModelException;
        TemplateModel getValue() throws TemplateModelException;
    }
    
    interface KeyValuePairIterator {
        boolean hasNext() throws TemplateModelException;
        KeyValuePair next() throws TemplateModelException;
    }
    
}
