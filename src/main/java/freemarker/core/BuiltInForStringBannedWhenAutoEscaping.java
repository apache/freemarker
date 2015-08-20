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

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * A string built-in whose usage is banned when auto-escaping with a markup-output format is active.
 * This is just a marker; the actual checking is in {@code FTL.jj}. 
 */
abstract class BuiltInForStringBannedWhenAutoEscaping extends SpecialBuiltIn {
    
    @Override
    TemplateModel _eval(Environment env)
    throws TemplateException {
        return calculateResult(BuiltInForString.getTargetString(target, env), env);
    }
    
    abstract TemplateModel calculateResult(String s, Environment env) throws TemplateException;
    
}