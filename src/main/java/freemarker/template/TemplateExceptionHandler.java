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

import freemarker.core.Environment;
import freemarker.template.utility.StringUtil;

/**
 * An API for objects that handle exceptions that are thrown during
 * template rendering.
 * @author <a href="mailto:jon@revusky.com">Jonathan Revusky</a>
 */

public interface TemplateExceptionHandler {
	
	/**
	  * handle the exception.
	  * @param te the exception that occurred.
	  * @param env The environment object that represents the rendering context
	  * @param out the character output stream to output to.
	  */
	void handleTemplateException(TemplateException te, Environment env, Writer out) 
	    throws TemplateException;
            
            
         /**
           * This is a TemplateExceptionHandler which simply skips errors. It does nothing
           * to handle the event. Note that the exception is still logged in any case, before
           * being passed to the handler.
           */
	TemplateExceptionHandler IGNORE_HANDLER = new TemplateExceptionHandler() {
		public void handleTemplateException(TemplateException te, Environment env, Writer out) {
		}
	};
        
         /**
           * This is a TemplateExceptionHandler that simply rethrows the exception.
           * Note that the exception is logged before being rethrown.
           */
	TemplateExceptionHandler RETHROW_HANDLER =new TemplateExceptionHandler() {
		public void handleTemplateException(TemplateException te, Environment env, Writer out) 
                    throws TemplateException  
                {
                    throw te;
		}
	};
        
        /**
          * This is a TemplateExceptionHandler used when you develop the templates. This handler
          * outputs the stack trace information to the client and then rethrows the exception.
          */
	TemplateExceptionHandler DEBUG_HANDLER =new TemplateExceptionHandler() {
		public void handleTemplateException(TemplateException te, Environment env, Writer out) 
                    throws TemplateException  
                {
                    PrintWriter pw = (out instanceof PrintWriter) 
                                 ? (PrintWriter) out 
                                 : new PrintWriter(out);
                    te.printStackTrace(pw);
                    pw.flush();
                    throw te;
		}
	}; 

	
        /**
          * This is a TemplateExceptionHandler used when you develop HTML templates. This handler
          * outputs the stack trace information to the client and then rethrows the exception, and
          * surrounds it with tags to make the error message readable with the browser.
          */
	TemplateExceptionHandler HTML_DEBUG_HANDLER = new TemplateExceptionHandler() {
		public void handleTemplateException(TemplateException te, Environment env, Writer out) 
                    throws TemplateException  
                {
                    PrintWriter pw = (out instanceof PrintWriter) 
                                 ? (PrintWriter) out 
                                 : new PrintWriter(out);
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
                    te.printStackTrace(stackPW, false);
                    stackPW.close();
                    pw.println();
                    pw.println(StringUtil.XMLEncNQG(stackTraceSW.toString()));
                    
                    pw.println("</pre></div></html>");
                    pw.flush();
                    throw te;
		}
		
	    private static final String FONT_RESET_CSS =
	            "color:#A80000; font-size:12px; font-style:normal; font-variant:normal; "
	            + "font-weight:normal; text-decoration:none; text-transform: none";
		
	};
}
