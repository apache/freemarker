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
import java.io.Writer;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.CallPlace;
import org.apache.freemarker.core.model.TemplateModel;

public class PositionalVarargsOnlyDirective extends TestTemplateDirectiveModel {

    public static final PositionalVarargsOnlyDirective INSTANCE = new PositionalVarargsOnlyDirective();

    private static final ArgumentArrayLayout ARGS_LAYOUT = ArgumentArrayLayout.create(
            0, true,
            null, false);

    private PositionalVarargsOnlyDirective() {
        //
    }

    @Override
    public void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
            throws TemplateException, IOException {
        out.write("#pvo(");
        printParam("pVarargs", args[ARGS_LAYOUT.getPositionalVarargsArgumentIndex()], out, true);
        out.write(")");
    }

    @Override
    public ArgumentArrayLayout getArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }


    @Override
    public boolean isNestedContentSupported() {
        return false;
    }

}
