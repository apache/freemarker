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

package org.apache.freemarker.spring.model;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * DOM Element Attribute value matcher.
 * This matches if there's an attribute having the same value as {@code attrName}.
 */
public class ElementAttributeMatcher extends BaseMatcher<Node> {

    private String attrName;
    private String expectedAttrValue;
    private String actualAttrValue;

    public ElementAttributeMatcher(String attrName, String expectedAttrValue) {
        if (attrName == null) {
            throw new IllegalArgumentException("Attribute name must not be null.");
        }

        this.attrName = attrName;
        this.expectedAttrValue = expectedAttrValue;
    }

    @Override
    public boolean matches(Object item) {
        actualAttrValue = getAttributeValue((Node) item, attrName);
        return (expectedAttrValue == null) ? (actualAttrValue == null) : expectedAttrValue.equals(actualAttrValue);
    }

    @Override
    public void describeTo(Description description) {
        if (expectedAttrValue == null) {
            description.appendText("The attribute value (@" + attrName + ") should be null");
        } else {
            description.appendText("The attribute value (@" + attrName + ") should be '" + expectedAttrValue + "'");
        }
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        description.appendText("was ").appendValue(actualAttrValue);
    }

    private String getAttributeValue(final Node node, final String attrName) {
        final NamedNodeMap attrsMap = node.getAttributes();

        if (attrsMap == null) {
            return null;
        }

        final int length = attrsMap.getLength();

        for (int i = 0; i < length; i++) {
            final Attr attr = (Attr) attrsMap.item(i);

            if (attrName.equals(attr.getName())) {
                return attr.getValue();
            }
        }

        return null;
    }
}
