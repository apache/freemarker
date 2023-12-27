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

package org.apache.freemarker.servlet.jsp.taglibmembers;

import jakarta.servlet.jsp.JspContext;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.tagext.JspFragment;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;

import java.io.IOException;

public class TestSimpleTag extends SimpleTagSupport {
    private int bodyLoopCount = 1;
    private String name;
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setBodyLoopCount(int bodyLoopCount) {
        this.bodyLoopCount = bodyLoopCount;
    }
    
    @Override
    public void doTag() throws JspException, IOException {
        JspContext ctx = getJspContext();
        JspWriter w = ctx.getOut();
        w.println("enter TestSimpleTag " + name);
        JspFragment f = getJspBody();
        for (int i = 0; i < bodyLoopCount; ++i) {
            w.println("invoking body i=" + i);
            f.invoke(w);
        }
        w.println("exit TestSimpleTag " + name);
    }
}