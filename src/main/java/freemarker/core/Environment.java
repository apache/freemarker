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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.Collator;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import freemarker.cache.TemplateNameFormat;
import freemarker.cache._CacheAPI;
import freemarker.ext.beans.BeansWrapper;
import freemarker.log.Logger;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.SimpleSequence;
import freemarker.template.Template;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateHashModelEx2;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.TemplateTransformModel;
import freemarker.template.TransformControl;
import freemarker.template.Version;
import freemarker.template._ObjectWrappers;
import freemarker.template._VersionInts;
import freemarker.template.utility.DateUtil;
import freemarker.template.utility.DateUtil.DateToISO8601CalendarFactory;
import freemarker.template.utility.NullWriter;
import freemarker.template.utility.StringUtil;
import freemarker.template.utility.TemplateModelUtils;
import freemarker.template.utility.UndeclaredThrowableException;

/**
 * Object that represents the runtime environment during template processing. For every invocation of a
 * <tt>Template.process()</tt> method, a new instance of this object is created, and then discarded when
 * <tt>process()</tt> returns. This object stores the set of temporary variables created by the template, the value of
 * settings set by the template, the reference to the data model root, etc. Everything that is needed to fulfill the
 * template processing job.
 *
 * <p>
 * Data models that need to access the <tt>Environment</tt> object that represents the template processing on the
 * current thread can use the {@link #getCurrentEnvironment()} method.
 *
 * <p>
 * If you need to modify or read this object before or after the <tt>process</tt> call, use
 * {@link Template#createProcessingEnvironment(Object rootMap, Writer out, ObjectWrapper wrapper)}
 */
public final class Environment extends Configurable {
    
    private static final ThreadLocal threadEnv = new ThreadLocal();

    private static final Logger LOG = Logger.getLogger("freemarker.runtime");
    private static final Logger ATTEMPT_LOGGER = Logger.getLogger("freemarker.runtime.attempt");

    private final Configuration configuration;
    private final boolean incompatibleImprovementsGE2328;
    private final TemplateHashModel rootDataModel;
    private TemplateElement[] instructionStack = new TemplateElement[16];
    private int instructionStackSize = 0;
    private final ArrayList recoveredErrorStack = new ArrayList();

    private TemplateNumberFormat cachedTemplateNumberFormat;
    private Map<String, TemplateNumberFormat> cachedTemplateNumberFormats;

    /**
     * Stores the date/time/date-time formatters that are used when no format is explicitly given at the place of
     * formatting. That is, in situations like ${lastModified} or even ${lastModified?date}, but not in situations like
     * ${lastModified?string.iso}.
     * 
     * <p>
     * The index of the array is calculated from what kind of formatter we want (see
     * {@link #getTemplateDateFormatCacheArrayIndex(int, boolean, boolean)}):<br>
     * Zoned input: 0: U, 1: T, 2: D, 3: DT<br>
     * Zoneless input: 4: U, 5: T, 6: D, 7: DT<br>
     * SQL D T TZ + Zoned input: 8: U, 9: T, 10: D, 11: DT<br>
     * SQL D T TZ + Zoneless input: 12: U, 13: T, 14: D, 15: DT
     * 
     * <p>
     * This is a lazily filled cache. It starts out as {@code null}, then when first needed the array will be created.
     * The array elements also start out as {@code null}-s, and they are filled as the particular kind of formatter is
     * first needed.
     */
    private TemplateDateFormat[] cachedTempDateFormatArray;
    /** Similar to {@link #cachedTempDateFormatArray}, but used when a formatting string was specified. */
    private HashMap<String, TemplateDateFormat>[] cachedTempDateFormatsByFmtStrArray;
    private static final int CACHED_TDFS_ZONELESS_INPUT_OFFS = 4;
    private static final int CACHED_TDFS_SQL_D_T_TZ_OFFS = CACHED_TDFS_ZONELESS_INPUT_OFFS * 2;
    private static final int CACHED_TDFS_LENGTH = CACHED_TDFS_SQL_D_T_TZ_OFFS * 2;

    /** Caches the result of {@link #isSQLDateAndTimeTimeZoneSameAsNormal()}. */
    private Boolean cachedSQLDateAndTimeTimeZoneSameAsNormal;

    @Deprecated
    private NumberFormat cNumberFormat;
    private TemplateNumberFormat cTemplateNumberFormat;
    private TemplateNumberFormat cTemplateNumberFormatWithPre2331IcIBug;

    /**
     * Should be a boolean "trueAndFalseStringsCached", but with Incompatible Improvements less than 2.3.22 the
     * effective value of {@code boolean_format} could change because of {@code #import} and {@code #include},
     * as those changed the parent template. So we need this cache invalidation trick.
     */
    private Configurable trueAndFalseStringsCachedForParent;
    private String cachedTrueString;
    private String cachedFalseString;

    /**
     * Used by the "iso_" built-ins to accelerate formatting.
     * 
     * @see #getISOBuiltInCalendarFactory()
     */
    private DateToISO8601CalendarFactory isoBuiltInCalendarFactory;

    private Collator cachedCollator;

    private Writer out;
    private Macro.Context currentMacroContext;
    private LocalContextStack localContextStack;
    private final Namespace mainNamespace;
    private Namespace currentNamespace, globalNamespace;
    private HashMap<String, Namespace> loadedLibs;
    private Configurable legacyParent;

    private boolean inAttemptBlock;
    private Throwable lastThrowable;

    private TemplateModel lastReturnValue;
    private Map<Object, Namespace> macroToNamespaceLookup = new IdentityHashMap<>();

    private TemplateNodeModel currentVisitorNode;
    private TemplateSequenceModel nodeNamespaces;
    // Things we keep track of for the fallback mechanism.
    private int nodeNamespaceIndex;
    private String currentNodeName, currentNodeNS;

    private String cachedURLEscapingCharset;
    private boolean cachedURLEscapingCharsetSet;

    private boolean fastInvalidReferenceExceptions;

    /**
     * Retrieves the environment object associated with the current thread, or {@code null} if there's no template
     * processing going on in this thread. Data model implementations that need access to the environment can call this
     * method to obtain the environment object that represents the template processing that is currently running on the
     * current thread.
     */
    public static Environment getCurrentEnvironment() {
        return (Environment) threadEnv.get();
    }

    static void setCurrentEnvironment(Environment env) {
        threadEnv.set(env);
    }

    public Environment(Template template, final TemplateHashModel rootDataModel, Writer out) {
        super(template);
        configuration = template.getConfiguration();
        incompatibleImprovementsGE2328 = configuration.getIncompatibleImprovements().intValue() >= _VersionInts.V_2_3_28;
        this.globalNamespace = new Namespace(null);
        this.currentNamespace = mainNamespace = new Namespace(template);
        this.out = out;
        this.rootDataModel = rootDataModel;
        importMacros(template);
    }

    /**
     * Despite its name it just returns {@link #getParent()}. If {@link Configuration#getIncompatibleImprovements()} is
     * at least 2.3.22, then that will be the same as {@link #getMainTemplate()}. Otherwise the returned value follows
     * the {@link Environment} parent switchings that occur at {@code #include}/{@code #import} and {@code #nested}
     * directive calls, that is, it's not very meaningful outside FreeMarker internals.
     * 
     * @deprecated Use {@link #getMainTemplate()} instead (or {@link #getCurrentNamespace()} and then
     *             {@link Namespace#getTemplate()}); the value returned by this method is often not what you expect when
     *             it comes to macro/function invocations.
     */
    @Deprecated
    public Template getTemplate() {
        return (Template) getParent();
    }

    /** Returns the same value as pre-IcI 2.3.22 getTemplate() did. */
    Template getTemplate230() {
        Template legacyParent = (Template) this.legacyParent;
        return legacyParent != null ? legacyParent : getTemplate();
    }

    /**
     * Returns the topmost {@link Template}, with other words, the one for which this {@link Environment} was created.
     * That template will never change, like {@code #include} or macro calls don't change it. This method never returns
     * {@code null}.
     * 
     * @see #getCurrentNamespace()
     * 
     * @since 2.3.22
     */
    public Template getMainTemplate() {
        return mainNamespace.getTemplate();
    }

    /**
     * Returns the {@link Template} that we are "lexically" inside at the moment. This template will change when
     * entering an {@code #include} or calling a macro or function in another template, or returning to yet another
     * template with {@code #nested}. When you are calling a directive that's implemented in Java or a Java method
     * from a template, the current template will be the last current template, not {@code null}. This method never
     * returns {@code null}.  
     * 
     * @see #getMainTemplate()
     * @see #getCurrentNamespace()
     * 
     * @since 2.3.23
     */
    @SuppressFBWarnings(value = "RANGE_ARRAY_INDEX", justification = "False alarm")
    public Template getCurrentTemplate() {
        int ln = instructionStackSize;
        return ln == 0 ? getMainTemplate() : instructionStack[ln - 1].getTemplate();
    }

    /**
     * Gets the currently executing <em>custom</em> directive's call place information, or {@code null} if there's no
     * executing custom directive. This currently only works for calls made from templates with the {@code <@...>}
     * syntax. This should only be called from the {@link TemplateDirectiveModel} that was invoked with {@code <@...>},
     * otherwise its return value is not defined by this API (it's usually {@code null}).
     * 
     * @since 2.3.22
     */
    @SuppressFBWarnings(value = "RANGE_ARRAY_INDEX", justification = "False alarm")
    public DirectiveCallPlace getCurrentDirectiveCallPlace() {
        int ln = instructionStackSize;
        if (ln == 0) return null;
        TemplateElement te = instructionStack[ln - 1];
        if (te instanceof UnifiedCall) return (UnifiedCall) te;
        if (te instanceof Macro && ln > 1 && instructionStack[ln - 2] instanceof UnifiedCall) {
            return (UnifiedCall) instructionStack[ln - 2];
        }
        return null;
    }

    /**
     * Deletes cached values that meant to be valid only during a single template execution.
     */
    private void clearCachedValues() {
        cachedTemplateNumberFormats = null;
        cachedTemplateNumberFormat = null;

        cachedTempDateFormatArray = null;
        cachedTempDateFormatsByFmtStrArray = null;

        cachedCollator = null;
        cachedURLEscapingCharset = null;
        cachedURLEscapingCharsetSet = false;
    }

    /**
     * Processes the template to which this environment belongs to.
     */
    public void process() throws TemplateException, IOException {
        Object savedEnv = threadEnv.get();
        threadEnv.set(this);
        try {
            // Cached values from a previous execution are possibly outdated.
            clearCachedValues();
            try {
                doAutoImportsAndIncludes(this);
                visit(getTemplate().getRootTreeNode());
                // It's here as we must not flush if there was an exception.
                if (getAutoFlush()) {
                    out.flush();
                }
            } finally {
                // It's just to allow the GC to free memory...
                clearCachedValues();
            }
        } finally {
            threadEnv.set(savedEnv);
        }
    }

    /**
     * "Visit" the template element.
     */
    void visit(TemplateElement element) throws IOException, TemplateException {
        // ATTENTION: This method body is manually "inlined" into visit(TemplateElement[]); keep them in sync!
        pushElement(element);
        try {
            TemplateElement[] templateElementsToVisit = element.accept(this);
            if (templateElementsToVisit != null) {
                for (TemplateElement el : templateElementsToVisit) {
                    if (el == null) {
                        break;  // Skip unused trailing buffer capacity 
                    }
                    visit(el);
                }
            }
        } catch (TemplateException te) {
            handleTemplateException(te);
        } finally {
            popElement();
        }
        // ATTENTION: This method body above is manually "inlined" into visit(TemplateElement[]); keep them in sync!
    }
    
    /**
     * @param elementBuffer
     *            The elements to visit; might contains trailing {@code null}-s. Can be {@code null}.
     * 
     * @since 2.3.24
     */
    final void visit(TemplateElement[] elementBuffer) throws IOException, TemplateException {
        if (elementBuffer == null) {
            return;
        }
        for (TemplateElement element : elementBuffer) {
            if (element == null) {
                break;  // Skip unused trailing buffer capacity 
            }
            
            // ATTENTION: This part is the manually "inlining" of visit(TemplateElement[]); keep them in sync!
            // We don't just let Hotspot to do it, as we want a hard guarantee regarding maximum stack usage. 
            pushElement(element);
            try {
                TemplateElement[] templateElementsToVisit = element.accept(this);
                if (templateElementsToVisit != null) {
                    for (TemplateElement el : templateElementsToVisit) {
                        if (el == null) {
                            break;  // Skip unused trailing buffer capacity 
                        }
                        visit(el);
                    }
                }
            } catch (TemplateException te) {
                handleTemplateException(te);
            } finally {
                popElement();
            }
            // ATTENTION: This part above is the manually "inlining" of visit(TemplateElement[]); keep them in sync!
        }
    }

    /**
     * Visits the elements while temporarily using the parameter output {@link Writer}.
     * 
     * @since 2.3.27
     */
    final void visit(TemplateElement[] elementBuffer, Writer out) throws IOException, TemplateException {
        Writer prevOut = this.out;
        this.out = out;
        try {
            visit(elementBuffer);
        } finally {
            this.out = prevOut;
        }
    }
    
    @SuppressFBWarnings(value = "RANGE_ARRAY_INDEX", justification = "Not called when stack is empty")
    private TemplateElement replaceTopElement(TemplateElement element) {
        return instructionStack[instructionStackSize - 1] = element;
    }

    private static final TemplateModel[] NO_OUT_ARGS = new TemplateModel[0];

    /**
     * @deprecated Should be internal API
     */
    @Deprecated
    public void visit(final TemplateElement element,
            TemplateDirectiveModel directiveModel, Map args,
            final List bodyParameterNames) throws TemplateException, IOException {
        visit(new TemplateElement[] { element }, directiveModel, args, bodyParameterNames);
    }
    
    void visit(final TemplateElement[] childBuffer,
            TemplateDirectiveModel directiveModel, Map args,
            final List bodyParameterNames) throws TemplateException, IOException {
        TemplateDirectiveBody nested;
        if (childBuffer == null) {
            nested = null;
        } else {
            nested = new NestedElementTemplateDirectiveBody(childBuffer);
        }
        final TemplateModel[] outArgs;
        if (bodyParameterNames == null || bodyParameterNames.isEmpty()) {
            outArgs = NO_OUT_ARGS;
        } else {
            outArgs = new TemplateModel[bodyParameterNames.size()];
        }
        if (outArgs.length > 0) {
            pushLocalContext(new LocalContext() {

                @Override
                public TemplateModel getLocalVariable(String name) {
                    int index = bodyParameterNames.indexOf(name);
                    return index != -1 ? outArgs[index] : null;
                }

                @Override
                public Collection getLocalVariableNames() {
                    return bodyParameterNames;
                }
            });
        }
        try {
            directiveModel.execute(this, args, outArgs, nested);
        } catch (FlowControlException e) {
            throw e;
        } catch (TemplateException e) {
            throw e;
        } catch (IOException e) {
            // For backward compatibility, we assume that this is because the output Writer has thrown it.
            throw e;
        } catch (Exception e) {
            if (EvalUtil.shouldWrapUncheckedException(e, this)) {
                throw new _MiscTemplateException(
                        e, this, "Directive has thrown an unchecked exception; see the cause exception.");
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new UndeclaredThrowableException(e);
            }
        } finally {
            if (outArgs.length > 0) {
                localContextStack.pop();
            }
        }
    }

