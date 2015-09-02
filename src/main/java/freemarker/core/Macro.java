/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package freemarker.core;

import freemarker.template.TemplateModel;

/**
 * Exist for backward compatibility only; it has represented a macro or function declaration in the AST. The current
 * representation isn't a public class, but is {@code insteanceof} this for backward compatibility.
 * 
 * <p>
 * Historical note: This class exists for backward compatibility with 2.3. 2.4 has introduced {@link UnboundTemplate}-s,
 * thus, the definition of a callable and the runtime callable value has become to two different things:
 * {@link UnboundCallable} and {@link BoundCallable}. Both extends this class for backward compatibility.
 * 
 * @see UnboundCallable
 * @see BoundCallable
 * 
 * @deprecated Subject to be changed or renamed any time; no "stable" replacement exists yet.
 */
@Deprecated
public abstract class Macro extends TemplateElement implements TemplateModel {
    
    // Not public
    Macro() { }

    public abstract String getCatchAll();

    public abstract String[] getArgumentNames();

    public abstract String getName();

    public abstract boolean isFunction();
    
}
