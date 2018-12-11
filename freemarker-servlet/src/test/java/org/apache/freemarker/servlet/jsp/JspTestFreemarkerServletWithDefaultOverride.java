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

package org.apache.freemarker.servlet.jsp;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.freemarker.servlet.jsp.TaglibFactory.ClasspathMetaInfTldSource;
import org.apache.freemarker.servlet.jsp.TaglibFactory.MetaInfTldSource;
import org.apache.freemarker.servlet.jsp.TaglibFactory.WebInfPerLibJarMetaInfTldSource;

import com.google.common.collect.ImmutableList;

public class JspTestFreemarkerServletWithDefaultOverride extends JspTestFreemarkerServlet {

    @Override
    protected List<String> createDefaultClassPathTlds() {
        return Collections.singletonList("/org/apache/freemarker/servlet/jsp/tldDiscovery-ClassPathTlds-1.tld");
    }

    @Override
    protected List<MetaInfTldSource> createDefaultMetaInfTldSources() {
        return ImmutableList.of(
                WebInfPerLibJarMetaInfTldSource.INSTANCE,
                new ClasspathMetaInfTldSource(Pattern.compile(".*displaytag.*\\.jar$")),
                new ClasspathMetaInfTldSource(Pattern.compile(".*")));
    }

}
