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

package freemarker.debug;

import java.util.EventObject;

/**
 * Event describing a suspension of an environment (ie because it hit a
 * breakpoint).
 */
public class EnvironmentSuspendedEvent extends EventObject
{
    private static final long serialVersionUID = 1L;

    private final String name;
    private final int line;
    private final DebuggedEnvironment env;

    public EnvironmentSuspendedEvent(Object source, String templateName, int line, DebuggedEnvironment env)
    {
        super(source);
        this.name = templateName;
        this.line = line;
        this.env = env;
    }

    /**
     * The name of the template where the execution of the environment
     * was suspended
     * @return String the template name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * The line number in the template where the execution of the environment
     * was suspended.
     * @return int the line number
     */
    public int getLine()
    {
        return line;
    }

    /**
     * The environment that was suspended
     * @return DebuggedEnvironment
     */
    public DebuggedEnvironment getEnvironment()
    {
        return env;
    }
}
