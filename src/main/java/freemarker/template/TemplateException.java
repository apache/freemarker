/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
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
import freemarker.core.ParseException;
import freemarker.core.TemplateElement;
import freemarker.core._CoreAPI;
import freemarker.core._ErrorDescriptionBuilder;

/**
 * Runtime exception in a template (as opposed to a parsing-time exception: {@link ParseException}).
 * It prints a special stack trace that contains the template-language stack trace along the usual Java stack trace.
 */
public class TemplateException extends Exception {

    private static final String THE_FAILING_INSTRUCTION = "The failing instruction";

    private static final boolean BEFORE_1_4 = before14();
    private static boolean before14() {
        Class ec = Exception.class;
        try {
            ec.getMethod("getCause", new Class[]{});
        } catch (Throwable e) {
            return true;
        }
        return false;
    }

    private static final Class[] EMPTY_CLASS_ARRAY = new Class[]{};

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[]{};

    // Set in constructor:
    private transient _ErrorDescriptionBuilder descriptionBuilder;
    private final Throwable causeException;
    private final transient Environment env;
    private transient TemplateElement[] ftlInstructionStackSnapshot;
    
    // Calculated on demand:
    private String renderedFtlInstructionStackSnapshot;  // clalc. from ftlInstructionStackSnapshot 
    private String renderedFtlInstructionStackSnapshotTop; // clalc. from ftlInstructionStackSnapshot
    private String description;  // calc. from descriptionBuilder, or set by the construcor
    private transient String messageWithoutStackTop;
    private transient String message; 

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
        this(description, cause, env, null);
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
        this(description, cause, env, null);
    }
    
    /**
     * Don't use this; this is to be used internally by FreeMarker.
     * @param preventAmbiguity its value is ignored; it's only to prevent constructor selection ambiguities for
     *     backward-compatibility
     */
    protected TemplateException(Throwable cause, Environment env, _ErrorDescriptionBuilder descriptionBuilder,
            boolean preventAmbiguity) {
        this(null, cause, env, descriptionBuilder);
    }
    
    private TemplateException(
            String renderedDescription,
            Throwable cause,
            Environment env, _ErrorDescriptionBuilder descriptionBuilder) {
        // Note: Keep this constructor lightweight.
        
        super();  // No args, because both the message and the cause exception is managed locally.
        
        if (env == null) env = Environment.getCurrentEnvironment();
        this.env = env;

        causeException = cause;  // for Java 1.2(?) compatibility
        
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
        
        String stackTop = getFTLInstructionStackTop();
        if (stackTop != null) {
            message = messageWithoutStackTop + "\n\n" + THE_FAILING_INSTRUCTION + stackTop;
            messageWithoutStackTop = message.substring(0, messageWithoutStackTop.length());  // to reuse the backing char[]
        } else {
            message = messageWithoutStackTop;
        }
    }
    
    /**
     * <p>Returns the underlying exception that caused this exception to be
     * generated.</p>
     * <p><b>Note:</b><br />
     * avoided calling it <code>getCause</code> to avoid name clash with
     * JDK 1.4 method. This would be problematic because the JDK 1.4 method
     * returns a <code>Throwable</code> rather than an {@link Exception}.</p>
     *
     * @return the underlying {@link Exception}, if any, that caused this
     * exception to be raised
     * 
     * @deprecated Use {@link #getCause()} instead, as this can't return runtime exceptions and errors as is.
     */
    public Exception getCauseException() {
        return causeException instanceof Exception
                ? (Exception) causeException
                : new Exception("Wrapped to Exception: " + causeException);
    }

    /**
     * Returns the cause exception.
     *
     * @return the underlying {@link Exception}, if any, that caused this
     * exception to be raised
     * 
     * @see Throwable#getCause()
     */
    public Throwable getCause() {
        return causeException;
    }
    
    /**
     * Returns the snapshot of the FTL stack strace at the time this exception was created.
     */
    public String getFTLInstructionStack() {
        synchronized (lock) {
            if (ftlInstructionStackSnapshot != null || renderedFtlInstructionStackSnapshot != null) {
                if (renderedFtlInstructionStackSnapshot == null) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    _CoreAPI.outputInstructionStack(ftlInstructionStackSnapshot, pw);
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
    
    private String getFTLInstructionStackTop() {
        synchronized (lock) {
            if (ftlInstructionStackSnapshot != null || renderedFtlInstructionStackSnapshotTop != null) {
                if (renderedFtlInstructionStackSnapshotTop == null) {
                    int stackSize = ftlInstructionStackSnapshot.length;
                    String s;
                    if (stackSize == 0) {
                        s = "";
                    } else {
                        s = (stackSize > 1 ? " (print stack trace for " + (stackSize - 1) + " more)" : "")
                            + ":\n==> "
                            + _CoreAPI.instructionStackItemToString(ftlInstructionStackSnapshot[0]);
                    }
                    if (renderedFtlInstructionStackSnapshotTop == null) {
                        renderedFtlInstructionStackSnapshotTop = s;
                        deleteFTLInstructionStackSnapshotIfNotNeeded();
                    }
                }
                return renderedFtlInstructionStackSnapshotTop.length() != 0 ? renderedFtlInstructionStackSnapshotTop : null;
            } else {
                return null;
            }
        }
    }
    
    private void deleteFTLInstructionStackSnapshotIfNotNeeded() {
        if (renderedFtlInstructionStackSnapshot != null && renderedFtlInstructionStackSnapshotTop != null) {
            ftlInstructionStackSnapshot = null;
        }
        
    }
    
    private String getDescription() {
        synchronized (lock) {
            if (description == null && descriptionBuilder != null) {
                description = descriptionBuilder.toString(getFailingInstruction());
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
                    out.print(THE_FAILING_INSTRUCTION);
                    out.println(" (FTL stack trace):");
                    out.print(stackTrace);
                } else {
                    ftlStackTrace = false;
                    javaStackTrace = true;
                }
            }
            
            if (javaStackTrace) {
                if (ftlStackTrace) {  // We are after an FTL stack trace
                    out.println();
                    out.println("Java stack trace (for programmers):");
                    out.println(_CoreAPI.STACK_SECTION_SEPARATOR);
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
                
                if (BEFORE_1_4 && causeException != null) {
                    out.println("Underlying cause: ");
                    out.printStandardStackTrace(causeException);
                }
                
                // Dirty hack to fight with stupid ServletException class whose
                // getCause() method doesn't work properly. Also an aid for pre-J2xE 1.4
                // users.
                try {
                    // Reflection is used to prevent dependency on Servlet classes.
                    Method m = causeException.getClass().getMethod("getRootCause", EMPTY_CLASS_ARRAY);
                    Throwable rootCause = (Throwable) m.invoke(causeException, EMPTY_OBJECT_ARRAY);
                    if (rootCause != null) {
                        Throwable j14Cause = null;
                        if (!BEFORE_1_4) {
                            m = causeException.getClass().getMethod("getCause", EMPTY_CLASS_ARRAY);
                            j14Cause = (Throwable) m.invoke(causeException, EMPTY_OBJECT_ARRAY);
                        }
                        if (j14Cause == null) {
                            out.println("ServletException root cause: ");
                            out.printStandardStackTrace(rootCause);
                        }
                    }
                } catch (Throwable exc) {
                    ; // ignore
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
        if (messageWasAlreadyPrintedForThisTrace != null && messageWasAlreadyPrintedForThisTrace.get() == Boolean.TRUE) {
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
    
    private void writeObject(ObjectOutputStream out) throws IOException, ClassNotFoundException {
        // These are calculated from transient fields, so this is the last chance to calculate them: 
        getFTLInstructionStack();
        getFTLInstructionStackTop();
        getDescription();
        
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
