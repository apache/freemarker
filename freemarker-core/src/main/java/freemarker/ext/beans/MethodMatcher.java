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

package freemarker.ext.beans;

import java.lang.reflect.Method;

/**
 * {@link MemberMatcher} for methods.
 *
 * <p>The return type (and visibility) of the methods will be ignored, only the method name and its parameter types
 * matter. (The {@link MemberAccessPolicy}, and even {@link BeansWrapper} itself will still filter by visibility, it's
 * just not the duty of the {@link MemberMatcher}.)
 *
 * @since 2.3.30
 */
final class MethodMatcher extends MemberMatcher<Method, ExecutableMemberSignature> {
    @Override
    protected ExecutableMemberSignature toMemberSignature(Method member) {
        return new ExecutableMemberSignature(member);
    }

    @Override
    protected boolean matchInUpperBoundTypeSubtypes() {
        return true;
    }
}
