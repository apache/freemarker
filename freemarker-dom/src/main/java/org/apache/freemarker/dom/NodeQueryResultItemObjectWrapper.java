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

package org.apache.freemarker.dom;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.ObjectWrappingException;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelAdapter;
import org.apache.freemarker.core.model.WrappingTemplateModel;
import org.apache.freemarker.core.model.impl.SimpleDate;
import org.apache.freemarker.core.model.impl.SimpleNumber;
import org.apache.freemarker.core.model.impl.SimpleScalar;
import org.w3c.dom.Node;

/**
 * Used for wrapping query result items (such as XPath query result items). Because {@link NodeModel} and such aren't
 * {@link WrappingTemplateModel}-s, we can't use the actual {@link ObjectWrapper} from the {@link Environment}, also,
 * even if we could, it might not be the right thing to do, because that  {@link ObjectWrapper} might not even wrap
 * {@link Node}-s via {@link NodeModel}.
 */
class NodeQueryResultItemObjectWrapper implements ObjectWrapper {

    static final NodeQueryResultItemObjectWrapper INSTANCE = new NodeQueryResultItemObjectWrapper();

    private NodeQueryResultItemObjectWrapper() {
        //
    }

    @Override
    public TemplateModel wrap(Object obj) throws ObjectWrappingException {
        if (obj instanceof NodeModel) {
            return (NodeModel) obj;
        }
        if (obj instanceof Node) {
            return NodeModel.wrap((Node) obj);
        } else {
            if (obj == null) {
                return null;
            }
            if (obj instanceof TemplateModel) {
                return (TemplateModel) obj;
            }
            if (obj instanceof TemplateModelAdapter) {
                return ((TemplateModelAdapter) obj).getTemplateModel();
            }

            if (obj instanceof String) {
                return new SimpleScalar((String) obj);
            }
            if (obj instanceof Number) {
                return new SimpleNumber((Number) obj);
            }
            if (obj instanceof Boolean) {
                return obj.equals(Boolean.TRUE) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
            }
            if (obj instanceof java.util.Date) {
                if (obj instanceof java.sql.Date) {
                    return new SimpleDate((java.sql.Date) obj);
                }
                if (obj instanceof java.sql.Time) {
                    return new SimpleDate((java.sql.Time) obj);
                }
                if (obj instanceof java.sql.Timestamp) {
                    return new SimpleDate((java.sql.Timestamp) obj);
                }
                return new SimpleDate((java.util.Date) obj, TemplateDateModel.UNKNOWN);
            }
            throw new ObjectWrappingException("Don't know how to wrap a W3C DOM query result item of this type: "
                    + obj.getClass().getName());
        }
    }
}
