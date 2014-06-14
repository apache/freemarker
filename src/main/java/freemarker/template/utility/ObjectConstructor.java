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

package freemarker.template.utility;

import java.util.List;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * An object that you can make available in a template
 * to instantiate arbitrary beans-wrapped objects in a template.
 * Beware of this class's security implications. It allows
 * the instantiation of arbitrary objects and invoking 
 * methods on them. Usage is something like:
 * <br>
 * <br>myDataModel.put("objectConstructor", new ObjectConstructor());
 * <br>
 * <br>And then from your FTL code:
 * <br>
 * <br>&lt;#assign aList = objectConstructor("java.util.ArrayList", 100)&gt;
 */
public class ObjectConstructor implements TemplateMethodModelEx
{
    public Object exec(List args) throws TemplateModelException {
        if (args.isEmpty()) {
            throw new TemplateModelException("This method must have at least one argument, the name of the class to instantiate.");
        }
        String classname = args.get(0).toString();
        Class cl = null;
        try {
            cl = ClassUtil.forName(classname);
        }
        catch (Exception e) {
            throw new TemplateModelException(e.getMessage());
        }
        BeansWrapper bw = BeansWrapper.getDefaultInstance();
        Object obj = bw.newInstance(cl, args.subList(1, args.size()));
        return bw.wrap(obj);
    }
}
