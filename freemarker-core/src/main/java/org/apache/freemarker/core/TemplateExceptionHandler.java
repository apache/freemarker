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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.freemarker.core.util._StringUtils;

/**
 * Used for the {@code templateExceptionHandler} configuration setting;
 * see {@link MutableProcessingConfiguration#setTemplateExceptionHandler(TemplateExceptionHandler)} for more.
 */
public interface TemplateExceptionHandler {
    
    /** 
     * Method called after a {@link TemplateException} was raised inside a template. The exception should be re-thrown
     * unless you want to suppress the exception.
     * 
     * <p>Note that you can check with {@link Environment#isInAttemptBlock()} if you are inside a {@code #attempt}
     * block, which then will handle handle this exception and roll back the output generated inside it.
     * 
     * <p>Note that {@link StopException}-s (raised by {@code #stop}) won't be captured.
     * 
     * <p>Note that you shouldn't log the exception in this method unless you suppress the exception. After
     * all, eventually the exception is catched somewhere and is (hopefully) logged.
     * 
     * @param te The exception that occurred; don't forget to re-throw it unless you want to suppress it
     * @param env The runtime environment of the template
     * @param out This is where the output of the template is written
     */
    void handleTemplateException(TemplateException te, Environment env, Writer out) throws TemplateException;
            
   /**
    * {@link TemplateExceptionHandler} that simply skips the failing instructions, letting the template continue
    * executing. It does nothing to handle the event. Note that the exception is still logged, as with all
    * other {@link TemplateExceptionHandler}-s.
    */
    TemplateExceptionHandler IGNORE = new TemplateExceptionHandler() {
        @Override
        public void handleTemplateException(TemplateException te, Environment env, Writer out) {
            // Do nothing
        }
    };
        
    /**
     * {@link TemplateExceptionHandler} that simply re-throws the exception; this should be used in most production
     * systems.
     */
    TemplateExceptionHandler RETHROW = new TemplateExceptionHandler() {
        @Override
        public void handleTemplateException(TemplateException te, Environment env, Writer out)
                throws TemplateException {
            throw te;
        }
    };
        
    /**
     * {@link TemplateExceptionHandler} useful when you developing non-HTML templates. This handler
     * outputs the stack trace information to the client and then re-throws the exception.
     */
    TemplateExceptionHandler DEBUG = new TemplateExceptionHandler() {
        @Override
        public void handleTemplateException(TemplateException te, Environment env, Writer out)
                throws TemplateException {
            if (!env.isInAttemptBlock()) {
                PrintWriter pw = (out instanceof PrintWriter) ? (PrintWriter) out : new PrintWriter(out);
                pw.print("FreeMarker template error (DEBUG mode; use RETHROW in production!):\n");
                te.printStackTrace(pw, false, true, true);
                
                pw.flush();  // To commit the HTTP response
            }
            throw te;
        }
    }; 
    
    /**
     * {@link TemplateExceptionHandler} useful when you developing HTML templates. This handler
     * outputs the stack trace information to the client, formatting it so that it will be usually well readable
     * in the browser, and then re-throws the exception.
     */
    TemplateExceptionHandler HTML_DEBUG = new TemplateExceptionHandler() {
        @Override
        public void handleTemplateException(TemplateException te, Environment env, Writer out)
                throws TemplateException {
            if (!env.isInAttemptBlock()) {
                boolean externalPw = out instanceof PrintWriter;
                PrintWriter pw = externalPw ? (PrintWriter) out : new PrintWriter(out);
                try {
                    pw.print("<!-- FREEMARKER ERROR MESSAGE STARTS HERE -->"
                            + "<!-- ]]> -->"
                            + "<script language=javascript>//\"></script>"
                            + "<script language=javascript>//'></script>"
                            + "<script language=javascript>//\"></script>"
                            + "<script language=javascript>//'></script>"
                            + "</title></xmp></script></noscript></style></object>"
                            + "</head></pre></table>"
                            + "</form></table></table></table></a></u></i></b>"
                            + "<div align='left' "
                            + "style='background-color:#FFFF7C; "
                            + "display:block; border-top:double; padding:4px; margin:0; "
                            + "font-family:Arial,sans-serif; ");
                    pw.print(FONT_RESET_CSS);
                    pw.print("'>"
                            + "<b style='font-size:12px; font-style:normal; font-weight:bold; "
                            + "text-decoration:none; text-transform: none;'>FreeMarker template error "
                            + " (HTML_DEBUG mode; use RETHROW in production!)</b>"
                            + "<pre style='display:block; background: none; border: 0; margin:0; padding: 0;"
                            + "font-family:monospace; ");
                    pw.print(FONT_RESET_CSS);
                    pw.println("; white-space: pre-wrap; white-space: -moz-pre-wrap; white-space: -pre-wrap; "
                            + "white-space: -o-pre-wrap; word-wrap: break-word;'>");
                    
                    StringWriter stackTraceSW = new StringWriter();
                    PrintWriter stackPW = new PrintWriter(stackTraceSW);
                    te.printStackTrace(stackPW, false, true, true);
                    stackPW.close();
                    pw.println();
                    pw.println(_StringUtils.XMLEncNQG(stackTraceSW.toString()));
                    
                    pw.println("</pre></div></html>");
                    pw.flush();  // To commit the HTTP response
                } finally {
                    if (!externalPw) pw.close();
                }
            }  // if (!env.isInAttemptBlock())
            
            throw te;
        }
        
        private static final String FONT_RESET_CSS =
                "color:#A80000; font-size:12px; font-style:normal; font-variant:normal; "
                + "font-weight:normal; text-decoration:none; text-transform: none";
        
    };
    
}
