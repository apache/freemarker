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

package freemarker.ext.beans;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import freemarker.template.utility.ClassUtil;

class UnsafeMethods {

    private static final Set UNSAFE_METHODS = createUnsafeMethodsSet();
    
    private UnsafeMethods() { }
    
    static boolean isUnsafeMethod(Method method) {
        return UNSAFE_METHODS.contains(method);        
    }
    
    private static final Set createUnsafeMethodsSet()
    {
        Properties props = new Properties();
        InputStream in = BeansWrapper.class.getResourceAsStream("unsafeMethods.txt");
        if(in != null)
        {
            String methodSpec = null;
            try
            {
                try
                {
                    props.load(in);
                }
                finally
                {
                    in.close();
                }
                Set set = new HashSet(props.size() * 4/3, 1f);
                Map primClasses = createPrimitiveClassesMap();
                for (Iterator iterator = props.keySet().iterator(); iterator.hasNext();)
                {
                    methodSpec = (String) iterator.next();
                    try {
                        set.add(parseMethodSpec(methodSpec, primClasses));
                    }
                    catch(ClassNotFoundException e) {
                        if(ClassIntrospector.DEVELOPMENT_MODE) {
                            throw e;
                        }
                    }
                    catch(NoSuchMethodException e) {
                        if(ClassIntrospector.DEVELOPMENT_MODE) {
                            throw e;
                        }
                    }
                }
                return set;
            }
            catch(Exception e)
            {
                throw new RuntimeException("Could not load unsafe method " + methodSpec + " " + e.getClass().getName() + " " + e.getMessage());
            }
        }
        return Collections.EMPTY_SET;
    }

    private static Method parseMethodSpec(String methodSpec, Map primClasses)
    throws
        ClassNotFoundException,
        NoSuchMethodException
    {
        int brace = methodSpec.indexOf('(');
        int dot = methodSpec.lastIndexOf('.', brace);
        Class clazz = ClassUtil.forName(methodSpec.substring(0, dot));
        String methodName = methodSpec.substring(dot + 1, brace);
        String argSpec = methodSpec.substring(brace + 1, methodSpec.length() - 1);
        StringTokenizer tok = new StringTokenizer(argSpec, ",");
        int argcount = tok.countTokens();
        Class[] argTypes = new Class[argcount];
        for (int i = 0; i < argcount; i++)
        {
            String argClassName = tok.nextToken();
            argTypes[i] = (Class)primClasses.get(argClassName);
            if(argTypes[i] == null)
            {
                argTypes[i] = ClassUtil.forName(argClassName);
            }
        }
        return clazz.getMethod(methodName, argTypes);
    }

    private static Map createPrimitiveClassesMap()
    {
        Map map = new HashMap();
        map.put("boolean", Boolean.TYPE);
        map.put("byte", Byte.TYPE);
        map.put("char", Character.TYPE);
        map.put("short", Short.TYPE);
        map.put("int", Integer.TYPE);
        map.put("long", Long.TYPE);
        map.put("float", Float.TYPE);
        map.put("double", Double.TYPE);
        return map;
    }

}
