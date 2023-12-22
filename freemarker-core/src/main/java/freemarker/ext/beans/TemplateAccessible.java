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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;

/**
 * Indicates that the the annotated member can be exposed to templates; if the annotated member will be actually
 * exposed depends on the {@link ObjectWrapper} in use, and how that was configured. When used with
 * {@link BeansWrapper} or its subclasses, most notably with {@link DefaultObjectWrapper}, and you also set the
 * {@link MemberAccessPolicy} to a {@link WhitelistMemberAccessPolicy}, it will acts as if the members annotated with
 * this are in the whitelist. Note that adding something to the whitelist doesn't necessary make it visible from
 * templates; see {@link WhitelistMemberAccessPolicy} documentation.
 *
 * @since 2.3.30
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface TemplateAccessible {
}
