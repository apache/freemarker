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

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;

/**
 * Used for implementing the "truncate" family of built-ins. There are several variations of the "truncate" built-ins,
 * each has a corresponding method here. See
 * {@link #truncateM(String, int, TemplateModel, Integer, Environment)}
 * as the starting point.
 *
 * <p>New methods may be added in later versions, whoever they won't be abstract for backward compatibility.
 *
 * @see Configurable#setTruncateBuiltinAlgorithm(TruncateBuiltinAlgorithm)
 *
 * @since 2.3.29
 */
public abstract class TruncateBuiltinAlgorithm {

    /**
     * Corresponds to {@code ?truncate_m(...)} in templates. This method decides automatically if it will truncate at
     * word boundary (see {@link #truncateWM}) or at character boundary (see {@link #truncateCM}). While it depends
     * on the implementation, the idea is that it should truncate at word boundary, unless that gives a too short
     * string, in which case it falls back to truncation at character duration.
     *
     * <p>The terminator and the return value can be {@link TemplateMarkupOutputModel} (FTL markup output type), not
     * just {@link String} (FTL string type), hence the "m" in the name.
     *
     * @param s
     *            The input string whose length need to be limited. The caller (the FreeMarker core normally) is
     *            expected to guarantee that this won't be {@code null}.
     *
     * @param maxLength
     *            The maximum length of the returned string, although the algorithm need not guarantee this strictly.
     *            For example, if this is less than the length of the {@code terminator} string, then most algorithms
     *            should still return the {@code terminator} string. Or, some sophisticated algorithm may counts in
     *            letters differently depending on their visual width. The goal is usually to prevent unusually long
     *            string values to ruin visual layout, while showing clearly to the user that the end of the string
     *            was cut off. If the input string is not longer than the maximum length, then it should be returned
     *            as is. The caller (the FreeMarker core normally) is expected to guarantee that this will be at
     *            least 0.
     *
     * @param terminator
     *            The string or markup to show at the end of the returned string if the string was actually truncated.
     *            This can be {@code null}, in which case the default terminator of the algorithm will be used. It
     *            can be an FTL string (a {@link TemplateScalarModel}) of any length (including 0), or a
     *            {@link TemplateMarkupOutputModel} (typically HTML markup). If it's {@link TemplateMarkupOutputModel},
     *            then the result is {@link TemplateMarkupOutputModel} of the same output format as well, otherwise
     *            it can remain {@link TemplateScalarModel}. Note that the length of the terminator counts into the
     *            result length that shouldn't be exceed ({@code maxLength}) (or at least the algorithm should make
     *            an effort to avoid that).
     *
     * @param terminatorLength
     *            The assumed length of the terminator. If this is {@code null} (and typically it is), then the method
     *            decides the length of the terminator. If this is not {@code null}, then the method must pretend
     *            that the terminator length is this. This can be used to specify the visual length of a terminator
     *            explicitly, which can't always be decided well programmatically.
     *
     * @param env
     *            The runtime environment from which this algorithm was called. The caller (the FreeMarker core
     *            normally) is expected to guarantee that this won't be {@code null}.
     *
     * @return The truncated text, which is either a {@link TemplateScalarModel} (FTL string), or a
     * {@link TemplateMarkupOutputModel}.
     *
     * @throws TemplateException
     *             If anything goes wrong during truncating. It's unlikely that an implementation will need this though.
     */
    public abstract TemplateModel truncateM(
            String s, int maxLength, TemplateModel terminator, Integer terminatorLength,
            Environment env) throws TemplateException;

    /**
     * Corresponds to {@code ?truncate(...)} in templates.
     * Similar to {@link #truncateM(String, int, TemplateModel, Integer, Environment)}, but only allows
     * an FTL string as terminator, and thence the return value is always an FTL string as well (not
     * {@link TemplateMarkupOutputModel}).
     */
    public abstract TemplateScalarModel truncate(
            String s, int maxLength, TemplateScalarModel terminator, Integer terminatorLength,
            Environment env) throws TemplateException;

    /**
     * Corresponds to {@code ?truncate_w(...)} in templates.
     * Same as {@link #truncateWM(String, int, TemplateModel, Integer, Environment)}, but only allows
     * an FTL string as terminator, and thence the return value is always an FTL string as well (not
     * {@link TemplateMarkupOutputModel}).
     */
    public abstract TemplateScalarModel truncateW(
            String s, int maxLength, TemplateScalarModel terminator, Integer terminatorLength,
            Environment env) throws TemplateException;

    /**
     * Corresponds to {@code ?truncate_w_m(...)} in templates.
     * Similar to {@link #truncateM(String, int, TemplateModel, Integer, Environment)}, but the
     * truncation should happen at word boundary (hence the "w"). That is, the truncation isn't allowed to truncate a
     * word. What counts as a word, is up to the implementation, but at least in {@link DefaultTruncateBuiltinAlgorithm}
     * words are the sections that are separated by whitespace (so punctuation doesn't separate words).
     */
    public abstract TemplateModel truncateWM(
            String s, int maxLength, TemplateModel terminator, Integer terminatorLength,
            Environment env) throws TemplateException;

    /**
     * Corresponds to {@code ?truncate_c_m(...)} in templates.
     * Same as {@link #truncateCM(String, int, TemplateModel, Integer, Environment)}, but only allows
     * an FTL string as terminator, and thence the return value is always an FTL string as well (not markup).
     */
    public abstract TemplateScalarModel truncateC(
            String s, int maxLength, TemplateScalarModel terminator, Integer terminatorLength,
            Environment env) throws TemplateException;

    /**
     * Corresponds to {@code ?truncate_c_m(...)} in templates.
     * Similar to {@link #truncateM(String, int, TemplateModel, Integer, Environment)}, but the
     * truncation should not prefer truncating at word boundaries over the closer approximation of the desired {@code
     * maxLength}. Hence, we say that it truncates at character boundary (hence the "c").
     */
    public abstract TemplateModel truncateCM(
            String s, int maxLength, TemplateModel terminator, Integer terminatorLength,
            Environment env) throws TemplateException;

}
