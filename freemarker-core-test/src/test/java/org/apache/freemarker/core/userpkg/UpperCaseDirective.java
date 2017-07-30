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

package org.apache.freemarker.core.userpkg;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.CallPlace;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateModel;

public class UpperCaseDirective implements TemplateDirectiveModel {

    public static final UpperCaseDirective INSTANCE = new UpperCaseDirective();

    private UpperCaseDirective() {
        //
    }

    @Override
    public void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
            throws TemplateException, IOException {
        StringWriter capturingOut = new StringWriter();
        callPlace.executeNestedContent(null, capturingOut, env);
        out.write(capturingOut.toString().toUpperCase());
    }

    @Override
    public ArgumentArrayLayout getArgumentArrayLayout() {
        return ArgumentArrayLayout.PARAMETERLESS;
    }

    @Override
    public boolean isNestedContentSupported() {
        return true;
    }

}
