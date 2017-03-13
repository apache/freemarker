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

import static org.junit.Assert.*;

import org.junit.Test;

public class GetPropertyNameFromReaderMethodNameTest {
    
    @Test
    public void test() {
       assertEquals("foo", _MethodUtil.getBeanPropertyNameFromReaderMethodName("getFoo", String.class)); 
       assertEquals("fo", _MethodUtil.getBeanPropertyNameFromReaderMethodName("getFo", String.class)); 
       assertEquals("f", _MethodUtil.getBeanPropertyNameFromReaderMethodName("getF", String.class));
       
       assertEquals("FO", _MethodUtil.getBeanPropertyNameFromReaderMethodName("getFO", String.class)); 
       assertEquals("FOo", _MethodUtil.getBeanPropertyNameFromReaderMethodName("getFOo", String.class)); 
       assertEquals("FOO", _MethodUtil.getBeanPropertyNameFromReaderMethodName("getFOO", String.class));
       assertEquals("fooBar", _MethodUtil.getBeanPropertyNameFromReaderMethodName("getFooBar", String.class));
       assertEquals("FOoBar", _MethodUtil.getBeanPropertyNameFromReaderMethodName("getFOoBar", String.class));

       assertEquals("foo", _MethodUtil.getBeanPropertyNameFromReaderMethodName("getFoo", boolean.class)); 
       assertEquals("foo", _MethodUtil.getBeanPropertyNameFromReaderMethodName("isFoo", boolean.class)); 
       assertNull(_MethodUtil.getBeanPropertyNameFromReaderMethodName("isFoo", Boolean.class));
       assertNull(_MethodUtil.getBeanPropertyNameFromReaderMethodName("isFoo", String.class));
       assertEquals("f", _MethodUtil.getBeanPropertyNameFromReaderMethodName("isF", boolean.class)); 
       
       assertEquals("foo", _MethodUtil.getBeanPropertyNameFromReaderMethodName("getfoo", String.class)); 
       assertEquals("fo", _MethodUtil.getBeanPropertyNameFromReaderMethodName("getfo", String.class)); 
       assertEquals("f", _MethodUtil.getBeanPropertyNameFromReaderMethodName("getf", String.class));
       assertEquals("_f", _MethodUtil.getBeanPropertyNameFromReaderMethodName("get_f", String.class));
       assertEquals("_F", _MethodUtil.getBeanPropertyNameFromReaderMethodName("get_F", String.class));
       assertEquals("_", _MethodUtil.getBeanPropertyNameFromReaderMethodName("get_", String.class));
       assertEquals("1f", _MethodUtil.getBeanPropertyNameFromReaderMethodName("get1f", String.class));
       assertEquals("1", _MethodUtil.getBeanPropertyNameFromReaderMethodName("get1", String.class));
       assertEquals("1F", _MethodUtil.getBeanPropertyNameFromReaderMethodName("get1F", String.class));
       assertEquals("foo", _MethodUtil.getBeanPropertyNameFromReaderMethodName("isfoo", boolean.class)); 
       
       assertNull(_MethodUtil.getBeanPropertyNameFromReaderMethodName("get", String.class));
       assertNull(_MethodUtil.getBeanPropertyNameFromReaderMethodName("is", boolean.class));
       assertNull(_MethodUtil.getBeanPropertyNameFromReaderMethodName("f", String.class));
    }

}
