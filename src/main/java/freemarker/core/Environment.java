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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

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
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.TemplateTransformModel;
import freemarker.template.TransformControl;
import freemarker.template._TemplateAPI;
import freemarker.template.utility.DateUtil;
import freemarker.template.utility.DateUtil.DateToISO8601CalendarFactory;
import freemarker.template.utility.NullWriter;
import freemarker.template.utility.StringUtil;
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

    // Do not use this object directly; clone it first! DecimalFormat isn't
    // thread-safe.
    private static final DecimalFormat C_NUMBER_FORMAT = new DecimalFormat(
            "0.################",
            new DecimalFormatSymbols(Locale.US));

    static {
        C_NUMBER_FORMAT.setGroupingUsed(false);
        C_NUMBER_FORMAT.setDecimalSeparatorAlwaysShown(false);
    }

    private final Configuration configuration;
    private final TemplateHashModel rootDataModel;
    private final ArrayList/* <TemplateElement> */ instructionStack = new ArrayList();
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

    private NumberFormat cNumberFormat;

    /**
     * Used by the "iso_" built-ins to accelerate formatting.
     * 
     * @see #getISOBuiltInCalendarFactory()
     */
    private DateToISO8601CalendarFactory isoBuiltInCalendarFactory;

    private Collator cachedCollator;

    private Writer out;
    private Macro.Context currentMacroContext;
    private ArrayList localContextStack;
    private final Namespace mainNamespace;
    private Namespace currentNamespace, globalNamespace;
    private HashMap loadedLibs;
    private Configurable legacyParent;

    private boolean inAttemptBlock;
    private Throwable lastThrowable;

    private TemplateModel lastReturnValue;
    private HashMap macroToNamespaceLookup = new HashMap();

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
     * That template will never change, like {@code #include} or macro calls don't change it.
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
     * template with {@code #nested}. As such, it's useful in {@link TemplateDirectiveModel} to find out if from where
     * the directive was called from.
     * 
     * @see #getMainTemplate()
     * @see #getCurrentNamespace()
     * 
     * @since 2.3.23
     */
    public Template getCurrentTemplate() {
        int ln = instructionStack.size();
        return ln == 0 ? getMainTemplate() : ((TemplateObject) instructionStack.get(ln - 1)).getTemplate();
    }

    /**
     * Gets the currently executing <em>custom</em> directive's call place information, or {@code null} if there's no
     * executing custom directive. This currently only works for calls made from templates with the {@code <@...>}
     * syntax. This should only be called from the {@link TemplateDirectiveModel} that was invoked with {@code <@...>},
     * otherwise its return value is not defined by this API (it's usually {@code null}).
     * 
     * @since 2.3.22
     */
    public DirectiveCallPlace getCurrentDirectiveCallPlace() {
        int ln = instructionStack.size();
        if (ln == 0) return null;
        TemplateElement te = (TemplateElement) instructionStack.get(ln - 1);
        if (te instanceof UnifiedCall) return (UnifiedCall) te;
        if (te instanceof Macro && ln > 1 && instructionStack.get(ln - 2) instanceof UnifiedCall) {
            return (UnifiedCall) instructionStack.get(ln - 2);
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
                visit(getTemplate().getRootTreeNode(), false);
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
     *
     * Param 'hideInParent' controls how the instruction stack is handled. If it is set to false, the current
     * element is pushed to the instruction stack.
     *
     * If it is set to true, we replace the top element for the time the parameter element is
     * visited, and then we restore the top element. The main purpose of this is to get rid of elements in the error
     * stack trace that from user perspective shouldn't have a stack frame. The typical example is
     * {@code [#if foo]...[@failsHere/]...[/#if]}, where the #if call shouldn't be in the stack trace. (Simply marking
     * #if as hidden in stack traces would be wrong, because we still want to show #if when its test expression fails.)
     */
    void visit(TemplateElement element, boolean hideInParent) throws IOException, TemplateException {
        TemplateElement parent = null;
        if(hideInParent) {
            parent = replaceTopElement(element);
        } else {
            pushElement(element);
        }
        try {
            TemplateElementsToVisit templateElementsToVisit = element.accept(this);
            if(null != templateElementsToVisit) {
                boolean hideInnerElementInParent = templateElementsToVisit.isHideInParent();
                for (TemplateElement templateElementToVisit : templateElementsToVisit.getTemplateElements()) {
                    if(null != templateElementToVisit) {
                        visit(templateElementToVisit, hideInnerElementInParent);
                    }
                }
            }
        } catch (TemplateException te) {
            handleTemplateException(te);
        }
        finally {
            if(null != parent) {
                replaceTopElement(parent);
            } else {
                popElement();
            }
        }
    }

    private TemplateElement replaceTopElement(TemplateElement element) {
        return (TemplateElement) instructionStack.set(instructionStack.size() - 1, element);
    }

    private static final TemplateModel[] NO_OUT_ARGS = new TemplateModel[0];

    public void visit(final TemplateElement element,
            TemplateDirectiveModel directiveModel, Map args,
            final List bodyParameterNames) throws TemplateException, IOException {
        TemplateDirectiveBody nested;
        if (element == null) {
            nested = null;
        } else {
            nested = new NestedElementTemplateDirectiveBody(element);
        }
        final TemplateModel[] outArgs;
        if (bodyParameterNames == null || bodyParameterNames.isEmpty()) {
            outArgs = NO_OUT_ARGS;
        } else {
            outArgs = new TemplateModel[bodyParameterNames.size()];
        }
        if (outArgs.length > 0) {
            pushLocalContext(new LocalContext() {

                public TemplateModel getLocalVariable(String name) {
                    int index = bodyParameterNames.indexOf(name);
                    return index != -1 ? outArgs[index] : null;
                }

                public Collection getLocalVariableNames() {
                    return bodyParameterNames;
                }
            });
        }
        try {
            directiveModel.execute(this, args, outArgs, nested);
        } finally {
            if (outArgs.length > 0) {
                popLocalContext();
            }
        }
    }

    /**
     * "Visit" the template element, passing the output through a TemplateTransformModel
     * 
     * @param element
     *            the element to visit through a transform
     * @param transform
     *            the transform to pass the element output through
     * @param args
     *            optional arguments fed to the transform
     */
    void visitAndTransform(TemplateElement element,
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
                        if (element != null) {
                            visit(element, true);
                        }
                    } while (tc != null && tc.afterBody() == TransformControl.REPEAT_EVALUATION);
                }
            } catch (Throwable t) {
                try {
                    if (tc != null) {
                        tc.onError(t);
                    } else {
                        throw t;
                    }
                } catch (TemplateException e) {
                    throw e;
                } catch (IOException e) {
                    throw e;
                } catch (RuntimeException e) {
                    throw e;
                } catch (Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new UndeclaredThrowableException(e);
                }
            } finally {
                out = prevOut;
                tw.close();
            }
        } catch (TemplateException te) {
            handleTemplateException(te);
        }
    }

    /**
     * Visit a block using buffering/recovery
     */
     void visitAttemptRecover(TemplateElement attemptBlock, RecoveryBlock recoveryBlock)
            throws TemplateException, IOException {
        Writer prevOut = this.out;
        StringWriter sw = new StringWriter();
        this.out = sw;
        TemplateException thrownException = null;
        boolean lastFIRE = setFastInvalidReferenceExceptions(false);
        boolean lastInAttemptBlock = inAttemptBlock;
        try {
            inAttemptBlock = true;
            visit(attemptBlock, true);
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
                visit(recoveryBlock, false);
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
        ArrayList prevLocalContextStack = localContextStack;
        TemplateElement nestedContent = invokingMacroContext.nestedContent;
        if (nestedContent != null) {
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
                visit(nestedContent, false);
            } finally {
                if (invokingMacroContext.nestedContentParameterNames != null) {
                    popLocalContext();
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
            popLocalContext();
        }
    }

    /**
     * Used for {@code #visit} and {@code #recurse}.
     */
    void invokeNodeHandlerFor(TemplateNodeModel node, TemplateSequenceModel namespaces)
            throws TemplateException, IOException {
        if (nodeNamespaces == null) {
            SimpleSequence ss = new SimpleSequence(1);
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
                invoke((Macro) macroOrTransform, null, null, null, null);
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
            invoke((Macro) macroOrTransform, null, null, null, null);
        } else if (macroOrTransform instanceof TemplateTransformModel) {
            visitAndTransform(null, (TemplateTransformModel) macroOrTransform, null);
        }
    }

    /**
     * Calls the macro or function with the given arguments and nested block.
     */
    void invoke(Macro macro,
            Map namedArgs, List positionalArgs,
            List bodyParameterNames, TemplateElement nestedBlock) throws TemplateException, IOException {
        if (macro == Macro.DO_NOTHING_MACRO) {
            return;
        }

        pushElement(macro);
        try {
            final Macro.Context macroCtx = macro.new Context(this, nestedBlock, bodyParameterNames);
            setMacroContextLocalsFromArguments(macroCtx, macro, namedArgs, positionalArgs);

            final Macro.Context prevMacroCtx = currentMacroContext;
            currentMacroContext = macroCtx;

            final ArrayList prevLocalContextStack = localContextStack;
            localContextStack = null;

            final Namespace prevNamespace = currentNamespace;
            currentNamespace = (Namespace) macroToNamespaceLookup.get(macro);

            try {
                macroCtx.runMacro(this);
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
            popElement();
        }
    }

    /**
     * Sets the local variables corresponding to the macro call arguments in the macro context.
     */
    private void setMacroContextLocalsFromArguments(
            final Macro.Context macroCtx,
            final Macro macro,
            final Map namedArgs, final List positionalArgs) throws TemplateException, _MiscTemplateException {
        String catchAllParamName = macro.getCatchAll();
        if (namedArgs != null) {
            final SimpleHash catchAllParamValue;
            if (catchAllParamName != null) {
                catchAllParamValue = new SimpleHash((ObjectWrapper) null);
                macroCtx.setLocalVar(catchAllParamName, catchAllParamValue);
            } else {
                catchAllParamValue = null;
            }

            for (Iterator it = namedArgs.entrySet().iterator(); it.hasNext();) {
                final Map.Entry argNameAndValExp = (Map.Entry) it.next();
                final String argName = (String) argNameAndValExp.getKey();
                final boolean isArgNameDeclared = macro.hasArgNamed(argName);
                if (isArgNameDeclared || catchAllParamName != null) {
                    Expression argValueExp = (Expression) argNameAndValExp.getValue();
                    TemplateModel argValue = argValueExp.eval(this);
                    if (isArgNameDeclared) {
                        macroCtx.setLocalVar(argName, argValue);
                    } else {
                        catchAllParamValue.put(argName, argValue);
                    }
                } else {
                    throw new _MiscTemplateException(this,
                            (macro.isFunction() ? "Function " : "Macro "), new _DelayedJQuote(macro.getName()),
                            " has no parameter with name ", new _DelayedJQuote(argName), ".");
                }
            }
        } else if (positionalArgs != null) {
            final SimpleSequence catchAllParamValue;
            if (catchAllParamName != null) {
                catchAllParamValue = new SimpleSequence((ObjectWrapper) null);
                macroCtx.setLocalVar(catchAllParamName, catchAllParamValue);
            } else {
                catchAllParamValue = null;
            }

            String[] argNames = macro.getArgumentNamesInternal();
            final int argsCnt = positionalArgs.size();
            if (argNames.length < argsCnt && catchAllParamName == null) {
                throw new _MiscTemplateException(this,
                        (macro.isFunction() ? "Function " : "Macro "), new _DelayedJQuote(macro.getName()),
                        " only accepts ", new _DelayedToString(argNames.length), " parameters, but got ",
                        new _DelayedToString(argsCnt), ".");
            }
            for (int i = 0; i < argsCnt; i++) {
                Expression argValueExp = (Expression) positionalArgs.get(i);
                TemplateModel argValue = argValueExp.eval(this);
                try {
                    if (i < argNames.length) {
                        String argName = argNames[i];
                        macroCtx.setLocalVar(argName, argValue);
                    } else {
                        catchAllParamValue.add(argValue);
                    }
                } catch (RuntimeException re) {
                    throw new _MiscTemplateException(re, this);
                }
            }
        }
    }

    /**
     * Defines the given macro in the current namespace (doesn't call it).
     */
    void visitMacroDef(Macro macro) {
        macroToNamespaceLookup.put(macro, currentNamespace);
        currentNamespace.put(macro.getName(), macro);
    }

    Namespace getMacroNamespace(Macro macro) {
        return (Namespace) macroToNamespaceLookup.get(macro);
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
        if (children == null) return;
        for (int i = 0; i < children.size(); i++) {
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
        // Logic to prevent double-handling of the exception in
        // nested visit() calls.
        if (lastThrowable == templateException) {
            throw templateException;
        }
        lastThrowable = templateException;

        // Log the exception, if logTemplateExceptions isn't false. However, even if it's false, if we are inside
        // an #attempt block, it has to be logged, as it certainly won't bubble up to the caller of FreeMarker.
        if (LOG.isErrorEnabled() && (isInAttemptBlock() || getLogTemplateExceptions())) {
            LOG.error("Error executing FreeMarker template", templateException);
        }

        // Stop exception is not passed to the handler, but
        // explicitly rethrown.
        if (templateException instanceof StopException) {
            throw templateException;
        }

        // Finally, pass the exception to the handler
        getTemplateExceptionHandler().handleTemplateException(templateException, this, out);
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
     * information is not available. The function caches the return value, so it's quick to call it repeately.
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
            throw MessageUtil.newCantFormatNumberException(format, exp, e, useTempModelExc);
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
                cachedTemplateNumberFormats = new HashMap<String, TemplateNumberFormat>();
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
        } else {
            return JavaTemplateNumberFormatFactory.INSTANCE.get(formatString, locale, this);
        }
    }

    /**
     * Returns the {@link NumberFormat} used for the <tt>c</tt> built-in. This is always US English
     * <code>"0.################"</code>, without grouping and without superfluous decimal separator.
     */
    public NumberFormat getCNumberFormat() {
        // It can't be cached in a static field, because DecimalFormat-s aren't
        // thread-safe.
        if (cNumberFormat == null) {
            cNumberFormat = (DecimalFormat) C_NUMBER_FORMAT.clone();
        }
        return cNumberFormat;
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
            throw MessageUtil.newCantFormatDateException(format, tdmSourceExpr, e, useTempModelExc);
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
            throw MessageUtil.newCantFormatDateException(format, blamedDateSourceExp, e, useTempModelExc);
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
     * {@link TemplateDateFormatFactory} implementations to delegate to a format based on a specific format string. It's
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
            throw MessageUtil.newCantFormatUnknownTypeDateException(blamedDateSourceExp, e);
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
            throw MessageUtil.newCantFormatUnknownTypeDateException(blamedDateSourceExp, e);
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
     * of some if the parameters.
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
                        cachedFormatsByFormatString = new HashMap<String, TemplateDateFormat>(4);
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
     * Returns the loop or macro local variable corresponding to this variable name. Possibly null. (Note that the
     * misnomer is kept for backward compatibility: loop variables are not local variables according to our
     * terminology.)
     */
    public TemplateModel getLocalVariable(String name) throws TemplateModelException {
        if (localContextStack != null) {
            for (int i = localContextStack.size() - 1; i >= 0; i--) {
                LocalContext lc = (LocalContext) localContextStack.get(i);
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
        TemplateModel result = getLocalVariable(name);
        if (result == null) {
            result = currentNamespace.get(name);
        }
        if (result == null) {
            result = getGlobalVariable(name);
        }
        return result;
    }

    /**
     * Returns the globally visible variable of the given name (or null). This is correspondent to FTL
     * <code>.globals.<i>name</i></code>. This will first look at variables that were assigned globally via: &lt;#global
     * ...&gt; and then at the data model exposed to the template.
     */
    public TemplateModel getGlobalVariable(String name) throws TemplateModelException {
        TemplateModel result = globalNamespace.get(name);
        if (result == null) {
            result = rootDataModel.get(name);
        }
        if (result == null) {
            result = configuration.getSharedVariable(name);
        }
        return result;
    }

    /**
     * Sets a variable that is visible globally. This is correspondent to FTL
     * <code>&lt;#global <i>name</i>=<i>model</i>&gt;</code>. This can be considered a convenient shorthand for:
     * getGlobalNamespace().put(name, model)
     */
    public void setGlobalVariable(String name, TemplateModel model) {
        globalNamespace.put(name, model);
    }

    /**
     * Sets a variable in the current namespace. This is correspondent to FTL
     * <code>&lt;#assign <i>name</i>=<i>model</i>&gt;</code>. This can be considered a convenient shorthand for:
     * getCurrentNamespace().put(name, model)
     */
    public void setVariable(String name, TemplateModel model) {
        currentNamespace.put(name, model);
    }

    /**
     * Sets a local variable (one effective only during a macro invocation). This is correspondent to FTL
     * <code>&lt;#local <i>name</i>=<i>model</i>&gt;</code>.
     * 
     * @param name
     *            the identifier of the variable
     * @param model
     *            the value of the variable.
     * @throws IllegalStateException
     *             if the environment is not executing a macro body.
     */
    public void setLocalVariable(String name, TemplateModel model) {
        if (currentMacroContext == null) {
            throw new IllegalStateException("Not executing macro body");
        }
        currentMacroContext.setLocalVar(name, model);
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
                LocalContext lc = (LocalContext) localContextStack.get(i);
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
        int ln = instructionStack.size();

        for (int i = 0; i < ln; i++) {
            TemplateElement stackEl = (TemplateElement) instructionStack.get(i);
            if (i == ln || stackEl.isShownInStackTrace()) {
                requiredLength++;
            }
        }

        if (requiredLength == 0) return null;

        TemplateElement[] result = new TemplateElement[requiredLength];
        int dstIdx = requiredLength - 1;
        for (int i = 0; i < ln; i++) {
            TemplateElement stackEl = (TemplateElement) instructionStack.get(i);
            if (i == ln || stackEl.isShownInStackTrace()) {
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
        sb.append(MessageUtil.shorten(stackEl.getDescription(), 40));

        sb.append("  [");
        Macro enclosingMacro = getEnclosingMacro(stackEl);
        if (enclosingMacro != null) {
            sb.append(MessageUtil.formatLocationForEvaluationError(
                    enclosingMacro, stackEl.beginLine, stackEl.beginColumn));
        } else {
            sb.append(MessageUtil.formatLocationForEvaluationError(
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
            localContextStack = new ArrayList();
        }
        localContextStack.add(localContext);
    }

    private void popLocalContext() {
        localContextStack.remove(localContextStack.size() - 1);
    }

    ArrayList getLocalContextStack() {
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
            return (Namespace) loadedLibs.get(name);
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
     * Returns the data-model (also known as the template context in some other template engines).
     */
    public TemplateHashModel getDataModel() {
        final TemplateHashModel result = new TemplateHashModel() {

            public boolean isEmpty() {
                return false;
            }

            public TemplateModel get(String key) throws TemplateModelException {
                TemplateModel value = rootDataModel.get(key);
                if (value == null) {
                    value = configuration.getSharedVariable(key);
                }
                return value;
            }
        };

        if (rootDataModel instanceof TemplateHashModelEx) {
            return new TemplateHashModelEx() {

                public boolean isEmpty() throws TemplateModelException {
                    return result.isEmpty();
                }

                public TemplateModel get(String key) throws TemplateModelException {
                    return result.get(key);
                }

                // NB: The methods below do not take into account
                // configuration shared variables even though
                // the hash will return them, if only for BWC reasons
                public TemplateCollectionModel values() throws TemplateModelException {
                    return ((TemplateHashModelEx) rootDataModel).values();
                }

                public TemplateCollectionModel keys() throws TemplateModelException {
                    return ((TemplateHashModelEx) rootDataModel).keys();
                }

                public int size() throws TemplateModelException {
                    return ((TemplateHashModelEx) rootDataModel).size();
                }
            };
        }
        return result;
    }

    /**
     * Returns the read-only hash of globally visible variables. This is the correspondent of FTL <code>.globals</code>
     * hash. That is, you see the variables created with <code>&lt;#global ...&gt;</code>, and the variables of the
     * data-model. To create new global variables, use {@link #setGlobalVariable setGlobalVariable}.
     */
    public TemplateHashModel getGlobalVariables() {
        return new TemplateHashModel() {

            public boolean isEmpty() {
                return false;
            }

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
        instructionStack.add(element);
    }

    private void popElement() {
        instructionStack.remove(instructionStack.size() - 1);
    }

    void replaceElementStackTop(TemplateElement instr) {
        instructionStack.set(instructionStack.size() - 1, instr);
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
        for (i = startIndex; i < nodeNamespaces.size(); i++) {
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
     * Emulates <code>include</code> directive, except that <code>name</code> must be tempate root relative.
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
     * {@code ignoreMissign} argument.
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
        final Template inheritedTemplate = getTemplate();

        if (encoding == null) {
            // This branch shouldn't exist, as it doesn't make much sense to inherit encoding. But we have to keep BC.
            encoding = inheritedTemplate.getEncoding();
            if (encoding == null) {
                encoding = configuration.getEncoding(this.getLocale());
            }
        }

        Object customLookupCondition = inheritedTemplate.getCustomLookupCondition();

        return configuration.getTemplate(
                name, getLocale(), customLookupCondition,
                encoding, parseAsFTL,
                ignoreMissing);
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
            visit(includedTemplate.getRootTreeNode(), false);
        } finally {
            if (parentReplacementOn) {
                setParent(prevTemplate);
            } else {
                legacyParent = prevTemplate;
            }
        }
    }

    /**
     * Emulates <code>import</code> directive, except that <code>name</code> must be tempate root relative.
     *
     * <p>
     * It's the same as <code>importLib(getTemplateForImporting(name), namespace)</code>. But, you may want to
     * separately call these two methods, so you can determine the source of exceptions more precisely, and thus achieve
     * more intelligent error handling.
     *
     * @see #getTemplateForImporting(String name)
     * @see #importLib(Template includedTemplate, String namespace)
     */
    public Namespace importLib(String name, String namespace)
            throws IOException, TemplateException {
        return importLib(getTemplateForImporting(name), namespace);
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
     * Emulates <code>import</code> directive.
     *
     * @param loadedTemplate
     *            the template to import. Note that it does <em>not</em> need to be a template returned by
     *            {@link #getTemplateForImporting(String name)}.
     */
    public Namespace importLib(Template loadedTemplate, String namespace)
            throws IOException, TemplateException {
        if (loadedLibs == null) {
            loadedLibs = new HashMap();
        }
        String templateName = loadedTemplate.getName();
        Namespace existingNamespace = (Namespace) loadedLibs.get(templateName);
        if (existingNamespace != null) {
            if (namespace != null) {
                setVariable(namespace, existingNamespace);
            }
        } else {
            Namespace newNamespace = new Namespace(loadedTemplate);
            if (namespace != null) {
                currentNamespace.put(namespace, newNamespace);
                if (currentNamespace == mainNamespace) {
                    globalNamespace.put(namespace, newNamespace);
                }
            }
            Namespace prevNamespace = this.currentNamespace;
            this.currentNamespace = newNamespace;
            loadedLibs.put(templateName, currentNamespace);
            Writer prevOut = out;
            this.out = NullWriter.INSTANCE;
            try {
                include(loadedTemplate);
            } finally {
                this.out = prevOut;
                this.currentNamespace = prevNamespace;
            }
        }
        return (Namespace) loadedLibs.get(templateName);
    }

    /**
     * Resolves a reference to a template (like the one used in {@code #include} or {@code #import}), assuming a base
     * name. This gives a full (that is, absolute), even if non-normalized template name, that could be used for
     * {@link Configuration#getTemplate(String)}. This is mostly used when a template refers to another template.
     * 
     * @param baseName
     *            The name to which relative {@code targetName}-s are relative to. Maybe {@code null}, which usually
     *            means that the base is the root "directory". Assuming {@link TemplateNameFormat#DEFAULT_2_3_0} or
     *            {@link TemplateNameFormat#DEFAULT_2_4_0}, the rules are as follows. If you want to specify a base
     *            directory here, it must end with {@code "/"}. If it doesn't end with {@code "/"}, it's parent
     *            directory will be used as the base path. Might starts with a scheme part (like {@code "foo://"}, or
     *            with {@link TemplateNameFormat#DEFAULT_2_4_0} even just {@code "foo:"}).
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
        if (isClassicCompatible()) {
            // Early FM only had absolute names.
            return targetName;
        }

        return _CacheAPI.toAbsoluteName(configuration.getTemplateNameFormat(), baseName, targetName);
    }

    String renderElementToString(TemplateElement te) throws IOException, TemplateException {
        Writer prevOut = out;
        try {
            StringWriter sw = new StringWriter();
            this.out = sw;
            visit(te, false);
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
            customStateVariables = new IdentityHashMap<Object, Object>();
            this.customStateVariables = customStateVariables;
        }
        return customStateVariables.put(identityKey, value);
    }

    final class NestedElementTemplateDirectiveBody implements TemplateDirectiveBody {

        private final TemplateElement element;

        private NestedElementTemplateDirectiveBody(TemplateElement element) {
            this.element = element;
        }

        public void render(Writer newOut) throws TemplateException, IOException {
            Writer prevOut = out;
            out = newOut;
            try {
                visit(element, false);
            } finally {
                out = prevOut;
            }
        }

        public TemplateElement getElement() {
            return element;
        }

    }

    public class Namespace extends SimpleHash {

        private final Template template;

        Namespace() {
            this.template = Environment.this.getTemplate();
        }

        Namespace(Template template) {
            this.template = template;
        }

        /**
         * @return the Template object with which this Namespace is associated.
         */
        public Template getTemplate() {
            return template == null ? Environment.this.getTemplate() : template;
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
        return configuration.getIncompatibleImprovements().intValue() < _TemplateAPI.VERSION_INT_2_3_22;
    }

    private boolean isIcI2324OrLater() {
        return configuration.getIncompatibleImprovements().intValue() >= _TemplateAPI.VERSION_INT_2_3_24;
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
