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

package org.apache.freemarker.core.templatesuite.models;

import java.util.List;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core._CallableUtils;
import org.apache.freemarker.core.model.TemplateMethodModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.impl.SimpleScalar;

/**
 * Testcase to see how FreeMarker deals with multiple Template models.
 */
public class MultiModel2 implements TemplateScalarModel, TemplateMethodModel {

    /**
     * Returns the scalar's value as a String.
     *
     * @return the String value of this scalar.
     */
    @Override
    public String getAsString() {
        return "Model2 is alive!";
    }

    /**
     * Executes a method call.
     *
     * @param args a <tt>List</tt> of <tt>String</tt> objects containing the values
     * of the arguments passed to the method.
     * @return the <tt>TemplateModel</tt> produced by the method, or null.
     */
    @Override
    public TemplateModel execute(List<? extends TemplateModel> args) throws TemplateException {
        StringBuilder  aResults = new StringBuilder( "Arguments are:<br />" );
        for (int i = 0; i < args.size(); i++) {
            TemplateModel arg = args.get(i);
            aResults.append(_CallableUtils.castArgToString(arg, i));
            aResults.append("<br />");
        }

        return new SimpleScalar( aResults.toString() );
    }
}
