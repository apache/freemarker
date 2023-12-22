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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Returned by {@link MemberAccessPolicy#forClass(Class)}. The idea is that {@link MemberAccessPolicy#forClass(Class)}
 * is called once per class, and then the methods of the resulting {@link ClassMemberAccessPolicy} object will be
 * called for each member of the class. This can speed up the process as the class-specific lookups will be done only
 * once per class, not once per member.
 *
 * @since 2.3.30
 */
public interface ClassMemberAccessPolicy {
    boolean isMethodExposed(Method method);
    boolean isConstructorExposed(Constructor<?> constructor);
    boolean isFieldExposed(Field field);
}
