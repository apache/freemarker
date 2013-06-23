/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.template;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import freemarker.core.Configurable;
import freemarker.core.Environment;
import freemarker.core.StopException;
import freemarker.template.utility.StringUtil;

/**
 * Used for the {@code template_exception_handler} configuration setting;
 * see {@link Configurable#setTemplateExceptionHandler(TemplateExceptionHandler)} for more.
 */
public interface TemplateExceptionHandler {
    
    /** 
     * Method called after a {@link TemplateException} was raised inside a template. The error is logged before this is
     * called, so there's no need to log it here. The exception should be re-thrown unless you want to
     * suppress the exception.
     * 
     * <p>Note that you can check with {@link Environment#isInAttemptBlock()} if you are inside a {@code #attempt}
     * block, which then will handle handle this exception and roll back the output generated inside it.
     * 
     * <p>Note that {@link StopException}-s (raised by {@code #stop}) won't be captured.
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
    TemplateExceptionHandler IGNORE_HANDLER = new TemplateExceptionHandler() {
        public void handleTemplateException(TemplateException te, Environment env, Writer out) {
            // Do nothing
        }
    };
        
    /**
     * {@link TemplateExceptionHandler} that simply re-throws the exception; this should be used in most production
     * systems.
     */
    TemplateExceptionHandler RETHROW_HANDLER =new TemplateExceptionHandler() {
        public void handleTemplateException(TemplateException te, Environment env, Writer out)
                throws TemplateException {
            throw te;
        }
    };
        
    /**
     * {@link TemplateExceptionHandler} useful when you developing non-HTML templates. This handler
     * outputs the stack trace information to the client and then re-throws the exception.
     */
    TemplateExceptionHandler DEBUG_HANDLER =new TemplateExceptionHandler() {
        public void handleTemplateException(TemplateException te, Environment env, Writer out)
                throws TemplateException {
            if (!env.isInAttemptBlock()) {
                PrintWriter pw = (out instanceof PrintWriter) ? (PrintWriter) out : new PrintWriter(out);
                te.printStackTrace(pw);
                pw.flush();
            }
            throw te;
        }
    }; 
    
    /**
     * {@link TemplateExceptionHandler} useful when you developing HTML templates. This handler
     * outputs the stack trace information to the client, formatting it so that it will be usually well readable
     * in the browser, and then re-throws the exception.
     */
    TemplateExceptionHandler HTML_DEBUG_HANDLER = new TemplateExceptionHandler() {
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
                            + "text-decoration:none; text-transform: none;'>FreeMarker template error</b>"
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
                    pw.println(StringUtil.XMLEncNQG(stackTraceSW.toString()));
                    
                    pw.println("</pre></div></html>");
                    pw.flush();
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
