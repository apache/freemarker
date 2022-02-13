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

import static freemarker.template.utility.StringUtil.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.function.Consumer;

import freemarker.template.Configuration;
import freemarker.template.SimpleTemporal;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateTemporalModel;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.DateUtil;

/**
 * For {@link Environment}-level tests related to {@link TemplateTemporalFormat}-s.
 */
public abstract class AbstractTemporalFormatTest {

    static protected String formatTemporal(Consumer<Configurable> configurator, Temporal... values) throws
            TemplateException {
        Configuration conf = new Configuration(Configuration.VERSION_2_3_32);

        configurator.accept(conf);

        Environment env = null;
        try {
            env = new Template(null, "", conf).createProcessingEnvironment(null, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        StringBuilder sb = new StringBuilder();
        for (Temporal value : values) {
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(env.formatTemporalToPlainText(new SimpleTemporal(value), null, false));
        }

        return sb.toString();
    }

    static protected void assertParsingResults(
            Consumer<Configurable> configurator,
            Object... stringsAndExpectedResults) throws TemplateException, TemplateValueFormatException {
        Configuration conf = new Configuration(Configuration.VERSION_2_3_32);
        conf.setTimeZone(DateUtil.UTC);
        conf.setLocale(Locale.US);

        configurator.accept(conf);

        Environment env = null;
        try {
            env = new Template(null, "", conf).createProcessingEnvironment(null, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (stringsAndExpectedResults.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "stringsAndExpectedResults.length must be even, but was " + stringsAndExpectedResults.length + ".");
        }
        for (int i = 0; i < stringsAndExpectedResults.length; i += 2) {
            Object value = stringsAndExpectedResults[i];
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("stringsAndExpectedResults[" + i + "] should be a String");
            }
            String string = (String) value;

            value = stringsAndExpectedResults[i + 1];
            if (!(value instanceof Temporal)) {
                throw new IllegalArgumentException("stringsAndExpectedResults[" + (i + 1) + "] should be a Temporal");
            }
            Temporal expectedResult = (Temporal) value;

            Class<? extends Temporal> temporalClass = expectedResult.getClass();
            TemplateTemporalFormat templateTemporalFormat = env.getTemplateTemporalFormat(temporalClass);

            Temporal actualResult;
            {
                Object actualResultObject = templateTemporalFormat.parse(string);
                if (actualResultObject instanceof Temporal) {
                    actualResult = (Temporal) actualResultObject;
                } else if (actualResultObject instanceof TemplateTemporalModel) {
                    actualResult = ((TemplateTemporalModel) actualResultObject).getAsTemporal();
                } else {
                    throw new AssertionError(
                            "Parsing result of " + jQuote(string) + " is not of an expected type: "
                                    + ClassUtil.getShortClassNameOfObject(actualResultObject));
                }
            }

            if (!expectedResult.equals(actualResult)) {
                throw new AssertionError(
                        "Parsing result of " + jQuote(string) + " "
                                + "(with temporalFormat[" + temporalClass.getSimpleName() + "]="
                                + jQuote(env.getTemporalFormat(temporalClass)) + ", "
                                + "timeZone=" + jQuote(env.getTimeZone().toZoneId()) + ", "
                                + "locale=" + jQuote(env.getLocale()) + ") "
                                + "differs from expected.\n"
                                + "Expected: " + expectedResult + "\n"
                                + "Actual:   " + actualResult);
            }
        }
    }

    static protected void assertParsingFails(
            Consumer<Configurable> configurator,
            String parsed,
            Class<? extends Temporal> temporalClass,
            Consumer<TemplateValueFormatException> exceptionAssertions) throws TemplateException,
            TemplateValueFormatException {
        Configuration conf = new Configuration(Configuration.VERSION_2_3_32);
        conf.setTimeZone(DateUtil.UTC);
        conf.setLocale(Locale.US);

        configurator.accept(conf);

        Environment env = null;
        try {
            env = new Template(null, "", conf).createProcessingEnvironment(null, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        TemplateTemporalFormat templateTemporalFormat = env.getTemplateTemporalFormat(temporalClass);

        try {
            templateTemporalFormat.parse(parsed);
            fail("Parsing " + jQuote(parsed) + " with " + templateTemporalFormat + " should have failed.");
        } catch (TemplateValueFormatException e) {
            exceptionAssertions.accept(e);
        }
    }

}
