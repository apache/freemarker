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

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateModel;

import javax.servlet.jsp.PageContext;

/**
 */
class PageContextFactory {
    static FreeMarkerPageContext getCurrentPageContext() throws TemplateException {
        Environment env = Environment.getCurrentEnvironment();
        TemplateModel pageContextModel = env.getGlobalVariable(PageContext.PAGECONTEXT);
        if (pageContextModel instanceof FreeMarkerPageContext) {
            return (FreeMarkerPageContext) pageContextModel;
        }
        FreeMarkerPageContext pageContext = new FreeMarkerPageContext();
        env.setGlobalVariable(PageContext.PAGECONTEXT, pageContext);
        return pageContext;
    }
    
}
