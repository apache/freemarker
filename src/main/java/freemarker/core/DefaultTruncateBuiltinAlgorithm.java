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

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.NullArgumentException;

/**
 * The default {@link TruncateBuiltinAlgorithm} implementation; see
 * {@link Configurable#setTruncateBuiltinAlgorithm(TruncateBuiltinAlgorithm)}.
 * To know the properties of this {@link TruncateBuiltinAlgorithm} implementation, see the
 * {@linkplain DefaultTruncateBuiltinAlgorithm#DefaultTruncateBuiltinAlgorithm(String, Integer, Boolean,
 * TemplateMarkupOutputModel, Integer, Boolean, boolean, Double) constructor}. You can find more explanation and
 * examples in the documentation of the {@code truncate} built-in in the FreeMarker Manual.
 *
 * @since 2.3.29
 */
public class DefaultTruncateBuiltinAlgorithm extends TruncateBuiltinAlgorithm {

    /** Used by {@link #ASCII_INSTANCE} as the terminator. */
    public static final String STANDARD_ASCII_TERMINATOR = "[...]";

    /** Used by {@link #UNICODE_INSTANCE} as the terminator. */
    public static final String STANDARD_UNICODE_TERMINATOR = "[\u2026]";

    /**
     * Used by {@link #ASCII_INSTANCE} and {@link #UNICODE_INSTANCE} as the markup terminator;
     * HTML {@code <span class='truncateTerminator'>[&#8230;]</span>}, where {@code &#8230;} is the ellipsis (&#8230;)
     * character. Note that while the ellipsis character is not in US-ASCII, this still works safely regardless of
     * output charset, as {@code &#8230;} itself only contains US-ASCII characters.
     */
    public static final TemplateHTMLOutputModel STANDARD_M_TERMINATOR;
    static {
        try {
            STANDARD_M_TERMINATOR = HTMLOutputFormat.INSTANCE.fromMarkup(
                    "<span class='truncateTerminator'>[&#8230;]</span>");
        } catch (TemplateModelException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The value used in the constructor of {@link #ASCII_INSTANCE} and  {@link #UNICODE_INSTANCE} as the
     * {@code wordBoundaryMinLength} argument.
     */
    public static final double DEFAULT_WORD_BOUNDARY_MIN_LENGTH = 0.75;

    /** Used if {@link #getMTerminatorLength(TemplateMarkupOutputModel)} can't detect the length. */
    private static final int FALLBACK_M_TERMINATOR_LENGTH = 3;

    private enum TruncationMode {
        CHAR_BOUNDARY, WORD_BOUNDARY, AUTO
    }

    /**
     * Instance that uses {@code "[...]"} as the {@code defaultTerminator} constructor argument, and thus is
     * safe to use for all output charsets. Because of that, this is the default of
     * {@link Configurable#setTruncateBuiltinAlgorithm(TruncateBuiltinAlgorithm)}. The
     * {@code defaultMTerminator} (markup terminator) is {@link #STANDARD_M_TERMINATOR}, and the
     * {@code wordBoundaryMinLength} is {@link #DEFAULT_WORD_BOUNDARY_MIN_LENGTH}, and {@code addSpaceAtWordBoundary}
     * is {@code true}.
     */
    public static final DefaultTruncateBuiltinAlgorithm ASCII_INSTANCE = new DefaultTruncateBuiltinAlgorithm(
            STANDARD_ASCII_TERMINATOR, STANDARD_M_TERMINATOR, true);

    /**
     * Instance that uses {@code "[\u2026]"} as the {@code defaultTerminator} constructor argument, which contains
     * ellipsis character ({@code "\u2026"}, U+2026), and thus only works with UTF-8, and the cp125x charsets (like
     * cp1250), and with some other rarely used ones. It does not work (becomes to a question mark) with ISO-8859-x
     * charsets (like ISO-8859-1), which are probably the most often used charsets after UTF-8.
     *
     * <p>The {@code defaultMTerminator} (markup terminator) is {@link #STANDARD_M_TERMINATOR}, and the
     * {@code wordBoundaryMinLength} is {@link #DEFAULT_WORD_BOUNDARY_MIN_LENGTH}, and {@code addSpaceAtWordBoundary}
     * is {@code true}.
     */
    public static final DefaultTruncateBuiltinAlgorithm UNICODE_INSTANCE = new DefaultTruncateBuiltinAlgorithm(
            STANDARD_UNICODE_TERMINATOR, STANDARD_M_TERMINATOR, true);

    private final TemplateScalarModel defaultTerminator;
    private final int defaultTerminatorLength;
    private final boolean defaultTerminatorRemovesDots;

    private final TemplateMarkupOutputModel<?> defaultMTerminator;
    private final Integer defaultMTerminatorLength;
    private final boolean defaultMTerminatorRemovesDots;

    private final double wordBoundaryMinLength;
    private final boolean addSpaceAtWordBoundary;

    /**
     * Creates an instance with a string (plain text) terminator and a markup terminator.
     * See parameters in {@link #DefaultTruncateBuiltinAlgorithm(String, Integer, Boolean, TemplateMarkupOutputModel,
     * Integer, Boolean, boolean, Double)}; the missing parameters will be {@code null}.
     */
    public DefaultTruncateBuiltinAlgorithm(
            String defaultTerminator,
            TemplateMarkupOutputModel<?> defaultMTerminator,
            boolean addSpaceAtWordBoundary) {
        this(
                defaultTerminator, null, null,
                defaultMTerminator, null, null,
                addSpaceAtWordBoundary, null);
    }

    /**
     * Creates an instance with string (plain text) terminator.
     * See parameters in {@link #DefaultTruncateBuiltinAlgorithm(String, Integer, Boolean, TemplateMarkupOutputModel,
     * Integer, Boolean, boolean, Double)}; the missing parameters will be {@code null}.
     */
    public DefaultTruncateBuiltinAlgorithm(
            String defaultTerminator,
            boolean addSpaceAtWordBoundary) {
        this(
                defaultTerminator, null, null,
                null, null, null,
                addSpaceAtWordBoundary, null);
    }

    /**
     * Creates an instance with markup terminator.
     * @param defaultTerminator
     *            The terminator to use if the invocation (like {@code s?truncate(20)}) doesn't specify it. The
     *            terminator is the text appended after a truncated string, to indicate that it was truncated.
     *            Typically it's {@code "[...]"} or {@code "..."}, or the same with UNICODE ellipsis character.
     * @param defaultTerminatorLength
     *            The assumed length of {@code defaultTerminator}, or {@code null} if it should be get via
     *            {@code defaultTerminator.length()}.
     * @param defaultTerminatorRemovesDots
     *            Whether dots and ellipsis characters that the {@code defaultTerminator} touches should be removed. If
     *            {@code null}, this will be auto-detected based on if {@code defaultTerminator} starts with dot or
     *            ellipsis. The goal is to avoid outcomes where we have more dots next to each other than there are in
     *            the terminator.
     * @param defaultMTerminator
     *            Similar to {@code defaultTerminator}, but is markup instead of plain text. This can be {@code null},
     *            in which case {@code defaultTerminator} will be used even if {@code ?truncate_m} or similar built-in
     *            is called.
     * @param defaultMTerminatorLength
     *            The assumed length of the terminator, or {@code null} if it should be get via
     *            {@link #getMTerminatorLength}.
     * @param defaultMTerminatorRemovesDots
     *            Similar to {@code defaultTerminatorRemovesDots}, but for {@code defaultMTerminator}. If {@code
     *            null}, and {@code defaultMTerminator} is HTML/XML/XHTML, then it will be examined of the
     *            first character of the terminator that's outside a HTML/XML tag or comment is dot or ellipsis
     *            (after resolving numerical character references). For other kind of markup it defaults to {@code
     *            true}, to be on the safe side.
     * @param addSpaceAtWordBoundary,
     *            Whether to add a space before the terminator if the truncation happens directly after the end of a
     *            word. For example, when "too long sentence" is truncated, it will be a like "too long [...]"
     *            instead of "too long[...]". When the truncation happens inside a word, this has on effect, i.e., it
     *            will be always like "too long se[...]" (no space before the terminator). Note that only whitespace is
     *            considered to be a word separator, not punctuation, so if this is {@code true}, you get results
     *            like "Some sentence. [...]".
     * @param wordBoundaryMinLength
     *            Used when {@link #truncate} or {@link #truncateM} has to decide between
     *            word boundary truncation and character boundary truncation; it's the minimum length, given as
     *            proportion of {@code maxLength}, that word boundary truncation has to produce. If the resulting
     *            length is less, we do character boundary truncation instead. For example, if {@code maxLength} is
     *            30, and this parameter is 0.85, then: 30*0.85 = 25.5, rounded up that's 26, so the resulting length
     *            must be at least 26. The result of character boundary truncation will be always accepted, even if its
     *            still too short. If this parameter is {@code null}, then {@link #DEFAULT_WORD_BOUNDARY_MIN_LENGTH}
     *            will be used. If this parameter is 0, then truncation always happens at word boundary. If this
     *            parameter is 1.0, then truncation doesn't prefer word boundaries over other places.
     */
    public DefaultTruncateBuiltinAlgorithm(
            String defaultTerminator, Integer defaultTerminatorLength,
            Boolean defaultTerminatorRemovesDots,
            TemplateMarkupOutputModel<?> defaultMTerminator, Integer defaultMTerminatorLength,
            Boolean defaultMTerminatorRemovesDots,
            boolean addSpaceAtWordBoundary, Double wordBoundaryMinLength) {
        NullArgumentException.check("defaultTerminator", defaultTerminator);
        this.defaultTerminator = new SimpleScalar(defaultTerminator);
        try {
            this.defaultTerminatorLength = defaultTerminatorLength != null ? defaultTerminatorLength
                    : defaultTerminator.length();

            this.defaultTerminatorRemovesDots = defaultTerminatorRemovesDots != null ? defaultTerminatorRemovesDots
                        : getTerminatorRemovesDots(defaultTerminator);
        } catch (TemplateModelException e) {
            throw new IllegalArgumentException("Failed to examine defaultTerminator", e);
        }

        this.defaultMTerminator = defaultMTerminator;
        if (defaultMTerminator != null) {
            try {
                this.defaultMTerminatorLength = defaultMTerminatorLength != null ? defaultMTerminatorLength
                        : getMTerminatorLength(defaultMTerminator);

                this.defaultMTerminatorRemovesDots = defaultMTerminatorRemovesDots != null
                        ? defaultMTerminatorRemovesDots
                        : getMTerminatorRemovesDots(defaultMTerminator);
            } catch (TemplateModelException e) {
                throw new IllegalArgumentException("Failed to examine defaultMTerminator", e);
            }
        } else {
            // There's no mTerminator, but these final fields must be set
            this.defaultMTerminatorLength = null;
            this.defaultMTerminatorRemovesDots = false;
        }

        if (wordBoundaryMinLength == null) {
            wordBoundaryMinLength = DEFAULT_WORD_BOUNDARY_MIN_LENGTH;
        } else if (wordBoundaryMinLength < 0 || wordBoundaryMinLength > 1) {
            throw new IllegalArgumentException("wordBoundaryMinLength must be between 0.0 and 1.0 (inclusive)");
        }
        this.wordBoundaryMinLength = wordBoundaryMinLength;

        this.addSpaceAtWordBoundary = addSpaceAtWordBoundary;
    }

    @Override
    public TemplateScalarModel truncate(
             String s, int maxLength,
            TemplateScalarModel terminator, Integer terminatorLength,
             Environment env) throws TemplateException {
        return (TemplateScalarModel) unifiedTruncate(
                s, maxLength, terminator, terminatorLength,
                TruncationMode.AUTO, false);
    }

    @Override
    public TemplateScalarModel truncateW(
            String s, int maxLength,
            TemplateScalarModel terminator, Integer terminatorLength,
            Environment env) throws TemplateException {
        return (TemplateScalarModel) unifiedTruncate(
                s, maxLength, terminator, terminatorLength,
                TruncationMode.WORD_BOUNDARY, false);
    }

    @Override
    public TemplateScalarModel truncateC(
            String s, int maxLength,
            TemplateScalarModel terminator, Integer terminatorLength,
            Environment env) throws TemplateException {
        return (TemplateScalarModel) unifiedTruncate(
                s, maxLength, terminator, terminatorLength,
                TruncationMode.CHAR_BOUNDARY, false);
    }

    @Override
    public TemplateModel truncateM(
            String s, int maxLength,
            TemplateModel terminator, Integer terminatorLength,
            Environment env) throws TemplateException {
        return unifiedTruncate(
                s, maxLength, terminator, terminatorLength,
                TruncationMode.AUTO, true);
    }

    @Override
    public TemplateModel truncateWM(
            String s, int maxLength,
            TemplateModel terminator, Integer terminatorLength,
            Environment env) throws TemplateException {
        return unifiedTruncate(
                s, maxLength, terminator, terminatorLength,
                TruncationMode.WORD_BOUNDARY, true);
    }

    @Override
    public TemplateModel truncateCM(
            String s, int maxLength,
            TemplateModel terminator, Integer terminatorLength,
            Environment env) throws TemplateException {
        return unifiedTruncate(
                s, maxLength, terminator, terminatorLength,
                TruncationMode.CHAR_BOUNDARY, true);
    }

    public String getDefaultTerminator() {
        try {
            return defaultTerminator.getAsString();
        } catch (TemplateModelException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * See similarly named parameter of {@link #DefaultTruncateBuiltinAlgorithm(String, Integer, Boolean,
     * TemplateMarkupOutputModel, Integer, Boolean, boolean, Double)} the construction}.
     */
    public int getDefaultTerminatorLength() {
        return defaultTerminatorLength;
    }

    /**
     * See similarly named parameter of {@link #DefaultTruncateBuiltinAlgorithm(String, Integer, Boolean,
     * TemplateMarkupOutputModel, Integer, Boolean, boolean, Double)} the construction}.
     */
    public boolean getDefaultTerminatorRemovesDots() {
        return defaultTerminatorRemovesDots;
    }

    /**
     * See similarly named parameter of {@link #DefaultTruncateBuiltinAlgorithm(String, Integer, Boolean,
     * TemplateMarkupOutputModel, Integer, Boolean, boolean, Double)} the construction}.
     */
    public TemplateMarkupOutputModel<?> getDefaultMTerminator() {
        return defaultMTerminator;
    }

    /**
     * See similarly named parameter of {@link #DefaultTruncateBuiltinAlgorithm(String, Integer, Boolean,
     * TemplateMarkupOutputModel, Integer, Boolean, boolean, Double)} the construction}.
     */
    public Integer getDefaultMTerminatorLength() {
        return defaultMTerminatorLength;
    }

    public boolean getDefaultMTerminatorRemovesDots() {
        return defaultMTerminatorRemovesDots;
    }

    /**
     * See similarly named parameter of {@link #DefaultTruncateBuiltinAlgorithm(String, Integer, Boolean,
     * TemplateMarkupOutputModel, Integer, Boolean, boolean, Double)} the construction}.
     */
    public double getWordBoundaryMinLength() {
        return wordBoundaryMinLength;
    }

    /**
     * See similarly named parameter of {@link #DefaultTruncateBuiltinAlgorithm(String, Integer, Boolean,
     * TemplateMarkupOutputModel, Integer, Boolean, boolean, Double)} the construction}.
     */
    public boolean getAddSpaceAtWordBoundary() {
        return addSpaceAtWordBoundary;
    }

    /**
     * Returns the (estimated) length of the argument terminator. It should only count characters that are visible for
     * the user (like in the web browser).
     *
     * <p>In the implementation in {@link DefaultTruncateBuiltinAlgorithm}, if the markup is HTML/XML/XHTML, then this
     * counts the characters outside tags and comments, and inside CDATA sections (ignoring the CDATA section
     * delimiters). Furthermore then it counts character and entity references as having length of 1. If the markup
     * is not HTML/XML/XHTML (or subclasses of those {@link MarkupOutputFormat}-s) then it doesn't know how to
     * measure it, and simply returns 3.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected int getMTerminatorLength(TemplateMarkupOutputModel<?> mTerminator) throws TemplateModelException {
        MarkupOutputFormat format = mTerminator.getOutputFormat();
        return isHTMLOrXML(format) ?
                getLengthWithoutTags(format.getMarkupString(mTerminator))
                : FALLBACK_M_TERMINATOR_LENGTH;
    }

    /**
     * Tells if the dots touched by the terminator text should be removed.
     *
     * <p>The implementation in {@link DefaultTruncateBuiltinAlgorithm} return {@code true} if the terminator starts
     * with dot (or ellipsis).
     *
     * @param terminator
     *            A {@link TemplateScalarModel} or {@link TemplateMarkupOutputModel}. Not {@code null}.
     */
    protected boolean getTerminatorRemovesDots(String terminator) throws TemplateModelException {
        return terminator.startsWith(".") || terminator.startsWith("\u2026");
    }

    /**
     * Same as {@link #getTerminatorRemovesDots(String)}, but invoked for a markup terminator.
     *
     * <p>The implementation in {@link DefaultTruncateBuiltinAlgorithm} will skip HTML/XML tags and comments,
     * and resolves relevant character references to find out if the first character is dot or ellipsis. But it only
     * does this for HTML/XMl/XHTML (or subclasses of those {@link MarkupOutputFormat}-s), otherwise it always
     * returns {@code true} to be on the safe side.
     */
    protected boolean getMTerminatorRemovesDots(TemplateMarkupOutputModel terminator) throws TemplateModelException {
        return isHTMLOrXML(terminator.getOutputFormat())
                ? doesHtmlOrXmlStartWithDot(terminator.getOutputFormat().getMarkupString(terminator))
                : true;
    }

    /**
     * Deals with both CB and WB truncation, hence it's unified.
     */
    private TemplateModel unifiedTruncate(
            String s, int maxLength,
            TemplateModel terminator, Integer terminatorLength,
            TruncationMode mode, boolean allowMarkupResult)
            throws TemplateException {
        if (s.length() <= maxLength) {
            return new SimpleScalar(s);
        }
        if (maxLength < 0) {
            throw new IllegalArgumentException("maxLength can't be negative");
        }

        Boolean terminatorRemovesDots;
        if (terminator == null) {
            if (allowMarkupResult && defaultMTerminator != null) {
                terminator = defaultMTerminator;
                terminatorLength = defaultMTerminatorLength;
                terminatorRemovesDots = defaultMTerminatorRemovesDots;
            } else {
                terminator = defaultTerminator;
                terminatorLength = defaultTerminatorLength;
                terminatorRemovesDots = defaultTerminatorRemovesDots;
            }
        } else {
            if (terminatorLength != null) {
                if (terminatorLength < 0) {
                    throw new IllegalArgumentException("terminatorLength can't be negative");
                }
            } else {
                terminatorLength = getTerminatorLength(terminator);
            }
            terminatorRemovesDots = null; // lazily calculated
        }

        StringBuilder truncatedS = unifiedTruncateWithoutTerminatorAdded(
                s,
                maxLength,
                terminator, terminatorLength, terminatorRemovesDots,
                mode);

        // The terminator is always shown, even if with that we exceed maxLength. Otherwise the user couldn't
        // see that the string was truncated.
        if (truncatedS == null || truncatedS.length() == 0) {
            return terminator;
        }

        if (terminator instanceof TemplateScalarModel) {
            truncatedS.append(((TemplateScalarModel) terminator).getAsString());
            return new SimpleScalar(truncatedS.toString());
        } else if (terminator instanceof TemplateMarkupOutputModel) {
            TemplateMarkupOutputModel markup = (TemplateMarkupOutputModel) terminator;
            MarkupOutputFormat outputFormat = markup.getOutputFormat();
            return outputFormat.concat(outputFormat.fromPlainTextByEscaping(truncatedS.toString()), markup);
        } else {
            throw new IllegalArgumentException("Unsupported terminator type: "
                    + ClassUtil.getFTLTypeDescription(terminator));
        }
    }

    private StringBuilder unifiedTruncateWithoutTerminatorAdded(
            String s, int maxLength,
            TemplateModel terminator, int terminatorLength, Boolean terminatorRemovesDots,
            TruncationMode mode) throws TemplateModelException {
        final int cbInitialLastCIdx = maxLength - terminatorLength - 1;
        int cbLastCIdx = cbInitialLastCIdx;

        // Why we do this here: If both Word Boundary and Character Boundary truncation will be attempted, then this way
        // we don't have to skip the WS twice.
        cbLastCIdx = skipTrailingWS(s, cbLastCIdx);
        if (cbLastCIdx < 0) {
            return null;
        }

        if (mode == TruncationMode.AUTO && wordBoundaryMinLength < 1.0 || mode == TruncationMode.WORD_BOUNDARY) {
            // Do word boundary truncation. Might not be possible due to minLength restriction (see below), in which
            // case truncedS stays null.
            StringBuilder truncedS = null;
            {
                final int wordTerminatorLength = addSpaceAtWordBoundary ? terminatorLength + 1 : terminatorLength;
                final int minIdx = mode == TruncationMode.AUTO
                        ? Math.max(((int) Math.ceil(maxLength * wordBoundaryMinLength)) - wordTerminatorLength - 1, 0)
                        : 0;

                int wbLastCIdx = Math.min(maxLength - wordTerminatorLength - 1, cbLastCIdx);
                boolean followingCIsWS
                        = s.length() > wbLastCIdx + 1 ? Character.isWhitespace(s.charAt(wbLastCIdx + 1)) : true;
                executeTruncateWB:
                while (wbLastCIdx >= minIdx) {
                    char curC = s.charAt(wbLastCIdx);
                    boolean curCIsWS = Character.isWhitespace(curC);
                    if (!curCIsWS && followingCIsWS) {
                        // Note how we avoid getMTerminatorRemovesDots until we absolutely need its result.
                        if (!addSpaceAtWordBoundary && isDot(curC)) {
                            if (terminatorRemovesDots == null) {
                                terminatorRemovesDots = getTerminatorRemovesDots(terminator);
                            }
                            if (terminatorRemovesDots) {
                                while (wbLastCIdx >= minIdx && isDotOrWS(s.charAt(wbLastCIdx))) {
                                    wbLastCIdx--;
                                }
                                if (wbLastCIdx < minIdx) {
                                    break executeTruncateWB;
                                }
                            }
                        }

                        truncedS = new StringBuilder(wbLastCIdx + 1 + wordTerminatorLength);
                        truncedS.append(s, 0, wbLastCIdx + 1);
                        if (addSpaceAtWordBoundary) {
                            truncedS.append(' ');
                        }
                        break executeTruncateWB;
                    }

                    followingCIsWS = curCIsWS;
                    wbLastCIdx--;
                } // executeTruncateWB: while (...)
            }
            if (truncedS != null
                    || mode == TruncationMode.WORD_BOUNDARY
                    || mode == TruncationMode.AUTO && wordBoundaryMinLength == 0.0) {
                return truncedS;
            }
            // We are in TruncationMode.AUTO. truncateW wasn't possible, so fall back to character boundary truncation.
        }

        // Do character boundary truncation.

        // If the truncation point is a word boundary, and thus we add a space before the terminator, then we may run
        // out of the maxLength by 1. In that case we have to truncate one character earlier.
        if (cbLastCIdx == cbInitialLastCIdx && addSpaceAtWordBoundary  && isWordEnd(s, cbLastCIdx)) {
            cbLastCIdx--;
            if (cbLastCIdx < 0) {
                return null;
            }
        }

        // Skip trailing WS, also trailing dots if necessary.
        boolean skippedDots;
        do {
            skippedDots = false;

            cbLastCIdx = skipTrailingWS(s, cbLastCIdx);
            if (cbLastCIdx < 0) {
                return null;
            }

            // Note how we avoid getMTerminatorRemovesDots until we absolutely need its result.
            if (isDot(s.charAt(cbLastCIdx)) && !(addSpaceAtWordBoundary && isWordEnd(s, cbLastCIdx))) {
                if (terminatorRemovesDots == null) {
                    terminatorRemovesDots = getTerminatorRemovesDots(terminator);
                }
                if (terminatorRemovesDots) {
                    cbLastCIdx = skipTrailingDots(s, cbLastCIdx);
                    if (cbLastCIdx < 0) {
                        return null;
                    }
                    skippedDots = true;
                }
            }
        } while (skippedDots);

        boolean addWordBoundarySpace = addSpaceAtWordBoundary && isWordEnd(s, cbLastCIdx);
        StringBuilder truncatedS = new StringBuilder(cbLastCIdx + 1 + (addWordBoundarySpace ? 1 : 0) + terminatorLength);
        truncatedS.append(s, 0, cbLastCIdx + 1);
        if (addWordBoundarySpace) {
            truncatedS.append(' ');
        }
        return truncatedS;
    }

    private int getTerminatorLength(TemplateModel terminator) throws TemplateModelException {
        return terminator instanceof TemplateScalarModel
                ? ((TemplateScalarModel) terminator).getAsString().length()
                : getMTerminatorLength((TemplateMarkupOutputModel<?>) terminator);
    }

    private boolean getTerminatorRemovesDots(TemplateModel terminator) throws TemplateModelException {
        return terminator instanceof TemplateScalarModel
                ? getTerminatorRemovesDots(((TemplateScalarModel) terminator).getAsString())
                : getMTerminatorRemovesDots((TemplateMarkupOutputModel<?>) terminator);
    }

    private int skipTrailingWS(String s, int lastCIdx) {
        while (lastCIdx >= 0 && Character.isWhitespace(s.charAt(lastCIdx))) {
            lastCIdx--;
        }
        return lastCIdx;
    }

    private int skipTrailingDots(String s, int lastCIdx) {
        while (lastCIdx >= 0 && isDot(s.charAt(lastCIdx))) {
            lastCIdx--;
        }
        return lastCIdx;
    }

    private boolean isWordEnd(String s, int lastCIdx) {
        return lastCIdx + 1 >= s.length() || Character.isWhitespace(s.charAt(lastCIdx + 1));
    }

    private static boolean isDot(char c) {
        return c == '.' || c == '\u2026';
    }

    private static boolean isDotOrWS(char c) {
        return isDot(c) || Character.isWhitespace(c);
    }

    private boolean isHTMLOrXML(MarkupOutputFormat<?> outputFormat) {
        return outputFormat instanceof HTMLOutputFormat || outputFormat instanceof  XMLOutputFormat;
    }

    /**
     * Returns the length of a string, ignoring HTML/XML tags and comments, also, character and entity references are
     * count as having length of 1, and CDATA sections are counted in with the length of their content. So for
     * example, the length of {@code "<span>x&amp;y</span>"} will be 3 (as visually it's {@code x&y}, which is 3
     * characters).
     */
    // Not private for testability
    static int getLengthWithoutTags(String s) {
        // Fixes/improvements here should be also done here: doesHtmlOrXmlStartWithDot

        int result = 0;
        int i = 0;
        int len = s.length();
        countChars: while (i < len) {
            char c = s.charAt(i++);
            if (c == '<') {
                if (s.startsWith("!--", i)) {
                    // <!--...-->
                    i += 3;
                    while (i + 2 < len && !(s.charAt(i) == '-' && s.charAt(i + 1) == '-' && s.charAt(i + 2) == '>')) {
                        i++;
                    }
                    i += 3;
                    if (i >= len) {
                        break countChars;
                    }
                } else if (s.startsWith("![CDATA[", i)) {
                    // <![CDATA[...]]>
                    i += 8;
                    while (i < len
                            && !(s.charAt(i) == ']'
                            && i + 2 < len && s.charAt(i + 1) == ']' && s.charAt(i + 2) == '>')) {
                        result++;
                        i++;
                    }
                    i += 3;
                    if (i >= len) {
                        break countChars;
                    }
                } else {
                    // <...>
                    while (i < len && s.charAt(i) != '>') {
                        i++;
                    }
                    i++;
                    if (i >= len) {
                        break countChars;
                    }
                }
            } else if (c == '&') {
                // &...;
                while (i < len && s.charAt(i) != ';') {
                    i++;
                }
                i++;
                result++;
                if (i >= len) {
                    break countChars;
                }
            } else {
                result++;
            }
        }
        return result;
    }

    /**
     * Check if the specified HTML or XML starts with dot or ellipsis, if we ignore tags and comments.
     */
    // Not private for testability
    static boolean doesHtmlOrXmlStartWithDot(String s) {
        // Fixes/improvements here should be also done here: getLengthWithoutTags

        int i = 0;
        int len = s.length();
        consumeChars: while (i < len) {
            char c = s.charAt(i++);
            if (c == '<') {
                if (s.startsWith("!--", i)) {
                    // <!--...-->
                    i += 3;
                    while (i + 2 < len
                            && !((c = s.charAt(i)) == '-' && s.charAt(i + 1) == '-' && s.charAt(i + 2) == '>')) {
                        i++;
                    }
                    i += 3;
                    if (i >= len) {
                        break consumeChars;
                    }
                } else if (s.startsWith("![CDATA[", i)) {
                    // <![CDATA[...]]>
                    i += 8;
                    while (i < len
                            && !((c = s.charAt(i)) == ']'
                            && i + 2 < len
                            && s.charAt(i + 1) == ']' && s.charAt(i + 2) == '>')) {
                        return isDot(c);
                    }
                    i += 3;
                    if (i >= len) {
                        break consumeChars;
                    }
                } else {
                    // <...>
                    while (i < len && s.charAt(i) != '>') {
                        i++;
                    }
                    i++;
                    if (i >= len) {
                        break consumeChars;
                    }
                }
            } else if (c == '&') {
                // &...;
                int start = i;
                while (i < len && s.charAt(i) != ';') {
                    i++;
                }
                return isDotCharReference(s.substring(start, i));
            } else {
                return isDot(c);
            }
        }
        return false;
    }

    // Not private for testability
    static boolean isDotCharReference(String name) {
        if (name.length() > 2 && name.charAt(0) == '#') {
            int charCode = getCodeFromNumericalCharReferenceName(name);
            return charCode == 0x2026 || charCode == 0x2e;
        }
        return name.equals("hellip") || name.equals("period");
    }

    // Not private for testability
    static int getCodeFromNumericalCharReferenceName(String name) {
        char c = name.charAt(1);
        boolean hex = c == 'x' || c == 'X';
        int code = 0;
        for (int pos = hex ? 2 : 1; pos < name.length(); pos++) {
            c = name.charAt(pos);
            code *= hex ? 16 : 10;
            if (c >= '0' && c <= '9') {
                code += c - '0';
            } else if (hex && c >= 'a' && c <= 'f') {
                code += c - 'a' + 10;
            } else if (hex && c >= 'A' && c <= 'F') {
                code += c - 'A' + 10;
            } else {
                return -1;
            }
        }
        return code;
    }

}
