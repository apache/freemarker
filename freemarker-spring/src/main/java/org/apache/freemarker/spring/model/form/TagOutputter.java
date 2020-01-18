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

package org.apache.freemarker.spring.model.form;

import java.io.IOException;
import java.io.Writer;
import java.util.Stack;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.springframework.util.StringUtils;

/**
 * Utility to writing HTML content.
 */
class TagOutputter {

    private final Environment env;

    private final Writer out;

    private final Stack<TagEntry> tagStack = new Stack<>();

    public TagOutputter(final Environment env, final Writer out) {
        this.env = env;
        this.out = out;
    }

    public Environment getEnvironment() {
        return env;
    }

    public void beginTag(String tagName) throws TemplateException, IOException {
        if (!tagStack.isEmpty()) {
            closeAndMarkAsBlockTag();
        }

        tagStack.push(new TagEntry(tagName));

        out.write('<');
        out.write(tagName);
    }

    public void writeAttribute(String attrName, String attrValue) throws TemplateException, IOException {
        final TagEntry current = tagStack.peek();

        if (current.isBlockTag()) {
            throw new TemplateException("Opening tag has already been closed.");
        }

        out.write(' ');
        out.write(attrName);
        out.write("=\"");
        out.write(attrValue);
        out.write("\"");
    }

    public void writeOptionalAttributeValue(String attrName, String attrValue) throws TemplateException, IOException {
        if (StringUtils.hasText(attrValue)) {
            writeAttribute(attrName, attrValue);
        }
    }

    public void appendValue(String value) throws TemplateException, IOException {
        if (tagStack.isEmpty()) {
            throw new IllegalStateException("No open tag entry available.");
        }

        closeAndMarkAsBlockTag();

        out.write(value);
    }

    public void forceBlock() throws TemplateException {
        TagEntry current = tagStack.peek();

        if (current.isBlockTag()) {
            return;
        }

        closeAndMarkAsBlockTag();
    }

    public void endTag() throws TemplateException, IOException {
        endTag(false);
    }

    public void endTag(boolean enforceClosingTag) throws TemplateException, IOException {
        if (tagStack.isEmpty()) {
            throw new IllegalStateException("No opening tag available.");
        }

        boolean renderClosingTag = true;
        TagEntry current = tagStack.peek();

        if (!current.isBlockTag()) {
            if (enforceClosingTag) {
                out.write('>');
            } else {
                out.write("/>");
                renderClosingTag = false;
            }
        }

        if (renderClosingTag) {
            out.write("</");
            out.write(current.getTagName());
            out.write(">");
        }

        tagStack.pop();
    }

    private void closeAndMarkAsBlockTag() throws TemplateException {
        TagEntry current = tagStack.peek();

        if (!current.isBlockTag()) {
            current.markAsBlockTag();

            try {
                out.write(">");
            } catch (IOException e) {
                throw new TemplateException("Failed to write output.", e);
            }
        }
    }

    private static class TagEntry {

        private final String tagName;

        private boolean blockTag;

        public TagEntry(String tagName) {
            this.tagName = tagName;
        }

        public String getTagName() {
            return this.tagName;
        }

        public void markAsBlockTag() {
            this.blockTag = true;
        }

        public boolean isBlockTag() {
            return this.blockTag;
        }
    }

}
