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
import static freemarker.test.hamcerst.Matchers.*;
import static org.hamcrest.CoreMatchers.*;
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
        Configuration conf = new Configuration(Configuration.VERSION_2_3_33);

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

    /**
     * Same as {@link #assertParsingResults(Consumer, MissingTimeZoneParserPolicy, Object...)} with 2nd argument {@link
     * MissingTimeZoneParserPolicy#ASSUME_CURRENT_TIME_ZONE}.
     */
    static protected void assertParsingResults(
            Consumer<Configurable> configurator,
            Object... inputsAndExpectedResults) throws TemplateException, TemplateValueFormatException {
        assertParsingResults(
                configurator,
                MissingTimeZoneParserPolicy.ASSUME_CURRENT_TIME_ZONE,
                inputsAndExpectedResults);
    }

    /**
     * Same as {@link #assertParsingResults(Consumer, MissingTimeZoneParserPolicy, Object...)}, but 2nd argument going
     * through all {@link MissingTimeZoneParserPolicy} values.
     */
    static protected void assertParsingResultsWithAllMissingTimeZonePolicies(
            Consumer<Configurable> configurator,
            Object... inputsAndExpectedResults) throws TemplateException, TemplateValueFormatException {
        for (MissingTimeZoneParserPolicy missingTimeZoneParserPolicy : MissingTimeZoneParserPolicy.values()) {
            assertParsingResults(
                    configurator,
                    MissingTimeZoneParserPolicy.ASSUME_CURRENT_TIME_ZONE,
                    inputsAndExpectedResults);
        }
    }

    /**
     * @param inputsAndExpectedResults
     *         Repeats this pattern: Parsed string ({@link String}), optional target temporal class ({@link Class}),
     *         expected result ({@link Temporal}). If the target temporal class is left out, it will detect it from the
     *         type of the expected result. ,
     */
    static protected void assertParsingResults(
            Consumer<Configurable> configurator,
            MissingTimeZoneParserPolicy missingTimeZoneParserPolicy,
            Object... inputsAndExpectedResults) throws TemplateException, TemplateValueFormatException {
        Configuration conf = new Configuration(Configuration.VERSION_2_3_33);
        conf.setTimeZone(DateUtil.UTC);
        conf.setLocale(Locale.US);

        configurator.accept(conf);

        Environment env = null;
        try {
            env = new Template(null, "", conf).createProcessingEnvironment(null, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        boolean hasTargetClasses = inputsAndExpectedResults.length >= 2 && inputsAndExpectedResults[1] instanceof Class;

        if (inputsAndExpectedResults.length == 0) {
            throw new IllegalArgumentException("inputsAndExpectedResults can't be empty.");
        }
        int i = 0;
        while (i < inputsAndExpectedResults.length) {
            Object value = inputsAndExpectedResults[i];

            if (!(value instanceof String)) {
                throw new IllegalArgumentException("inputsAndExpectedResults[" + i + "] should be a "
                        + "String, but it's this " + ClassUtil.getShortClassNameOfObject(value) + ": " + value);
            }
            String stringToParse = (String) value;
            i++;

            final Class<? extends Temporal> temporalClass;
            final Temporal expectedResult;
            if (i == inputsAndExpectedResults.length) {
                throw new IllegalArgumentException("inputsAndExpectedResults[" + i + "] is out of array bounds; "
                        + "expecting a Temporal or Class<? extends Temporal> there.");
            }
            value = inputsAndExpectedResults[i];
            if (value instanceof Temporal) {
                expectedResult = (Temporal) value;
                temporalClass = expectedResult.getClass();
                i++;
            } else if (value instanceof Class) {
                temporalClass = (Class<? extends Temporal>) value;
                i++;

                if (i == inputsAndExpectedResults.length) {
                    throw new IllegalArgumentException("inputsAndExpectedResults[" + i + "] is out of array bounds; "
                            + "expecting a Temporal there.");
                }
                value = inputsAndExpectedResults[i];
                if (!(value instanceof Temporal)) {
                    throw new IllegalArgumentException("inputsAndExpectedResults[" + i + "] should be a "
                            + "Temporal, but it's this " + ClassUtil.getShortClassNameOfObject(value) + ": " + value);
                }
                expectedResult = (Temporal) value;
                i++;
            } else {
                throw new IllegalArgumentException("inputsAndExpectedResults[" + i + "] should be a "
                        + "Temporal or Class<? extends Temporal>, but it's this "
                        + ClassUtil.getShortClassNameOfObject(value) + ": " + value);
            }

            TemplateTemporalFormat templateTemporalFormat = env.getTemplateTemporalFormat(temporalClass);

            Temporal actualResult;
            {
                Object actualResultObject = templateTemporalFormat.parse(stringToParse, missingTimeZoneParserPolicy);
                if (actualResultObject instanceof Temporal) {
                    actualResult = (Temporal) actualResultObject;
                } else if (actualResultObject instanceof TemplateTemporalModel) {
                    actualResult = ((TemplateTemporalModel) actualResultObject).getAsTemporal();
                } else {
                    throw new AssertionError(
                            "Parsing result of " + jQuote(stringToParse) + " is not of an expected type: "
                                    + ClassUtil.getShortClassNameOfObject(actualResultObject));
                }
            }

            if (!expectedResult.equals(actualResult)) {
                throw new AssertionError(
                        "Parsing result of " + jQuote(stringToParse) + " "
                                + "(with temporalFormat[" + temporalClass.getSimpleName() + "]="
                                + jQuote(env.getTemporalFormat(temporalClass)) + ", "
                                + "missingTimeZoneParserPolicy=" + missingTimeZoneParserPolicy + ", "
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
            String stringToParse,
            Class<? extends Temporal> temporalClass,
            Consumer<TemplateValueFormatException> exceptionAssertions) throws TemplateException,
            TemplateValueFormatException {
        assertParsingFails(
                configurator,
                MissingTimeZoneParserPolicy.ASSUME_CURRENT_TIME_ZONE,
                stringToParse, temporalClass,
                exceptionAssertions);
    }

    static protected void assertParsingFails(
            Consumer<Configurable> configurator,
            MissingTimeZoneParserPolicy missingTimeZoneParserPolicy,
            String stringToParse, Class<? extends Temporal> temporalClass,
            Consumer<TemplateValueFormatException> exceptionAssertions) throws TemplateException,
            TemplateValueFormatException {
        Configuration conf = new Configuration(Configuration.VERSION_2_3_33);
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
            templateTemporalFormat.parse(stringToParse, missingTimeZoneParserPolicy);
            fail("Parsing " + jQuote(stringToParse) + " with " + templateTemporalFormat + " should have failed.");
        } catch (TemplateValueFormatException e) {
            exceptionAssertions.accept(e);
        }
    }

    protected static void assertMissingTimeZoneFailPolicyTriggered(TemplateValueFormatException e) {
        assertThat(
                e.getMessage(),
                allOf(
                        containsStringIgnoringCase("doesn't contain time zone, nor offset"),
                        containsString(MissingTimeZoneParserPolicy.class.getName() + "."
                                + MissingTimeZoneParserPolicy.FAIL)));
    }

}
