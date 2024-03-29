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

package freemarker.ext.jsp;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspEngineInfo;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;

class FreeMarkerJspFactory extends JspFactory {
    private static final String SPECIFICATION_VERSION = "2.2";

    // This still ends with "21", just in case someone used that key for some workaround.
    private static final String JSPCTX_KEY = "freemarker.ext.jsp.FreeMarkerJspFactory21#jspAppContext";

    String getSpecificationVersion() {
        return SPECIFICATION_VERSION;
    }
    
    @Override
    public JspEngineInfo getEngineInfo() {
        return new JspEngineInfo() {
            @Override
            public String getSpecificationVersion() {
                return FreeMarkerJspFactory.this.getSpecificationVersion();
            }
        };
    }

    @Override
    public PageContext getPageContext(Servlet servlet, ServletRequest request, 
            ServletResponse response, String errorPageURL, 
            boolean needsSession, int bufferSize, boolean autoFlush) {
        // This is never meant to be called. JSP pages compiled to Java 
        // bytecode use this API, but in FreeMarker, we're running templates,
        // and not JSP pages precompiled to bytecode, therefore we have no use
        // for this API.
        throw new UnsupportedOperationException();
    }

    @Override
    public void releasePageContext(PageContext ctx) {
        // This is never meant to be called. JSP pages compiled to Java 
        // bytecode use this API, but in FreeMarker, we're running templates,
        // and not JSP pages precompiled to bytecode, therefore we have no use
        // for this API.
        throw new UnsupportedOperationException();
    }

    @Override
    public JspApplicationContext getJspApplicationContext(ServletContext ctx) {
        JspApplicationContext jspctx = (JspApplicationContext) ctx.getAttribute(
                JSPCTX_KEY);
        if (jspctx == null) {
            synchronized (ctx) {
                jspctx = (JspApplicationContext) ctx.getAttribute(JSPCTX_KEY);
                if (jspctx == null) {
                    jspctx = new FreeMarkerJspApplicationContext();
                    ctx.setAttribute(JSPCTX_KEY, jspctx);
                }
            }
        }
        return jspctx;
    }

}