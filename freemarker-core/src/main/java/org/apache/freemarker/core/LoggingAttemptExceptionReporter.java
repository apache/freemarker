/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link AttemptExceptionReporter} implementation, factored out from {@link AttemptExceptionReporter} so that
 * we can have static field.
 */
class LoggingAttemptExceptionReporter implements AttemptExceptionReporter {


    private static final Logger LOG = LoggerFactory.getLogger(LoggingAttemptExceptionReporter.class);

    private final boolean logAsWarn;

    public LoggingAttemptExceptionReporter(boolean logAsWarn) {
        this.logAsWarn = logAsWarn;
    }

    public void report(TemplateException te, Environment env) {
        String message = "Error executing FreeMarker template part in the #attempt block";
        if (!logAsWarn) {
            LOG.error(message, te);
        } else {
            LOG.warn(message, te);
        }
    }

}
