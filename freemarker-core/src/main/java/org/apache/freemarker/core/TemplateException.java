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

package org.apache.freemarker.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;

import org.apache.freemarker.core.util._CollectionUtils;

/**
 * Runtime exception in a template (as opposed to a parsing-time exception: {@link ParseException}).
 * It prints a special stack trace that contains the template-language stack trace along the usual Java stack trace.
 */
public class TemplateException extends Exception {

    private static final String FTL_INSTRUCTION_STACK_TRACE_TITLE
            = "FTL stack trace (\"~\" means nesting-related):";

    // Set in constructor:
    // TODO [FM3] These all must be final, or else tha class is not thread safe
    private transient _ErrorDescriptionBuilder descriptionBuilder;
    private final transient Environment env;
    private final transient ASTExpression blamedExpression;
    private transient ASTElement[] ftlInstructionStackSnapshot;
    
    // Calculated on demand:
    private String renderedFtlInstructionStackSnapshot;  // clalc. from ftlInstructionStackSnapshot 
    private String renderedFtlInstructionStackSnapshotTop; // clalc. from ftlInstructionStackSnapshot
    private String description;  // calc. from descriptionBuilder, or set by the construcor
    private transient String messageWithoutStackTop;
    private transient String message;
    private boolean blamedExpressionStringCalculated;
    private String blamedExpressionString;
    private boolean positionsCalculated;
    private String templateLookupName;
    private String templateSourceName;
    private Integer lineNumber; 
    private Integer columnNumber; 
    private Integer endLineNumber; 
    private Integer endColumnNumber; 

    // Concurrency:
    private transient Object lock = new Object();
    private transient ThreadLocal messageWasAlreadyPrintedForThisTrace;
    
    /**
     * Constructs a TemplateException with no specified detail message
     * or underlying cause.
     */
    public TemplateException(Environment env) {
        this(null, null, env);
    }

    /**
     * Constructs a TemplateException with the given detail message,
     * but no underlying cause exception.
     *
     * @param description the description of the error that occurred
     */
    public TemplateException(String description, Environment env) {
        this(description, null, env);
    }

    /**
     * The same as {@link #TemplateException(Throwable, Environment)}; it's exists only for binary
     * backward-compatibility.
     */
    public TemplateException(Exception cause, Environment env) {
        this(null, cause, env);
    }

    /**
     * Constructs a TemplateException with the given underlying Exception,
     * but no detail message.
     *
     * @param cause the underlying {@link Exception} that caused this
     * exception to be raised
     */
    public TemplateException(Throwable cause, Environment env) {
        this(null, cause, env);
    }

    public TemplateException(String description, Throwable cause) {
        this(description, cause, null);
    }

    /**
     * Constructs a TemplateException with both a description of the error
     * that occurred and the underlying Exception that caused this exception
     * to be raised.
     *
     * @param description the description of the error that occurred
     * @param cause the underlying {@link Exception} that caused this exception to be raised
     * @param env Can be null{@code null}, in which case {@link Environment#getCurrentEnvironment()} is used.
     */
    public TemplateException(String description, Throwable cause, Environment env) {
        this(description, cause, env, null, null);
    }

