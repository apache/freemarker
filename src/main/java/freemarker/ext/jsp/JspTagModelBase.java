package freemarker.ext.jsp;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.StringUtil;

class JspTagModelBase
{
    private final Class tagClass;
    private final Method dynaSetter;
    private final Map propertySetters = new HashMap();
    
    protected JspTagModelBase(Class tagClass) throws IntrospectionException {
        this.tagClass = tagClass;
        BeanInfo bi = Introspector.getBeanInfo(tagClass);
        PropertyDescriptor[] pda = bi.getPropertyDescriptors();
        for (int i = 0; i < pda.length; i++) {
            PropertyDescriptor pd = pda[i];
            Method m = pd.getWriteMethod();
            if(m != null) {
                propertySetters.put(pd.getName(), m);
            }
        }
        // Check to see if the tag implements the JSP2.0 DynamicAttributes
        // interface, to allow setting of arbitrary attributes
        Method dynaSetter;
        try {
            dynaSetter = tagClass.getMethod("setDynamicAttribute",
                            new Class[] {String.class, String.class, Object.class});
        }
        catch (NoSuchMethodException nsme) {
            dynaSetter = null;
        }
        this.dynaSetter = dynaSetter;
    }
    
    Object getTagInstance() throws IllegalAccessException, InstantiationException {
        return tagClass.newInstance();
    }
    
    void setupTag(Object tag, Map args, ObjectWrapper wrapper)
    throws 
        TemplateModelException, 
        InvocationTargetException, 
        IllegalAccessException
    {
        BeansWrapper bwrapper = 
            wrapper instanceof BeansWrapper
            ? (BeansWrapper)wrapper
            : BeansWrapper.getDefaultInstance();
        if(args != null && !args.isEmpty()) {
            Object[] aarg = new Object[1];
            for (Iterator iter = args.entrySet().iterator(); iter.hasNext();)
            {
                Map.Entry entry = (Map.Entry) iter.next();
                Object arg = bwrapper.unwrap((TemplateModel)entry.getValue());
                aarg[0] = arg;
                Method m = (Method)propertySetters.get(entry.getKey());
                if (m == null) {
                    if (dynaSetter == null) {
                        throw new TemplateModelException("Unknown property "
                                + StringUtil.jQuote(entry.getKey().toString())
                                + " on instance of " + tagClass.getName());
                    }
                    else {
                        dynaSetter.invoke(tag, new Object[] {null, entry.getKey(), aarg[0]});
                    }
                }
                else {
                    if(arg instanceof BigDecimal) {
                        aarg[0] = BeansWrapper.coerceBigDecimal(
                                (BigDecimal)arg, m.getParameterTypes()[0]);
                    }
                    m.invoke(tag, aarg);
                }
            }
        }
    }

}
