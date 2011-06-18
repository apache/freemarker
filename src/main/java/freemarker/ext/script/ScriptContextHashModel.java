package freemarker.ext.script;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.script.ScriptContext;

import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * Class that wraps a {@link ScriptContext} into a {@link TemplateHashModelEx}
 * @author Attila Szegedi
 * @version $Id: $
 */
class ScriptContextHashModel implements TemplateHashModelEx
{
    private final ScriptContext context;
    private final ObjectWrapper wrapper;
    
    ScriptContextHashModel(ScriptContext context, ObjectWrapper wrapper)
    {
        this.context = context;
        this.wrapper = wrapper;
    }

    public TemplateModel get(String key) throws TemplateModelException
    {
        Object retval;
        synchronized(context)
        {
            retval = context.getAttribute(key);
        }
        return wrapper.wrap(retval);
    }

    public boolean isEmpty() throws TemplateModelException
    {
        synchronized(context)
        {
            for(int scope : context.getScopes())
            {
                if(!context.getBindings(scope).isEmpty())
                {
                    return false;
                }
            }
        }
        return true;
    }

    public TemplateCollectionModel keys() throws TemplateModelException
    {
        Set<String> keys;
        synchronized(context)
        {
            keys = getAllKeys();
        }
        return (TemplateCollectionModel)wrapper.wrap(keys);
    }

    private Set<String> getAllKeys()
    {
        Set<String> keys = new HashSet<String>();
        for(int scope : context.getScopes())
        {
            keys.addAll(context.getBindings(scope).keySet());
        }
        return keys;
    }

    public int size()
    {
        Set<String> keys;
        synchronized(context)
        {
            keys = getAllKeys();
        }
        return keys.size();
    }

    public TemplateCollectionModel values() throws TemplateModelException
    {
        List<Object> values;
        synchronized(context)
        {
            Set<String> keys = getAllKeys();
            values = new ArrayList<Object>(keys.size());
            for (String key : keys)
            {
                int scope = context.getAttributesScope(key);
                if(scope != -1)
                {
                    Object value = context.getAttribute(key);
                    if(value != null)
                    {
                        values.add(value);
                    }
                }
            }
        }
        return (TemplateCollectionModel)wrapper.wrap(values);
    }
}