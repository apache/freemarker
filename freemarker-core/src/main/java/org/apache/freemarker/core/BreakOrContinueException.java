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

package org.apache.freemarker.core;

/**
 * Used for implementing #break and #continue. 
 */
// TODO [FM3] This is not a good mechanism (like what if we have <#list ...><@m><#break><@></#list>, and inside `m`
// there's <#list ...><#nested></#list>)
class BreakOrContinueException extends FlowControlException {
    static final BreakOrContinueException BREAK_INSTANCE = new BreakOrContinueException();
    static final BreakOrContinueException CONTINUE_INSTANCE = new BreakOrContinueException();
    
    private BreakOrContinueException() { }
}