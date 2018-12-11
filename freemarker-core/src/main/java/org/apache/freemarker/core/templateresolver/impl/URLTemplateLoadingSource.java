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
package org.apache.freemarker.core.templateresolver.impl;

import java.net.URL;

import org.apache.freemarker.core.templateresolver.TemplateLoadingSource;
import org.apache.freemarker.core.util._NullArgumentException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressWarnings("serial")
public class URLTemplateLoadingSource implements TemplateLoadingSource {

    private final URL url;

    public URLTemplateLoadingSource(URL url) {
        _NullArgumentException.check("url", url);
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }

    @Override
    @SuppressFBWarnings("EQ_CHECK_FOR_OPERAND_NOT_COMPATIBLE_WITH_THIS")
    public boolean equals(Object obj) {
        return url.equals(obj);
    }

    @Override
    public String toString() {
        return url.toString();
    }
    
}
