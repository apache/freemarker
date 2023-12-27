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
package org.apache.freemarker.spring.web.view;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.TagSupport;

import java.io.IOException;

/**
 * Simple Example JSP Tag Library for unit test.
 */
@SuppressWarnings("serial")
public class EchoTag extends TagSupport {

    private String message;

    public EchoTag() {
        super();
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public int doEndTag() throws JspException {
        try {
            pageContext.getOut().print(message);
        } catch (IOException e) {
            throw new JspException("Message printing error.", e);
        }
        return EVAL_PAGE;
    }

    @Override
    public void release() {
        this.message = null;
    }

}
