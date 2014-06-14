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

import java.io.IOException;

import freemarker.debug.impl.DebuggerService;
import freemarker.template.TemplateException;

/**
 */
public class DebugBreak extends TemplateElement
{
    public DebugBreak(TemplateElement nestedBlock)
    {
        this.nestedBlock = nestedBlock;
        nestedBlock.parent = this;
        copyLocationFrom(nestedBlock);
    }
    
    protected void accept(Environment env) throws TemplateException, IOException
    {
        if(!DebuggerService.suspendEnvironment(env, this.getTemplate().getName(), nestedBlock.getBeginLine()))
        {
            nestedBlock.accept(env);
        }
        else
        {
            throw new StopException(env, "Stopped by debugger");        
        }
    }

    protected String dump(boolean canonical) {
        if (canonical) {
            StringBuffer sb = new StringBuffer();
            sb.append("<#-- ");
            sb.append("debug break");
            if (nestedBlock == null) {
                sb.append(" /-->");
            } else {
                sb.append(" -->");
                sb.append(nestedBlock.getCanonicalForm());                
                sb.append("<#--/ debug break -->");
            }
            return sb.toString();
        } else {
            return "debug break";
        }
    }
    
    String getNodeTypeSymbol() {
        return "#debug_break";
    }

    int getParameterCount() {
        return 0;
    }

    Object getParameterValue(int idx) {
        throw new IndexOutOfBoundsException();
    }

    ParameterRole getParameterRole(int idx) {
        throw new IndexOutOfBoundsException();
    }
        
}
