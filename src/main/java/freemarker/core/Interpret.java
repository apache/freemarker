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

package freemarker.core;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.TemplateTransformModel;


/**
 * A method that takes a parameter and evaluates it as a scalar,
 * then treats that scalar as template source code and returns a
 * transform model that evaluates the template in place.
 * The template inherits the configuration and environment of the executing
 * template. By default, its name will be equal to 
 * <tt>executingTemplate.getName() + "$anonymous_interpreted"</tt>. You can
 * specify another parameter to the method call in which case the
 * template name suffix is the specified id instead of "anonymous_interpreted".
 * @author Attila Szegedi
 */
class Interpret extends BuiltIn
{
    /**
     * Constructs a template on-the-fly and returns it embedded in a
     * {@link TemplateTransformModel}.
     * 
     * <p>The built-in has two arguments:
     * the arguments passed to the method. It can receive at
     * least one and at most two arguments, both must evaluate to a scalar. 
     * The first scalar is interpreted as a template source code and a template
     * is built from it. The second (optional) is used to give the generated
     * template a name.
     * 
     * @return a {@link TemplateTransformModel} that when executed inside
     * a <tt>&lt;transform></tt> block will process the generated template
     * just as if it had been <tt>&lt;transform></tt>-ed at that point.
     */
    TemplateModel _eval(Environment env)
            throws TemplateException 
    {
        TemplateModel model = target.eval(env);
        Expression sourceExpr = null;
        String id = "anonymous_interpreted";
        if(model instanceof TemplateSequenceModel)
        {
            sourceExpr = ((Expression)new DynamicKeyName(target, new NumberLiteral(new Integer(0))).copyLocationFrom(target));
            if(((TemplateSequenceModel)model).size() > 1)
            {
                id = ((Expression)new DynamicKeyName(target, new NumberLiteral(new Integer(1))).copyLocationFrom(target)).evalAndCoerceToString(env);
            }
        }
        else if (model instanceof TemplateScalarModel)
        {
            sourceExpr = target;
        }
        else
        {
            throw new UnexpectedTypeException(target, model, "sequence or string", env);
        }
        String templateSource = sourceExpr.evalAndCoerceToString(env);
        Template parentTemplate = env.getTemplate();
        
        final Template interpretedTemplate;
        try
        {
            interpretedTemplate = new Template(
                    (parentTemplate.getName() != null ? parentTemplate.getName() : "nameless_template") + "->" + id,
                    templateSource,
                    parentTemplate.getConfiguration());
        }
        catch(IOException e)
        {
            throw new _MiscTemplateException(this, e, env, new Object[] {
                        "Template parsing with \"?", key, "\" has failed with this error:\n\n",
                        MessageUtil.EMBEDDED_MESSAGE_BEGIN,
                        new _DelayedGetMessage(e),
                        MessageUtil.EMBEDDED_MESSAGE_END,
                        "\n\nThe failed expression:" });
        }
        
        interpretedTemplate.setLocale(env.getLocale());
        return new TemplateProcessorModel(interpretedTemplate);
    }

    private class TemplateProcessorModel
    implements
        TemplateTransformModel
    {
        private final Template template;
        
        TemplateProcessorModel(Template template)
        {
            this.template = template;
        }
        
        public Writer getWriter(final Writer out, Map args) throws TemplateModelException, IOException
        {
            try
            {
                Environment env = Environment.getCurrentEnvironment();
                boolean lastFIRE = env.setFastInvalidReferenceExceptions(false);
                try {
                    env.include(template);
                } finally {
                    env.setFastInvalidReferenceExceptions(lastFIRE);
                }
            }
            catch(Exception e)
            {
                throw new _TemplateModelException(e, new Object[] {
                        "Template created with \"?", key, "\" has stopped with this error:\n\n",
                        MessageUtil.EMBEDDED_MESSAGE_BEGIN,
                        new _DelayedGetMessage(e),
                        MessageUtil.EMBEDDED_MESSAGE_END });
            }
    
            return new Writer(out)
            {
                public void close()
                {
                }
                
                public void flush() throws IOException
                {
                    out.flush();
                }
                
                public void write(char[] cbuf, int off, int len) throws IOException
                {
                    out.write(cbuf, off, len);
                }
            };
        }
    }
}
