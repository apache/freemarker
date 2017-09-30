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

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;
import freemarker.template.TransformControl;
import freemarker.test.TemplateTest;

public class TemplateTransformModelTest extends TemplateTest {
    
    @Test(expected=IOException.class)
    public void testFailsWithWrongClosing() throws IOException, TemplateException {
        addToDataModel("t", WrongTransform.INSTANCE);
        assertOutput("a<@t>b</@t>c", "abc");
    }
    
    // Works since 2.3.27
    @Test
    public void testEnclosingWriterUser() throws IOException, TemplateException {
        addToDataModel("t", EnclosingWriterUserTransform.INSTANCE);
        assertOutput("a<@t>b</@t>c", "abc");
    }

    @Test
    public void testCloseCalled() throws IOException, TemplateException {
        addToDataModel("t", UpperCaseInParenthesesTransform.INSTANCE);
        assertOutput("a<@t>b</@t>c", "a(B)c");
        assertOutput("<#list 1..2 as _>a<@t>b<#continue>c</@t>d</#list>.", "a(B)a(B).");
    }
    
    @Test
    public void testExceptionHandler() throws IOException, TemplateException {
        Configuration cfg = getConfiguration();
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        
        addToDataModel("t", ExceptionHandlerTransform.INSTANCE);
        assertOutput("1<@t>2</@t>3", "1(2)C3");
        assertOutput("1<@t>2${noSuchVar}x</@t>3", "1(2EC3");
        
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_27);
        assertOutput("<#list 1..1 as _>1<@t>2<#break>x</@t></#list>3", "1(2C3");
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_26);
        assertOutput("<#list 1..1 as _>1<@t>2<#break>x</@t></#list>3", "1(2EC3");
    }
    
    public static final class UpperCaseInParenthesesTransform implements TemplateTransformModel {

        public static final UpperCaseInParenthesesTransform INSTANCE = new UpperCaseInParenthesesTransform();
        
        private UpperCaseInParenthesesTransform() {
            //
        }
        
        public Writer getWriter(Writer out, Map args) throws TemplateModelException, IOException {
            out.write('(');
            return new FilterWriter(out) {

                @Override
                public void write(int c) throws IOException {
                    out.write(Character.toUpperCase(c));
                }

                @Override
                public void write(char[] cbuf, int off, int len) throws IOException {
                    for (int i = 0; i < cbuf.length; i++) {
                        cbuf[i] = Character.toUpperCase(cbuf[i]);
                    }
                    super.write(cbuf, off, len);
                }

                @Override
                public void close() throws IOException {
                    out.write(')');
                }
            };
        }
        
    }    

    public static final class EnclosingWriterUserTransform implements TemplateTransformModel {

        private static final EnclosingWriterUserTransform INSTANCE = new EnclosingWriterUserTransform();
        
        private EnclosingWriterUserTransform() {
            //
        }
        
        public Writer getWriter(Writer out, Map args) throws TemplateModelException, IOException {
            return out;
        }
    }
    
    public static final class WrongTransform implements TemplateTransformModel {
        
        private static final WrongTransform INSTANCE = new WrongTransform(); 

        public Writer getWriter(Writer out, Map args) throws TemplateModelException, IOException {
            return new FilterWriter(out) { };  // Deliberately forgot to override close()
        }
        
    }
    
    public static final class ExceptionHandlerTransform implements TemplateTransformModel {

        private static final ExceptionHandlerTransform INSTANCE = new ExceptionHandlerTransform(); 
        
        public Writer getWriter(Writer out, Map args) throws TemplateModelException, IOException {
            return new ExceptoinHandlerTransformWriter(out);
        }
        
        class ExceptoinHandlerTransformWriter extends FilterWriter implements TransformControl {

            protected ExceptoinHandlerTransformWriter(Writer out) throws IOException {
                super(out);
            }
            
            @Override
            public void close() throws IOException {
                out.write('C');
            }

            public int onStart() throws TemplateModelException, IOException {
                out.write('(');
                return TransformControl.EVALUATE_BODY;
            }

            public int afterBody() throws TemplateModelException, IOException {
                out.write(')');
                return TransformControl.END_EVALUATION;
            }

            public void onError(Throwable t) throws Throwable {
                out.write("E");
            }
            
        }
        
    }
}
