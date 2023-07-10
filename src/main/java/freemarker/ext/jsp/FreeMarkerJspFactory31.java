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

import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.JspApplicationContext;

/**
 */
class FreeMarkerJspFactory31 extends JakartaFreeMarkerJspFactory {
    private static final String JSPCTX_KEY =  
        FreeMarkerJspFactory31.class.getName() + "#jspAppContext";

    @Override
    protected String getSpecificationVersion() {
        return "3.1";
    }
    
    @Override
    public JspApplicationContext getJspApplicationContext(ServletContext ctx) {
        JspApplicationContext jspctx = (JspApplicationContext) ctx.getAttribute(
                JSPCTX_KEY);
        if (jspctx == null) {
            synchronized (ctx) {
                jspctx = (JspApplicationContext) ctx.getAttribute(JSPCTX_KEY);
                if (jspctx == null) {
                    jspctx = new JakartaFreeMarkerJspApplicationContext();
                    ctx.setAttribute(JSPCTX_KEY, jspctx);
                }
            }
        }
        return jspctx;
    }
}