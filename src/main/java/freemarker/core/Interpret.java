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
            throw new UnexpectedTypeException(
                    target, model,
                    "sequence or string", new Class[] { TemplateSequenceModel.class, TemplateScalarModel.class },
                    env);
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
