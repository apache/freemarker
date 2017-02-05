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
package freemarker.cache;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Stores shared state between {@link TemplateLoader} operations that are executed close to each other in the same
 * thread. For example, a {@link TemplateLoader} that reads from a database might wants to store the database
 * connection in it for reuse. The goal of sessions is mostly to increase performance. However, because a
 * {@link TemplateCache#getTemplate(String, java.util.Locale, Object, String, boolean)} call is executed inside a single
 * session, sessions can be also be utilized to ensure that the template lookup (see {@link TemplateLookupStrategy})
 * happens on a consistent view (a snapshot) of the backing storage, if the backing storage mechanism supports such
 * thing.
 * 
 * <p>
 * The {@link TemplateLoaderSession} implementation is (usually) specific to the {@link TemplateLoader}
 * implementation. If your {@link TemplateLoader} implementation can't take advantage of sessions, you don't have to
 * implement this interface, just return {@code null} for {@link TemplateLoader#createSession()}.
 * 
 * <p>
 * {@link TemplateLoaderSession}-s should be lazy, that is, creating an instance should be very fast and should not
 * cause I/O. Only when (and if ever) the shared resource stored in the session is needed for the first time should the
 * shared resource be initialized.
 *
 * <p>
 * {@link TemplateLoaderSession}-s need not be thread safe.
 */
public interface TemplateLoaderSession {

    /**
     * Closes this session, freeing any resources it holds. Further operations involving this session should fail, with
     * the exception of {@link #close()} itself, which should be silently ignored.
     * 
     * <p>
     * The {@link Reader} or {@link InputStream} contained in the {@link TemplateLoadingResult} must be closed before
     * {@link #close()} is called on the session in which the {@link TemplateLoadingResult} was created. Except, if
     * closing the {@link Reader} or {@link InputStream} has thrown an exception, the caller should just proceed with
     * closing the session regardless. After {@link #close()} was called on the session, the methods of the
     * {@link Reader} or {@link InputStream} is allowed to throw an exception, or behave in any other erratic way.
     * (Because the caller of this interface is usually FreeMarker (the {@link TemplateCache}), normally you don't have
     * to deal with these rules, but it's useful to know the expectations if you implement
     * {@link TemplateLoaderSession}.)
     * 
     * <p>The caller of {@link TemplateLoader#createSession()} has to guarantee that {@link #close()} will be called on
     * the created session.
     */
    public void close() throws IOException;
    
    /**
     * Tells if this session is closed.
     */
    public boolean isClosed();

}
