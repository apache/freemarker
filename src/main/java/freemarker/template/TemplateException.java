/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.template;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;

import freemarker.core.Environment;
import freemarker.core.Expression;
import freemarker.core.InvalidReferenceException;
import freemarker.core.ParseException;
import freemarker.core.TemplateElement;
import freemarker.core.TemplateObject;
import freemarker.core._CoreAPI;
import freemarker.core._ErrorDescriptionBuilder;
import freemarker.template.utility.CollectionUtils;

/**
 * Runtime exception in a template (as opposed to a parsing-time exception: {@link ParseException}).
 * It prints a special stack trace that contains the template-language stack trace along the usual Java stack trace.
 */
public class TemplateException extends Exception {

    private static final int FTL_STACK_TOP_FEW_MAX_LINES = 6;
    private static final String FTL_INSTRUCTION_STACK_TRACE_TITLE
            = "FTL stack trace (\"~\" means nesting-related):";

    // Set in constructor:
    private transient _ErrorDescriptionBuilder descriptionBuilder;
    private final transient Environment env;
    private final transient Expression blamedExpression;
    private transient TemplateElement[] ftlInstructionStackSnapshot;
    
    // Calculated on demand:
    private String renderedFtlInstructionStackSnapshot;  // clalc. from ftlInstructionStackSnapshot 
    private String renderedFtlInstructionStackSnapshotTop; // clalc. from ftlInstructionStackSnapshot
    private String description;  // calc. from descriptionBuilder, or set by the construcor
    private transient String messageWithoutStackTop;
    private transient String message;
    private boolean blamedExpressionStringCalculated;
    private String blamedExpressionString;
    private boolean positionsCalculated;
    private String templateName;
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
        this((String) null, null, env);
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
        this((String) null, cause, env);
    }

    /**
     * Constructs a TemplateException with the given underlying Exception,
     * but no detail message.
     *
     * @param cause the underlying {@link Exception} that caused this
     * exception to be raised
     * 
     * @since 2.3.20
     */
    public TemplateException(Throwable cause, Environment env) {
        this((String) null, cause, env);
    }
    
    /**
     * The same as {@link #TemplateException(String, Throwable, Environment)}; it's exists only for binary
     * backward-compatibility.
     */
    public TemplateException(String description, Exception cause, Environment env) {
        this(description, cause, env, null, null);
    }

    /**
     * Constructs a TemplateException with both a description of the error
     * that occurred and the underlying Exception that caused this exception
     * to be raised.
     *
     * @param description the description of the error that occurred
     * @param cause the underlying {@link Exception} that caused this exception to be raised
     * 
     * @since 2.3.20
     */
    public TemplateException(String description, Throwable cause, Environment env) {
        this(description, cause, env, null, null);
    }
    
    /**
     * Don't use this; this is to be used internally by FreeMarker. No backward compatibility guarantees.
     * 
     * @param blamedExpr Maybe {@code null}. The FTL stack in the {@link Environment} only specifies the error location
     *          with "template element" granularity, and this can be used to point to the expression inside the
     *          template element.    
     */
    protected TemplateException(Throwable cause, Environment env, Expression blamedExpr,
            _ErrorDescriptionBuilder descriptionBuilder) {
        this(null, cause, env, blamedExpr, descriptionBuilder);
    }
    
    private TemplateException(
            String renderedDescription,
            Throwable cause,            
            Environment env, Expression expression,
            _ErrorDescriptionBuilder descriptionBuilder) {
        // Note: Keep this constructor lightweight.
        
        super(cause);  // Message managed locally.
        
        if (env == null) env = Environment.getCurrentEnvironment();
        this.env = env;
        
        this.blamedExpression = expression;
        
        this.descriptionBuilder = descriptionBuilder;
        description = renderedDescription;
        
        if(env != null) ftlInstructionStackSnapshot = _CoreAPI.getInstructionStackSnapshot(env);
    }
    
    private void renderMessages()  {
        String description = getDescription();
        
        if(description != null && description.length() != 0) {
            messageWithoutStackTop = description;
        } else if (getCause() != null) {
            messageWithoutStackTop = "No error description was specified for this error; low-level message: "
                    + getCause().getClass().getName() + ": " + getCause().getMessage();
        } else {
            messageWithoutStackTop = "[No error description was available.]";
        }
        
        String stackTopFew = getFTLInstructionStackTopFew();
        if (stackTopFew != null) {
            message = messageWithoutStackTop + "\n\n"
                    + _CoreAPI.ERROR_MESSAGE_HR + "\n"
                    + FTL_INSTRUCTION_STACK_TRACE_TITLE + "\n"
                    + stackTopFew
                    + _CoreAPI.ERROR_MESSAGE_HR;
            messageWithoutStackTop = message.substring(0, messageWithoutStackTop.length());  // to reuse backing char[]
        } else {
            message = messageWithoutStackTop;
        }
    }
    
    private void calculatePosition() {
        synchronized (lock) {
            if (!positionsCalculated) {
                // The expressions is the argument of the template element, so we prefer it as it's more specific. 
                TemplateObject templateObject = blamedExpression != null
                        ? (TemplateObject) blamedExpression
                        : (
                                ftlInstructionStackSnapshot != null && ftlInstructionStackSnapshot.length != 0
                                ? ftlInstructionStackSnapshot[0] : null);
                // Line number blow 0 means no info, negative means position in ?eval-ed value that we won't use here.
                if (templateObject != null && templateObject.getBeginLine() > 0) {
                    final Template template = templateObject.getTemplate();
                    templateName = template != null ? template.getName() : null;
                    templateSourceName = template != null ? template.getSourceName() : null;
                    lineNumber = new Integer(templateObject.getBeginLine());
                    columnNumber = new Integer(templateObject.getBeginColumn());
                    endLineNumber = new Integer(templateObject.getEndLine());
                    endColumnNumber = new Integer(templateObject.getEndColumn());
                }
                positionsCalculated = true;
                deleteFTLInstructionStackSnapshotIfNotNeeded();
            }
        }
    }
    
    /**
     * @deprecated Java 1.4 has introduced {@link #getCause()} - use that instead, especially as this can't return
     * runtime exceptions and errors as is.
     */
    public Exception getCauseException() {
        return getCause() instanceof Exception
                ? (Exception) getCause()
                : new Exception("Wrapped to Exception: " + getCause(), getCause());
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
                    _CoreAPI.outputInstructionStack(ftlInstructionStackSnapshot, false, pw);
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
                        _CoreAPI.outputInstructionStack(ftlInstructionStackSnapshot, true, sw);
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

    private TemplateElement getFailingInstruction() {
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
    public void printStackTrace(PrintStream out) {
        printStackTrace(out, true, true, true);
    }

    /**
     * Overrides {@link Throwable#printStackTrace(PrintWriter)} so that it will include the FTL stack trace.
     */
    public void printStackTrace(PrintWriter out) {
        printStackTrace(out, true, true, true);
    }
    
    /**
     * @param heading should the heading at the top be printed 
     * @param ftlStackTrace should the FTL stack trace be printed 
     * @param javaStackTrace should the Java stack trace be printed
     *  
     * @since 2.3.20
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
     *  
     * @since 2.3.20
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
                    out.println(_CoreAPI.ERROR_MESSAGE_HR);
                    out.println(FTL_INSTRUCTION_STACK_TRACE_TITLE);
                    out.print(stackTrace);
                    out.println(_CoreAPI.ERROR_MESSAGE_HR);
                } else {
                    ftlStackTrace = false;
                    javaStackTrace = true;
                }
            }
            
            if (javaStackTrace) {
                if (ftlStackTrace) {  // We are after an FTL stack trace
                    out.println();
                    out.println("Java stack trace (for programmers):");
                    out.println(_CoreAPI.ERROR_MESSAGE_HR);
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
                            Method m = getCause().getClass().getMethod("getRootCause", CollectionUtils.EMPTY_CLASS_ARRAY);
                            Throwable rootCause = (Throwable) m.invoke(getCause(), CollectionUtils.EMPTY_OBJECT_ARRAY);
                            if (rootCause != null) {
                                out.println("ServletException root cause: ");
                                out.printStandardStackTrace(rootCause);
                            }
                        } catch (Throwable exc) {
                            ; // ignore
                        }
                    }
                }
            }  // if (javaStackTrace)
        }
    }
    
    /**
     * Prints the stack trace as if wasn't overridden by {@link TemplateException}. 
     * @since 2.3.20
     */
    public void printStandardStackTrace(PrintStream ps) {
        super.printStackTrace(ps);
    }

    /**
     * Prints the stack trace as if wasn't overridden by {@link TemplateException}. 
     * @since 2.3.20
     */
    public void printStandardStackTrace(PrintWriter pw) {
        super.printStackTrace(pw);
    }

    public String getMessage() {
        if (messageWasAlreadyPrintedForThisTrace != null
                && messageWasAlreadyPrintedForThisTrace.get() == Boolean.TRUE) {
            return "[... Exception message was already printed; see it above ...]";
        } else {
            synchronized (lock) {  // Switch to double-check + volatile with Java 5
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
     * 
     * @since 2.3.21
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
     * Returns the name ({@link Template#getName()}) of the template where the error has occurred, or {@code null} if
     * the information isn't available. This shouldn't be used for showing the error position; use
     * {@link #getTemplateSourceName()} instead.
     * 
     * @deprecated Use {@link #getTemplateSourceName()} instead, unless you are really sure that this is what you want.
     *             This method isn't really deprecated, it's just marked so to warn users about this.
     * 
     * @since 2.3.21
     */
    public String getTemplateName() {
        synchronized (lock) {
            if (!positionsCalculated) {
                calculatePosition();
            }
            return templateName;
        }
    }

    /**
     * Returns the source name ({@link Template#getSourceName()}) of the template where the error has occurred, or
     * {@code null} if the information isn't available. This is what should be used for showing the error position.
     * 
     * @since 2.3.22
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
     * 1-based column number of the failing section, or {@code null} if the information is not available.
     * 
     * @since 2.3.21
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
     * 
     * @since 2.3.21
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
     * 
     * @since 2.3.21
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
     * 
     * @since 2.3.21
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

        public void print(Object obj) {
            out.print(obj);
        }

        public void println(Object obj) {
            out.println(obj);
        }

        public void println() {
            out.println();
        }

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

        public void print(Object obj) {
            out.print(obj);
        }

        public void println(Object obj) {
            out.println(obj);
        }

        public void println() {
            out.println();
        }

        public void printStandardStackTrace(Throwable exception) {
            if (exception instanceof TemplateException) {
                ((TemplateException) exception).printStandardStackTrace(out);
            } else {
                exception.printStackTrace(out);
            }
        }
        
    }
    
}
