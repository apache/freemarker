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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;

import freemarker.core.Environment;
import freemarker.core.Internal_CoreAPI;
import freemarker.core.ParseException;
import freemarker.core.TemplateElement;

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
    private final String rawDescription;
    private final Throwable causeException;
    private final transient Environment env;
    private transient TemplateElement[] ftlInstructionStackSnapshot;
    
    // Calculated on demand:
    private String renderedFtlInstructionStackSnapshot;
    private String renderedFtlInstructionStackSnapshotTop;
    private transient String description; 
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
     * Constructs a TemplateException with the given underlying Exception,
     * but no detail message.
     *
     * @param cause the underlying <code>Exception</code> that caused this
     * exception to be raised
     */
    public TemplateException(Exception cause, Environment env) {
        this(null, cause, env);
    }

    /**
     * Constructs a TemplateException with both a description of the error
     * that occurred and the underlying Exception that caused this exception
     * to be raised.
     *
     * @param description the description of the error that occurred
     * @param cause the underlying {@link Exception} that caused this exception to be raised
     */
    public TemplateException(String description, Exception cause, Environment env) {
        super(null, cause);
        
        rawDescription = description;
        causeException = cause;  // for Java 1.2(?) compatibility
        this.env = env;
        if(env != null) ftlInstructionStackSnapshot = env.getInstructionStackSnapshot();
    }

    private void renderMessageAndDescription()  {
        if(rawDescription != null && rawDescription.length() != 0) {
            description = rawDescription;
        } else if (getCause() != null) {
            description = "No error description was specified for this error; low-level message: "
                    + getCause().getClass().getName() + ": " + getCause().getMessage();
        } else {
            description = "[No error description was available.]";
        }
        
        String stackTop = getFTLInstructionStackTop();
        if (stackTop != null) {
            message = description + "\n\n" + THE_FAILING_INSTRUCTION + stackTop;
            description = message.substring(0, description.length());  // to reuse the backing char[] of `message`
        } else {
            message = description;
        }
    }
    
    /**
     * <p>Returns the underlying exception that caused this exception to be
     * generated.</p>
     * <p><b>Note:</b><br />
     * avoided calling it <code>getCause</code> to avoid name clash with
     * JDK 1.4 method. This would be problematic because the JDK 1.4 method
     * returns a <code>Throwable</code> rather than an <code>Exception</code>.</p>
     *
     * @return the underlying <code>Exception</code>, if any, that caused this
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
     * @see Throwable#getCause()
     * @return the underlying <code>Exception</code>, if any, that caused this
     * exception to be raised
     */
    public Throwable getCause() {
        return causeException;
    }
    
    /**
     * Returns the snapshot of the FTL stack strace at the time this exception was created.
     */
    public String getFTLInstructionStack() {
        if (ftlInstructionStackSnapshot != null || renderedFtlInstructionStackSnapshot != null) {
            if (renderedFtlInstructionStackSnapshot == null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                Environment.outputInstructionStack(ftlInstructionStackSnapshot, true, pw);
                pw.close();
                synchronized (lock) {
                    if (renderedFtlInstructionStackSnapshot == null) {
                        renderedFtlInstructionStackSnapshot = sw.toString();
                        deleteFTLInstructionStackSnapshotIfNotNeeded();
                    }
                }
            }
            return renderedFtlInstructionStackSnapshot;
        } else {
            return null;
        }
    }
    
    private String getFTLInstructionStackTop() {
        if (ftlInstructionStackSnapshot != null || renderedFtlInstructionStackSnapshotTop != null) {
            if (renderedFtlInstructionStackSnapshotTop == null) {
                int stackSize = ftlInstructionStackSnapshot.length;
                String s = (stackSize > 1 ? " (print stack trace for " + (stackSize - 1) + " more)" : "")
                        + ":\n==> "
                        + Internal_CoreAPI.instructionStackItemToString(ftlInstructionStackSnapshot[0]);
                synchronized (lock) {
                    if (renderedFtlInstructionStackSnapshotTop == null) {
                        renderedFtlInstructionStackSnapshotTop = s;
                        deleteFTLInstructionStackSnapshotIfNotNeeded();
                    }
                }
            }
            return renderedFtlInstructionStackSnapshotTop;
        } else {
            return null;
        }
    }
    
    private void deleteFTLInstructionStackSnapshotIfNotNeeded() {
        if (renderedFtlInstructionStackSnapshot != null && renderedFtlInstructionStackSnapshotTop != null) {
            ftlInstructionStackSnapshot = null;
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
     * Overrides {@link Throwable#printStackTrace(java.io.PrintStream)} so that it will include the FTL stack trace.
     */
    public void printStackTrace(java.io.PrintStream ps) {
        synchronized (ps) {
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(ps), true);
            printStackTrace(pw);
            pw.flush();
        }
    }

    /**
     * Overrides {@link Throwable#printStackTrace(PrintWriter)} so that it will include the FTL stack trace.
     */
    public void printStackTrace(PrintWriter pw) {
        printStackTrace(pw, true);
    }
    
    public void printStackTrace(PrintWriter pw, boolean printHeading) {
        synchronized (pw) {
            if (printHeading) { 
                pw.println("FreeMarker template error:");
            }
            pw.println(getDescription());  // Not getMessage()!
            String stackTrace = getFTLInstructionStack();
            if (stackTrace != null) {
                pw.println();
                pw.print(THE_FAILING_INSTRUCTION);
                pw.println(" (FTL stack trace):");
                pw.println(stackTrace);
            } else {
                pw.println();
            }
            pw.println("Java stack trace (for programmers):");
            pw.println("----------");
            synchronized (lock) {
                if (messageWasAlreadyPrintedForThisTrace == null) {
                    messageWasAlreadyPrintedForThisTrace = new ThreadLocal();
                }
                messageWasAlreadyPrintedForThisTrace.set(Boolean.TRUE);
            }
            try {
                super.printStackTrace(pw);
            } finally {
                messageWasAlreadyPrintedForThisTrace.set(Boolean.FALSE);
            }
            if (BEFORE_1_4 && causeException != null) {
                pw.println("Underlying cause: ");
                causeException.printStackTrace(pw);
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
                        pw.println("ServletException root cause: ");
                        rootCause.printStackTrace(pw);
                    }
                }
            } catch (Throwable exc) {
                ; // ignore
            }
        }
    }

    /**
     * Similar to {@link #getMessage()}, but it doesn't contain the position of the failing instruction at then end
     * of the text. It might contains the position of the failing <em>expression</em> though as part of the expression
     * quotation, as that's the part of the description. 
     */
    public String getDescription() {
        if (description == null) {
            synchronized (lock) {
                if (description == null) {
                    renderMessageAndDescription();
                }
            }
        }
        return description;
    }
    
    public String getMessage() {
        if (messageWasAlreadyPrintedForThisTrace != null && messageWasAlreadyPrintedForThisTrace.get() == Boolean.TRUE) {
            return "[... Exception message was already printed; see it above ...]";
        } else {
            if (message == null) {
                synchronized (lock) {
                    if (message == null) {
                        renderMessageAndDescription();
                    }
                }
            }
            return message;
        }
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException, ClassNotFoundException {
        // Since the FTL stack trace is transient, this is the last chance to calculate these: 
        getFTLInstructionStack();
        getFTLInstructionStackTop();
        
        out.defaultWriteObject();
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        lock = new Object();
        in.defaultReadObject();
    }
    
}
