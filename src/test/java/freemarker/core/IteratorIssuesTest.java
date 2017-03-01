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
package freemarker.core;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.Test;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.test.TemplateTest;

public class IteratorIssuesTest extends TemplateTest {
    
    private static final String FTL_HAS_CONTENT_AND_LIST
            = "<#if it?hasContent><#list it as i>${i}</#list><#else>empty</#if>";
    private static final String OUT_HAS_CONTENT_AND_LIST_ABC = "abc";
    private static final String OUT_HAS_CONTENT_AND_LIST_EMPTY = "empty";
    
    private static final String FTL_LIST_AND_HAS_CONTENT
            = "<#list it as i>${i}${it?hasContent?then('+', '-')}</#list>";
    private static final String OUT_LIST_AND_HAS_CONTENT_BW_WRONG = "a+b+c+";
    private static final String OUT_LIST_AND_HAS_CONTENT_BW_GOOD = "a+b+c-";

    @Test
    public void testHasContentAndListDOW230() throws Exception {
        addToDataModel("it", getDOW230().wrap(getAbcIt()));
        assertOutput(FTL_HAS_CONTENT_AND_LIST, OUT_HAS_CONTENT_AND_LIST_ABC);
        
        addToDataModel("it", getDOW230().wrap(getEmptyIt()));
        assertOutput(FTL_HAS_CONTENT_AND_LIST, OUT_HAS_CONTENT_AND_LIST_EMPTY);
    }

    @Test
    public void testHasContentAndListDOW2323() throws Exception {
        addToDataModel("it", getDOW2323().wrap(getAbcIt()));
        assertOutput(FTL_HAS_CONTENT_AND_LIST, OUT_HAS_CONTENT_AND_LIST_ABC);
        
        addToDataModel("it", getDOW2323().wrap(getEmptyIt()));
        assertOutput(FTL_HAS_CONTENT_AND_LIST, OUT_HAS_CONTENT_AND_LIST_EMPTY);
    }

    @Test
    public void testHasContentAndListBW230() throws Exception {
        addToDataModel("it", getBW230().wrap(getAbcIt()));
        assertOutput(FTL_HAS_CONTENT_AND_LIST, OUT_HAS_CONTENT_AND_LIST_ABC);
        
        addToDataModel("it", getBW230().wrap(getEmptyIt()));
        assertOutput(FTL_HAS_CONTENT_AND_LIST, "");
    }
    
    @Test
    public void testHasContentAndListBW2323() throws Exception {
        addToDataModel("it", getBW2323().wrap(getAbcIt()));
        assertOutput(FTL_HAS_CONTENT_AND_LIST, OUT_HAS_CONTENT_AND_LIST_ABC);
        
        addToDataModel("it", getBW230().wrap(getEmptyIt()));
        assertOutput(FTL_HAS_CONTENT_AND_LIST, "");
    }
    
    @Test
    public void testHasContentAndListBW2324() throws Exception {
        addToDataModel("it", getBW2324().wrap(getAbcIt()));
        assertOutput(FTL_HAS_CONTENT_AND_LIST, OUT_HAS_CONTENT_AND_LIST_ABC);
        
        addToDataModel("it", getBW2324().wrap(getEmptyIt()));
        assertOutput(FTL_HAS_CONTENT_AND_LIST, OUT_HAS_CONTENT_AND_LIST_EMPTY);
    }
    
    @Test
    public void testListAndHasContentDOW230() throws Exception {
        addToDataModel("it", getDOW230().wrap(getAbcIt()));
        assertErrorContains(FTL_LIST_AND_HAS_CONTENT, "can be listed only once");
    }

    @Test
    public void testListAndHasContentDOW2323() throws Exception {
        addToDataModel("it", getDOW2323().wrap(getAbcIt()));
        assertErrorContains(FTL_LIST_AND_HAS_CONTENT, "can be listed only once");
    }

    @Test
    public void testListAndHasContentBW230() throws Exception {
        addToDataModel("it", getBW230().wrap(getAbcIt()));
        assertOutput(FTL_LIST_AND_HAS_CONTENT, OUT_LIST_AND_HAS_CONTENT_BW_WRONG);
    }

    @Test
    public void testListAndHasContentBW2323() throws Exception {
        addToDataModel("it", getBW2323().wrap(getAbcIt()));
        assertOutput(FTL_LIST_AND_HAS_CONTENT, OUT_LIST_AND_HAS_CONTENT_BW_WRONG);
    }

    @Test
    public void testListAndHasContentBW2324() throws Exception {
        addToDataModel("it", getBW2324().wrap(getAbcIt()));
        assertOutput(FTL_LIST_AND_HAS_CONTENT, OUT_LIST_AND_HAS_CONTENT_BW_GOOD);
    }
    
    private Iterator getAbcIt() {
        return Arrays.asList(new String[] { "a", "b", "c" }).iterator();
    }

    private Iterator getEmptyIt() {
        return Arrays.asList(new String[] {  }).iterator();
    }
    
    private DefaultObjectWrapper getDOW230() {
        return new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_0).build();
    }
    
    private DefaultObjectWrapper getDOW2323() {
        return new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_23).build();
    }

    private BeansWrapper getBW230() {
        return new BeansWrapperBuilder(Configuration.VERSION_2_3_0).build();
    }

    private BeansWrapper getBW2323() {
        return new BeansWrapperBuilder(Configuration.VERSION_2_3_23).build();
    }

    private BeansWrapper getBW2324() {
        return new BeansWrapperBuilder(Configuration.VERSION_2_3_24).build();
    }
    
}
