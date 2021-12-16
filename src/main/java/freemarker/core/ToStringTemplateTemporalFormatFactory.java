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

import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Gives a {@link TemplateTemporalFormat} that simply calls {@link Object#toString()}
 *
 * @Deprected TODO [FREEMARKER-35] I guess we shouldn't need this.
 *
 * @since 2.3.32
 */
class ToStringTemplateTemporalFormatFactory extends TemplateTemporalFormatFactory {

    static final ToStringTemplateTemporalFormatFactory INSTANCE = new ToStringTemplateTemporalFormatFactory();

    private ToStringTemplateTemporalFormatFactory() {
        // Not meant to be called from outside
    }

    @Override
    public TemplateTemporalFormat get(String params, Class<? extends Temporal> temporalClass, Locale locale, TimeZone timeZone, Environment env) throws
            TemplateValueFormatException {
        if (!params.isEmpty()) {
            throw new InvalidFormatParametersException("toString format doesn't support parameters");
        }
        return new ToStringTemplateTemporalFormat(timeZone);
    }
}