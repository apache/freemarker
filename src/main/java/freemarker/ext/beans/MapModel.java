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

package freemarker.ext.beans;

import java.util.List;
import java.util.Map;
import java.util.Set;

import freemarker.ext.util.ModelFactory;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * <p>A special case of {@link BeanModel} that adds implementation
 * for {@link TemplateMethodModelEx} on map objects that is a shortcut for the
 * <tt>Map.get()</tt> method. Note that if the passed argument itself is a
 * reflection-wrapper model, then the map lookup will be performed using the
 * wrapped object as the key. Note that you can call <tt>get()</tt> using the
 * <tt>map.key</tt> syntax inherited from {@link BeanModel} as well, 
 * however in that case the key is always a string.</p>
 * <p>The class itself does not implement the {@link freemarker.template.TemplateCollectionModel}.
 * You can, however use <tt>map.entrySet()</tt>, <tt>map.keySet()</tt>, or
 * <tt>map.values()</tt> to obtain {@link freemarker.template.TemplateCollectionModel} instances for 
 * various aspects of the map.</p>
 * @author Attila Szegedi
 */
public class MapModel
extends
    StringModel
implements
    TemplateMethodModelEx
{
    static final ModelFactory FACTORY =
        new ModelFactory()
        {
            public TemplateModel create(Object object, ObjectWrapper wrapper)
            {
                return new MapModel((Map)object, (BeansWrapper)wrapper);
            }
        };

    /**
     * Creates a new model that wraps the specified map object.
     * @param map the map object to wrap into a model.
     * @param wrapper the {@link BeansWrapper} associated with this model.
     * Every model has to have an associated {@link BeansWrapper} instance. The
     * model gains many attributes from its wrapper, including the caching 
     * behavior, method exposure level, method-over-item shadowing policy etc.
     */
    public MapModel(Map map, BeansWrapper wrapper)
    {
        super(map, wrapper);
    }

    /**
     * The first argument is used as a key to call the map's <tt>get</tt> method.
     */
    public Object exec(List arguments)
    throws
        TemplateModelException
    {
        Object key = unwrap((TemplateModel)arguments.get(0));
        return wrap(((Map)object).get(key));
    }

    /**
     * Overridden to invoke the generic get method by casting to Map instead of 
     * through reflection - should yield better performance.
     */
    protected TemplateModel invokeGenericGet(Map keyMap, Class clazz, String key)
    throws TemplateModelException
    {
        Map map = (Map) object;
        Object val = map.get(key);
        if(val == null) {
            if(key.length() == 1) {
                // just check for Character key if this is a single-character string
                Character charKey = new Character(key.charAt(0));
                val = map.get(charKey);
                if (val == null && !(map.containsKey(key) || map.containsKey(charKey))) {
                    return UNKNOWN;
                }
            }
            else if(!map.containsKey(key)) {
                return UNKNOWN;
            }
        }
        return wrap(val);
    }

    public boolean isEmpty()
    {
        return ((Map)object).isEmpty() && super.isEmpty();
    }

    public int size()
    {
        return keySet().size();
    }

    protected Set keySet()
    {
        Set set = super.keySet();
        set.addAll(((Map)object).keySet());
        return set;
    }
}
