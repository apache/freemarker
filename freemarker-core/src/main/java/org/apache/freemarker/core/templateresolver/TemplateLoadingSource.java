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
package org.apache.freemarker.core.templateresolver;

import org.apache.freemarker.core.templateresolver.impl.ByteArrayTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.FileTemplateLoader;

import java.io.Serializable;
import java.util.HashMap;

/**
 * The point of {@link TemplateLoadingSource} is that with their {@link Object#equals(Object)} method we can tell if two
 * cache entries were generated from the same physical resource or not. Comparing the template names isn't enough,
 * because a {@link TemplateLoader} may use some kind of fallback mechanism, such as delegating to other
 * {@link TemplateLoader}-s until the template is found. Like if we have two {@link FileTemplateLoader}-s with different
 * physical root directories, both can contain {@code "foo/bar.f3ah"}, but obviously the two files aren't the same.
 * 
 * <p>
 * When implementing this interface, check these:
 * 
 * <ul>
 * <li>{@link Object#equals(Object)} must not be based on object identity, because two instances of
 * {@link TemplateLoadingSource} that describe the same resource must be equivalent.
 * 
 * <li>Each {@link TemplateLoader} implementation should have its own {@link TemplateLoadingSource} implementation, so
 * that {@link TemplateLoadingSource}-s coming from different {@link TemplateLoader} implementations can't be
 * accidentally equal (according to {@link Object#equals(Object)}).
 * 
 * <li>{@link Object#equals(Object)} must still work properly if there are multiple instances of the same
 * {@link TemplateLoader} implementation. Like if you have an implementation that loads from a database table, the
 * {@link TemplateLoadingSource} should certainly contain the JDBC connection string, the table name and the row ID, not
 * just the row ID.
 * 
 * <li>Together with {@link Object#equals(Object)}, {@link Object#hashCode()} must be also overridden. The template
 * source may be used as a {@link HashMap} key.
 * 
 * <li>Because {@link TemplateLoadingSource}-s are {@link Serializable}, they can't contain non-{@link Serializable}
 * fields. Most notably, a reference to the creator {@link TemplateLoader}, so if it's an inner class of the
 * {@link TemplateLoader}, it should be static.
 * 
 * <li>Consider that cache entries, in which the source is stored, may move between JVM-s (because of clustering with a
 * distributed cache). Thus they should remain meaningful for the purpose of {@link Object#equals(Object)} even in
 * another JVM.
 * 
 * <li>A {@link TemplateLoader} may chose not to support distributed caches, like {@link ByteArrayTemplateLoader}
 * doesn't support that for example. In that case the serialization related points above can be ignored, but the
 * {@link TemplateLoadingSource} implementation should define the {@code writeObject} method (a Java serialization
 * feature) and throw an exception there to block serialization attempts.
 * </ul>
 */
public interface TemplateLoadingSource extends Serializable {
    // Empty
}
