/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.core.ThreadInterruptionSupportTemplatePostProcessor.TemplateProcessingThreadInterruptedException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.utility.NullWriter;

public class TheadInterruptingSupportTest {
    
    private static final Logger LOG = LoggerFactory.getLogger(TheadInterruptingSupportTest.class);

    private static final int TEMPLATE_INTERRUPTION_TIMEOUT = 5000;
    private final Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);

    @Test
    public void test() throws IOException, InterruptedException {
        assertCanBeInterrupted("<#list 1.. as x></#list>");
        assertCanBeInterrupted("<#list 1.. as x>${x}</#list>");
        assertCanBeInterrupted("<#list 1.. as x>t${x}</#list>");
        assertCanBeInterrupted("<#list 1.. as x><#list 1.. as y>${y}</#list></#list>");
        assertCanBeInterrupted("<#list 1.. as x>${x}<#else>nope</#list>");
        assertCanBeInterrupted("<#list 1..>[<#items as x>${x}</#items>]<#else>nope</#list>");
        assertCanBeInterrupted("<@customLoopDirective />");
        assertCanBeInterrupted("<@customLoopDirective>x</@>");
        assertCanBeInterrupted("<@customLoopDirective><#if true>x</#if></@>");
        assertCanBeInterrupted("<#macro selfCalling><@sleepDirective/><@selfCalling /></#macro><@selfCalling />");
        assertCanBeInterrupted("<#function selfCalling><@sleepDirective/>${selfCalling()}</#function>${selfCalling()}");
        assertCanBeInterrupted("<#list 1.. as _><#attempt><@sleepDirective/><#recover>suppress</#attempt></#list>");
        assertCanBeInterrupted("<#attempt><#list 1.. as _></#list><#recover>suppress</#attempt>");
    }

    private void assertCanBeInterrupted(final String templateSourceCode) throws IOException, InterruptedException {
        TemplateRunnerThread trt = new TemplateRunnerThread(templateSourceCode);
        trt.start();
        synchronized (trt) {
            while (!trt.isStarted()) {
                trt.wait();
            }
        }
        Thread.sleep(50); // Just to ensure (hope...) that the template execution reaches "deep" enough
        trt.interrupt();
        trt.join(TEMPLATE_INTERRUPTION_TIMEOUT);
        assertTrue(trt.isTemplateProcessingInterrupted());
    }

    public class TemplateRunnerThread extends Thread {

        private final Template template;
        private boolean started;
        private boolean templateProcessingInterrupted;

        public TemplateRunnerThread(String templateSourceCode) throws IOException {
            template = new Template(null, "<@startedDirective/>" + templateSourceCode, cfg);
            _CoreAPI.addThreadInterruptedChecks(template);
        }

        @Override
        public void run() {
            try {
                template.process(this, NullWriter.INSTANCE);
            } catch (TemplateProcessingThreadInterruptedException e) {
                //LOG.debug("Template processing interrupted", e);
                synchronized (this) {
                    templateProcessingInterrupted = true;
                }
            } catch (Throwable e) {
                LOG.error("Template processing failed", e);
            }
        }

        public synchronized boolean isTemplateProcessingInterrupted() {
            return templateProcessingInterrupted;
        }
        
        public synchronized boolean isStarted() {
            return started;
        }
        
        public TemplateDirectiveModel getStartedDirective() {
            return new StartedDirective();
        }

        public TemplateDirectiveModel getCustomLoopDirective() {
            return new CustomLoopDirective();
        }
        
        public TemplateDirectiveModel getSleepDirective() {
            return new SleepDirective();
        }

        public class StartedDirective implements TemplateDirectiveModel {
            
            public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
                    throws TemplateException, IOException {
                synchronized (TemplateRunnerThread.this) {
                    started = true;
                    TemplateRunnerThread.this.notifyAll();
                }
            }
            
        }

        public class CustomLoopDirective implements TemplateDirectiveModel {

            public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
                    throws TemplateException, IOException {
                while (true) {
                    body.render(NullWriter.INSTANCE);
                }
            }
            
        }
        
        public class SleepDirective implements TemplateDirectiveModel {

            public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
                    throws TemplateException, IOException {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Thread.sleep has reset the interrupted flag (because it has thrown InterruptedException).  
                    Thread.currentThread().interrupt();
                }
            }
            
        }
        
    }

}
