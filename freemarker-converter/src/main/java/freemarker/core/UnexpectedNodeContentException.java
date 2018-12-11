/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package freemarker.core;

import org.apache.freemarker.converter.ConverterException;
import org.apache.freemarker.core.util._StringUtils;

public class UnexpectedNodeContentException extends ConverterException {
    public UnexpectedNodeContentException(TemplateObject node, String errorMessage, Object msgParam) {
        super("Unexpected AST content for " + _StringUtils.jQuote(node.getNodeTypeSymbol()) + " node (class: "
                + node.getClass().getName() + "):\n"
                + renderMessage(errorMessage, msgParam),
                node.getBeginLine(), node.getBeginColumn());
    }

    private static String renderMessage(String errorMessage, Object msgParam) {
        int substIdx = errorMessage.indexOf("{}");
        if (substIdx == -1) {
            return errorMessage;
        }
        return errorMessage.substring(0, substIdx) + formatParam(msgParam) + errorMessage.substring(substIdx + 2);
    }

    private static String formatParam(Object param) {
        if (param == null) {
            return "null";
        }
        if (param instanceof String || param instanceof Character) {
            return _StringUtils.jQuote(param);
        }
        return param.toString();
    }

}