    /**
     * "Visit" the template element, passing the output through a TemplateTransformModel
     * 
     * @param elementBuffer
     *            the element to visit through a transform; might contains trailing {@code null}-s
     * @param transform
     *            the transform to pass the element output through
     * @param args
     *            optional arguments fed to the transform
     */
    void visitAndTransform(TemplateElement[] elementBuffer,
            TemplateTransformModel transform,
            Map args)
                    throws TemplateException, IOException {
        try {
            Writer tw = transform.getWriter(out, args);
            if (tw == null) tw = EMPTY_BODY_WRITER;
            TransformControl tc = tw instanceof TransformControl
                    ? (TransformControl) tw
                    : null;

            Writer prevOut = out;
            out = tw;
            try {
                if (tc == null || tc.onStart() != TransformControl.SKIP_BODY) {
                    do {
                        visit(elementBuffer);
                    } while (tc != null && tc.afterBody() == TransformControl.REPEAT_EVALUATION);
                }
            } catch (Throwable t) {
                try {
                    if (tc != null
                            && !(t instanceof FlowControlException
                                    && getConfiguration().getIncompatibleImprovements().intValue()
                                    >= _VersionInts.V_2_3_27)) {
                        tc.onError(t);
                    } else {
                        throw t;
                    }
                } catch (TemplateException | IOException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    if (EvalUtil.shouldWrapUncheckedException(e, this)) {
                        throw new _MiscTemplateException(
                                e, this, "Transform has thrown an unchecked exception; see the cause exception.");
                    } else if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    } else {
                        throw new UndeclaredThrowableException(e);
                    }
                }
            } finally {
                out = prevOut;
                if (prevOut != tw) {
                    tw.close();
                }
            }
        } catch (TemplateException te) {
            handleTemplateException(te);
        }
    }

    /**
     * Visit a block using buffering/recovery
     */
     void visitAttemptRecover(
             AttemptBlock attemptBlock, TemplateElement attemptedSection, RecoveryBlock recoverySection)
             throws TemplateException, IOException {
        Writer prevOut = this.out;
        StringWriter sw = new StringWriter();
        this.out = sw;
        TemplateException thrownException = null;
        boolean lastFIRE = setFastInvalidReferenceExceptions(false);
        boolean lastInAttemptBlock = inAttemptBlock;
        try {
            inAttemptBlock = true;
            visit(attemptedSection);
        } catch (TemplateException te) {
            thrownException = te;
        } finally {
            inAttemptBlock = lastInAttemptBlock;
            setFastInvalidReferenceExceptions(lastFIRE);
            this.out = prevOut;
        }
        if (thrownException != null) {
            if (ATTEMPT_LOGGER.isDebugEnabled()) {
                ATTEMPT_LOGGER.debug("Error in attempt block " +
                        attemptBlock.getStartLocationQuoted(), thrownException);
            }
            try {
                recoveredErrorStack.add(thrownException);
                visit(recoverySection);
            } finally {
                recoveredErrorStack.remove(recoveredErrorStack.size() - 1);
            }
        } else {
            out.write(sw.toString());
        }
    }

    String getCurrentRecoveredErrorMessage() throws TemplateException {
        if (recoveredErrorStack.isEmpty()) {
            throw new _MiscTemplateException(this, ".error is not available outside of a #recover block");
        }
        return ((Throwable) recoveredErrorStack.get(recoveredErrorStack.size() - 1)).getMessage();
    }

    /**
     * Tells if we are inside an <tt>#attempt</tt> block (but before <tt>#recover</tt>). This can be useful for
     * {@link TemplateExceptionHandler}-s, as then they may don't want to print the error to the output, as
     * <tt>#attempt</tt> will roll it back anyway.
     * 
     * @since 2.3.20
     */
    public boolean isInAttemptBlock() {
        return inAttemptBlock;
    }

    /**
     * Used for {@code #nested}.
     */
    void invokeNestedContent(BodyInstruction.Context bodyCtx) throws TemplateException, IOException {
        Macro.Context invokingMacroContext = getCurrentMacroContext();
        LocalContextStack prevLocalContextStack = localContextStack;
        TemplateObject callPlace = invokingMacroContext.callPlace;
        TemplateElement[] nestedContentBuffer = callPlace instanceof TemplateElement
                ? ((TemplateElement) callPlace).getChildBuffer() : null;
        if (nestedContentBuffer != null) {
            this.currentMacroContext = invokingMacroContext.prevMacroContext;
            currentNamespace = invokingMacroContext.nestedContentNamespace;

            final Configurable prevParent;
            final boolean parentReplacementOn = isBeforeIcI2322();
            prevParent = getParent();
            if (parentReplacementOn) {
                setParent(currentNamespace.getTemplate());
            } else {
                legacyParent = currentNamespace.getTemplate();
            }

            this.localContextStack = invokingMacroContext.prevLocalContextStack;
            if (invokingMacroContext.nestedContentParameterNames != null) {
                pushLocalContext(bodyCtx);
            }
            try {
                visit(nestedContentBuffer);
            } finally {
                if (invokingMacroContext.nestedContentParameterNames != null) {
                    localContextStack.pop();
                }
                this.currentMacroContext = invokingMacroContext;
                currentNamespace = getMacroNamespace(invokingMacroContext.getMacro());
                if (parentReplacementOn) {
                    setParent(prevParent);
                } else {
                    legacyParent = prevParent;
                }
                this.localContextStack = prevLocalContextStack;
            }
        }
    }

    /**
     * "visit" an IteratorBlock
     */
    boolean visitIteratorBlock(IteratorBlock.IterationContext ictxt)
            throws TemplateException, IOException {
        pushLocalContext(ictxt);
        try {
            return ictxt.accept(this);
        } catch (TemplateException te) {
            handleTemplateException(te);
            return true;
        } finally {
            localContextStack.pop();
        }
    }

    /**
     * @param loopVarName
     *            Then name of the loop variable that's also visible in FTL at the moment, whose context we are looking
     *            for.
     * @return The matching context or {@code null} if no such context exists.
     */
    IteratorBlock.IterationContext findEnclosingIterationContextWithVisibleVariable(String loopVarName) {
        return findEnclosingIterationContext(loopVarName);
    }

    /**
     * @return The matching context or {@code null} if no such context exists.
     */
    IteratorBlock.IterationContext findClosestEnclosingIterationContext() {
        return findEnclosingIterationContext(null);
    }

    private IteratorBlock.IterationContext findEnclosingIterationContext(String visibleLoopVarName) {
        LocalContextStack ctxStack = getLocalContextStack();
        if (ctxStack != null) {
            for (int i = ctxStack.size() - 1; i >= 0; i--) {
                Object ctx = ctxStack.get(i);
                if (ctx instanceof IteratorBlock.IterationContext
                        && (visibleLoopVarName == null
                            || ((IteratorBlock.IterationContext) ctx)
                                    .hasVisibleLoopVar(visibleLoopVarName))) {
                    return (IteratorBlock.IterationContext) ctx;
                }
            }
        }
        return null;
    }

    /**
     * Evaluate expression with shadowing a single variable with a new local variable.
     */
    TemplateModel evaluateWithNewLocal(Expression exp, String lambdaArgName, TemplateModel lamdaArgValue)
            throws TemplateException {
        pushLocalContext(new LocalContextWithNewLocal(lambdaArgName, lamdaArgValue));
        try {
            return exp.eval(this);
        } finally {
            localContextStack.pop();
        }
    }

    /**
     * Specialization for 1 local variables.
     */
    private static class LocalContextWithNewLocal implements LocalContext {
        private final String lambdaArgName;
        private final TemplateModel lambdaArgValue;

        public LocalContextWithNewLocal(String lambdaArgName, TemplateModel lambdaArgValue) {
            this.lambdaArgName = lambdaArgName;
            this.lambdaArgValue = lambdaArgValue;
        }

        @Override
        public TemplateModel getLocalVariable(String name) throws TemplateModelException {
            return name.equals(lambdaArgName) ? lambdaArgValue : null;
        }

        @Override
        public Collection getLocalVariableNames() throws TemplateModelException {
            return Collections.singleton(lambdaArgName);
        }
    }

    /**
     * Used for {@code #visit} and {@code #recurse}.
     */
    void invokeNodeHandlerFor(TemplateNodeModel node, TemplateSequenceModel namespaces)
            throws TemplateException, IOException {
        if (nodeNamespaces == null) {
            SimpleSequence ss = new SimpleSequence(1, _ObjectWrappers.SAFE_OBJECT_WRAPPER);
            ss.add(currentNamespace);
            nodeNamespaces = ss;
        }
        int prevNodeNamespaceIndex = this.nodeNamespaceIndex;
        String prevNodeName = this.currentNodeName;
        String prevNodeNS = this.currentNodeNS;
        TemplateSequenceModel prevNodeNamespaces = nodeNamespaces;
        TemplateNodeModel prevVisitorNode = currentVisitorNode;
        currentVisitorNode = node;
        if (namespaces != null) {
            this.nodeNamespaces = namespaces;
        }
        try {
            TemplateModel macroOrTransform = getNodeProcessor(node);
            if (macroOrTransform instanceof Macro) {
                invokeMacro((Macro) macroOrTransform, null, null, null, null);
            } else if (macroOrTransform instanceof TemplateTransformModel) {
                visitAndTransform(null, (TemplateTransformModel) macroOrTransform, null);
            } else {
                String nodeType = node.getNodeType();
                if (nodeType != null) {
                    // If the node's type is 'text', we just output it.
                    if ((nodeType.equals("text") && node instanceof TemplateScalarModel)) {
                        out.write(((TemplateScalarModel) node).getAsString());
                    } else if (nodeType.equals("document")) {
                        recurse(node, namespaces);
                    }
                    // We complain here, unless the node's type is 'pi', or "comment" or "document_type", in which case
                    // we just ignore it.
                    else if (!nodeType.equals("pi")
                            && !nodeType.equals("comment")
                            && !nodeType.equals("document_type")) {
                        throw new _MiscTemplateException(
                                this, noNodeHandlerDefinedDescription(node, node.getNodeNamespace(), nodeType));
                    }
                } else {
                    throw new _MiscTemplateException(
                            this, noNodeHandlerDefinedDescription(node, node.getNodeNamespace(), "default"));
                }
            }
        } finally {
            this.currentVisitorNode = prevVisitorNode;
            this.nodeNamespaceIndex = prevNodeNamespaceIndex;
            this.currentNodeName = prevNodeName;
            this.currentNodeNS = prevNodeNS;
            this.nodeNamespaces = prevNodeNamespaces;
        }
    }

    private Object[] noNodeHandlerDefinedDescription(
            TemplateNodeModel node, String ns, String nodeType)
                    throws TemplateModelException {
        String nsPrefix;
        if (ns != null) {
            if (ns.length() > 0) {
                nsPrefix = " and namespace ";
            } else {
                nsPrefix = " and no namespace";
            }
        } else {
            nsPrefix = "";
            ns = "";
        }
        return new Object[] { "No macro or directive is defined for node named ",
                new _DelayedJQuote(node.getNodeName()), nsPrefix, ns,
                ", and there is no fallback handler called @", nodeType, " either." };
    }

    void fallback() throws TemplateException, IOException {
        TemplateModel macroOrTransform = getNodeProcessor(currentNodeName, currentNodeNS, nodeNamespaceIndex);
        if (macroOrTransform instanceof Macro) {
            invokeMacro((Macro) macroOrTransform, null, null, null, null);
        } else if (macroOrTransform instanceof TemplateTransformModel) {
            visitAndTransform(null, (TemplateTransformModel) macroOrTransform, null);
        }
    }

    /**
     * Calls a macro with the given arguments and nested block.
     */
    void invokeMacro(Macro macro,
            Map<String, ? extends Expression> namedArgs, List<? extends Expression> positionalArgs,
            List<String> bodyParameterNames, TemplateObject callPlace) throws TemplateException, IOException {
        invokeMacroOrFunctionCommonPart(macro, namedArgs, positionalArgs, bodyParameterNames, callPlace);
    }

    /**
     * Calls an FTL function, and returns its return value.
     */
    TemplateModel invokeFunction(
            Environment env, Macro func, List<? extends Expression> argumentExps, TemplateObject callPlace)
            throws TemplateException {
        env.setLastReturnValue(null);
        if (!func.isFunction()) {
            throw new _MiscTemplateException(env, "A macro cannot be called in an expression. (Functions can be.)");
        }
        Writer prevOut = env.getOut();
        try {
            env.setOut(NullWriter.INSTANCE);
            env.invokeMacro(func, null, argumentExps, null, callPlace);
        } catch (IOException e) {
            // Should not occur
            throw new TemplateException("Unexpected exception during function execution", e, env);
        } finally {
            env.setOut(prevOut);
        }
        return env.getLastReturnValue();
    }

    private void invokeMacroOrFunctionCommonPart(Macro macroOrFunction,
            Map<String, ? extends Expression> namedArgs, List<? extends Expression> positionalArgs,
            List<String> bodyParameterNames, TemplateObject callPlace) throws TemplateException,
            IOException {
        if (macroOrFunction == Macro.DO_NOTHING_MACRO) {
            return;
        }

        boolean elementPushed;
        if (!incompatibleImprovementsGE2328) {
            // Doing this so early is wrong, as now the arguments will be evaluated while the called macro/function is
            // in the element stack. Thus .current_template_name will be wrong for example.
            pushElement(macroOrFunction);
            elementPushed = true;
        } else {
            elementPushed = false;
        }
        try {
            final Macro.Context macroCtx = macroOrFunction.new Context(this, callPlace, bodyParameterNames);
            // Causes the evaluation of argument expressions:
            setMacroContextLocalsFromArguments(macroCtx, macroOrFunction, namedArgs, positionalArgs);

            if (!elementPushed) { // When incompatibleImprovements >= 2.3.28
                pushElement(macroOrFunction);
                elementPushed = true;
            }

            final Macro.Context prevMacroCtx = currentMacroContext;
            currentMacroContext = macroCtx;

            final LocalContextStack prevLocalContextStack = localContextStack;
            localContextStack = null;

            final Namespace prevNamespace = currentNamespace;
            currentNamespace = getMacroNamespace(macroOrFunction);

            try {
                macroCtx.checkParamsSetAndApplyDefaults(this);
                visit(macroOrFunction.getChildBuffer());
            } catch (ReturnInstruction.Return re) {
                // Not an error, just a <#return>
            } catch (TemplateException te) {
                handleTemplateException(te);
            } finally {
                currentMacroContext = prevMacroCtx;
                localContextStack = prevLocalContextStack;
                currentNamespace = prevNamespace;
            }
        } finally {
            if (elementPushed) {
                popElement();
            }
        }
    }

    /**
     * Sets the local variables corresponding to the macro call arguments in the macro context.
     */
    private void setMacroContextLocalsFromArguments(
            final Macro.Context macroCtx,
            final Macro macro,
            final Map<String, ? extends Expression> namedArgs, final List<? extends Expression> positionalArgs)
            throws TemplateException {
        String catchAllParamName = macro.getCatchAll();
        SimpleHash namedCatchAllParamValue = null;
        SimpleSequence positionalCatchAllParamValue = null;
        int nextPositionalArgToAssignIdx = 0;

        // Used for ?with_args(...):
        WithArgsState withArgsState = getWithArgState(macro);
        if (withArgsState != null) {
            TemplateHashModelEx byNameWithArgs = withArgsState.byName;
            TemplateSequenceModel byPositionWithArgs = withArgsState.byPosition;

            if (byNameWithArgs != null) {
                TemplateHashModelEx2.KeyValuePairIterator withArgsKVPIter
                        = TemplateModelUtils.getKeyValuePairIterator(byNameWithArgs);
                while (withArgsKVPIter.hasNext()) {
                    TemplateHashModelEx2.KeyValuePair withArgKVP = withArgsKVPIter.next();

                    String argName;
                    {
                        TemplateModel argNameTM = withArgKVP.getKey();
                        if (!(argNameTM instanceof TemplateScalarModel)) {
                            throw new _TemplateModelException(
                                    "Expected string keys in the \"with args\" hash, but one of the keys was ",
                                    new _DelayedAOrAn(new _DelayedFTLTypeDescription(argNameTM)), ".");
                        }
                        argName = EvalUtil.modelToString((TemplateScalarModel) argNameTM, null, null);
                    }

                    TemplateModel argValue = withArgKVP.getValue();
                    // What if argValue is null? It still has to occur in the named catch-all parameter, to be similar
                    // to <@macroWithCatchAll a=null b=null />, which will also add the keys to the catch-all hash.
                    // Similarly, we also still fail if the name is not declared.
                    final boolean isArgNameDeclared = macro.hasArgNamed(argName);
                    if (isArgNameDeclared) {
                        macroCtx.setLocalVar(argName, argValue);
                    } else if (catchAllParamName != null) {
                        if (namedCatchAllParamValue == null) {
                            namedCatchAllParamValue = initNamedCatchAllParameter(macroCtx, catchAllParamName);
                        }
                        if (!withArgsState.orderLast) {
                            namedCatchAllParamValue.put(argName, argValue);
                        } else {
                            List<NameValuePair> orderLastByNameCatchAll = withArgsState.orderLastByNameCatchAll;
                            if (orderLastByNameCatchAll == null) {
                                orderLastByNameCatchAll = new ArrayList<>();
                                withArgsState.orderLastByNameCatchAll = orderLastByNameCatchAll;
                            }
                            orderLastByNameCatchAll.add(new NameValuePair(argName, argValue));
                        }
                    } else {
                        throw newUndeclaredParamNameException(macro, argName);
                    }
                } // while (withArgsKVPIter.hasNext())
            } else if (byPositionWithArgs != null) {
                if (!withArgsState.orderLast) { // ?withArgs
                    String[] argNames = macro.getArgumentNamesNoCopy();
                    final int argsCnt = byPositionWithArgs.size();
                    if (argNames.length < argsCnt && catchAllParamName == null) {
                        throw newTooManyArgumentsException(macro, argNames, argsCnt);
                    }
                    for (int argIdx = 0; argIdx < argsCnt; argIdx++) {
                        TemplateModel argValue = byPositionWithArgs.get(argIdx);
                        try {
                            if (nextPositionalArgToAssignIdx < argNames.length) {
                                String argName = argNames[nextPositionalArgToAssignIdx++];
                                macroCtx.setLocalVar(argName, argValue);
                            } else {
                                if (positionalCatchAllParamValue == null) {
                                    positionalCatchAllParamValue = initPositionalCatchAllParameter(macroCtx, catchAllParamName);
                                }
                                positionalCatchAllParamValue.add(argValue);
                            }
                        } catch (RuntimeException re) {
                            throw new _MiscTemplateException(re, this);
                        }
                    }
                } else { // ?withArgsLast
                    if (namedArgs != null && !namedArgs.isEmpty() && byPositionWithArgs.size() != 0) {
                        // Unlike with ?withArgs, here we can't know in general which argument byPositionWithArgs[0]
                        // meant to refer to, as the named arguments have already taken some indexes.
                        throw new _MiscTemplateException("Call can't pass parameters by name, as there's " +
                                "\"with args last\" in effect that specifies parameters by position.");
                    }
                    if (catchAllParamName == null) {
                        // To fail before Expression-s for some normal arguments are evaluated:
                        int totalPositionalArgCnt =
                                (positionalArgs != null ? positionalArgs.size() : 0) + byPositionWithArgs.size();
                        if (totalPositionalArgCnt > macro.getArgumentNamesNoCopy().length) {
                            throw newTooManyArgumentsException(macro, macro.getArgumentNamesNoCopy(), totalPositionalArgCnt);
                        }
                    }
                }
            }
        } // if (withArgsState != null)

        if (namedArgs != null) {
            if (catchAllParamName != null && namedCatchAllParamValue == null && positionalCatchAllParamValue == null) {
                // If a macro call has no argument (like <@m />), before 2.3.30 we assumed it's a by-name call. But now
                // if we have ?with_args(args), its argument type decides if the call is by-name or by-position.
                if (namedArgs.isEmpty() && withArgsState != null && withArgsState.byPosition != null) {
                    positionalCatchAllParamValue = initPositionalCatchAllParameter(macroCtx, catchAllParamName);
                } else {
                    namedCatchAllParamValue = initNamedCatchAllParameter(macroCtx, catchAllParamName);
                }
            }

            for (Map.Entry<String, ? extends Expression> argNameAndValExp : namedArgs.entrySet()) {
                final String argName = argNameAndValExp.getKey();
                final boolean isArgNameDeclared = macro.hasArgNamed(argName);
                if (isArgNameDeclared || namedCatchAllParamValue != null) {
                    final Expression argValueExp = argNameAndValExp.getValue();
                    TemplateModel argValue = argValueExp.eval(this);
                    if (isArgNameDeclared) {
                        macroCtx.setLocalVar(argName, argValue);
                    } else {
                        namedCatchAllParamValue.put(argName, argValue);
                    }
                } else {
                    if (positionalCatchAllParamValue != null) {
                        throw newBothNamedAndPositionalCatchAllParamsException(macro);
                    } else {
                        throw newUndeclaredParamNameException(macro, argName);
                    }
                }
            }
        } else if (positionalArgs != null) {
            if (catchAllParamName != null && positionalCatchAllParamValue == null && namedCatchAllParamValue == null) {
                if (positionalArgs.isEmpty() && withArgsState != null && withArgsState.byName != null) {
                    namedCatchAllParamValue = initNamedCatchAllParameter(macroCtx, catchAllParamName);
                } else {
                    positionalCatchAllParamValue = initPositionalCatchAllParameter(macroCtx, catchAllParamName);
                }
            }

            String[] argNames = macro.getArgumentNamesNoCopy();
            final int argsCnt = positionalArgs.size();
            final int argsWithWithArgsCnt = argsCnt + nextPositionalArgToAssignIdx;
            if (argNames.length < argsWithWithArgsCnt && positionalCatchAllParamValue == null) {
                if (namedCatchAllParamValue != null) {
                    throw newBothNamedAndPositionalCatchAllParamsException(macro);
                } else {
                    throw newTooManyArgumentsException(macro, argNames, argsWithWithArgsCnt);
                }
            }
            for (int srcPosArgIdx = 0; srcPosArgIdx < argsCnt; srcPosArgIdx++) {
                Expression argValueExp = positionalArgs.get(srcPosArgIdx);
                TemplateModel argValue;
                try {
                    argValue = argValueExp.eval(this);
                } catch (RuntimeException e) {
                    throw new _MiscTemplateException(e, this);
                }
                if (nextPositionalArgToAssignIdx < argNames.length) {
                    String argName = argNames[nextPositionalArgToAssignIdx++];
                    macroCtx.setLocalVar(argName, argValue);
                } else {
                    positionalCatchAllParamValue.add(argValue);
                }
            }
        } // else if (positionalArgs != null)

        if (withArgsState != null && withArgsState.orderLast) {
            if (withArgsState.orderLastByNameCatchAll != null) {
                for (NameValuePair nameValuePair : withArgsState.orderLastByNameCatchAll) {
                    if (!namedCatchAllParamValue.containsKey(nameValuePair.name)) {
                        namedCatchAllParamValue.put(nameValuePair.name, nameValuePair.value);
                    }
                }
            } else if (withArgsState.byPosition != null) {
                TemplateSequenceModel byPosition = withArgsState.byPosition;
                int withArgCnt = byPosition.size();
                String[] argNames = macro.getArgumentNamesNoCopy();
                for (int withArgIdx = 0; withArgIdx < withArgCnt; withArgIdx++) {
                    TemplateModel withArgValue = byPosition.get(withArgIdx);
                    if (nextPositionalArgToAssignIdx < argNames.length) {
                        String argName = argNames[nextPositionalArgToAssignIdx++];
                        macroCtx.setLocalVar(argName, withArgValue);
                    } else {
                        // It was checked much earlier that we don't have too many arguments, so this must work:
                        positionalCatchAllParamValue.add(withArgValue);
                    }
                }
            }
        }
    }

    private static WithArgsState getWithArgState(Macro macro) {
        Macro.WithArgs withArgs = macro.getWithArgs();
        return withArgs == null ? null : new WithArgsState(withArgs.getByName(), withArgs.getByPosition(),
                withArgs.isOrderLast());
    }

    private static final class WithArgsState {
        private final TemplateHashModelEx byName;
        private final TemplateSequenceModel byPosition;
        private final boolean orderLast;
        private List<NameValuePair> orderLastByNameCatchAll;

        public WithArgsState(TemplateHashModelEx byName, TemplateSequenceModel byPosition, boolean orderLast) {
            this.byName = byName;
            this.byPosition = byPosition;
            this.orderLast = orderLast;
        }
    }

    private static final class NameValuePair {
        private final String name;
        private final TemplateModel value;

        public NameValuePair(String name, TemplateModel value) {
            this.name = name;
            this.value = value;
        }
    }

    private _MiscTemplateException newTooManyArgumentsException(Macro macro, String[] argNames, int argsCnt) {
        return new _MiscTemplateException(this,
                (macro.isFunction() ? "Function " : "Macro "), new _DelayedJQuote(macro.getName()),
                " only accepts ", new _DelayedToString(argNames.length), " parameters, but got ",
                new _DelayedToString(argsCnt), ".");
    }

    private static SimpleSequence initPositionalCatchAllParameter(Macro.Context macroCtx, String catchAllParamName) {
        SimpleSequence positionalCatchAllParamValue;
        positionalCatchAllParamValue = new SimpleSequence(_ObjectWrappers.SAFE_OBJECT_WRAPPER);
        macroCtx.setLocalVar(catchAllParamName, positionalCatchAllParamValue);
        return positionalCatchAllParamValue;
    }

    private static SimpleHash initNamedCatchAllParameter(Macro.Context macroCtx, String catchAllParamName) {
        SimpleHash namedCatchAllParamValue;
        namedCatchAllParamValue = new SimpleHash(
                new LinkedHashMap<String, Object>(), _ObjectWrappers.SAFE_OBJECT_WRAPPER, 0);
        macroCtx.setLocalVar(catchAllParamName, namedCatchAllParamValue);
        return namedCatchAllParamValue;
    }

    private _MiscTemplateException newUndeclaredParamNameException(Macro macro, String argName) {
        return new _MiscTemplateException(this,
                (macro.isFunction() ? "Function " : "Macro "), new _DelayedJQuote(macro.getName()),
                " has no parameter with name ", new _DelayedJQuote(argName), ". Valid parameter names are: "
                , new _DelayedJoinWithComma(macro.getArgumentNamesNoCopy()));
    }

    private _MiscTemplateException newBothNamedAndPositionalCatchAllParamsException(Macro macro) {
        return new _MiscTemplateException(this,
                (macro.isFunction() ? "Function " : "Macro "), new _DelayedJQuote(macro.getName()),
                " call can't have both named and positional arguments that has to go into catch-all parameter.");
    }

    /**
     * Defines the given macro in the current namespace (doesn't call it).
     */
    void visitMacroDef(Macro macro) {
        macroToNamespaceLookup.put(macro.getNamespaceLookupKey(), currentNamespace);
        currentNamespace.put(macro.getName(), macro);
    }

    Namespace getMacroNamespace(Macro macro) {
        return macroToNamespaceLookup.get(macro.getNamespaceLookupKey());
    }

    void recurse(TemplateNodeModel node, TemplateSequenceModel namespaces)
            throws TemplateException, IOException {
        if (node == null) {
            node = this.getCurrentVisitorNode();
            if (node == null) {
                throw new _TemplateModelException(
                        "The target node of recursion is missing or null.");
            }
        }
        TemplateSequenceModel children = node.getChildNodes();
        if (children == null) {
            return;
        }
        int size = children.size();
        for (int i = 0; i < size; i++) {
            TemplateNodeModel child = (TemplateNodeModel) children.get(i);
            if (child != null) {
                invokeNodeHandlerFor(child, namespaces);
            }
        }
    }

    Macro.Context getCurrentMacroContext() {
        return currentMacroContext;
    }

    private void handleTemplateException(TemplateException templateException)
            throws TemplateException {
        if (templateException instanceof TemplateModelException
                && ((TemplateModelException) templateException).getReplaceWithCause()
                && templateException.getCause() instanceof TemplateException) {
            templateException = (TemplateException) templateException.getCause();
        }
        
        // Logic to prevent double-handling of the exception in
        // nested visit() calls.
        if (lastThrowable == templateException) {
            throw templateException;
        }
        lastThrowable = templateException;

        if (getLogTemplateExceptions() && LOG.isErrorEnabled()
                && !isInAttemptBlock() /* because then the AttemptExceptionReporter will report this */) {
            LOG.error("Error executing FreeMarker template", templateException);
        }

        try {
            // Stop exception is not passed to the handler, but
            // explicitly rethrown.
            if (templateException instanceof StopException) {
                throw templateException;
            }
    
            // Finally, pass the exception to the handler
            getTemplateExceptionHandler().handleTemplateException(templateException, this, out);
        } catch (TemplateException e) {
            // Note that if the TemplateExceptionHandler doesn't rethrow the exception, we don't get in there.
            if (isInAttemptBlock()) {
                this.getAttemptExceptionReporter().report(templateException, this);
            }
            throw e;
        }
    }

    @Override
    public void setTemplateExceptionHandler(TemplateExceptionHandler templateExceptionHandler) {
        super.setTemplateExceptionHandler(templateExceptionHandler);
        lastThrowable = null;
    }

    @Override
    public void setLocale(Locale locale) {
        Locale prevLocale = getLocale();
        super.setLocale(locale);
        if (!locale.equals(prevLocale)) {
            cachedTemplateNumberFormats = null;
            if (cachedTemplateNumberFormat != null && cachedTemplateNumberFormat.isLocaleBound()) {
                cachedTemplateNumberFormat = null;
            }

            if (cachedTempDateFormatArray != null) {
                for (int i = 0; i < CACHED_TDFS_LENGTH; i++) {
                    final TemplateDateFormat f = cachedTempDateFormatArray[i];
                    if (f != null && f.isLocaleBound()) {
                        cachedTempDateFormatArray[i] = null;
                    }
                }
            }

            cachedTempDateFormatsByFmtStrArray = null;

            cachedCollator = null;
        }
    }

    @Override
    public void setTimeZone(TimeZone timeZone) {
        TimeZone prevTimeZone = getTimeZone();
        super.setTimeZone(timeZone);

        if (!timeZone.equals(prevTimeZone)) {
            if (cachedTempDateFormatArray != null) {
                for (int i = 0; i < CACHED_TDFS_SQL_D_T_TZ_OFFS; i++) {
                    TemplateDateFormat f = cachedTempDateFormatArray[i];
                    if (f != null && f.isTimeZoneBound()) {
                        cachedTempDateFormatArray[i] = null;
                    }
                }
            }
            if (cachedTempDateFormatsByFmtStrArray != null) {
                for (int i = 0; i < CACHED_TDFS_SQL_D_T_TZ_OFFS; i++) {
                    cachedTempDateFormatsByFmtStrArray[i] = null;
                }
            }

            cachedSQLDateAndTimeTimeZoneSameAsNormal = null;
        }
    }

    @Override
    public void setSQLDateAndTimeTimeZone(TimeZone timeZone) {
        TimeZone prevTimeZone = getSQLDateAndTimeTimeZone();
        super.setSQLDateAndTimeTimeZone(timeZone);

        if (!nullSafeEquals(timeZone, prevTimeZone)) {
            if (cachedTempDateFormatArray != null) {
                for (int i = CACHED_TDFS_SQL_D_T_TZ_OFFS; i < CACHED_TDFS_LENGTH; i++) {
                    TemplateDateFormat format = cachedTempDateFormatArray[i];
                    if (format != null && format.isTimeZoneBound()) {
                        cachedTempDateFormatArray[i] = null;
                    }
                }
            }
            if (cachedTempDateFormatsByFmtStrArray != null) {
                for (int i = CACHED_TDFS_SQL_D_T_TZ_OFFS; i < CACHED_TDFS_LENGTH; i++) {
                    cachedTempDateFormatsByFmtStrArray[i] = null;
                }
            }

            cachedSQLDateAndTimeTimeZoneSameAsNormal = null;
        }
    }

    // Replace with Objects.equals in Java 7
    private static boolean nullSafeEquals(Object o1, Object o2) {
        if (o1 == o2) return true;
        if (o1 == null || o2 == null) return false;
        return o1.equals(o2);
    }

    /**
     * Tells if the same concrete time zone is used for SQL date-only and time-only values as for other
     * date/time/date-time values.
     */
    boolean isSQLDateAndTimeTimeZoneSameAsNormal() {
        if (cachedSQLDateAndTimeTimeZoneSameAsNormal == null) {
            cachedSQLDateAndTimeTimeZoneSameAsNormal = Boolean.valueOf(
                    getSQLDateAndTimeTimeZone() == null
                            || getSQLDateAndTimeTimeZone().equals(getTimeZone()));
        }
        return cachedSQLDateAndTimeTimeZoneSameAsNormal.booleanValue();
    }

    @Override
    public void setURLEscapingCharset(String urlEscapingCharset) {
        cachedURLEscapingCharsetSet = false;
        super.setURLEscapingCharset(urlEscapingCharset);
    }

    /*
     * Note that altough it's not allowed to set this setting with the <tt>setting</tt> directive, it still must be
     * allowed to set it from Java code while the template executes, since some frameworks allow templates to actually
     * change the output encoding on-the-fly.
     */
    @Override
    public void setOutputEncoding(String outputEncoding) {
        cachedURLEscapingCharsetSet = false;
        super.setOutputEncoding(outputEncoding);
    }

    /**
     * Returns the name of the charset that should be used for URL encoding. This will be <code>null</code> if the
     * information is not available. The function caches the return value, so it's quick to call it repeatedly.
     */
    String getEffectiveURLEscapingCharset() {
        if (!cachedURLEscapingCharsetSet) {
            cachedURLEscapingCharset = getURLEscapingCharset();
            if (cachedURLEscapingCharset == null) {
                cachedURLEscapingCharset = getOutputEncoding();
            }
            cachedURLEscapingCharsetSet = true;
        }
        return cachedURLEscapingCharset;
    }

    Collator getCollator() {
        if (cachedCollator == null) {
            cachedCollator = Collator.getInstance(getLocale());
        }
        return cachedCollator;
    }

    /**
     * Compares two {@link TemplateModel}-s according the rules of the FTL "==" operator.
     * 
     * @since 2.3.20
     */
    public boolean applyEqualsOperator(TemplateModel leftValue, TemplateModel rightValue)
            throws TemplateException {
        return EvalUtil.compare(leftValue, EvalUtil.CMP_OP_EQUALS, rightValue, this);
    }

    /**
     * Compares two {@link TemplateModel}-s according the rules of the FTL "==" operator, except that if the two types
     * are incompatible, they are treated as non-equal instead of throwing an exception. Comparing dates of different
     * types (date-only VS time-only VS date-time) will still throw an exception, however.
     * 
     * @since 2.3.20
     */
    public boolean applyEqualsOperatorLenient(TemplateModel leftValue, TemplateModel rightValue)
            throws TemplateException {
        return EvalUtil.compareLenient(leftValue, EvalUtil.CMP_OP_EQUALS, rightValue, this);
    }

    /**
     * Compares two {@link TemplateModel}-s according the rules of the FTL "&lt;" operator.
     * 
     * @since 2.3.20
     */
    public boolean applyLessThanOperator(TemplateModel leftValue, TemplateModel rightValue)
            throws TemplateException {
        return EvalUtil.compare(leftValue, EvalUtil.CMP_OP_LESS_THAN, rightValue, this);
    }

    /**
     * Compares two {@link TemplateModel}-s according the rules of the FTL "&lt;" operator.
     * 
     * @since 2.3.20
     */
    public boolean applyLessThanOrEqualsOperator(TemplateModel leftValue, TemplateModel rightValue)
            throws TemplateException {
        return EvalUtil.compare(leftValue, EvalUtil.CMP_OP_LESS_THAN_EQUALS, rightValue, this);
    }

    /**
     * Compares two {@link TemplateModel}-s according the rules of the FTL "&gt;" operator.
     * 
     * @since 2.3.20
     */
    public boolean applyGreaterThanOperator(TemplateModel leftValue, TemplateModel rightValue)
            throws TemplateException {
        return EvalUtil.compare(leftValue, EvalUtil.CMP_OP_GREATER_THAN, rightValue, this);
    }

    /**
     * Compares two {@link TemplateModel}-s according the rules of the FTL "&gt;=" operator.
     * 
     * @since 2.3.20
     */
    public boolean applyWithGreaterThanOrEqualsOperator(TemplateModel leftValue, TemplateModel rightValue)
            throws TemplateException {
        return EvalUtil.compare(leftValue, EvalUtil.CMP_OP_GREATER_THAN_EQUALS, rightValue, this);
    }

    public void setOut(Writer out) {
        this.out = out;
    }

    public Writer getOut() {
        return out;
    }

    @Override
    public void setNumberFormat(String formatName) {
        super.setNumberFormat(formatName);
        cachedTemplateNumberFormat = null;
    }

    /**
     * Format number with the default number format.
     * 
     * @param exp
     *            The blamed expression if an error occurs; it's only needed for better error messages
     */
    String formatNumberToPlainText(TemplateNumberModel number, Expression exp, boolean useTempModelExc)
            throws TemplateException {
        return formatNumberToPlainText(number, getTemplateNumberFormat(exp, useTempModelExc), exp, useTempModelExc);
    }

    /**
     * Format number with the number format specified as the parameter, with the current locale.
     * 
     * @param exp
     *            The blamed expression if an error occurs; it's only needed for better error messages
     */
    String formatNumberToPlainText(
            TemplateNumberModel number, TemplateNumberFormat format, Expression exp,
            boolean useTempModelExc)
            throws TemplateException {
        try {
            return EvalUtil.assertFormatResultNotNull(format.formatToPlainText(number));
        } catch (TemplateValueFormatException e) {
            throw _MessageUtil.newCantFormatNumberException(format, exp, e, useTempModelExc);
        }
    }

    /**
     * Format number with the number format specified as the parameter, with the current locale.
     * 
     * @param exp
     *            The blamed expression if an error occurs; it's only needed for better error messages
     */
    String formatNumberToPlainText(Number number, BackwardCompatibleTemplateNumberFormat format, Expression exp)
            throws TemplateModelException, _MiscTemplateException {
        try {
            return format.format(number);
        } catch (UnformattableValueException e) {
            throw new _MiscTemplateException(exp, e, this,
                    "Failed to format number with ", new _DelayedJQuote(format.getDescription()), ": ",
                    e.getMessage());
        }
    }

    /**
     * Returns the current number format ({@link #getNumberFormat()}) as {@link TemplateNumberFormat}.
     * 
     * <p>
     * Performance notes: The result is stored for reuse, so calling this method frequently is usually not a problem.
     * However, at least as of this writing (2.3.24), changing the current locale {@link #setLocale(Locale)} or changing
     * the current number format ({@link #setNumberFormat(String)}) will drop the stored value, so it will have to be
     * recalculated.
     * 
     * @since 2.3.24
     */
    public TemplateNumberFormat getTemplateNumberFormat() throws TemplateValueFormatException {
        TemplateNumberFormat format = cachedTemplateNumberFormat;
        if (format == null) {
            format = getTemplateNumberFormat(getNumberFormat(), false);
            cachedTemplateNumberFormat = format;
        }
        return format;
    }

    /**
     * Returns the number format as {@link TemplateNumberFormat} for the given format string and the current locale.
     * (The current locale is the locale returned by {@link #getLocale()}.) Note that the result will be cached in the
     * {@link Environment} instance (though at least in 2.3.24 the cache will be flushed if the current locale of the
     * {@link Environment} is changed).
     * 
     * @param formatString
     *            A string that you could also use as the value of the {@code numberFormat} configuration setting. Can't
     *            be {@code null}.
     * 
     * @since 2.3.24
     */
    public TemplateNumberFormat getTemplateNumberFormat(String formatString) throws TemplateValueFormatException {
        return getTemplateNumberFormat(formatString, true);
    }

    /**
     * Returns the number format as {@link TemplateNumberFormat}, for the given format string and locale. To get a
     * number format for the current locale, use {@link #getTemplateNumberFormat(String)} instead.
     * 
     * <p>
     * Note on performance (which was true at least for 2.3.24): Unless the locale happens to be equal to the current
     * locale, the {@link Environment}-level format cache can't be used, so the format string has to be parsed and the
     * matching factory has to be get an invoked, which is much more expensive than getting the format from the cache.
     * Thus the returned format should be stored by the caller for later reuse (but only within the current thread and
     * in relation to the current {@link Environment}), if it will be needed frequently.
     * 
     * @param formatString
     *            A string that you could also use as the value of the {@code numberFormat} configuration setting.
     * @param locale
     *            The locale of the number format; not {@code null}.
     * 
     * @since 2.3.24
     */
    public TemplateNumberFormat getTemplateNumberFormat(String formatString, Locale locale)
            throws TemplateValueFormatException {
        if (locale.equals(getLocale())) {
            getTemplateNumberFormat(formatString);
        }

        return getTemplateNumberFormatWithoutCache(formatString, locale);
    }

    /**
     * Convenience wrapper around {@link #getTemplateNumberFormat()} to be called during expression evaluation.
     */
    TemplateNumberFormat getTemplateNumberFormat(Expression exp, boolean useTempModelExc) throws TemplateException {
        TemplateNumberFormat format;
        try {
            format = getTemplateNumberFormat();
        } catch (TemplateValueFormatException e) {
            _ErrorDescriptionBuilder desc = new _ErrorDescriptionBuilder(
                    "Failed to get number format object for the current number format string, ",
                    new _DelayedJQuote(getNumberFormat()), ": ", e.getMessage())
                    .blame(exp); 
            throw useTempModelExc
                    ? new _TemplateModelException(e, this, desc) : new _MiscTemplateException(e, this, desc);
        }
        return format;
    }

    /**
     * Convenience wrapper around {@link #getTemplateNumberFormat(String)} to be called during expression evaluation.
     * 
     * @param exp
     *            The blamed expression if an error occurs; it's only needed for better error messages
     */
    TemplateNumberFormat getTemplateNumberFormat(String formatString, Expression exp, boolean useTempModelExc)
            throws TemplateException {
        TemplateNumberFormat format;
        try {
            format = getTemplateNumberFormat(formatString);
        } catch (TemplateValueFormatException e) {
            _ErrorDescriptionBuilder desc = new _ErrorDescriptionBuilder(
                    "Failed to get number format object for the ", new _DelayedJQuote(formatString),
                    " number format string: ", e.getMessage())
                    .blame(exp);
            throw useTempModelExc
                    ? new _TemplateModelException(e, this, desc) : new _MiscTemplateException(e, this, desc);
        }
        return format;
    }

    /**
     * Gets the {@link TemplateNumberFormat} <em>for the current locale</em>.
     * 
     * @param formatString
     *            Not {@code null}
     * @param cacheResult
     *            If the results should stored in the {@link Environment}-level cache. It will still try to get the
     *            result from the cache regardless of this parameter.
     */
    private TemplateNumberFormat getTemplateNumberFormat(String formatString, boolean cacheResult)
            throws TemplateValueFormatException {
        if (cachedTemplateNumberFormats == null) {
            if (cacheResult) {
                cachedTemplateNumberFormats = new HashMap<>();
            }
        } else {
            TemplateNumberFormat format = cachedTemplateNumberFormats.get(formatString);
            if (format != null) {
                return format;
            }
        }

        TemplateNumberFormat format = getTemplateNumberFormatWithoutCache(formatString, getLocale());

        if (cacheResult) {
            cachedTemplateNumberFormats.put(formatString, format);
        }
        return format;
    }

    static final String COMPUTER_FORMAT_STRING = "computer";

    /**
     * Returns the {@link TemplateNumberFormat} for the given parameters without using the {@link Environment}-level
     * cache. Of course, the {@link TemplateNumberFormatFactory} involved might still uses its own cache.
     * 
     * @param formatString
     *            Not {@code null}
     * @param locale
     *            Not {@code null}
     */
    private TemplateNumberFormat getTemplateNumberFormatWithoutCache(String formatString, Locale locale)
            throws TemplateValueFormatException {
        int formatStringLen = formatString.length();
        if (formatStringLen > 1
                && formatString.charAt(0) == '@'
                && (isIcI2324OrLater() || hasCustomFormats())
                && Character.isLetter(formatString.charAt(1))) {
            final String name;
            final String params;
            {
                int endIdx;
                findParamsStart: for (endIdx = 1; endIdx < formatStringLen; endIdx++) {
                    char c = formatString.charAt(endIdx);
                    if (c == ' ' || c == '_') {
                        break findParamsStart;
                    }
                }
                name = formatString.substring(1, endIdx);
                params = endIdx < formatStringLen ? formatString.substring(endIdx + 1) : "";
            }

            TemplateNumberFormatFactory formatFactory = getCustomNumberFormat(name);
            if (formatFactory == null) {
                throw new UndefinedCustomFormatException(
                        "No custom number format was defined with name " + StringUtil.jQuote(name));
            }

            return formatFactory.get(params, locale, this);
        } else if (formatStringLen >= 1 && formatString.charAt(0) == 'c'
                && (formatStringLen == 1 || formatString.equals(COMPUTER_FORMAT_STRING))) {
            return getCTemplateNumberFormatWithPre2331IcIBug();
        } else {
            return JavaTemplateNumberFormatFactory.INSTANCE.get(formatString, locale, this);
        }
    }

    /**
     * Returns the {@link NumberFormat} used for the <tt>c</tt> built-in, except, if
     * {@linkplain Configuration#setIncompatibleImprovements(Version) Incompatible Improvements} is less than 2.3.31,
     * this will wrongly give the format that the <tt>c</tt> built-in used before Incompatible Improvements 2.3.21.
     * See more at {@link Configuration#Configuration(Version)}.
     *
     * @deprecated Use {@link #getCTemplateNumberFormat()} instead. This method can't return the format used when
     * {@linkplain Configuration#setIncompatibleImprovements(Version) Incompatible Improvements} is 2.3.32,
     * or greater, and instead it will fall back to return the format that was used for 2.3.31. Also, as its described
     * earlier, this method was inconsistent with {@code ?c} between Incompatible Improvements 2.3.21 and 2.3.30, while
     * {@link #getCTemplateNumberFormat()} behaves as {@code ?c} for all Incompatible Improvements value.
     */
    @Deprecated
    public NumberFormat getCNumberFormat() {
        if (cNumberFormat == null) {
            CFormat cFormat = getCFormat();
            if (cFormat == LegacyCFormat.INSTANCE && configuration.getIncompatibleImprovements().intValue() < _VersionInts.V_2_3_31) {
                // Emulate old bug
                cNumberFormat = ((LegacyCFormat) cFormat).getLegacyNumberFormat(_VersionInts.V_2_3_20);
            } else {
                cNumberFormat = cFormat.getLegacyNumberFormat(this);
            }
        }
        return cNumberFormat;
    }

    /**
     * Returns the {@link TemplateNumberFormat} that {@code ?c}/{@code ?cn} uses.
     * Calling this method for many times is fine, as it internally caches the result object.
     * Remember that {@link TemplateNumberFormat}-s aren't thread-safe objects, so the resulting object should only
     * be used in the same thread where this {@link Environment} runs.
     *
     * @since 2.3.32
     */
    public TemplateNumberFormat getCTemplateNumberFormat() {
        if (cTemplateNumberFormat == null) {
            cTemplateNumberFormat = getCFormat().getTemplateNumberFormat(this);
        }
        return cTemplateNumberFormat;
    }

    /**
     * Like {@link #getCTemplateNumberFormat()}, but emulates the same bug as
     * {@link #getCNumberFormat()} if a legacy default {@link CFormat} is used.
     */
    private TemplateNumberFormat getCTemplateNumberFormatWithPre2331IcIBug() {
        if (cTemplateNumberFormatWithPre2331IcIBug == null) {
            CFormat cFormat = getCFormat();
            if (cFormat == LegacyCFormat.INSTANCE && configuration.getIncompatibleImprovements().intValue() < _VersionInts.V_2_3_31) {
                // Emulate old bug
                cTemplateNumberFormatWithPre2331IcIBug = ((LegacyCFormat) cFormat).getTemplateNumberFormat(_VersionInts.V_2_3_20);
            } else {
                cTemplateNumberFormatWithPre2331IcIBug = cFormat.getTemplateNumberFormat(this);
            }
        }
        return cTemplateNumberFormatWithPre2331IcIBug;
    }

    @Override
    public void setCFormat(CFormat cFormat) {
        CFormat prevCFormat = getCFormat();
        super.setCFormat(cFormat);
        if (prevCFormat != cFormat) {
            cTemplateNumberFormat = null;
            cTemplateNumberFormatWithPre2331IcIBug = null;
            cNumberFormat = null;
            if (cachedTemplateNumberFormats != null) {
                cachedTemplateNumberFormats.remove(C_FORMAT_STRING);
                cachedTemplateNumberFormats.remove(COMPUTER_FORMAT_STRING);
            }
            clearCachedTrueAndFalseString();
        }
    }

    @Override
    public void setTimeFormat(String timeFormat) {
        String prevTimeFormat = getTimeFormat();
        super.setTimeFormat(timeFormat);
        if (!timeFormat.equals(prevTimeFormat)) {
            if (cachedTempDateFormatArray != null) {
                for (int i = 0; i < CACHED_TDFS_LENGTH; i += CACHED_TDFS_ZONELESS_INPUT_OFFS) {
                    cachedTempDateFormatArray[i + TemplateDateModel.TIME] = null;
                }
            }
        }
    }

    @Override
    public void setDateFormat(String dateFormat) {
        String prevDateFormat = getDateFormat();
        super.setDateFormat(dateFormat);
        if (!dateFormat.equals(prevDateFormat)) {
            if (cachedTempDateFormatArray != null) {
                for (int i = 0; i < CACHED_TDFS_LENGTH; i += CACHED_TDFS_ZONELESS_INPUT_OFFS) {
                    cachedTempDateFormatArray[i + TemplateDateModel.DATE] = null;
                }
            }
        }
    }

    @Override
    public void setDateTimeFormat(String dateTimeFormat) {
        String prevDateTimeFormat = getDateTimeFormat();
        super.setDateTimeFormat(dateTimeFormat);
        if (!dateTimeFormat.equals(prevDateTimeFormat)) {
            if (cachedTempDateFormatArray != null) {
                for (int i = 0; i < CACHED_TDFS_LENGTH; i += CACHED_TDFS_ZONELESS_INPUT_OFFS) {
                    cachedTempDateFormatArray[i + TemplateDateModel.DATETIME] = null;
                }
            }
        }
    }

    @Override
    public void setBooleanFormat(String booleanFormat) {
        super.setBooleanFormat(booleanFormat);
        clearCachedTrueAndFalseString();
    }

    String formatBoolean(boolean value, boolean fallbackToTrueFalse) throws TemplateException {
        if (value) {
            String s = getTrueStringValue();
            if (s == null) {
                if (fallbackToTrueFalse) {
                    return MiscUtil.C_TRUE;
                } else {
                    throw new _MiscTemplateException(getNullBooleanFormatErrorDescription());
                }
            } else {
                return s;
            }
        } else {
            String s = getFalseStringValue();
            if (s == null) {
                if (fallbackToTrueFalse) {
                    return MiscUtil.C_FALSE;
                } else {
                    throw new _MiscTemplateException(getNullBooleanFormatErrorDescription());
                }
            } else {
                return s;
            }
        }
    }

    private _ErrorDescriptionBuilder getNullBooleanFormatErrorDescription() {
        return new _ErrorDescriptionBuilder(
                "Can't convert boolean to string automatically, because the \"", BOOLEAN_FORMAT_KEY ,"\" setting was ",
                new _DelayedJQuote(getBooleanFormat()),
                (getBooleanFormat().equals(BOOLEAN_FORMAT_LEGACY_DEFAULT)
                        ? ", which is the legacy deprecated default, and we treat it as if no format was set. "
                        + "This is the default configuration; you should provide the format explicitly for each "
                        + "place where you print a boolean."
                        : ".")
        ).tips(
                "Write something like myBool?string('yes', 'no') to specify boolean formatting in place.",
                new Object[]{
                        "If you want \"true\"/\"false\" result as you are generating computer-language output "
                                + "(not for direct human consumption), then use \"?c\", like ${myBool?c}. (If you "
                                + "always generate computer-language output, then it's might be reasonable to set "
                                + "the \"", BOOLEAN_FORMAT_KEY, "\" setting to \"c\" instead.)",
                },
                new Object[] {
                        "If you need the same two values on most places, the programmers can set the \"",
                        BOOLEAN_FORMAT_KEY ,"\" setting to something like \"yes,no\". However, then it will be easy to "
                        + "unwillingly format booleans like that."
                }
        );
    }

    /**
     * Returns the string to which {@code true} is converted to for human audience, or {@code null} if automatic
     * coercion to string is not allowed.
     *
     * <p>This value is deduced from the {@code "boolean_format"} setting.
     * Confusingly, for backward compatibility (at least until 2.4) that defaults to {@code "true,false"}, yet this
     * defaults to {@code null}. That's so because {@code "true,false"} is treated exceptionally, as that default is a
     * historical mistake in FreeMarker, since it targets computer language output, not human writing. Thus it's
     * ignored, and instead we admit that we don't know how to show boolean values.
     */
    String getTrueStringValue() {
        if (trueAndFalseStringsCachedForParent == getParent()) {
            return cachedTrueString;
        }
        cacheTrueAndFalseStrings();
        return cachedTrueString;
    }

    /**
     * Same as {@link #getTrueStringValue()} but with {@code false}.
     */
    String getFalseStringValue() {
        if (trueAndFalseStringsCachedForParent == getParent()) {
            return cachedFalseString;
        }
        cacheTrueAndFalseStrings();
        return cachedFalseString;
    }

    private void clearCachedTrueAndFalseString() {
        trueAndFalseStringsCachedForParent = null;
        cachedTrueString = null;
        cachedFalseString = null;
    }

    private void cacheTrueAndFalseStrings() {
        String[] parsedBooleanFormat = parseBooleanFormat(getBooleanFormat());
        if (parsedBooleanFormat != null) {
            if (parsedBooleanFormat.length == 0) {
                CFormat cFormat = getCFormat();
                cachedTrueString = cFormat.getTrueString();
                cachedFalseString = cFormat.getFalseString();
            } else {
                cachedTrueString = parsedBooleanFormat[0];
                cachedFalseString = parsedBooleanFormat[1];
            }
        } else {
            // This happens for BOOLEAN_FORMAT_LEGACY_DEFAULT deliberately. That's the default for BC, but it's not a
            // good default for human audience formatting, so we pretend that it wasn't set.
            cachedTrueString = null;
            cachedFalseString = null;
        }
        trueAndFalseStringsCachedForParent = getParent();
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    TemplateModel getLastReturnValue() {
        return lastReturnValue;
    }

    void setLastReturnValue(TemplateModel lastReturnValue) {
        this.lastReturnValue = lastReturnValue;
    }

    void clearLastReturnValue() {
        this.lastReturnValue = null;
    }

    /**
     * @param tdmSourceExpr
     *            The blamed expression if an error occurs; only used for error messages.
     */
    String formatDateToPlainText(TemplateDateModel tdm, Expression tdmSourceExpr,
            boolean useTempModelExc) throws TemplateException {
        TemplateDateFormat format = getTemplateDateFormat(tdm, tdmSourceExpr, useTempModelExc);
        
        try {
            return EvalUtil.assertFormatResultNotNull(format.formatToPlainText(tdm));
        } catch (TemplateValueFormatException e) {
            throw _MessageUtil.newCantFormatDateException(format, tdmSourceExpr, e, useTempModelExc);
        }
    }

    /**
     * @param blamedDateSourceExp
     *            The blamed expression if an error occurs; only used for error messages.
     * @param blamedFormatterExp
     *            The blamed expression if an error occurs; only used for error messages.
     */
    String formatDateToPlainText(TemplateDateModel tdm, String formatString,
            Expression blamedDateSourceExp, Expression blamedFormatterExp,
            boolean useTempModelExc) throws TemplateException {
        Date date = EvalUtil.modelToDate(tdm, blamedDateSourceExp);
        
        TemplateDateFormat format = getTemplateDateFormat(
                formatString, tdm.getDateType(), date.getClass(),
                blamedDateSourceExp, blamedFormatterExp,
                useTempModelExc);
        
        try {
            return EvalUtil.assertFormatResultNotNull(format.formatToPlainText(tdm));
        } catch (TemplateValueFormatException e) {
            throw _MessageUtil.newCantFormatDateException(format, blamedDateSourceExp, e, useTempModelExc);
        }
    }

    /**
     * Gets a {@link TemplateDateFormat} using the date/time/datetime format settings and the current locale and time
     * zone. (The current locale is the locale returned by {@link #getLocale()}. The current time zone is
     * {@link #getTimeZone()} or {@link #getSQLDateAndTimeTimeZone()}).
     * 
     * @param dateType
     *            The FTL date type; see the similar parameter of
     *            {@link TemplateDateFormatFactory#get(String, int, Locale, TimeZone, boolean, Environment)}
     * @param dateClass
     *            The exact {@link Date} class, like {@link java.sql.Date} or {@link java.sql.Time}; this can influences
     *            time zone selection. See also: {@link #setSQLDateAndTimeTimeZone(TimeZone)}
     */
    public TemplateDateFormat getTemplateDateFormat(int dateType, Class<? extends Date> dateClass)
            throws TemplateValueFormatException {
        boolean isSQLDateOrTime = isSQLDateOrTimeClass(dateClass);
        return getTemplateDateFormat(dateType, shouldUseSQLDTTimeZone(isSQLDateOrTime), isSQLDateOrTime);
    }
    
    /**
     * Gets a {@link TemplateDateFormat} for the specified format string and the current locale and time zone. (The
     * current locale is the locale returned by {@link #getLocale()}. The current time zone is {@link #getTimeZone()} or
     * {@link #getSQLDateAndTimeTimeZone()}).
     * 
     * <p>
     * Note on performance: The result will be cached in the {@link Environment} instance. However, at least in 2.3.24
     * the cached entries that depend on the current locale or the current time zone or the current date/time/datetime
     * format of the {@link Environment} will be lost when those settings are changed.
     * 
     * @param formatString
     *            Like {@code "iso m"} or {@code "dd.MM.yyyy HH:mm"} or {@code "@somethingCustom"} or
     *            {@code "@somethingCustom params"}
     * 
     * @since 2.3.24
     */
    public TemplateDateFormat getTemplateDateFormat(
            String formatString, int dateType, Class<? extends Date> dateClass)
                    throws TemplateValueFormatException {
        boolean isSQLDateOrTime = isSQLDateOrTimeClass(dateClass);
        return getTemplateDateFormat(
                formatString, dateType,
                shouldUseSQLDTTimeZone(isSQLDateOrTime), isSQLDateOrTime, true);
    }

    /**
     * Like {@link #getTemplateDateFormat(String, int, Class)}, but allows you to use a different locale than the
     * current one. If you want to use the current locale, use {@link #getTemplateDateFormat(String, int, Class)}
     * instead.
     * 
     * <p>
     * Performance notes regarding the locale and time zone parameters of
     * {@link #getTemplateDateFormat(String, int, Locale, TimeZone, boolean)} apply.
     * 
     * @param locale
     *            Can't be {@code null}; See the similar parameter of
     *            {@link TemplateDateFormatFactory#get(String, int, Locale, TimeZone, boolean, Environment)}
     * 
     * @see #getTemplateDateFormat(String, int, Class)
     * 
     * @since 2.4
     */
    public TemplateDateFormat getTemplateDateFormat(
            String formatString,
            int dateType, Class<? extends Date> dateClass,
            Locale locale)
                    throws TemplateValueFormatException {
        boolean isSQLDateOrTime = isSQLDateOrTimeClass(dateClass);
        boolean useSQLDTTZ = shouldUseSQLDTTimeZone(isSQLDateOrTime);
        return getTemplateDateFormat(
                formatString,
                dateType, locale, useSQLDTTZ ? getSQLDateAndTimeTimeZone() : getTimeZone(), isSQLDateOrTime);        
    }

    /**
     * Like {@link #getTemplateDateFormat(String, int, Class)}, but allows you to use a different locale and time zone
     * than the current one. If you want to use the current locale and time zone, use
     * {@link #getTemplateDateFormat(String, int, Class)} instead.
     * 
     * <p>
     * Performance notes regarding the locale and time zone parameters of
     * {@link #getTemplateDateFormat(String, int, Locale, TimeZone, boolean)} apply.
     * 
     * @param timeZone
     *            The {@link TimeZone} used if {@code dateClass} is not an SQL date-only or time-only type. Can't be
     *            {@code null}.
     * @param sqlDateAndTimeTimeZone
     *            The {@link TimeZone} used if {@code dateClass} is an SQL date-only or time-only type. Can't be
     *            {@code null}.
     * 
     * @see #getTemplateDateFormat(String, int, Class)
     * 
     * @since 2.4
     */
    public TemplateDateFormat getTemplateDateFormat(
            String formatString,
            int dateType, Class<? extends Date> dateClass,
            Locale locale, TimeZone timeZone, TimeZone sqlDateAndTimeTimeZone)
                    throws TemplateValueFormatException {
        boolean isSQLDateOrTime = isSQLDateOrTimeClass(dateClass);
        boolean useSQLDTTZ = shouldUseSQLDTTimeZone(isSQLDateOrTime);
        return getTemplateDateFormat(
                formatString,
                dateType, locale, useSQLDTTZ ? sqlDateAndTimeTimeZone : timeZone, isSQLDateOrTime);        
    }
    
    /**
     * Gets a {@link TemplateDateFormat} for the specified parameters. This is mostly meant to be used by
     * {@link TemplateDateFormatFactory} implementations to delegate to a format based on a specific format string. It
     * works well for that, as its parameters are the same low level values as the parameters of
     * {@link TemplateDateFormatFactory#get(String, int, Locale, TimeZone, boolean, Environment)}. For other tasks
     * consider the other overloads of this method.
     * 
     * <p>
     * Note on performance (which was true at least for 2.3.24): Unless the locale happens to be equal to the current
     * locale and the time zone with one of the current time zones ({@link #getTimeZone()} or
     * {@link #getSQLDateAndTimeTimeZone()}), the {@link Environment}-level format cache can't be used, so the format
     * string has to be parsed and the matching factory has to be get an invoked, which is much more expensive than
     * getting the format from the cache. Thus the returned format should be stored by the caller for later reuse (but
     * only within the current thread and in relation to the current {@link Environment}), if it will be needed
     * frequently.
     * 
     * @param formatString
     *            Like {@code "iso m"} or {@code "dd.MM.yyyy HH:mm"} or {@code "@somethingCustom"} or
     *            {@code "@somethingCustom params"}
     * @param dateType
     *            The FTL date type; see the similar parameter of
     *            {@link TemplateDateFormatFactory#get(String, int, Locale, TimeZone, boolean, Environment)}
     * @param timeZone
     *            Not {@code null}; See the similar parameter of
     *            {@link TemplateDateFormatFactory#get(String, int, Locale, TimeZone, boolean, Environment)}
     * @param locale
     *            Not {@code null}; See the similar parameter of
     *            {@link TemplateDateFormatFactory#get(String, int, Locale, TimeZone, boolean, Environment)}
     * @param zonelessInput
     *            See the similar parameter of
     *            {@link TemplateDateFormatFactory#get(String, int, Locale, TimeZone, boolean, Environment)}
     * 
     * @since 2.3.24
     */
    public TemplateDateFormat getTemplateDateFormat(
            String formatString,
            int dateType, Locale locale, TimeZone timeZone, boolean zonelessInput)
                    throws TemplateValueFormatException {
        Locale currentLocale = getLocale();
        if (locale.equals(currentLocale)) {
            int equalCurrentTZ;
            TimeZone currentTimeZone = getTimeZone();
            if (timeZone.equals(currentTimeZone)) {
                equalCurrentTZ = 1;
            } else {
                TimeZone currentSQLDTTimeZone = getSQLDateAndTimeTimeZone();
                if (timeZone.equals(currentSQLDTTimeZone)) {
                    equalCurrentTZ = 2;
                } else {
                    equalCurrentTZ = 0;
                }
            }
            if (equalCurrentTZ != 0) {
                return getTemplateDateFormat(formatString, dateType, equalCurrentTZ == 2, zonelessInput, true);
            }
            // Falls through
        }
        return getTemplateDateFormatWithoutCache(formatString, dateType, locale, timeZone, zonelessInput);
    }
    
    TemplateDateFormat getTemplateDateFormat(TemplateDateModel tdm, Expression tdmSourceExpr, boolean useTempModelExc)
            throws TemplateModelException, TemplateException {
        Date date = EvalUtil.modelToDate(tdm, tdmSourceExpr);
        
        TemplateDateFormat format = getTemplateDateFormat(
                tdm.getDateType(), date.getClass(), tdmSourceExpr,
                useTempModelExc);
        return format;
    }

    /**
     * Same as {@link #getTemplateDateFormat(int, Class)}, but translates the exceptions to {@link TemplateException}-s.
     */
    TemplateDateFormat getTemplateDateFormat(
            int dateType, Class<? extends Date> dateClass, Expression blamedDateSourceExp, boolean useTempModelExc)
                    throws TemplateException {
        try {
            return getTemplateDateFormat(dateType, dateClass);
        } catch (UnknownDateTypeFormattingUnsupportedException e) {
            throw _MessageUtil.newCantFormatUnknownTypeDateException(blamedDateSourceExp, e);
        } catch (TemplateValueFormatException e) {
            String settingName;
            String settingValue;
            switch (dateType) {
            case TemplateDateModel.TIME:
                settingName = Configurable.TIME_FORMAT_KEY;
                settingValue = getTimeFormat();
                break;
            case TemplateDateModel.DATE:
                settingName = Configurable.DATE_FORMAT_KEY;
                settingValue = getDateFormat();
                break;
            case TemplateDateModel.DATETIME:
                settingName = Configurable.DATETIME_FORMAT_KEY;
                settingValue = getDateTimeFormat();
                break;
            default:
                settingName = "???";
                settingValue = "???";
            }
            
            _ErrorDescriptionBuilder desc = new _ErrorDescriptionBuilder(
                    "The value of the \"", settingName,
                    "\" FreeMarker configuration setting is a malformed date/time/datetime format string: ",
                    new _DelayedJQuote(settingValue), ". Reason given: ",
                    e.getMessage());                    
            throw useTempModelExc ? new _TemplateModelException(e, desc) : new _MiscTemplateException(e, desc);
        }
    }

    /**
     * Same as {@link #getTemplateDateFormat(String, int, Class)}, but translates the exceptions to
     * {@link TemplateException}-s.
     */
    TemplateDateFormat getTemplateDateFormat(
            String formatString, int dateType, Class<? extends Date> dateClass,
            Expression blamedDateSourceExp, Expression blamedFormatterExp,
            boolean useTempModelExc)
                    throws TemplateException {
        try {
            return getTemplateDateFormat(formatString, dateType, dateClass);
        } catch (UnknownDateTypeFormattingUnsupportedException e) {
            throw _MessageUtil.newCantFormatUnknownTypeDateException(blamedDateSourceExp, e);
        } catch (TemplateValueFormatException e) {
            _ErrorDescriptionBuilder desc = new _ErrorDescriptionBuilder(
                    "Can't create date/time/datetime format based on format string ",
                    new _DelayedJQuote(formatString), ". Reason given: ",
                    e.getMessage())
                    .blame(blamedFormatterExp);
            throw useTempModelExc ? new _TemplateModelException(e, desc) : new _MiscTemplateException(e, desc);
        }
    }

    /**
     * Used to get the {@link TemplateDateFormat} according the date/time/datetime format settings, for the current
     * locale and time zone. See {@link #getTemplateDateFormat(String, int, Locale, TimeZone, boolean)} for the meaning
     * of some of the parameters.
     */
    private TemplateDateFormat getTemplateDateFormat(int dateType, boolean useSQLDTTZ, boolean zonelessInput)
            throws TemplateValueFormatException {
        if (dateType == TemplateDateModel.UNKNOWN) {
            throw new UnknownDateTypeFormattingUnsupportedException();
        }
        int cacheIdx = getTemplateDateFormatCacheArrayIndex(dateType, zonelessInput, useSQLDTTZ);
        TemplateDateFormat[] cachedTemplateDateFormats = this.cachedTempDateFormatArray;
        if (cachedTemplateDateFormats == null) {
            cachedTemplateDateFormats = new TemplateDateFormat[CACHED_TDFS_LENGTH];
            this.cachedTempDateFormatArray = cachedTemplateDateFormats;
        }
        TemplateDateFormat format = cachedTemplateDateFormats[cacheIdx];
        if (format == null) {
            final String formatString;
            switch (dateType) {
            case TemplateDateModel.TIME:
                formatString = getTimeFormat();
                break;
            case TemplateDateModel.DATE:
                formatString = getDateFormat();
                break;
            case TemplateDateModel.DATETIME:
                formatString = getDateTimeFormat();
                break;
            default:
                throw new IllegalArgumentException("Invalid date type enum: " + Integer.valueOf(dateType));
            }

            format = getTemplateDateFormat(formatString, dateType, useSQLDTTZ, zonelessInput, false);
            
            cachedTemplateDateFormats[cacheIdx] = format;
        }
        return format;
    }

    /**
     * Used to get the {@link TemplateDateFormat} for the specified parameters, using the {@link Environment}-level
     * cache. As the {@link Environment}-level cache currently only stores formats for the current locale and time zone,
     * there's no parameter to specify those.
     * 
     * @param cacheResult
     *            If the results should stored in the {@link Environment}-level cache. It will still try to get the
     *            result from the cache regardless of this parameter.
     */
    private TemplateDateFormat getTemplateDateFormat(
            String formatString, int dateType, boolean useSQLDTTimeZone, boolean zonelessInput,
            boolean cacheResult)
                    throws TemplateValueFormatException {
        HashMap<String, TemplateDateFormat> cachedFormatsByFormatString;
        readFromCache: do {
            HashMap<String, TemplateDateFormat>[] cachedTempDateFormatsByFmtStrArray = this.cachedTempDateFormatsByFmtStrArray;
            if (cachedTempDateFormatsByFmtStrArray == null) {
                if (cacheResult) {
                    cachedTempDateFormatsByFmtStrArray = new HashMap[CACHED_TDFS_LENGTH];
                    this.cachedTempDateFormatsByFmtStrArray = cachedTempDateFormatsByFmtStrArray;
                } else {
                    cachedFormatsByFormatString = null;
                    break readFromCache;
                }
            }

            TemplateDateFormat format;
            {
                int cacheArrIdx = getTemplateDateFormatCacheArrayIndex(dateType, zonelessInput, useSQLDTTimeZone);
                cachedFormatsByFormatString = cachedTempDateFormatsByFmtStrArray[cacheArrIdx];
                if (cachedFormatsByFormatString == null) {
                    if (cacheResult) {
                        cachedFormatsByFormatString = new HashMap<>(4);
                        cachedTempDateFormatsByFmtStrArray[cacheArrIdx] = cachedFormatsByFormatString;
                        format = null;
                    } else {
                        break readFromCache;
                    }
                } else {
                    format = cachedFormatsByFormatString.get(formatString);
                }
            }

            if (format != null) {
                return format;
            }
            // Cache miss; falls through
        } while (false);

        TemplateDateFormat format = getTemplateDateFormatWithoutCache(
                formatString,
                dateType, getLocale(), useSQLDTTimeZone ? getSQLDateAndTimeTimeZone() : getTimeZone(),
                zonelessInput);
        if (cacheResult) {
            // We know here that cachedFormatsByFormatString != null
            cachedFormatsByFormatString.put(formatString, format);
        }
        return format;
    }

    /**
     * Returns the {@link TemplateDateFormat} for the given parameters without using the {@link Environment}-level
     * cache. Of course, the {@link TemplateDateFormatFactory} involved might still uses its own cache, which can be
     * global (class-loader-level) or {@link Environment}-level.
     * 
     * @param formatString
     *            See the similar parameter of {@link TemplateDateFormatFactory#get}
     * @param dateType
     *            See the similar parameter of {@link TemplateDateFormatFactory#get}
     * @param zonelessInput
     *            See the similar parameter of {@link TemplateDateFormatFactory#get}
     */
    private TemplateDateFormat getTemplateDateFormatWithoutCache(
            String formatString, int dateType, Locale locale, TimeZone timeZone, boolean zonelessInput)
                    throws TemplateValueFormatException {
        final int formatStringLen = formatString.length();
        final String formatParams;

        TemplateDateFormatFactory formatFactory;
        char firstChar = formatStringLen != 0 ? formatString.charAt(0) : 0;

        // As of Java 8, 'x' and 'i' (lower case) are illegal date format letters, so this is backward-compatible.
        if (firstChar == 'x'
                && formatStringLen > 1
                && formatString.charAt(1) == 's') {
            formatFactory = XSTemplateDateFormatFactory.INSTANCE;
            formatParams = formatString; // for speed, we don't remove the prefix
        } else if (firstChar == 'i'
                && formatStringLen > 2
                && formatString.charAt(1) == 's'
                && formatString.charAt(2) == 'o') {
            formatFactory = ISOTemplateDateFormatFactory.INSTANCE;
            formatParams = formatString; // for speed, we don't remove the prefix
        } else if (firstChar == '@'
                && formatStringLen > 1
                && (isIcI2324OrLater() || hasCustomFormats())
                && Character.isLetter(formatString.charAt(1))) {
            final String name;
            {
                int endIdx;
                findParamsStart: for (endIdx = 1; endIdx < formatStringLen; endIdx++) {
                    char c = formatString.charAt(endIdx);
                    if (c == ' ' || c == '_') {
                        break findParamsStart;
                    }
                }
                name = formatString.substring(1, endIdx);
                formatParams = endIdx < formatStringLen ? formatString.substring(endIdx + 1) : "";
            }

            formatFactory = getCustomDateFormat(name);
            if (formatFactory == null) {
                throw new UndefinedCustomFormatException(
                        "No custom date format was defined with name " + StringUtil.jQuote(name));
            }
        } else {
            formatParams = formatString;
            formatFactory = JavaTemplateDateFormatFactory.INSTANCE;
        }

        return formatFactory.get(formatParams, dateType, locale, timeZone,
                zonelessInput, this);
    }

    boolean shouldUseSQLDTTZ(Class dateClass) {
        // Attention! If you update this method, update all overloads of it!
        return dateClass != Date.class // This pre-condition is only for speed
                && !isSQLDateAndTimeTimeZoneSameAsNormal()
                && isSQLDateOrTimeClass(dateClass);
    }

    private boolean shouldUseSQLDTTimeZone(boolean sqlDateOrTime) {
        // Attention! If you update this method, update all overloads of it!
        return sqlDateOrTime && !isSQLDateAndTimeTimeZoneSameAsNormal();
    }

    /**
     * Tells if the given class is or is subclass of {@link java.sql.Date} or {@link java.sql.Time}.
     */
    private static boolean isSQLDateOrTimeClass(Class dateClass) {
        // We do shortcuts for the most common cases.
        return dateClass != java.util.Date.class
                && (dateClass == java.sql.Date.class || dateClass == Time.class
                        || (dateClass != Timestamp.class
                                && (java.sql.Date.class.isAssignableFrom(dateClass)
                                        || Time.class.isAssignableFrom(dateClass))));
    }

    private int getTemplateDateFormatCacheArrayIndex(int dateType, boolean zonelessInput, boolean sqlDTTZ) {
        return dateType
                + (zonelessInput ? CACHED_TDFS_ZONELESS_INPUT_OFFS : 0)
                + (sqlDTTZ ? CACHED_TDFS_SQL_D_T_TZ_OFFS : 0);
    }

    /**
     * Returns the {@link DateToISO8601CalendarFactory} used by the the "iso_" built-ins. Be careful when using this; it
     * should only by used with
     * {@link DateUtil#dateToISO8601String(Date, boolean, boolean, boolean, int, TimeZone, DateToISO8601CalendarFactory)}
     * and {@link DateUtil#dateToXSString(Date, boolean, boolean, boolean, int, TimeZone, DateToISO8601CalendarFactory)}
     * .
     */
    DateToISO8601CalendarFactory getISOBuiltInCalendarFactory() {
        if (isoBuiltInCalendarFactory == null) {
            isoBuiltInCalendarFactory = new DateUtil.TrivialDateToISO8601CalendarFactory();
        }
        return isoBuiltInCalendarFactory;
    }

    TemplateTransformModel getTransform(Expression exp) throws TemplateException {
        TemplateTransformModel ttm = null;
        TemplateModel tm = exp.eval(this);
        if (tm instanceof TemplateTransformModel) {
            ttm = (TemplateTransformModel) tm;
        } else if (exp instanceof Identifier) {
            tm = configuration.getSharedVariable(exp.toString());
            if (tm instanceof TemplateTransformModel) {
                ttm = (TemplateTransformModel) tm;
            }
        }
        return ttm;
    }

    /**
     * Returns the loop or macro local variable corresponding to this variable name.
     * Returns {@code null} if no such variable exists with the given name, or the variable was set to
     * {@code null}. Doesn't read namespace or global variables.
     */
    public TemplateModel getLocalVariable(String name) throws TemplateModelException {
        TemplateModel val = getNullableLocalVariable(name);
        return val != TemplateNullModel.INSTANCE ? val : null;
    }

    /**
     * Similar to {@link #getLocalVariable(String)}, but might return {@link TemplateNullModel}. Only used internally,
     * as {@link TemplateNullModel} is internal.
     *
     * @since 2.3.29
     */
    private final TemplateModel getNullableLocalVariable(String name) throws TemplateModelException {
        if (localContextStack != null) {
            for (int i = localContextStack.size() - 1; i >= 0; i--) {
                LocalContext lc = localContextStack.get(i);
                TemplateModel tm = lc.getLocalVariable(name);
                if (tm != null) {
                    return tm;
                }
            }
        }
        return currentMacroContext == null ? null : currentMacroContext.getLocalVariable(name);
    }

    /**
     * Returns the variable that is visible in this context, or {@code null} if the variable is not found. This is the
     * correspondent to an FTL top-level variable reading expression. That is, it tries to find the the variable in this
     * order:
     * <ol>
     * <li>An loop variable (if we're in a loop or user defined directive body) such as foo_has_next
     * <li>A local variable (if we're in a macro)
     * <li>A variable defined in the current namespace (say, via &lt;#assign ...&gt;)
     * <li>A variable defined globally (say, via &lt;#global ....&gt;)
     * <li>Variable in the data model:
     * <ol>
     * <li>A variable in the root hash that was exposed to this rendering environment in the Template.process(...) call
     * <li>A shared variable set in the configuration via a call to Configuration.setSharedVariable(...)
     * </ol>
     * </li>
     * </ol>
     */
    public TemplateModel getVariable(String name) throws TemplateModelException {
        TemplateModel result = getNullableLocalVariable(name);
        if (result != null) {
            return result != TemplateNullModel.INSTANCE ? result : null;
        }

        result = currentNamespace.get(name);
        if (result != null) {
            return result;

        }
        return getGlobalVariable(name);
    }

    /**
     * Returns the globally visible variable of the given name, or {@code null}. This corresponds to FTL
     * <code>.globals.<i>name</i></code>. This will first look at variables that were assigned globally via:
     * {@code <#global ...>} and then at the data model exposed to the template, and then at the
     * {@linkplain Configuration#setSharedVariables(Map)} shared variables} in the {@link Configuration}.
     */
    public TemplateModel getGlobalVariable(String name) throws TemplateModelException {
        TemplateModel result = globalNamespace.get(name);
        if (result != null) {
            return result;
        }

        return getDataModelOrSharedVariable(name);
    }

    /**
     * Returns the variable from the data-model, or if it's not there, then from the
     * {@linkplain Configuration#setSharedVariables(Map)} shared variables}
     *
     * @since 2.3.30
     */
    public TemplateModel getDataModelOrSharedVariable(String name) throws TemplateModelException {
        TemplateModel dataModelVal = rootDataModel.get(name);
        if (dataModelVal != null) {
            return dataModelVal;
        }

        return configuration.getSharedVariable(name);
    }

    /**
     * Sets a variable in the global namespace, like {@code <#global name=value>}.
     * This can be considered a convenient shorthand for {@code getGlobalNamespace().put(name, model)}.
     *
     * <p>Note that this is not an exact pair of {@link #getGlobalVariable(String)}, as that falls back to higher scopes
     * if the variable is not in the global namespace.
     *
     * @param name
     *            The name of the variable.
     * @param value
     *            The new value of the variable. {@code null} in effect removes the local variable (reading it will fall
     *            back to higher scope).
     */
    public void setGlobalVariable(String name, TemplateModel value) {
        globalNamespace.put(name, value);
    }

    /**
     * Sets a variable in the current namespace, like {@code <#assign name=value>}.
     * This can be considered a convenient shorthand for: {@code getCurrentNamespace().put(name, model)}.
     *
     * @param name
     *            The name of the variable.
     * @param value
     *            The new value of the variable. {@code null} in effect removes the local variable (reading it will fall
     *            back to higher scope).
     */
    public void setVariable(String name, TemplateModel value) {
        currentNamespace.put(name, value);
    }

    /**
     * Sets a local variable that's on the top-level inside a macro or function invocation, like
     * {@code <#local name=value>}.
     * Note that just like {@code <#local name=value>}, this will not set loop variables; it will totally ignore
     * them, and might sets a local variable that a loop variable currently "shadows". As such, it's not exactly the
     * pair of {@link #getLocalVariable(String)}, which also reads loop variables.
     *
     * @param name
     *            The name of the variable.
     * @param value
     *            The new value of the variable. {@code null} in effect removes the local variable (reading it will fall
     *            back to higher scope).
     * @throws IllegalStateException
     *             if the environment is not executing a macro body.
     */
    public void setLocalVariable(String name, TemplateModel value) {
        if (currentMacroContext == null) {
            throw new IllegalStateException("Not executing macro body");
        }
        currentMacroContext.setLocalVar(name, value);
    }

    /**
     * Returns a set of variable names that are known at the time of call. This includes names of all shared variables
     * in the {@link Configuration}, names of all global variables that were assigned during the template processing,
     * names of all variables in the current name-space, names of all local variables and loop variables. If the passed
     * root data model implements the {@link TemplateHashModelEx} interface, then all names it retrieves through a call
     * to {@link TemplateHashModelEx#keys()} method are returned as well. The method returns a new Set object on each
     * call that is completely disconnected from the Environment. That is, modifying the set will have no effect on the
     * Environment object.
     */
    public Set getKnownVariableNames() throws TemplateModelException {
        // shared vars.
        Set set = configuration.getSharedVariableNames();

        // root hash
        if (rootDataModel instanceof TemplateHashModelEx) {
            TemplateModelIterator rootNames = ((TemplateHashModelEx) rootDataModel).keys().iterator();
            while (rootNames.hasNext()) {
                set.add(((TemplateScalarModel) rootNames.next()).getAsString());
            }
        }

        // globals
        for (TemplateModelIterator tmi = globalNamespace.keys().iterator(); tmi.hasNext();) {
            set.add(((TemplateScalarModel) tmi.next()).getAsString());
        }

        // current name-space
        for (TemplateModelIterator tmi = currentNamespace.keys().iterator(); tmi.hasNext();) {
            set.add(((TemplateScalarModel) tmi.next()).getAsString());
        }

        // locals and loop vars
        if (currentMacroContext != null) {
            set.addAll(currentMacroContext.getLocalVariableNames());
        }
        if (localContextStack != null) {
            for (int i = localContextStack.size() - 1; i >= 0; i--) {
                LocalContext lc = localContextStack.get(i);
                set.addAll(lc.getLocalVariableNames());
            }
        }
        return set;
    }

    /**
     * Prints the current FTL stack trace. Useful for debugging. {@link TemplateException}s incorporate this information
     * in their stack traces.
     */
    public void outputInstructionStack(PrintWriter pw) {
        outputInstructionStack(getInstructionStackSnapshot(), false, pw);
        pw.flush();
    }

    private static final int TERSE_MODE_INSTRUCTION_STACK_TRACE_LIMIT = 10;

    /**
     * Prints an FTL stack trace based on a stack trace snapshot.
     * 
     * @param w
     *            If it's a {@link PrintWriter}, {@link PrintWriter#println()} will be used for line-breaks.
     * @see #getInstructionStackSnapshot()
     * @since 2.3.21
     */
    static void outputInstructionStack(
            TemplateElement[] instructionStackSnapshot, boolean terseMode, Writer w) {
        final PrintWriter pw = (PrintWriter) (w instanceof PrintWriter ? w : null);
        try {
            if (instructionStackSnapshot != null) {
                final int totalFrames = instructionStackSnapshot.length;
                int framesToPrint = terseMode
                        ? (totalFrames <= TERSE_MODE_INSTRUCTION_STACK_TRACE_LIMIT
                                ? totalFrames
                                : TERSE_MODE_INSTRUCTION_STACK_TRACE_LIMIT - 1)
                        : totalFrames;
                boolean hideNestringRelatedFrames = terseMode && framesToPrint < totalFrames;
                int nestingRelatedFramesHidden = 0;
                int trailingFramesHidden = 0;
                int framesPrinted = 0;
                for (int frameIdx = 0; frameIdx < totalFrames; frameIdx++) {
                    TemplateElement stackEl = instructionStackSnapshot[frameIdx];
                    final boolean nestingRelatedElement = (frameIdx > 0 && stackEl instanceof BodyInstruction)
                            || (frameIdx > 1 && instructionStackSnapshot[frameIdx - 1] instanceof BodyInstruction);
                    if (framesPrinted < framesToPrint) {
                        if (!nestingRelatedElement || !hideNestringRelatedFrames) {
                            w.write(frameIdx == 0
                                    ? "\t- Failed at: "
                                    : (nestingRelatedElement
                                            ? "\t~ Reached through: "
                                            : "\t- Reached through: "));
                            w.write(instructionStackItemToString(stackEl));
                            if (pw != null) pw.println();
                            else
                                w.write('\n');
                            framesPrinted++;
                        } else {
                            nestingRelatedFramesHidden++;
                        }
                    } else {
                        trailingFramesHidden++;
                    }
                }

                boolean hadClosingNotes = false;
                if (trailingFramesHidden > 0) {
                    w.write("\t... (Had ");
                    w.write(String.valueOf(trailingFramesHidden + nestingRelatedFramesHidden));
                    w.write(" more, hidden for tersenes)");
                    hadClosingNotes = true;
                }
                if (nestingRelatedFramesHidden > 0) {
                    if (hadClosingNotes) {
                        w.write(' ');
                    } else {
                        w.write('\t');
                    }
                    w.write("(Hidden " + nestingRelatedFramesHidden + " \"~\" lines for terseness)");
                    if (pw != null) pw.println();
                    else
                        w.write('\n');
                    hadClosingNotes = true;
                }
                if (hadClosingNotes) {
                    if (pw != null) pw.println();
                    else
                        w.write('\n');
                }
            } else {
                w.write("(The stack was empty)");
                if (pw != null) pw.println();
                else
                    w.write('\n');
            }
        } catch (IOException e) {
            LOG.error("Failed to print FTL stack trace", e);
        }
    }

    /**
     * Returns the snapshot of what would be printed as FTL stack trace.
     * 
     * @since 2.3.20
     */
    TemplateElement[] getInstructionStackSnapshot() {
        int requiredLength = 0;
        int ln = instructionStackSize;

        for (int i = 0; i < ln; i++) {
            TemplateElement stackEl = instructionStack[i];
            if (i == ln - 1 || stackEl.isShownInStackTrace()) {
                requiredLength++;
            }
        }

        if (requiredLength == 0) return null;

        TemplateElement[] result = new TemplateElement[requiredLength];
        int dstIdx = requiredLength - 1;
        for (int i = 0; i < ln; i++) {
            TemplateElement stackEl = instructionStack[i];
            if (i == ln - 1 || stackEl.isShownInStackTrace()) {
                result[dstIdx--] = stackEl;
            }
        }

        return result;
    }

    static String instructionStackItemToString(TemplateElement stackEl) {
        StringBuilder sb = new StringBuilder();
        appendInstructionStackItem(stackEl, sb);
        return sb.toString();
    }

    static void appendInstructionStackItem(TemplateElement stackEl, StringBuilder sb) {
        sb.append(_MessageUtil.shorten(stackEl.getDescription(), 40));

        sb.append("  [");
        Macro enclosingMacro = getEnclosingMacro(stackEl);
        if (enclosingMacro != null) {
            sb.append(_MessageUtil.formatLocationForEvaluationError(
                    enclosingMacro, stackEl.beginLine, stackEl.beginColumn));
        } else {
            sb.append(_MessageUtil.formatLocationForEvaluationError(
                    stackEl.getTemplate(), stackEl.beginLine, stackEl.beginColumn));
        }
        sb.append("]");
    }

    static private Macro getEnclosingMacro(TemplateElement stackEl) {
        while (stackEl != null) {
            if (stackEl instanceof Macro) return (Macro) stackEl;
            stackEl = stackEl.getParentElement();
        }
        return null;
    }

    private void pushLocalContext(LocalContext localContext) {
        if (localContextStack == null) {
            localContextStack = new LocalContextStack();
        }
        localContextStack.push(localContext);
    }

    LocalContextStack getLocalContextStack() {
        return localContextStack;
    }

    /**
     * Returns the name-space for the name if exists, or null.
     * 
     * @param name
     *            the template path that you have used with the <code>import</code> directive or
     *            {@link #importLib(String, String)} call, in normalized form. That is, the path must be an absolute
     *            path, and it must not contain "/../" or "/./". The leading "/" is optional.
     */
    public Namespace getNamespace(String name) {
        if (name.startsWith("/")) name = name.substring(1);
        if (loadedLibs != null) {
            return loadedLibs.get(name);
        } else {
            return null;
        }
    }

    /**
     * Returns the main namespace. This corresponds to the FTL {@code .main} hash.
     */
    public Namespace getMainNamespace() {
        return mainNamespace;
    }

    /**
     * Returns the current namespace. This corresponds to the FTL {@code .namespace} hash. Initially, the current name
     * space is the main namespace, but when inside an {@code #import}-ed template, it will change to the namespace of
     * that import. Note that {@code #include} doesn't affect the namespace, so if you are in an {@code #import}-ed
     * template and then from there do an {@code #include}, the current namespace will remain the namespace of the
     * {@code #import}.
     */
    public Namespace getCurrentNamespace() {
        return currentNamespace;
    }

    /**
     * Returns the name-space that contains the globally visible non-data-model variables (usually created with
     * {@code &lt;#global ...&gt;}).
     */
    public Namespace getGlobalNamespace() {
        return globalNamespace;
    }

    /**
     * Returns a view of the data-model (also known as the template context in some other template engines) 
     * that falls back to {@linkplain Configuration#setSharedVariable(String, TemplateModel) shared variables}.
     */
    public TemplateHashModel getDataModel() {
        return rootDataModel instanceof TemplateHashModelEx
                ? new TemplateHashModelEx() {
                    @Override
                    public boolean isEmpty() throws TemplateModelException {
                        return false;
                    }

                    @Override
                    public TemplateModel get(String key) throws TemplateModelException {
                        return getDataModelOrSharedVariable(key);
                    }

                    // NB: The methods below do not take into account
                    // configuration shared variables even though
                    // the hash will return them, if only for BWC reasons
                    @Override
                    public TemplateCollectionModel values() throws TemplateModelException {
                        return ((TemplateHashModelEx) rootDataModel).values();
                    }

                    @Override
                    public TemplateCollectionModel keys() throws TemplateModelException {
                        return ((TemplateHashModelEx) rootDataModel).keys();
                    }

                    @Override
                    public int size() throws TemplateModelException {
                        return ((TemplateHashModelEx) rootDataModel).size();
                    }
                }
            : new TemplateHashModel() {
                @Override
                public boolean isEmpty() {
                    return false;
                }

                @Override
                public TemplateModel get(String key) throws TemplateModelException {
                    TemplateModel value = rootDataModel.get(key);
                    return value != null ? value : configuration.getSharedVariable(key);
                }
            };
    }

    /**
     * Returns the read-only hash of globally visible variables. This is the correspondent of FTL <code>.globals</code>
     * hash. That is, you see the variables created with <code>&lt;#global ...&gt;</code>, and the variables of the
     * data-model. To create new global variables, use {@link #setGlobalVariable setGlobalVariable}.
     */
    public TemplateHashModel getGlobalVariables() {
        return new TemplateHashModel() {

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public TemplateModel get(String key) throws TemplateModelException {
                TemplateModel result = globalNamespace.get(key);
                if (result == null) {
                    result = rootDataModel.get(key);
                }
                if (result == null) {
                    result = configuration.getSharedVariable(key);
                }
                return result;
            }
        };
    }

    private void pushElement(TemplateElement element) {
        final int newSize = ++instructionStackSize;
        TemplateElement[] instructionStack = this.instructionStack;
        if (newSize > instructionStack.length) {
            final TemplateElement[] newInstructionStack = new TemplateElement[newSize * 2];
            for (int i = 0; i < instructionStack.length; i++) {
                newInstructionStack[i] = instructionStack[i]; 
            }
            instructionStack = newInstructionStack;
            this.instructionStack = instructionStack;
        }
        instructionStack[newSize - 1] = element;
    }

    private void popElement() {
        instructionStackSize--;
    }

    void replaceElementStackTop(TemplateElement instr) {
        instructionStack[instructionStackSize - 1] = instr;
    }

    public TemplateNodeModel getCurrentVisitorNode() {
        return currentVisitorNode;
    }

    /**
     * sets TemplateNodeModel as the current visitor node. <tt>.current_node</tt>
     */
    public void setCurrentVisitorNode(TemplateNodeModel node) {
        currentVisitorNode = node;
    }

    TemplateModel getNodeProcessor(TemplateNodeModel node) throws TemplateException {
        String nodeName = node.getNodeName();
        if (nodeName == null) {
            throw new _MiscTemplateException(this, "Node name is null.");
        }
        TemplateModel result = getNodeProcessor(nodeName, node.getNodeNamespace(), 0);

        if (result == null) {
            String type = node.getNodeType();

            /* DD: Original version: */
            if (type == null) {
                type = "default";
            }
            result = getNodeProcessor("@" + type, null, 0);

            /*
             * DD: Jonathan's non-BC version and IMHO otherwise wrong version: if (type != null) { result =
             * getNodeProcessor("@" + type, null, 0); } if (result == null) { result = getNodeProcessor("@default",
             * null, 0); }
             */
        }
        return result;
    }

    private TemplateModel getNodeProcessor(final String nodeName, final String nsURI, int startIndex)
            throws TemplateException {
        TemplateModel result = null;
        int i;
        int size = nodeNamespaces.size();
        for (i = startIndex; i < size; i++) {
            Namespace ns = null;
            try {
                ns = (Namespace) nodeNamespaces.get(i);
            } catch (ClassCastException cce) {
                throw new _MiscTemplateException(this,
                        "A \"using\" clause should contain a sequence of namespaces or strings that indicate the "
                                + "location of importable macro libraries.");
            }
            result = getNodeProcessor(ns, nodeName, nsURI);
            if (result != null)
                break;
        }
        if (result != null) {
            this.nodeNamespaceIndex = i + 1;
            this.currentNodeName = nodeName;
            this.currentNodeNS = nsURI;
        }
        return result;
    }

    private TemplateModel getNodeProcessor(Namespace ns, String localName, String nsURI) throws TemplateException {
        TemplateModel result = null;
        if (nsURI == null) {
            result = ns.get(localName);
            if (!(result instanceof Macro) && !(result instanceof TemplateTransformModel)) {
                result = null;
            }
        } else {
            Template template = ns.getTemplate();
            String prefix = template.getPrefixForNamespace(nsURI);
            if (prefix == null) {
                // The other template cannot handle this node
                // since it has no prefix registered for the namespace
                return null;
            }
            if (prefix.length() > 0) {
                result = ns.get(prefix + ":" + localName);
                if (!(result instanceof Macro) && !(result instanceof TemplateTransformModel)) {
                    result = null;
                }
            } else {
                if (nsURI.length() == 0) {
                    result = ns.get(Template.NO_NS_PREFIX + ":" + localName);
                    if (!(result instanceof Macro) && !(result instanceof TemplateTransformModel)) {
                        result = null;
                    }
                }
                if (nsURI.equals(template.getDefaultNS())) {
                    result = ns.get(Template.DEFAULT_NAMESPACE_PREFIX + ":" + localName);
                    if (!(result instanceof Macro) && !(result instanceof TemplateTransformModel)) {
                        result = null;
                    }
                }
                if (result == null) {
                    result = ns.get(localName);
                    if (!(result instanceof Macro) && !(result instanceof TemplateTransformModel)) {
                        result = null;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Emulates <code>include</code> directive, except that <code>name</code> must be template root relative.
     *
     * <p>
     * It's the same as <code>include(getTemplateForInclusion(name, encoding, parse))</code>. But, you may want to
     * separately call these two methods, so you can determine the source of exceptions more precisely, and thus achieve
     * more intelligent error handling.
     *
     * @see #getTemplateForInclusion(String name, String encoding, boolean parse)
     * @see #include(Template includedTemplate)
     */
    public void include(String name, String encoding, boolean parse)
            throws IOException, TemplateException {
        include(getTemplateForInclusion(name, encoding, parse));
    }

    /**
     * Same as {@link #getTemplateForInclusion(String, String, boolean, boolean)} with {@code false}
     * {@code ignoreMissing} argument.
     */
    public Template getTemplateForInclusion(String name, String encoding, boolean parse)
            throws IOException {
        return getTemplateForInclusion(name, encoding, parse, false);
    }

    /**
     * Gets a template for inclusion; used for implementing {@link #include(Template includedTemplate)}. The advantage
     * over simply using <code>config.getTemplate(...)</code> is that it chooses the default encoding exactly as the
     * <code>include</code> directive does, although that encoding selection mechanism is a historical baggage and
     * considered to be harmful.
     *
     * @param name
     *            the name of the template, relatively to the template root directory (not the to the directory of the
     *            currently executing template file). (Note that you can use
     *            {@link freemarker.cache.TemplateCache#getFullTemplatePath} to convert paths to template root relative
     *            paths.) For more details see the identical parameter of
     *            {@link Configuration#getTemplate(String, Locale, String, boolean, boolean)}
     * 
     * @param encoding
     *            the charset of the obtained template. If {@code null}, the encoding of the top template that is
     *            currently being processed in this {@link Environment} is used, which can lead to odd situations, so
     *            using {@code null} is not recommended. In most applications, the value of
     *            {@link Configuration#getEncoding(Locale)} (or {@link Configuration#getDefaultEncoding()}) should be
     *            used here.
     * 
     * @param parseAsFTL
     *            See identical parameter of {@link Configuration#getTemplate(String, Locale, String, boolean, boolean)}
     * 
     * @param ignoreMissing
     *            See identical parameter of {@link Configuration#getTemplate(String, Locale, String, boolean, boolean)}
     * 
     * @return Same as {@link Configuration#getTemplate(String, Locale, String, boolean, boolean)}
     * @throws IOException
     *             Same as exceptions thrown by
     *             {@link Configuration#getTemplate(String, Locale, String, boolean, boolean)}
     * 
     * @since 2.3.21
     */
    public Template getTemplateForInclusion(String name, String encoding, boolean parseAsFTL, boolean ignoreMissing)
            throws IOException {
        return configuration.getTemplate(
                name, getLocale(), getIncludedTemplateCustomLookupCondition(),
                encoding != null ? encoding : getIncludedTemplateEncoding(),
                parseAsFTL,
                ignoreMissing);
    }

    private Object getIncludedTemplateCustomLookupCondition() {
        return getTemplate().getCustomLookupCondition();
    }

    private String getIncludedTemplateEncoding() {
        String encoding;
        // This branch shouldn't exist, as it doesn't make much sense to inherit encoding. But we have to keep BC.
        encoding = getTemplate().getEncoding();
        if (encoding == null) {
            encoding = configuration.getEncoding(this.getLocale());
        }
        return encoding;
    }

    /**
     * Processes a Template in the context of this <code>Environment</code>, including its output in the
     * <code>Environment</code>'s Writer.
     *
     * @param includedTemplate
     *            the template to process. Note that it does <em>not</em> need to be a template returned by
     *            {@link #getTemplateForInclusion(String name, String encoding, boolean parse)}.
     */
    public void include(Template includedTemplate)
            throws TemplateException, IOException {
        final Template prevTemplate;
        final boolean parentReplacementOn = isBeforeIcI2322();
        prevTemplate = getTemplate();
        if (parentReplacementOn) {
            setParent(includedTemplate);
        } else {
            legacyParent = includedTemplate;
        }

        importMacros(includedTemplate);
        try {
            visit(includedTemplate.getRootTreeNode());
        } finally {
            if (parentReplacementOn) {
                setParent(prevTemplate);
            } else {
                legacyParent = prevTemplate;
            }
        }
    }

    /**
     * Emulates <code>import</code> directive, except that <code>templateName</code> must be template root relative.
     *
     * <p>
     * It's the same as <code>importLib(getTemplateForImporting(templateName), namespace)</code>. But, you may want to
     * separately call these two methods, so you can determine the source of exceptions more precisely, and thus achieve
     * more intelligent error handling.
     * 
     * <p>
     * If it will be a lazy or an eager import is decided by the value of {@link #getLazyImports()}. You
     * can also directly control that aspect by using {@link #importLib(String, String, boolean)} instead.
     *
     * @return Not {@code null}. This is possibly a lazily self-initializing namespace, which means that it will only
     *         try to get and process the imported template when you access its content.
     *
     * @see #getTemplateForImporting(String templateName)
     * @see #importLib(Template includedTemplate, String namespaceVarName)
     * @see #importLib(String, String, boolean)
     */
    public Namespace importLib(String templateName, String targetNsVarName)
            throws IOException, TemplateException {
        return importLib(templateName, targetNsVarName, getLazyImports());
    }

    /**
     * Does what the <code>#import</code> directive does, but with an already loaded template.
     *
     * @param loadedTemplate
     *            The template to import. Note that it does <em>not</em> need to be a template returned by
     *            {@link #getTemplateForImporting(String name)}. Not {@code null}.
     * @param targetNsVarName
     *            The name of the FTL variable that will store the namespace. If {@code null}, the namespace
     *            won't be stored in a variable (but it's still returned).
     *            
     * @return The namespace of the imported template, already initialized. 
     *            
     * @see #getTemplateForImporting(String name)
     * @see #importLib(Template includedTemplate, String namespaceVarName)
     */
    public Namespace importLib(Template loadedTemplate, String targetNsVarName)
            throws IOException, TemplateException {
        return importLib(null, loadedTemplate, targetNsVarName);
    }

    /**
     * Like {@link #importLib(String, String)}, but you can specify if you want a
     * {@linkplain #setLazyImports(boolean) lazy import} or not.
     * 
     * @return Not {@code null}. This is possibly a lazily self-initializing namespace, which mean that it will only try
     *         to get and process the imported template when you access its content.
     * 
     * @since 2.3.25
     */
    public Namespace importLib(String templateName, String targetNsVarName, boolean lazy)
            throws IOException, TemplateException {
        return lazy
                ? importLib(templateName, null, targetNsVarName)
                : importLib(null, getTemplateForImporting(templateName), targetNsVarName);
    }
    
    /**
     * Gets a template for importing; used with {@link #importLib(Template importedTemplate, String namespace)}. The
     * advantage over simply using <code>config.getTemplate(...)</code> is that it chooses the encoding as the
     * <code>import</code> directive does.
     *
     * @param name
     *            the name of the template, relatively to the template root directory (not the to the directory of the
     *            currently executing template file!). (Note that you can use
     *            {@link freemarker.cache.TemplateCache#getFullTemplatePath} to convert paths to template root relative
     *            paths.)
     */
    public Template getTemplateForImporting(String name) throws IOException {
        return getTemplateForInclusion(name, null, true);
    }

    /**
     * @param templateName
     *            Ignored if {@code loadedTemaplate} is set (so we do eager import), otherwise it can't be {@code null}.
     *            Assumed to be template root directory relative (not relative to the current template).
     * @param loadedTemplate
     *            {@code null} exactly if we want a lazy import
     */
    private Namespace importLib(
            String templateName, final Template loadedTemplate, final String targetNsVarName)
            throws IOException, TemplateException {
        final boolean lazyImport;
        if (loadedTemplate != null) {
            lazyImport = false;
            // As we have an already normalized name, we use it. 2.3.x note: We should use the template.sourceName as
            // namespace key, but historically we use the looked up name (template.name); check what lazy import does if
            // that will be fixed, as that can't do the template lookup, yet the keys must be the same.
            templateName = loadedTemplate.getName();
        } else {
            lazyImport = true;
            // We can't cause a template lookup here (see TemplateLookupStrategy), as that can be expensive. We exploit
            // that (at least in 2.3.x) the name used for eager import namespace key isn't the template.sourceName, but
            // the looked up name (template.name), which we can get quickly:
            TemplateNameFormat tnf = getConfiguration().getTemplateNameFormat();
            templateName = _CacheAPI.normalizeRootBasedName(tnf, templateName);
        }
        
        if (loadedLibs == null) {
            loadedLibs = new HashMap();
        }
        final Namespace existingNamespace = loadedLibs.get(templateName);
        if (existingNamespace != null) {
            if (targetNsVarName != null) {
                setVariable(targetNsVarName, existingNamespace);
                if (isIcI2324OrLater() && currentNamespace == mainNamespace) {
                    globalNamespace.put(targetNsVarName, existingNamespace);
                }
            }
            if (!lazyImport && existingNamespace instanceof LazilyInitializedNamespace) {
                ((LazilyInitializedNamespace) existingNamespace).ensureInitializedTME();
            }
        } else {
            final Namespace newNamespace
                    = lazyImport ? new LazilyInitializedNamespace(templateName) : new Namespace(loadedTemplate);
            loadedLibs.put(templateName, newNamespace);
            
            if (targetNsVarName != null) {
                setVariable(targetNsVarName, newNamespace);
                if (currentNamespace == mainNamespace) {
                    globalNamespace.put(targetNsVarName, newNamespace);
                }
            }
            
            if (!lazyImport) {
                initializeImportLibNamespace(newNamespace, loadedTemplate);
            }
        }
        return loadedLibs.get(templateName);
    }

    private void initializeImportLibNamespace(final Namespace newNamespace, Template loadedTemplate)
            throws TemplateException, IOException {
        Namespace prevNamespace = this.currentNamespace;
        this.currentNamespace = newNamespace;
        Writer prevOut = out;
        this.out = NullWriter.INSTANCE;
        try {
            include(loadedTemplate);
        } finally {
            this.out = prevOut;
            this.currentNamespace = prevNamespace;
        }
    }

    /**
     * Resolves a reference to a template (like the one used in {@code #include} or {@code #import}), assuming a base
     * name. This gives a root based, even if non-normalized and possibly non-absolute (but then relative to the root)
     * template name, that could be used for {@link Configuration#getTemplate(String)}. This is mostly used when a
     * template refers to another template.
     * <p>
     * If you need to guarantee that the result is also an absolute path, then apply
     * {@link #rootBasedToAbsoluteTemplateName(String)} on it.
     * 
     * @param baseName
     *            The name to which relative {@code targetName}-s are relative to. Maybe {@code null} (happens when
     *            resolving names in nameless templates), which means that the base is the root "directory", and so the
     *            {@code targetName} is returned without change. Assuming {@link TemplateNameFormat#DEFAULT_2_3_0} or
     *            {@link TemplateNameFormat#DEFAULT_2_4_0}, the rules are as follows. If you want to specify a base
     *            directory here, it must end with {@code "/"}. If it doesn't end with {@code "/"}, it's parent
     *            directory will be used as the base path. Might starts with a scheme part (like {@code "foo://"}, or
     *            with {@link TemplateNameFormat#DEFAULT_2_4_0} even just with {@code "foo:"}).
     * @param targetName
     *            The name of the template, which is either a relative or absolute name. Assuming
     *            {@link TemplateNameFormat#DEFAULT_2_3_0} or {@link TemplateNameFormat#DEFAULT_2_4_0}, the rules are as
     *            follows. If it starts with {@code "/"} or contains a scheme part separator ({@code "://"}, also, with
     *            {@link TemplateNameFormat#DEFAULT_2_4_0} a {@code ":"} with no {@code "/"} anywhere before it) then
     *            it's an absolute name, otherwise it's a relative path. Relative paths are interpreted relatively to
     *            the {@code baseName}. Absolute names are simply returned as is, ignoring the {@code baseName}, except,
     *            when the {@code baseName} has scheme part while the {@code targetName} doesn't have, then the schema
     *            of the {@code baseName} is prepended to the {@code targetName}.
     * 
     * @since 2.3.22
     */
    public String toFullTemplateName(String baseName, String targetName)
            throws MalformedTemplateNameException {
        if (isClassicCompatible() /* FM1 only had absolute names */ || baseName == null) {
            return targetName;
        }

        return _CacheAPI.toRootBasedName(configuration.getTemplateNameFormat(), baseName, targetName);
    }

    /**
     * Converts a root based name (a name that's either relative to the root, or is absolute), which are typically used
     * by the API (such as for {@link Configuration#getTemplate(String)}), to an absolute name, which can be safely
     * passed to {@code <#include path>} and such, as it won't be misinterpreted to be relative to the directory of the
     * template. For example, {@code "foo/bar.ftl"} is converted to {@code "/foo/bar.ftl"}, while {@code "/foo/bar"} or
     * {@code "foo://bar/baz"} remains as is, as they are already absolute names (see {@link TemplateNameFormat} for
     * more about the format of names).
     * 
     * <p>
     * You only need this if the template name will be passed to {@code <#include name>}, {@code <#import name>},
     * {@code .get_optional_template(name)} or a similar construct in a template, otherwise using non-absolute root
     * based names is fine.
     * 
     * @since 2.3.28
     */
    public String rootBasedToAbsoluteTemplateName(String rootBasedName) throws MalformedTemplateNameException {
        return _CacheAPI.rootBasedNameToAbsoluteName(configuration.getTemplateNameFormat(), rootBasedName);
    }

    String renderElementToString(TemplateElement te) throws IOException, TemplateException {
        Writer prevOut = out;
        try {
            StringWriter sw = new StringWriter();
            this.out = sw;
            visit(te);
            return sw.toString();
        } finally {
            this.out = prevOut;
        }
    }

    void importMacros(Template template) {
        for (Iterator it = template.getMacros().values().iterator(); it.hasNext();) {
            visitMacroDef((Macro) it.next());
        }
    }

    /**
     * @return the namespace URI registered for this prefix, or null. This is based on the mappings registered in the
     *         current namespace.
     */
    public String getNamespaceForPrefix(String prefix) {
        return currentNamespace.getTemplate().getNamespaceForPrefix(prefix);
    }

    public String getPrefixForNamespace(String nsURI) {
        return currentNamespace.getTemplate().getPrefixForNamespace(nsURI);
    }

    /**
     * @return the default node namespace for the current FTL namespace
     */
    public String getDefaultNS() {
        return currentNamespace.getTemplate().getDefaultNS();
    }

    /**
     * A hook that Jython uses.
     */
    public Object __getitem__(String key) throws TemplateModelException {
        return BeansWrapper.getDefaultInstance().unwrap(getVariable(key));
    }

    /**
     * A hook that Jython uses.
     */
    public void __setitem__(String key, Object o) throws TemplateException {
        setGlobalVariable(key, getObjectWrapper().wrap(o));
    }

    private IdentityHashMap<Object, Object> customStateVariables;

    /**
     * Returns the value of a custom state variable, or {@code null} if it's missing; see
     * {@link #setCustomState(Object, Object)} for more.
     * 
     * @since 2.3.24
     */
    public Object getCustomState(Object identityKey) {
        if (customStateVariables == null) {
            return null;
        }
        return customStateVariables.get(identityKey);
    }

    /**
     * Sets the value of a custom state variable. Custom state variables meant to be used by
     * {@link TemplateNumberFormatFactory}-es, {@link TemplateDateFormatFactory}-es, and similar user-implementable,
     * pluggable objects, which want to maintain an {@link Environment}-scoped state (such as a cache).
     * 
     * @param identityKey
     *            The key that identifies the variable, by its object identity (not by {@link Object#equals(Object)}).
     *            This should be something like a {@code private static final Object CUSTOM_STATE_KEY = new Object();}
     *            in the class that needs this state variable.
     * @param value
     *            The value of the variable. Can be anything, even {@code null}.
     * 
     * @return The previous value of the variable, or {@code null} if the variable didn't exist.
     * 
     * @since 2.3.24
     */
    public Object setCustomState(Object identityKey, Object value) {
        IdentityHashMap<Object, Object> customStateVariables = this.customStateVariables;
        if (customStateVariables == null) {
            customStateVariables = new IdentityHashMap<>();
            this.customStateVariables = customStateVariables;
        }
        return customStateVariables.put(identityKey, value);
    }

    final class NestedElementTemplateDirectiveBody implements TemplateDirectiveBody {

        private final TemplateElement[] childBuffer;

        private NestedElementTemplateDirectiveBody(TemplateElement[] childBuffer) {
            this.childBuffer = childBuffer;
        }

        @Override
        public void render(Writer newOut) throws TemplateException, IOException {
            Writer prevOut = out;
            out = newOut;
            try {
                visit(childBuffer);
            } finally {
                out = prevOut;
            }
        }
        
        TemplateElement[] getChildrenBuffer() {
            return childBuffer;
        }

    }

    public class Namespace extends SimpleHash {

        private Template template;

        Namespace() {
            super(_ObjectWrappers.SAFE_OBJECT_WRAPPER);
            this.template = Environment.this.getTemplate();
        }

        Namespace(Template template) {
            super(_ObjectWrappers.SAFE_OBJECT_WRAPPER);
            this.template = template;
        }

        /**
         * @return the Template object with which this Namespace is associated.
         */
        public Template getTemplate() {
            return template == null ? Environment.this.getTemplate() : template;
        }
        
        void setTemplate(Template template) {
            this.template = template; 
        }
        
    }
    
    private enum InitializationStatus {
        UNINITIALIZED, INITIALIZING, INITIALIZED, FAILED
    }
    
    class LazilyInitializedNamespace extends Namespace {
        
        private final String templateName;
        private final Locale locale;
        private final String encoding;
        private final Object customLookupCondition;
        
        private InitializationStatus status = InitializationStatus.UNINITIALIZED;
        
        /**
         * @param templateName
         *            Must be root relative
         */
        private LazilyInitializedNamespace(String templateName) {
            super(null);
            
            this.templateName = templateName;
            // Make snapshot of all settings that influence template resolution:
            this.locale = getLocale();
            this.encoding = getIncludedTemplateEncoding();
            this.customLookupCondition = getIncludedTemplateCustomLookupCondition();
        }

        private void ensureInitializedTME() throws TemplateModelException {
            if (status != InitializationStatus.INITIALIZED && status != InitializationStatus.INITIALIZING) {
                if (status == InitializationStatus.FAILED) {
                    throw new TemplateModelException(
                            "Lazy initialization of the imported namespace for "
                            + StringUtil.jQuote(templateName)
                            + " has already failed earlier; won't retry it.");
                }
                try {
                    status = InitializationStatus.INITIALIZING;
                    initialize();
                    status = InitializationStatus.INITIALIZED;
                } catch (Exception e) {
                    // [FM3] Rethrow TemplateException-s as is
                    throw new TemplateModelException(
                            "Lazy initialization of the imported namespace for "
                            + StringUtil.jQuote(templateName)
                            + " has failed; see cause exception", e);
                } finally {
                    if (status != InitializationStatus.INITIALIZED) {
                        status = InitializationStatus.FAILED;
                    }
                }
            }
        }
        
        private void ensureInitializedRTE() {
            try {
                ensureInitializedTME();
            } catch (TemplateModelException e) {
                throw new RuntimeException(e.getMessage(), e.getCause());
            }
        }

        private void initialize() throws IOException, TemplateException {
            setTemplate(configuration.getTemplate(
                    templateName, locale, customLookupCondition, encoding,
                    true, false));
            Locale lastLocale = getLocale();
            try {
                setLocale(locale);
                initializeImportLibNamespace(this, getTemplate());
            } finally {
                setLocale(lastLocale);
            }
        }

        @Override
        protected Map copyMap(Map map) {
            ensureInitializedRTE();
            return super.copyMap(map);
        }

        @Override
        public Template getTemplate() {
            ensureInitializedRTE();
            return super.getTemplate();
        }

        @Override
        public void put(String key, Object value) {
            ensureInitializedRTE();
            super.put(key, value);
        }

        @Override
        public void put(String key, boolean b) {
            ensureInitializedRTE();
            super.put(key, b);
        }

        @Override
        public TemplateModel get(String key) throws TemplateModelException {
            ensureInitializedTME();
            return super.get(key);
        }

        @Override
        public boolean containsKey(String key) {
            ensureInitializedRTE();
            return super.containsKey(key);
        }

        @Override
        public void remove(String key) {
            ensureInitializedRTE();
            super.remove(key);
        }

        @Override
        public void putAll(Map m) {
            ensureInitializedRTE();
            super.putAll(m);
        }

        @Override
        public Map toMap() throws TemplateModelException {
            ensureInitializedTME();
            return super.toMap();
        }

        @Override
        public String toString() {
            ensureInitializedRTE();
            return super.toString();
        }

        @Override
        public int size() {
            ensureInitializedRTE();
            return super.size();
        }

        @Override
        public boolean isEmpty() {
            ensureInitializedRTE();
            return super.isEmpty();
        }

        @Override
        public TemplateCollectionModel keys() {
            ensureInitializedRTE();
            return super.keys();
        }

        @Override
        public TemplateCollectionModel values() {
            ensureInitializedRTE();
            return super.values();
        }

        @Override
        public KeyValuePairIterator keyValuePairIterator() {
            ensureInitializedRTE();
            return super.keyValuePairIterator();
        }

        
    }

    private static final Writer EMPTY_BODY_WRITER = new Writer() {

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            if (len > 0) {
                throw new IOException(
                        "This transform does not allow nested content.");
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    private boolean isBeforeIcI2322() {
        return configuration.getIncompatibleImprovements().intValue() < _VersionInts.V_2_3_22;
    }

    boolean isIcI2324OrLater() {
        return configuration.getIncompatibleImprovements().intValue() >= _VersionInts.V_2_3_24;
    }

    /**
     * See {@link #setFastInvalidReferenceExceptions(boolean)}.
     */
    boolean getFastInvalidReferenceExceptions() {
        return fastInvalidReferenceExceptions;
    }

    /**
     * Sets if for invalid references {@link InvalidReferenceException#FAST_INSTANCE} should be thrown, or a new
     * {@link InvalidReferenceException}. The "fast" instance is used if we know that the error will be handled so that
     * its message will not be logged or shown anywhere.
     */
    boolean setFastInvalidReferenceExceptions(boolean b) {
        boolean res = fastInvalidReferenceExceptions;
        fastInvalidReferenceExceptions = b;
        return res;
    }

}
