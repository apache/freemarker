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

package freemarker.test.utility;

import java.io.IOException;
import java.util.Map;

import freemarker.core.Environment;
import freemarker.core._CoreAPI;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.utility.ClassUtil;

public class AssertDirective implements TemplateDirectiveModel {

    public static AssertDirective INSTANCE = new AssertDirective();
    
    private static final String TEST_PARAM = "test";
    
    private AssertDirective() { }
    
    public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
            throws TemplateException, IOException {
        TemplateModel test = null;
        for (Object paramEnt  : params.entrySet()) {
            Map.Entry<String, TemplateModel> param = (Map.Entry) paramEnt;
            String paramName = param.getKey();
            if (paramName.equals(TEST_PARAM)) {
                test = param.getValue();
            } else {
                throw new UnsupportedParameterException(paramName, env);
            }
        }
        if (test == null) {
            throw new MissingRequiredParameterException(TEST_PARAM, env);
        }
        _CoreAPI.checkHasNoNestedContent(body);
        
        if (!(test instanceof TemplateBooleanModel)) {
            throw new AssertationFailedInTemplateException("Assertion failed:\n"
                    + "The value had to be boolean, but it was of type" + ClassUtil.getFTLTypeDescription(test),
                    env);
        }
        if (!((TemplateBooleanModel) test).getAsBoolean()) {
            throw new AssertationFailedInTemplateException("Assertion failed:\n"
                    + "the value was false.",
                    env);
        }
        
    }

}
