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

import freemarker.core.CollectionAndSequence;
import freemarker.ext.util.ModelFactory;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.WrappingTemplateModel;

/**
 * Model used by {@link BeansWrapper} when <tt>simpleMapWrapper</tt>
 * mode is enabled. Provides a simple hash model interface to the
 * underlying map (does not copy like {@link freemarker.template.SimpleHash}),
 * and a method interface to non-string keys.
 * @author Chris Nokleberg
 */
public class SimpleMapModel extends WrappingTemplateModel 
implements TemplateHashModelEx, TemplateMethodModelEx, AdapterTemplateModel, 
WrapperTemplateModel 
{
    static final ModelFactory FACTORY =
        new ModelFactory()
        {
            public TemplateModel create(Object object, ObjectWrapper wrapper)
            {
                return new SimpleMapModel((Map)object, (BeansWrapper)wrapper);
            }
        };

    private final Map map;
    
    public SimpleMapModel(Map map, BeansWrapper wrapper)
    {
        super(wrapper);
        this.map = map;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        Object val = map.get(key);
        if(val == null) {
            if(key.length() == 1) {
                // just check for Character key if this is a single-character string
                Character charKey = new Character(key.charAt(0));
                val = map.get(charKey);
                if (val == null && !(map.containsKey(key) || map.containsKey(charKey))) {
                    return null;
                }
            }
            else if(!map.containsKey(key)) {
                return null;
            }
        }
        return wrap(val);
    }
    
    public Object exec(List args) throws TemplateModelException {
        Object key = ((BeansWrapper)getObjectWrapper()).unwrap((TemplateModel)args.get(0));
        Object value = map.get(key);
        if (value == null && !map.containsKey(key)) {
            return null;
        }
        return wrap(value);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public int size() {
        return map.size();
    }

    public TemplateCollectionModel keys() {
        return new CollectionAndSequence(new SimpleSequence(map.keySet(), getObjectWrapper()));
    }

    public TemplateCollectionModel values() {
        return new CollectionAndSequence(new SimpleSequence(map.values(), getObjectWrapper()));
    }
    
    public Object getAdaptedObject(Class hint) {
        return map;
    }
    
    public Object getWrappedObject() {
        return map;
    }
}