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

import java.util.Arrays;
import java.util.Iterator;

import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapperBuilder;
import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

public class IteratorIssuesTest extends TemplateTest {

    private static final DefaultObjectWrapper OW = new DefaultObjectWrapperBuilder(Configuration.VERSION_3_0_0).build();

    private static final String FTL_HAS_CONTENT_AND_LIST
            = "<#if it?hasContent><#list it as i>${i}</#list><#else>empty</#if>";
    private static final String OUT_HAS_CONTENT_AND_LIST_ABC = "abc";
    private static final String OUT_HAS_CONTENT_AND_LIST_EMPTY = "empty";

    private static final String FTL_LIST_AND_HAS_CONTENT
            = "<#list it as i>${i}${it?hasContent?then('+', '-')}</#list>";
    private static final String OUT_LIST_AND_HAS_CONTENT_BW_GOOD = "a+b+c-";

    @Test
    public void testHasContentAndList() throws Exception {
        addToDataModel("it", OW.wrap(getAbcIt()));
        assertOutput(FTL_HAS_CONTENT_AND_LIST, OUT_HAS_CONTENT_AND_LIST_ABC);

        addToDataModel("it", OW.wrap(getEmptyIt()));
        assertOutput(FTL_HAS_CONTENT_AND_LIST, OUT_HAS_CONTENT_AND_LIST_EMPTY);
    }

    @Test
    public void testListAndHasContent() throws Exception {
        addToDataModel("it", OW.wrap(getAbcIt()));
        assertErrorContains(FTL_LIST_AND_HAS_CONTENT, "can be listed only once");
    }

    private Iterator getAbcIt() {
        return Arrays.asList(new String[] { "a", "b", "c" }).iterator();
    }

    private Iterator getEmptyIt() {
        return Arrays.asList(new String[] {  }).iterator();
    }

}
