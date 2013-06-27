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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import freemarker.template.SimpleNumber;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.Collections12;
import freemarker.template.utility.StringUtil;

/**
 * A class that will wrap a reflected method call into a
 * {@link freemarker.template.TemplateMethodModel} interface. 
 * It is used by {@link BeanModel} to wrap reflected method calls
 * for non-overloaded methods.
 * @author Attila Szegedi, szegedia at users dot sourceforge dot net
 */
public final class SimpleMethodModel extends SimpleMemberModel
    implements
    TemplateMethodModelEx,
    TemplateSequenceModel
{
    private final Object object;
    private final BeansWrapper wrapper;

    /**
     * Creates a model for a specific method on a specific object.
     * @param object the object to call the method on. Can be
     * <tt>null</tt> for static methods.
     * @param method the method that will be invoked.
     */
    SimpleMethodModel(Object object, Method method, Class[] argTypes, 
            BeansWrapper wrapper)
    {
        super(method, argTypes);
        this.object = object;
        this.wrapper = wrapper;
    }

    /**
     * Invokes the method, passing it the arguments from the list.
     */
    public Object exec(List arguments)
        throws
        TemplateModelException
    {
        try
        {
            return wrapper.invokeMethod(object, (Method)getMember(), 
                    unwrapArguments(arguments, wrapper));
        }
        catch(TemplateModelException e)
        {
            throw e;
        }
        catch(Exception e)
        {
            while(e instanceof InvocationTargetException)
            {
                Throwable t = ((InvocationTargetException)e).getTargetException();
                if(t instanceof Exception)
                {
                    e = (Exception)t;
                }
                else
                {
                    break;
                }
            }
            if((getMember().getModifiers() & Modifier.STATIC) != 0)
            {
                throw new TemplateModelException("Method " + StringUtil.jQuote(getMember()) + 
                        " threw an exception; see cause exception", e);
            }
            else
            {
                throw new TemplateModelException(
                        "Method " + StringUtil.jQuote(getMember()) + 
                        " threw an exception when invoked on "
                        + object.getClass().getName() + " object "
                        + StringUtil.jQuote(StringUtil.tryToString(object)) + ". See cause exception.",
                        e);
            }
        }
    }
    
    public TemplateModel get(int index) throws TemplateModelException
    {
        return (TemplateModel) exec(Collections12.singletonList(
                new SimpleNumber(new Integer(index))));
    }

    public int size() throws TemplateModelException {
        throw new TemplateModelException(
                "Getting the number of items or enumerating the items is not supported on this "
                + ClassUtil.getFTLTypeDescription(this) + " value.\n"
                + "("
                + "Hint 1: Maybe you wanted to call this method first and then do something with its return value. "
                + "Hint 2: Getting items by intex possibly works, hence it's a \"+sequence\"."
                + ")");
    }
    
    public String toString() {
        return getMember().toString();
    }
}