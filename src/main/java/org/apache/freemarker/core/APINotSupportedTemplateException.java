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

package org.apache.freemarker.core;

import org.apache.freemarker.core.model.TemplateModel;

/**
 * Thrown when {@code ?api} is not supported by a value.
 */
class APINotSupportedTemplateException extends TemplateException {

    APINotSupportedTemplateException(Environment env, ASTExpression blamedExpr, TemplateModel model) {
        super(null, env, blamedExpr, buildDescription(env, blamedExpr, model));
    }

    protected static _ErrorDescriptionBuilder buildDescription(Environment env, ASTExpression blamedExpr,
            TemplateModel tm) {
        final _ErrorDescriptionBuilder desc = new _ErrorDescriptionBuilder(
                "The value doesn't support ?api. See requirements in the FreeMarker Manual. ("
                + "FTL type: ", new _DelayedFTLTypeDescription(tm),
                ", TemplateModel class: ", new _DelayedShortClassName(tm.getClass()),
                ", ObjectWapper: ", new _DelayedToString(env.getObjectWrapper()), ")"
        ).blame(blamedExpr);

        if (blamedExpr.isLiteral()) {
            desc.tip("Only adapted Java objects can possibly have API, not values created inside templates.");
        }

        return desc;
    }

}
