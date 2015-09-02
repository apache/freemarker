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


/**
 * Used be {@link TemplateLoader#getLastModified(Object)} to indicate an error getting the last modification date. That
 * should be just an {@link IOException}, but due to backward compatibility constraints that wasn't possible;
 * {@link TemplateLoader#getLastModified(Object)} doesn't allow throwing checked exception.
 * 
 * @since 2.4.0
 */
public class GetLastModifiedException extends RuntimeException {

    public GetLastModifiedException(String message, Throwable cause) {
        super(message, cause);
    }

    public GetLastModifiedException(String message) {
        super(message);
    }

}