    /**
     * Do not use; To be used internally by FreeMarker. No backward compatibility guarantees.
     * 
     * @param blamedExpr Maybe {@code null}. The FTL stack in the {@link Environment} only specifies the error location
     *          with "template element" granularity, and this can be used to point to the expression inside the
     *          template element.    
     */
    protected TemplateException(Throwable cause, Environment env, ASTExpression blamedExpr,
            _ErrorDescriptionBuilder descriptionBuilder) {
        this(null, cause, env, blamedExpr, descriptionBuilder);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // BEGIN FM2 TemplateException constructors
    // -----------------------------------------------------------------------------------------------------------------
    // TODO [FM3] This was mindlessly copy-pasted. Make order here...

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:

    public TemplateException(String description) {
        this(description, (Throwable) null);
    }

    TemplateException(Environment env, String description) {
        this(description, env);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:

    TemplateException(Throwable cause, String description) {
        this(cause, null, description);
    }

    TemplateException(Throwable cause) {
        this(cause, null, (String) null);
    }

    TemplateException(Throwable cause, Environment env, String description) {
        this(description, cause, env);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:

    TemplateException(_ErrorDescriptionBuilder description) {
        this(null, description);
    }

    TemplateException(Environment env, _ErrorDescriptionBuilder description) {
        this(null, env, description);
    }

    TemplateException(Throwable cause, Environment env, _ErrorDescriptionBuilder description) {
        this(cause, env, null, description);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:

    /**
     * @param descriptionParts
     *         Array of objects that will be rendered into {@link String} on demand (if and when the exception message
     *         is ever needed).
     */
    public TemplateException(Object... descriptionParts) {
        this((Environment) null, descriptionParts);
    }

    public TemplateException(Environment env, Object... descriptionParts) {
        this((Throwable) null, env, descriptionParts);
    }

    public TemplateException(Throwable cause, Object... descriptionParts) {
        this(cause, null, descriptionParts);
    }

    public TemplateException(Throwable cause, Environment env, Object... descriptionParts) {
        this(cause, env, null, new _ErrorDescriptionBuilder(descriptionParts));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:

    TemplateException(ASTExpression blamed, Object... descriptionParts) {
        this(blamed, null, descriptionParts);
    }

    TemplateException(ASTExpression blamed, Environment env, Object... descriptionParts) {
        this(blamed, null, env, descriptionParts);
    }

    TemplateException(ASTExpression blamed, Throwable cause, Environment env, Object... descriptionParts) {
        this(cause, env, blamed, new _ErrorDescriptionBuilder(descriptionParts).blame(blamed));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:

    TemplateException(ASTExpression blamed, String description) {
        this(blamed, null, description);
    }

    TemplateException(ASTExpression blamed, Environment env, String description) {
        this(blamed, null, env, description);
    }

    TemplateException(ASTExpression blamed, Throwable cause, Environment env, String description) {
        this(cause, env, blamed, new _ErrorDescriptionBuilder(description).blame(blamed));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // END FM2 TemplateException constructors
    // -----------------------------------------------------------------------------------------------------------------

    private TemplateException(
            String renderedDescription,
            Throwable cause,            
            Environment env, ASTExpression blamedExpression,
            _ErrorDescriptionBuilder descriptionBuilder) {
        // Note: Keep this constructor lightweight.
        
        super(cause);  // Message managed locally.
        
        if (env == null) env = Environment.getCurrentEnvironment();
        this.env = env;
        
        this.blamedExpression = blamedExpression;
        
        this.descriptionBuilder = descriptionBuilder;
        description = renderedDescription;
        
        if (env != null) {
            ftlInstructionStackSnapshot = env.getInstructionStackSnapshot();
        }
    }
    
    private void renderMessages() {
        String description = getDescription();
        
        if (description != null && description.length() != 0) {
            messageWithoutStackTop = description;
        } else if (getCause() != null) {
            messageWithoutStackTop = "No high-level description was specified for this error; "
                    + "low-level message (from cause exception): "
                    + getCause().getClass().getName() + ": " + getCause().getMessage();
        } else {
            messageWithoutStackTop = "[No error description was available.]";
        }
        
        String stackTopFew = getFTLInstructionStackTopFew();
        if (stackTopFew != null) {
            message = messageWithoutStackTop + "\n\n"
                    + MessageUtils.ERROR_MESSAGE_HR + "\n"
                    + FTL_INSTRUCTION_STACK_TRACE_TITLE + "\n"
                    + stackTopFew
                    + MessageUtils.ERROR_MESSAGE_HR;
            messageWithoutStackTop = message.substring(0, messageWithoutStackTop.length());  // to reuse backing char[]
        } else {
            message = messageWithoutStackTop;
        }
    }
    
    private void calculatePosition() {
        synchronized (lock) {
            if (!positionsCalculated) {
                // The expressions is the argument of the template element, so we prefer it as it's more specific. 
                ASTNode templateObject = blamedExpression != null
                        ? blamedExpression
                        : (
                                ftlInstructionStackSnapshot != null && ftlInstructionStackSnapshot.length != 0
                                ? ftlInstructionStackSnapshot[0] : null);
                // Line number blow 0 means no info, negative means position in ?eval-ed value that we won't use here.
                if (templateObject != null && templateObject.getBeginLine() > 0) {
                    final Template template = templateObject.getTemplate();
                    templateLookupName = template.getLookupName();
                    templateSourceName = template.getSourceName();
                    lineNumber = Integer.valueOf(templateObject.getBeginLine());
                    columnNumber = Integer.valueOf(templateObject.getBeginColumn());
                    endLineNumber = Integer.valueOf(templateObject.getEndLine());
                    endColumnNumber = Integer.valueOf(templateObject.getEndColumn());
                }
                positionsCalculated = true;
                deleteFTLInstructionStackSnapshotIfNotNeeded();
            }
        }
    }

    /**
     * Returns the snapshot of the FTL stack trace at the time this exception was created.
     */
    public String getFTLInstructionStack() {
        synchronized (lock) {
            if (ftlInstructionStackSnapshot != null || renderedFtlInstructionStackSnapshot != null) {
                if (renderedFtlInstructionStackSnapshot == null) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    Environment.outputInstructionStack(ftlInstructionStackSnapshot, false, pw);
                    pw.close();
                    if (renderedFtlInstructionStackSnapshot == null) {
                        renderedFtlInstructionStackSnapshot = sw.toString();
                        deleteFTLInstructionStackSnapshotIfNotNeeded();
                    }
                }
                return renderedFtlInstructionStackSnapshot;
            } else {
                return null;
            }
        }
    }
    
    private String getFTLInstructionStackTopFew() {
        synchronized (lock) {
            if (ftlInstructionStackSnapshot != null || renderedFtlInstructionStackSnapshotTop != null) {
                if (renderedFtlInstructionStackSnapshotTop == null) {
                    int stackSize = ftlInstructionStackSnapshot.length;
                    String s;
                    if (stackSize == 0) {
                        s = "";
                    } else {
                        StringWriter sw = new StringWriter();
                        Environment.outputInstructionStack(ftlInstructionStackSnapshot, true, sw);
                        s = sw.toString();
                    }
                    if (renderedFtlInstructionStackSnapshotTop == null) {
                        renderedFtlInstructionStackSnapshotTop = s;
                        deleteFTLInstructionStackSnapshotIfNotNeeded();
                    }
                }
                return renderedFtlInstructionStackSnapshotTop.length() != 0
                        ? renderedFtlInstructionStackSnapshotTop : null;
            } else {
                return null;
            }
        }
    }
    
    private void deleteFTLInstructionStackSnapshotIfNotNeeded() {
        if (renderedFtlInstructionStackSnapshot != null && renderedFtlInstructionStackSnapshotTop != null
                && (positionsCalculated || blamedExpression != null)) {
            ftlInstructionStackSnapshot = null;
        }
        
    }
    
    private String getDescription() {
        synchronized (lock) {
            if (description == null && descriptionBuilder != null) {
                description = descriptionBuilder.toString(
                        getFailingInstruction(),
                        env != null ? env.getShowErrorTips() : true);
                descriptionBuilder = null;
            }
            return description;
        }
    }

    private ASTElement getFailingInstruction() {
        if (ftlInstructionStackSnapshot != null && ftlInstructionStackSnapshot.length > 0) {
            return ftlInstructionStackSnapshot[0];
        } else {
            return null;
        }
    }

    /**
     * @return the execution environment in which the exception occurred.
     *    {@code null} if the exception was deserialized. 
     */
    public Environment getEnvironment() {
        return env;
    }

    /**
     * Overrides {@link Throwable#printStackTrace(PrintStream)} so that it will include the FTL stack trace.
     */
    @Override
    public void printStackTrace(PrintStream out) {
        printStackTrace(out, true, true, true);
    }

    /**
     * Overrides {@link Throwable#printStackTrace(PrintWriter)} so that it will include the FTL stack trace.
     */
    @Override
    public void printStackTrace(PrintWriter out) {
        printStackTrace(out, true, true, true);
    }
    
    /**
     * @param heading should the heading at the top be printed 
     * @param ftlStackTrace should the FTL stack trace be printed
     * @param javaStackTrace should the Java stack trace be printed
     */
    public void printStackTrace(PrintWriter out, boolean heading, boolean ftlStackTrace, boolean javaStackTrace) {
        synchronized (out) {
            printStackTrace(new PrintWriterStackTraceWriter(out), heading, ftlStackTrace, javaStackTrace);
        }
    }

    /**
     * @param heading should the heading at the top be printed 
     * @param ftlStackTrace should the FTL stack trace be printed
     * @param javaStackTrace should the Java stack trace be printed
     */
    public void printStackTrace(PrintStream out, boolean heading, boolean ftlStackTrace, boolean javaStackTrace) {
        synchronized (out) {
            printStackTrace(new PrintStreamStackTraceWriter(out), heading, ftlStackTrace, javaStackTrace);
        }
    }
    
    private void printStackTrace(StackTraceWriter out, boolean heading, boolean ftlStackTrace, boolean javaStackTrace) {
        synchronized (out) {
            if (heading) { 
                out.println("FreeMarker template error:");
            }
            
            if (ftlStackTrace) {
                String stackTrace = getFTLInstructionStack();
                if (stackTrace != null) {
                    out.println(getMessageWithoutStackTop());  // Not getMessage()!
                    out.println();
                    out.println(MessageUtils.ERROR_MESSAGE_HR);
                    out.println(FTL_INSTRUCTION_STACK_TRACE_TITLE);
                    out.print(stackTrace);
                    out.println(MessageUtils.ERROR_MESSAGE_HR);
                } else {
                    ftlStackTrace = false;
                    javaStackTrace = true;
                }
            }
            
            if (javaStackTrace) {
                if (ftlStackTrace) {  // We are after an FTL stack trace
                    out.println();
                    out.println("Java stack trace (for programmers):");
                    out.println(MessageUtils.ERROR_MESSAGE_HR);
                    synchronized (lock) {
                        if (messageWasAlreadyPrintedForThisTrace == null) {
                            messageWasAlreadyPrintedForThisTrace = new ThreadLocal();
                        }
                        messageWasAlreadyPrintedForThisTrace.set(Boolean.TRUE);
                    }
                    
                    try {
                        out.printStandardStackTrace(this);
                    } finally {
                        messageWasAlreadyPrintedForThisTrace.set(Boolean.FALSE);
                    }
                } else {  // javaStackTrace only
                    out.printStandardStackTrace(this);
                }
                
                if (getCause() != null) {
                    // Dirty hack to fight with ServletException class whose getCause() method doesn't work properly:
                    Throwable causeCause = getCause().getCause();
                    if (causeCause == null) {
                        try {
                            // Reflection is used to prevent dependency on Servlet classes.
                            Method m = getCause().getClass().getMethod("getRootCause", _CollectionUtils.EMPTY_CLASS_ARRAY);
                            Throwable rootCause = (Throwable) m.invoke(getCause(), _CollectionUtils.EMPTY_OBJECT_ARRAY);
                            if (rootCause != null) {
                                out.println("ServletException root cause: ");
                                out.printStandardStackTrace(rootCause);
                            }
                        } catch (Throwable exc) {
                            // ignore
                        }
                    }
                }
            }  // if (javaStackTrace)
        }
    }
    
    /**
     * Prints the stack trace as if wasn't overridden by {@link TemplateException}. 
     */
    public void printStandardStackTrace(PrintStream ps) {
        super.printStackTrace(ps);
    }

    /**
     * Prints the stack trace as if wasn't overridden by {@link TemplateException}. 
     */
    public void printStandardStackTrace(PrintWriter pw) {
        super.printStackTrace(pw);
    }

    @Override
    public String getMessage() {
        if (messageWasAlreadyPrintedForThisTrace != null
                && messageWasAlreadyPrintedForThisTrace.get() == Boolean.TRUE) {
            return "[... Exception message was already printed; see it above ...]";
        } else {
            synchronized (lock) {
                if (message == null) renderMessages();
                return message;
            }
        }
    }
    
    /**
     * Similar to {@link #getMessage()}, but it doesn't contain the position of the failing instruction at then end
     * of the text. It might contains the position of the failing <em>expression</em> though as part of the expression
     * quotation, as that's the part of the description. 
     */
    public String getMessageWithoutStackTop() {
        synchronized (lock) {
            if (messageWithoutStackTop == null) renderMessages();
            return messageWithoutStackTop;
        }
    }
    
    /**
     * 1-based line number of the failing section, or {@code null} if the information is not available.
     */
    public Integer getLineNumber() {
        synchronized (lock) {
            if (!positionsCalculated) {
                calculatePosition();
            }
            return lineNumber;
        }
    }

    /**
     * Returns the {@linkplain Template#getSourceName() source name} of the template where the error has occurred, or
     * {@code null} if the information isn't available. This is what should be used for showing the error position.
     */
    public String getTemplateSourceName() {
        synchronized (lock) {
            if (!positionsCalculated) {
                calculatePosition();
            }
            return templateSourceName;
        }
    }

    /**
     * Returns the {@linkplain Template#getLookupName()} () lookup name} of the template where the error has
     * occurred, or {@code null} if the information isn't available. Do not use this for showing the error position;
     * use {@link #getTemplateSourceName()}.
     */
    public String getTemplateLookupName() {
        synchronized (lock) {
            if (!positionsCalculated) {
                calculatePosition();
            }
            return templateLookupName;
        }
    }

    /**
     * Returns the {@linkplain #getTemplateSourceName() template source name}, or if that's {@code null} then the
     * {@linkplain #getTemplateLookupName() template lookup name}. This name is primarily meant to be used in error
     * messages.
     */
    public String getTemplateSourceOrLookupName() {
        return getTemplateSourceName() != null ? getTemplateSourceName() : getTemplateLookupName();
    }

    /**
     * 1-based column number of the failing section, or {@code null} if the information is not available.
     */
    public Integer getColumnNumber() {
        synchronized (lock) {
            if (!positionsCalculated) {
                calculatePosition();
            }
            return columnNumber;
        }
    }

    /**
     * 1-based line number of the last line that contains the failing section, or {@code null} if the information is not
     * available.
     */
    public Integer getEndLineNumber() {
        synchronized (lock) {
            if (!positionsCalculated) {
                calculatePosition();
            }
            return endLineNumber;
        }
    }

    /**
     * 1-based column number of the last character of the failing template section, or {@code null} if the information
     * is not available. Note that unlike with Java string API-s, this column number is inclusive.
     */
    public Integer getEndColumnNumber() {
        synchronized (lock) {
            if (!positionsCalculated) {
                calculatePosition();
            }
            return endColumnNumber;
        }
    }
    
    /**
     * If there was a blamed expression attached to this exception, it returns its canonical form, otherwise it returns
     * {@code null}. This expression should always be inside the failing FTL instruction.
     *  
     * <p>The typical application of this is getting the undefined expression from {@link InvalidReferenceException}-s.
     */
    public String getBlamedExpressionString() {
        synchronized (lock) {
            if (!blamedExpressionStringCalculated) {
                if (blamedExpression != null) {
                    blamedExpressionString = blamedExpression.getCanonicalForm();
                }
                blamedExpressionStringCalculated = true;
            }
            return blamedExpressionString;
        }
    }
    
    ASTExpression getBlamedExpression() {
        return blamedExpression;
    }

    private void writeObject(ObjectOutputStream out) throws IOException, ClassNotFoundException {
        // These are calculated from transient fields, so this is the last chance to calculate them: 
        getFTLInstructionStack();
        getFTLInstructionStackTopFew();
        getDescription();
        calculatePosition();
        getBlamedExpressionString();
        
        out.defaultWriteObject();
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        lock = new Object();
        in.defaultReadObject();
    }
    
    /** Delegate to a {@link PrintWriter} or to a {@link PrintStream}. */
    private interface StackTraceWriter {
        void print(Object obj);
        void println(Object obj);
        void println();
        void printStandardStackTrace(Throwable exception);
    }
    
    private static class PrintStreamStackTraceWriter implements StackTraceWriter {
        
        private final PrintStream out;

        PrintStreamStackTraceWriter(PrintStream out) {
            this.out = out;
        }

        @Override
        public void print(Object obj) {
            out.print(obj);
        }

        @Override
        public void println(Object obj) {
            out.println(obj);
        }

        @Override
        public void println() {
            out.println();
        }

        @Override
        public void printStandardStackTrace(Throwable exception) {
            if (exception instanceof TemplateException) {
                ((TemplateException) exception).printStandardStackTrace(out);
            } else {
                exception.printStackTrace(out);
            }
        }
        
    }

    private static class PrintWriterStackTraceWriter implements StackTraceWriter {
        
        private final PrintWriter out;

        PrintWriterStackTraceWriter(PrintWriter out) {
            this.out = out;
        }

        @Override
        public void print(Object obj) {
            out.print(obj);
        }

        @Override
        public void println(Object obj) {
            out.println(obj);
        }

        @Override
        public void println() {
            out.println();
        }

        @Override
        public void printStandardStackTrace(Throwable exception) {
            if (exception instanceof TemplateException) {
                ((TemplateException) exception).printStandardStackTrace(out);
            } else {
                exception.printStackTrace(out);
            }
        }
        
    }
    
}
