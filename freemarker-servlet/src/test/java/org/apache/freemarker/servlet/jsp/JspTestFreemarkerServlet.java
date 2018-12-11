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

import javax.servlet.ServletContext;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.servlet.FreemarkerServlet;

public class JspTestFreemarkerServlet extends FreemarkerServlet {

    static volatile boolean emulateNoUrlToFileConversions;
    static volatile boolean emulateNoJarURLConnections;
    static volatile boolean emulateJarEntryUrlOpenStreamFails;
    
    static void resetToDefaults() {
        emulateNoUrlToFileConversions = false;
        emulateNoJarURLConnections = false;
        emulateJarEntryUrlOpenStreamFails = false;
    }

    @Override
    protected TaglibFactory createTaglibFactory(ObjectWrapper objectWrapper, ServletContext servletContext)
            throws TemplateException {
        final TaglibFactory taglibFactory = super.createTaglibFactory(objectWrapper, servletContext);
        taglibFactory.test_emulateNoUrlToFileConversions = emulateNoUrlToFileConversions;
        taglibFactory.test_emulateNoJarURLConnections = emulateNoJarURLConnections;
        taglibFactory.test_emulateJarEntryUrlOpenStreamFails = emulateJarEntryUrlOpenStreamFails;
        return taglibFactory;
    }

}
