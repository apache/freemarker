package freemarker.ext.script;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Class that represents a parsed FreeMarker {@link Template} object as a 
 * JSR-223 {@link CompiledScript}.
 * @author Attila Szegedi
 * @version $Id: $
 */
class CompiledFreeMarkerScript extends CompiledScript
{
    private final FreeMarkerScriptEngine engine;
    private final Template template;
    
    CompiledFreeMarkerScript(FreeMarkerScriptEngine engine, Template template)
    {
        this.engine = engine;
        this.template = template;
    }

    @Override
    public Object eval(ScriptContext context) throws ScriptException
    {
        try
        {
            boolean stringOutput = Boolean.TRUE.equals(context.getAttribute(
                    FreeMarkerScriptConstants.STRING_OUTPUT));
            
            Writer w = stringOutput ? new StringWriter() : context.getWriter();
            
            template.process(new ScriptContextHashModel(context, 
                    template.getObjectWrapper()), w);
            
            return stringOutput ? w.toString() : null;
        }
        catch(IOException e)
        {
            throw new ScriptException(e);
        }
        catch(TemplateException e)
        {
            throw new ScriptException(e);
        }
    }

    @Override
    public ScriptEngine getEngine()
    {
        return engine;
    }
}
