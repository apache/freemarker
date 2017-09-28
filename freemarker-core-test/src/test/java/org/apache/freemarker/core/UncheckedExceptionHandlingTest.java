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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Writer;

import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.CallableUtils;
import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class UncheckedExceptionHandlingTest extends TemplateTest {
    
    @Override
    protected Object createDataModel() {
        return ImmutableMap.of(
                "f", MyErronousFunction.INSTANCE,
                "d", MyErronousDirective.INSTANCE,
                "fd", MyFilterDirective.INSTANCE);
    }

    @Test
    public void test() {
        assertThat(
                assertErrorContains("${f()}", TemplateException.class, "unchecked exception").getCause(),
                instanceOf(MyUncheckedException.class));
        assertThat(
                assertErrorContains("<@d />", TemplateException.class, "unchecked exception").getCause(),
                instanceOf(MyUncheckedException.class));
        assertThat(
                assertErrorContains("${f('NPE')}", TemplateException.class, "unchecked exception").getCause(),
                instanceOf(NullPointerException.class));
        assertThat(
                assertErrorContains("<@d 'NPE' />", TemplateException.class, "unchecked exception").getCause(),
                instanceOf(NullPointerException.class));
    }

    @Test
    public void testFlowControlWorks() throws IOException, TemplateException {
        assertOutput("<#list 1..2 as i>a<@fd>b<#break>c</@>d</#list>.", "ab.");
        assertOutput("<#list 1..2 as i>a<@fd>b<#continue>c</@>d</#list>.", "abab.");
        assertOutput("<#function f()><@fd><#return 1></@></#function>${f()}.", "1.");
    }
    
    public static class MyErronousFunction implements TemplateFunctionModel {
        private static final MyErronousFunction INSTANCE = new MyErronousFunction();

        @Override
        public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                throws TemplateException {
            if ("NPE".equals(CallableUtils.getOptionalStringArgument(args, 0, this))) {
                throw new NullPointerException();
            } else {
                throw new MyUncheckedException();
            }
        }

        @Override
        public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
            return ArgumentArrayLayout.SINGLE_POSITIONAL_PARAMETER;
        }

    }

    public static class MyErronousDirective implements TemplateDirectiveModel {
        private static final MyErronousDirective INSTANCE = new MyErronousDirective();

        @Override
        public void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
                throws TemplateException {
            if ("NPE".equals(CallableUtils.getOptionalStringArgument(args, 0, this))) {
                throw new NullPointerException();
            } else {
                throw new MyUncheckedException();
            }
        }

        @Override
        public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
            return ArgumentArrayLayout.SINGLE_POSITIONAL_PARAMETER;
        }

        @Override
        public boolean isNestedContentSupported() {
            return false;
        }
        
    }

    public static class MyFilterDirective implements TemplateDirectiveModel {
        private static final MyFilterDirective INSTANCE = new MyFilterDirective();
        
        @Override
        public void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
                throws TemplateException, IOException {
            callPlace.executeNestedContent(null, out, env);
        }

        @Override
        public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
            return ArgumentArrayLayout.PARAMETERLESS;
        }

        @Override
        public boolean isNestedContentSupported() {
            return true;
        }
        
    }
    
    public static class MyUncheckedException extends RuntimeException {
        //
    }
    
}