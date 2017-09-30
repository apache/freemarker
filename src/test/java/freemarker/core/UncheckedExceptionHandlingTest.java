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
package freemarker.core;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.FilterWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.Version;
import freemarker.test.TemplateTest;

public class UncheckedExceptionHandlingTest extends TemplateTest {
    
    @Override
    protected Object createDataModel() {
        return ImmutableMap.of(
                "f", MyErronousFunction.INSTANCE,
                "d", MyErronousDirective.INSTANCE,
                "fd", MyFilterDirective.INSTANCE);
    }

    @Test
    public void testBackwardCompatible() {
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_26);
        assertErrorContains("${f()}", MyUncheckedException.class);
        assertErrorContains("<@d />", MyUncheckedException.class);
        assertErrorContains("${f('NPE')}", NullPointerException.class);
        assertErrorContains("<@d type='NPE' />", NullPointerException.class);
    }

    @Test
    public void testMostlyBackwardCompatible() {
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_27);
        assertErrorContains("${f()}", MyUncheckedException.class);
        assertErrorContains("<@d />", MyUncheckedException.class);
        assertThat(
                assertErrorContains("${f('NPE')}", TemplateException.class, "thrown an unchecked").getCause(),
                instanceOf(NullPointerException.class));
        assertThat(
                assertErrorContains("<@d type='NPE' />", TemplateException.class, "thrown an unchecked").getCause(),
                instanceOf(NullPointerException.class));
    }


    @Test
    public void testNoBackwardCompatible() {
        Configuration cfg = getConfiguration();
        cfg.setWrapUncheckedExceptions(true);
        
        for (Version ici : new Version[] { Configuration.VERSION_2_3_26, Configuration.VERSION_2_3_27 }) {
            cfg.setIncompatibleImprovements(ici);
            
            assertThat(
                    assertErrorContains("${f()}", TemplateException.class, "thrown an unchecked").getCause(),
                    instanceOf(MyUncheckedException.class));
            assertThat(
                    assertErrorContains("<@d />", TemplateException.class, "thrown an unchecked").getCause(),
                    instanceOf(MyUncheckedException.class));
            assertThat(
                    assertErrorContains("${f('NPE')}", TemplateException.class, "thrown an unchecked").getCause(),
                    instanceOf(NullPointerException.class));
            assertThat(
                    assertErrorContains("<@d type='NPE' />", TemplateException.class, "thrown an unchecked").getCause(),
                    instanceOf(NullPointerException.class));
        }
    }

    @Test
    public void testFlowControlWorks() throws IOException, TemplateException {
        Configuration cfg = getConfiguration();
        for (boolean wrapUnchecked : new boolean[] { false, true }) {
            cfg.setWrapUncheckedExceptions(wrapUnchecked);
            
            assertOutput("<#list 1..2 as i>a<@fd>b<#break>c</@>d</#list>.", "ab.");
            assertOutput("<#list 1..2 as i>a<@fd>b<#continue>c</@>d</#list>.", "abab.");
            assertOutput("<#function f()><@fd><#return 1></@></#function>${f()}.", "1.");
        }
    }
    
    public static class MyErronousFunction implements TemplateMethodModelEx {
        private static final MyErronousFunction INSTANCE = new MyErronousFunction();

        public Object exec(List arguments) throws TemplateModelException {
            if (!arguments.isEmpty() && equalsNPE((TemplateModel) arguments.get(0))) {
                throw new NullPointerException();
            } else {
                throw new MyUncheckedException();
            }
        }

    }

    public static class MyErronousDirective implements TemplateDirectiveModel {
        private static final MyErronousDirective INSTANCE = new MyErronousDirective();
        
        public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
                throws TemplateException, IOException {
            if (equalsNPE((TemplateModel) params.get("type"))) {
                throw new NullPointerException();
            } else {
                throw new MyUncheckedException();
            }
        }

    }

    public static class MyFilterDirective implements TemplateDirectiveModel {
        private static final MyFilterDirective INSTANCE = new MyFilterDirective();
        
        public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
                throws TemplateException, IOException {
            body.render(new FilterWriter(env.getOut()) { });
        }

    }
    
    public static class MyUncheckedException extends RuntimeException {
        //
    }
    
    private static boolean equalsNPE(TemplateModel tm) throws TemplateModelException {
        return (tm instanceof TemplateScalarModel) && "NPE".equals(((TemplateScalarModel) tm).getAsString());
    }

}
