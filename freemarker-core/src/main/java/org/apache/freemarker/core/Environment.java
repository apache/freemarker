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
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateCallableModel;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateModelWithOriginName;
import org.apache.freemarker.core.model.TemplateNodeModel;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.impl.SimpleHash;
import org.apache.freemarker.core.templateresolver.MalformedTemplateNameException;
import org.apache.freemarker.core.templateresolver.TemplateResolver;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateNameFormat;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.apache.freemarker.core.util._DateUtils;
import org.apache.freemarker.core.util._DateUtils.DateToISO8601CalendarFactory;
import org.apache.freemarker.core.util._NullWriter;
import org.apache.freemarker.core.util._StringUtils;
import org.apache.freemarker.core.valueformat.TemplateDateFormat;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateNumberFormat;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateValueFormatException;
import org.apache.freemarker.core.valueformat.UndefinedCustomFormatException;
import org.apache.freemarker.core.valueformat.UnknownDateTypeFormattingUnsupportedException;
import org.apache.freemarker.core.valueformat.impl.ISOTemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.impl.JavaTemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.impl.JavaTemplateNumberFormatFactory;
import org.apache.freemarker.core.valueformat.impl.XSTemplateDateFormatFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Object that represents the runtime environment during template processing. For every invocation of a
 * <tt>Template.process()</tt> method, a new instance of this object is created, and then discarded when
 * <tt>process()</tt> returns. This object stores the set of temporary variables created by the template, the value of
 * settings set by the template, the reference to the data model root, etc. Everything that is needed to fulfill the
 * template processing job.
 * <p>
 * Data models that need to access the <tt>Environment</tt> object that represents the template processing on the
 * current thread can use the {@link #getCurrentEnvironment()} method.
 * <p>
 * If you need to modify or read this object before or after the <tt>process</tt> call, use
 * {@link Template#createProcessingEnvironment(Object rootMap, Writer out, ObjectWrapper wrapper)}
 * <p>
 * The {@link ProcessingConfiguration} reader methods of this class don't throw {@link CoreSettingValueNotSetException}
 * because unset settings are ultimately inherited from {@link Configuration}.
 */
public final class Environment extends MutableProcessingConfiguration<Environment> implements CustomStateScope {

    private static final Logger LOG = LoggerFactory.getLogger(Environment.class);

    private static final ThreadLocal<Environment> TLS_ENVIRONMENT = new ThreadLocal();

    // Do not use this object directly; deepClone it first! DecimalFormat isn't
    // thread-safe.
    private static final DecimalFormat C_NUMBER_FORMAT;
    static {
        C_NUMBER_FORMAT = new DecimalFormat("0.################", new DecimalFormatSymbols(Locale.US));
        C_NUMBER_FORMAT.setGroupingUsed(false);
        C_NUMBER_FORMAT.setDecimalSeparatorAlwaysShown(false);
    }

    private final Configuration configuration;
    private final TemplateHashModel rootDataModel;
    private ASTElement[] instructionStack = new ASTElement[16];
    private int instructionStackSize = 0;
    private final ArrayList recoveredErrorStack = new ArrayList();

    private TemplateNumberFormat cachedTemplateNumberFormat;
    private Map<String, TemplateNumberFormat> cachedTemplateNumberFormats;
    private Map<CustomStateKey, Object> customStateMap;

    private TemplateBooleanFormat cachedTemplateBooleanFormat;

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
    private ASTDirMacroOrFunction.Context currentMacroContext;
    private LocalContextStack localContextStack;
    private final Template mainTemplate;
    private final Namespace mainNamespace;
    private Namespace currentNamespace, globalNamespace;
    private HashMap<String, Namespace> loadedLibs;

    private boolean inAttemptBlock;
    private Throwable lastThrowable;

    private TemplateModel lastReturnValue;

    private TemplateNodeModel currentVisitorNode;
    private TemplateSequenceModel nodeNamespaces;
    // Things we keep track of for the fallback mechanism.
    private int nodeNamespaceIndex;
    private String currentNodeName, currentNodeNS;

    private Charset cachedURLEscapingCharset;
    private boolean cachedURLEscapingCharsetSet;

    private boolean fastInvalidReferenceExceptions;

    /**
     * Retrieves the environment object associated with the current thread, or {@code null} if there's no template
     * processing going on in this thread. Data model implementations that need access to the environment can call this
     * method to obtain the environment object that represents the template processing that is currently running on the
     * current thread.
     */
    public static Environment getCurrentEnvironment() {
        return TLS_ENVIRONMENT.get();
    }

    public static Environment getCurrentEnvironmentNotNull() {
        Environment currentEnvironment = getCurrentEnvironment();
        if (currentEnvironment == null) {
            throw new IllegalStateException("There's no FreeMarker Environemnt in this this thread.");
        }
        return currentEnvironment;
    }

    static void setCurrentEnvironment(Environment env) {
        TLS_ENVIRONMENT.set(env);
    }

    public Environment(Template template, final TemplateHashModel rootDataModel, Writer out) {
        mainTemplate = template;
        configuration = template.getConfiguration();
        globalNamespace = new Namespace(null);
        currentNamespace = mainNamespace = new Namespace(mainTemplate);
        this.out = out;
        this.rootDataModel = rootDataModel;
        importMacros(template);
    }

    /**
     * Returns the topmost {@link Template}, with other words, the one for which this {@link Environment} was created.
     * That template will never change, like {@code #include} or macro calls don't change it.
     * 
     * @see #getCurrentNamespace()
     */
    public Template getMainTemplate() {
        return mainTemplate;
    }

    /**
     * Returns the {@link Template} that we are "lexically" inside at the moment. This template will change when
     * entering an {@code #include} or calling a macro or function in another template, or returning to yet another
     * template with {@code #nested}.
     * 
     * @see #getMainTemplate()
     * @see #getCurrentNamespace()
     */
    @SuppressFBWarnings(value = "RANGE_ARRAY_INDEX", justification = "False alarm")
    public Template getCurrentTemplate() {
        int ln = instructionStackSize;
        return ln == 0 ? getMainTemplate() : instructionStack[ln - 1].getTemplate();
    }

    public Template getCurrentTemplateNotNull() {
        Template currentTemplate = getCurrentTemplate();
        if (currentTemplate == null) {
            throw new IllegalStateException("There's no current template at the moment.");
        }
        return currentTemplate;
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
        Environment savedEnv = TLS_ENVIRONMENT.get();
        TLS_ENVIRONMENT.set(this);
        try {
            // Cached values from a previous execution are possibly outdated.
            clearCachedValues();
            try {
                doAutoImportsAndIncludes(this);
                visit(getMainTemplate().getRootASTNode());
                // It's here as we must not flush if there was an exception.
                if (getAutoFlush()) {
                    out.flush();
                }
            } finally {
                // It's just to allow the GC to free memory...
                clearCachedValues();
            }
        } finally {
            TLS_ENVIRONMENT.set(savedEnv);
        }
    }

    /**
     * Executes the auto-imports and auto-includes for the main template of this environment.
     * This is not meant to be called or overridden by code outside of FreeMarker.
     */
    private void doAutoImportsAndIncludes(Environment env) throws TemplateException, IOException {
        Template t = getMainTemplate();
        doAutoImports(t);
        doAutoIncludes(t);
    }

    private void doAutoImports(Template t) throws IOException, TemplateException {
        Map<String, String> envAutoImports = isAutoImportsSet() ? getAutoImports() : null;
        Map<String, String> tAutoImports = t.isAutoImportsSet() ? t.getAutoImports() : null;

        boolean lazyAutoImports = getLazyAutoImports() != null ? getLazyAutoImports() : getLazyImports();

        for (Map.Entry<String, String> autoImport : configuration.getAutoImports().entrySet()) {
            String nsVarName = autoImport.getKey();
            if ((tAutoImports == null || !tAutoImports.containsKey(nsVarName))
                    && (envAutoImports == null || !envAutoImports.containsKey(nsVarName))) {
                importLib(autoImport.getValue(), nsVarName, lazyAutoImports);
            }
        }
        if (tAutoImports != null) {
            for (Map.Entry<String, String> autoImport : tAutoImports.entrySet()) {
                String nsVarName = autoImport.getKey();
                if (envAutoImports == null || !envAutoImports.containsKey(nsVarName)) {
                    importLib(autoImport.getValue(), nsVarName, lazyAutoImports);
                }
            }
        }
        if (envAutoImports != null) {
            for (Map.Entry<String, String> autoImport : envAutoImports.entrySet()) {
                String nsVarName = autoImport.getKey();
                importLib(autoImport.getValue(), nsVarName, lazyAutoImports);
            }
        }
    }

    private void doAutoIncludes(Template t) throws TemplateException, IOException {
        // We can't store autoIncludes in LinkedHashSet-s because setAutoIncludes(List) allows duplicates,
        // unfortunately. Yet we have to prevent duplicates among Configuration levels, with the lowest levels having
        // priority. So we build some Set-s to do that, but we avoid the most common cases where they aren't needed.

        List<String> tAutoIncludes = t.isAutoIncludesSet() ? t.getAutoIncludes() : null;
        List<String> envAutoIncludes = isAutoIncludesSet() ? getAutoIncludes() : null;

        for (String templateName : configuration.getAutoIncludes()) {
            if ((tAutoIncludes == null || !tAutoIncludes.contains(templateName))
                    && (envAutoIncludes == null || !envAutoIncludes.contains(templateName))) {
                include(configuration.getTemplate(templateName, getLocale()));
            }
        }

        if (tAutoIncludes != null) {
            for (String templateName : tAutoIncludes) {
                if (envAutoIncludes == null || !envAutoIncludes.contains(templateName)) {
                    include(configuration.getTemplate(templateName, getLocale()));
                }
            }
        }

        if (envAutoIncludes != null) {
            for (String templateName : envAutoIncludes) {
                include(configuration.getTemplate(templateName, getLocale()));
            }
        }
    }

    /**
     * "Visit" the template element.
     */
    void visit(ASTElement element) throws IOException, TemplateException {
        // ATTENTION: This method body is manually "inlined" into visit(ASTElement[]); keep them in sync!
        pushElement(element);
        try {
            ASTElement[] templateElementsToVisit = element.accept(this);
            if (templateElementsToVisit != null) {
                for (ASTElement el : templateElementsToVisit) {
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
        // ATTENTION: This method body above is manually "inlined" into visit(ASTElement[]); keep them in sync!
    }
    
    /**
     * @param elementBuffer
     *            The elements to visit; might contains trailing {@code null}-s. Can be {@code null}.
     */
    final void visit(ASTElement[] elementBuffer) throws IOException, TemplateException {
        if (elementBuffer == null) {
            return;
        }
        for (ASTElement element : elementBuffer) {
            if (element == null) {
                break;  // Skip unused trailing buffer capacity 
            }
            
            // ATTENTION: This part is the manually "inlining" of visit(ASTElement[]); keep them in sync!
            // We don't just let Hotspot to do it, as we want a hard guarantee regarding maximum stack usage. 
            pushElement(element);
            try {
                ASTElement[] templateElementsToVisit = element.accept(this);
                if (templateElementsToVisit != null) {
                    for (ASTElement el : templateElementsToVisit) {
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
            // ATTENTION: This part above is the manually "inlining" of visit(ASTElement[]); keep them in sync!
        }
    }

    @SuppressFBWarnings(value = "RANGE_ARRAY_INDEX", justification = "Not called when stack is empty")
    private ASTElement replaceTopElement(ASTElement element) {
        return instructionStack[instructionStackSize - 1] = element;
    }

    private static final TemplateModel[] NO_OUT_ARGS = new TemplateModel[0];

    void visit(
            ASTElement[] childBuffer,
            final StringToIndexMap nestedContentParamNames, final TemplateModel[] nestedContentParamValues,
            Writer out)
        throws IOException, TemplateException {
        if (nestedContentParamNames == null) { // This is by far the most frequent case
            visit(childBuffer, out);
        } else {
            pushLocalContext(new LocalContext() {
                @Override
                public TemplateModel getLocalVariable(String name) throws TemplateModelException {
                    int index = nestedContentParamNames.get(name);
                    return index != -1 ? nestedContentParamValues[index] : null;
                }

                @Override
                public Collection<String> getLocalVariableNames() throws TemplateModelException {
                    return nestedContentParamNames.getKeys();
                }
            });
            try {
                visit(childBuffer, out);
            } finally {
                popLocalContext();
            }
        }
    }

    void visit(ASTElement[] childBuffer, Writer out) throws IOException, TemplateException {
        // TODO [FM][CF] The plan is that `out` will be the root read only sink, so then it will work differently
        Writer prevOut = this.out;
        this.out = out;
        try {
            visit(childBuffer);
        } finally {
            this.out = prevOut;
        }
    }

    /**
     * Visit a block using buffering/recovery
     */
     void visitAttemptRecover(
             ASTDirAttemptRecoverContainer attemptBlock, ASTElement attemptedSection, ASTDirRecover recoverySection)
             throws TemplateException, IOException {
        Writer prevOut = out;
        StringWriter sw = new StringWriter();
         out = sw;
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
            out = prevOut;
        }
        if (thrownException != null) {
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
            throw new TemplateException(this, ".error is not available outside of a #recover block");
        }
        return ((Throwable) recoveredErrorStack.get(recoveredErrorStack.size() - 1)).getMessage();
    }

    /**
     * Tells if we are inside an <tt>#attempt</tt> block (but before <tt>#recover</tt>). This can be useful for
     * {@link TemplateExceptionHandler}-s, as then they may don't want to print the error to the output, as
     * <tt>#attempt</tt> will roll it back anyway.
     */
    public boolean isInAttemptBlock() {
        return inAttemptBlock;
    }

    /**
     * Used to execute the nested content of a macro call during a macro execution. It's not enough to simply call
     * {@link CallPlace#executeNestedContent(TemplateModel[], Writer, Environment)} in such case, because an executing
     * macro modifies the template language environment for its purposes, and the nested content expects that to be
     * like before the macro invocation. So things has to be temporarily restored.
     */
    void executeNestedContentOfMacro(TemplateModel[] nestedContentParamValues)
            throws TemplateException, IOException {
        ASTDirMacroOrFunction.Context invokingMacroContext = getCurrentMacroContext();
        CallPlace callPlace = invokingMacroContext.callPlace;
        if (callPlace.hasNestedContent()) {
            currentMacroContext = invokingMacroContext.prevMacroContext;
            currentNamespace = invokingMacroContext.nestedContentNamespace;
            LocalContextStack prevLocalContextStack = localContextStack;
            localContextStack = invokingMacroContext.prevLocalContextStack;
            try {
                callPlace.executeNestedContent(nestedContentParamValues, out, this);
            } finally {
                currentMacroContext = invokingMacroContext;
                currentNamespace = invokingMacroContext.callable.namespace;
                localContextStack = prevLocalContextStack;
            }
        }
    }

    /**
     * "visit" an ASTDirList
     */
    boolean visitIteratorBlock(ASTDirList.IterationContext ictxt)
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
            NativeSequence seq = new NativeSequence(1);
            seq.add(currentNamespace);
            nodeNamespaces = seq;
        }
        int prevNodeNamespaceIndex = nodeNamespaceIndex;
        String prevNodeName = currentNodeName;
        String prevNodeNS = currentNodeNS;
        TemplateSequenceModel prevNodeNamespaces = nodeNamespaces;
        TemplateNodeModel prevVisitorNode = currentVisitorNode;
        currentVisitorNode = node;
        if (namespaces != null) {
            nodeNamespaces = namespaces;
        }
        try {
            TemplateDirectiveModel nodeProcessor = getNodeProcessor(node);
            if (nodeProcessor != null) {
                _CallableUtils.executeWith0Arguments(
                        nodeProcessor, NonTemplateCallPlace.INSTANCE, out, this);
            } else if (nodeProcessor == null) {
                String nodeType = node.getNodeType();
                if (nodeType != null) {
                    // TODO [FM3] We are supposed to be o.a.f.dom unaware in the core, plus these types can mean
                    // something else with another wrapper. So we should encode the default behavior into the
                    // // TemplateNodeModel somehow.

                    // If the node's type is 'text', we just output it.
                    if ((nodeType.equals("text") && node instanceof TemplateScalarModel)) {
                        out.write(((TemplateScalarModel) node).getAsString()); // TODO [FM3] Escaping?
                    } else if (nodeType.equals("document")) {
                        recurse(node, namespaces);
                    }
                    // We complain here, unless the node's type is 'pi', or "comment" or "document_type", in which case
                    // we just ignore it.
                    else if (!nodeType.equals("pi")
                            && !nodeType.equals("comment")
                            && !nodeType.equals("document_type")) {
                        throw new TemplateException(
                                this, noNodeHandlerDefinedDescription(node, node.getNodeNamespace(), nodeType));
                    }
                } else {
                    throw new TemplateException(
                            this, noNodeHandlerDefinedDescription(node, node.getNodeNamespace(), "default"));
                }
            }
        } finally {
            currentVisitorNode = prevVisitorNode;
            nodeNamespaceIndex = prevNodeNamespaceIndex;
            currentNodeName = prevNodeName;
            currentNodeNS = prevNodeNS;
            nodeNamespaces = prevNodeNamespaces;
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
        TemplateDirectiveModel nodeProcessor = getNodeProcessor(currentNodeName, currentNodeNS, nodeNamespaceIndex);
        if (nodeProcessor != null) {
            _CallableUtils.executeWith0Arguments(
                    nodeProcessor, NonTemplateCallPlace.INSTANCE, out, this);
        }
    }

    /**
     * Defines the given macro in the current namespace (doesn't call it).
     */
    void visitMacroOrFunctionDefinition(ASTDirMacroOrFunction definition) {
        Namespace currentNamespace = this.getCurrentNamespace();
        currentNamespace.put(
                definition.getName(),
                definition.isFunction()
                        ? new TemplateLanguageFunction(definition, currentNamespace)
                        : new TemplateLanguageDirective(definition, currentNamespace));
    }

    void recurse(TemplateNodeModel node, TemplateSequenceModel namespaces)
            throws TemplateException, IOException {
        if (node == null) {
            node = getCurrentVisitorNode();
            if (node == null) {
                throw new TemplateModelException(
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

    ASTDirMacroOrFunction.Context getCurrentMacroContext() {
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
    protected TemplateExceptionHandler getDefaultTemplateExceptionHandler() {
        return getMainTemplate().getTemplateExceptionHandler();
    }

    @Override
    protected AttemptExceptionReporter getDefaultAttemptExceptionReporter() {
        return getMainTemplate().getAttemptExceptionReporter();
    }

    @Override
    protected ArithmeticEngine getDefaultArithmeticEngine() {
        return getMainTemplate().getArithmeticEngine();
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
    protected Locale getDefaultLocale() {
        return getMainTemplate().getLocale();
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
    protected TimeZone getDefaultTimeZone() {
        return getMainTemplate().getTimeZone();
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

    @Override
    protected TimeZone getDefaultSQLDateAndTimeTimeZone() {
        return getMainTemplate().getSQLDateAndTimeTimeZone();
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
    private boolean isSQLDateAndTimeTimeZoneSameAsNormal() {
        if (cachedSQLDateAndTimeTimeZoneSameAsNormal == null) {
            cachedSQLDateAndTimeTimeZoneSameAsNormal = Boolean.valueOf(
                    getSQLDateAndTimeTimeZone() == null
                            || getSQLDateAndTimeTimeZone().equals(getTimeZone()));
        }
        return cachedSQLDateAndTimeTimeZoneSameAsNormal.booleanValue();
    }

    @Override
    public void setURLEscapingCharset(Charset urlEscapingCharset) {
        cachedURLEscapingCharsetSet = false;
        super.setURLEscapingCharset(urlEscapingCharset);
    }

    @Override
    protected Charset getDefaultURLEscapingCharset() {
        return getMainTemplate().getURLEscapingCharset();
    }

    @Override
    protected TemplateClassResolver getDefaultNewBuiltinClassResolver() {
        return getMainTemplate().getNewBuiltinClassResolver();
    }

    @Override
    protected boolean getDefaultAutoFlush() {
        return getMainTemplate().getAutoFlush();
    }

    @Override
    protected boolean getDefaultShowErrorTips() {
        return getMainTemplate().getShowErrorTips();
    }

    @Override
    protected boolean getDefaultAPIBuiltinEnabled() {
        return getMainTemplate().getAPIBuiltinEnabled();
    }

    @Override
    protected boolean getDefaultLazyImports() {
        return getMainTemplate().getLazyImports();
    }

    @Override
    protected Boolean getDefaultLazyAutoImports() {
        return getMainTemplate().getLazyAutoImports();
    }

    @Override
    protected Map<String, String> getDefaultAutoImports() {
        return getMainTemplate().getAutoImports();
    }

    @Override
    protected List<String> getDefaultAutoIncludes() {
        return getMainTemplate().getAutoIncludes();
    }

    @Override
    protected Object getDefaultCustomSetting(Serializable key, Object defaultValue, boolean useDefaultValue) {
        return useDefaultValue ? getMainTemplate().getCustomSetting(key, defaultValue)
                : getMainTemplate().getCustomSetting(key);
    }

    @Override
    protected void collectDefaultCustomSettingsSnapshot(Map<Serializable, Object> target) {
        target.putAll(getMainTemplate().getCustomSettings(true));
    }

    /*
     * Note that although it's not allowed to set this setting with the <tt>setting</tt> directive, it still must be
     * allowed to set it from Java code while the template executes, since some frameworks allow templates to actually
     * change the output encoding on-the-fly.
     */
    @Override
    public void setOutputEncoding(Charset outputEncoding) {
        cachedURLEscapingCharsetSet = false;
        super.setOutputEncoding(outputEncoding);
    }

    @Override
    protected Charset getDefaultOutputEncoding() {
        return getMainTemplate().getOutputEncoding();
    }

    /**
     * Returns the name of the charset that should be used for URL encoding. This will be <code>null</code> if the
     * information is not available. The function caches the return value, so it's quick to call it repeatedly.
     */
    Charset getEffectiveURLEscapingCharset() {
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
     */
    public boolean applyEqualsOperator(TemplateModel leftValue, TemplateModel rightValue)
            throws TemplateException {
        return _EvalUtils.compare(leftValue, _EvalUtils.CMP_OP_EQUALS, rightValue, this);
    }

    /**
     * Compares two {@link TemplateModel}-s according the rules of the FTL "==" operator, except that if the two types
     * are incompatible, they are treated as non-equal instead of throwing an exception. Comparing dates of different
     * types (date-only VS time-only VS date-time) will still throw an exception, however.
     */
    public boolean applyEqualsOperatorLenient(TemplateModel leftValue, TemplateModel rightValue)
            throws TemplateException {
        return _EvalUtils.compareLenient(leftValue, _EvalUtils.CMP_OP_EQUALS, rightValue, this);
    }

    /**
     * Compares two {@link TemplateModel}-s according the rules of the FTL "&lt;" operator.
     */
    public boolean applyLessThanOperator(TemplateModel leftValue, TemplateModel rightValue)
            throws TemplateException {
        return _EvalUtils.compare(leftValue, _EvalUtils.CMP_OP_LESS_THAN, rightValue, this);
    }

    /**
     * Compares two {@link TemplateModel}-s according the rules of the FTL "&lt;" operator.
     */
    public boolean applyLessThanOrEqualsOperator(TemplateModel leftValue, TemplateModel rightValue)
            throws TemplateException {
        return _EvalUtils.compare(leftValue, _EvalUtils.CMP_OP_LESS_THAN_EQUALS, rightValue, this);
    }

    /**
     * Compares two {@link TemplateModel}-s according the rules of the FTL "&gt;" operator.
     */
    public boolean applyGreaterThanOperator(TemplateModel leftValue, TemplateModel rightValue)
            throws TemplateException {
        return _EvalUtils.compare(leftValue, _EvalUtils.CMP_OP_GREATER_THAN, rightValue, this);
    }

    /**
     * Compares two {@link TemplateModel}-s according the rules of the FTL "&gt;=" operator.
     */
    public boolean applyWithGreaterThanOrEqualsOperator(TemplateModel leftValue, TemplateModel rightValue)
            throws TemplateException {
        return _EvalUtils.compare(leftValue, _EvalUtils.CMP_OP_GREATER_THAN_EQUALS, rightValue, this);
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

    @Override
    protected String getDefaultNumberFormat() {
        return getMainTemplate().getNumberFormat();
    }

    @Override
    protected Map<String, TemplateNumberFormatFactory> getDefaultCustomNumberFormats() {
        return getMainTemplate().getCustomNumberFormats();
    }

    @Override
    protected TemplateNumberFormatFactory getDefaultCustomNumberFormat(String name) {
        return getMainTemplate().getCustomNumberFormat(name);
    }

    @Override
    protected String getDefaultBooleanFormat() {
        return getMainTemplate().getBooleanFormat();
    }

    String formatBoolean(boolean value, boolean fallbackToTrueFalse) throws TemplateException {
        TemplateBooleanFormat templateBooleanFormat = getTemplateBooleanFormat();
        if (value) {
            String s = templateBooleanFormat.getTrueStringValue();
            if (s == null) {
                if (fallbackToTrueFalse) {
                    return TemplateBooleanFormat.C_TRUE;
                } else {
                    throw new TemplateException(getNullBooleanFormatErrorDescription());
                }
            } else {
                return s;
            }
        } else {
            String s = templateBooleanFormat.getFalseStringValue();
            if (s == null) {
                if (fallbackToTrueFalse) {
                    return TemplateBooleanFormat.C_FALSE;
                } else {
                    throw new TemplateException(getNullBooleanFormatErrorDescription());
                }
            } else {
                return s;
            }
        }
    }

    TemplateBooleanFormat getTemplateBooleanFormat() {
        TemplateBooleanFormat format = cachedTemplateBooleanFormat;
        if (format == null) {
            format = TemplateBooleanFormat.getInstance(getBooleanFormat());
            cachedTemplateBooleanFormat = format;
        }
        return format;
    }

    @Override
    public void setBooleanFormat(String booleanFormat) {
        String previousFormat = getBooleanFormat();
        super.setBooleanFormat(booleanFormat);
        if (!booleanFormat.equals(previousFormat)) {
            cachedTemplateBooleanFormat = null;
        }
    }

    private _ErrorDescriptionBuilder getNullBooleanFormatErrorDescription() {
        return new _ErrorDescriptionBuilder(
                "Can't convert boolean to string automatically, because the \"", BOOLEAN_FORMAT_KEY ,"\" setting was ",
                new _DelayedJQuote(getBooleanFormat()),
                (getBooleanFormat().equals(TemplateBooleanFormat.C_TRUE_FALSE)
                        ? ", which is the legacy default computer-language format, and hence isn't accepted."
                        : ".")
        ).tips(
                "If you just want \"true\"/\"false\" result as you are generting computer-language output, "
                        + "use \"?c\", like ${myBool?c}.",
                "You can write myBool?string('yes', 'no') and like to specify boolean formatting in place.",
                new Object[] {
                        "If you need the same two values on most places, the programmers should set the \"",
                        BOOLEAN_FORMAT_KEY ,"\" setting to something like \"yes,no\"." }
        );
    }

    /**
     * Format number with the default number format.
     * 
     * @param exp
     *            The blamed expression if an error occurs; it's only needed for better error messages
     */
    String formatNumberToPlainText(TemplateNumberModel number, ASTExpression exp, boolean useTempModelExc)
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
            TemplateNumberModel number, TemplateNumberFormat format, ASTExpression exp,
            boolean useTempModelExc)
            throws TemplateException {
        try {
            return _EvalUtils.assertFormatResultNotNull(format.formatToPlainText(number));
        } catch (TemplateValueFormatException e) {
            throw MessageUtils.newCantFormatNumberException(format, exp, e, useTempModelExc);
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
    TemplateNumberFormat getTemplateNumberFormat(ASTExpression exp, boolean useTempModelExc) throws TemplateException {
        TemplateNumberFormat format;
        try {
            format = getTemplateNumberFormat();
        } catch (TemplateValueFormatException e) {
            _ErrorDescriptionBuilder desc = new _ErrorDescriptionBuilder(
                    "Failed to get number format object for the current number format string, ",
                    new _DelayedJQuote(getNumberFormat()), ": ", e.getMessage())
                    .blame(exp); 
            throw useTempModelExc
                    ? new _TemplateModelException(e, this, desc) : new TemplateException(e, this, desc);
        }
        return format;
    }

    /**
     * Convenience wrapper around {@link #getTemplateNumberFormat(String)} to be called during expression evaluation.
     * 
     * @param exp
     *            The blamed expression if an error occurs; it's only needed for better error messages
     */
    TemplateNumberFormat getTemplateNumberFormat(String formatString, ASTExpression exp, boolean useTempModelExc)
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
                    ? new _TemplateModelException(e, this, desc) : new TemplateException(e, this, desc);
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
                        "No custom number format was defined with name " + _StringUtils.jQuote(name));
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
    protected String getDefaultTimeFormat() {
        return getMainTemplate().getTimeFormat();
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
    protected String getDefaultDateFormat() {
        return getMainTemplate().getDateFormat();
    }

    @Override
    public void setDateTimeFormat(String dateTimeFormat) {
        String prevDateTimeFormat = getDateTimeFormat();
        super.setDateTimeFormat(dateTimeFormat);
        if (!dateTimeFormat.equals(prevDateTimeFormat)) {
            if (cachedTempDateFormatArray != null) {
                for (int i = 0; i < CACHED_TDFS_LENGTH; i += CACHED_TDFS_ZONELESS_INPUT_OFFS) {
                    cachedTempDateFormatArray[i + TemplateDateModel.DATE_TIME] = null;
                }
            }
        }
    }

    @Override
    protected String getDefaultDateTimeFormat() {
        return getMainTemplate().getDateTimeFormat();
    }

    @Override
    protected Map<String, TemplateDateFormatFactory> getDefaultCustomDateFormats() {
        return getMainTemplate().getCustomDateFormats();
    }

    @Override
    protected TemplateDateFormatFactory getDefaultCustomDateFormat(String name) {
        return getMainTemplate().getCustomDateFormat(name);
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
        lastReturnValue = null;
    }

    /**
     * @param tdmSourceExpr
     *            The blamed expression if an error occurs; only used for error messages.
     */
    String formatDateToPlainText(TemplateDateModel tdm, ASTExpression tdmSourceExpr,
            boolean useTempModelExc) throws TemplateException {
        TemplateDateFormat format = getTemplateDateFormat(tdm, tdmSourceExpr, useTempModelExc);
        
        try {
            return _EvalUtils.assertFormatResultNotNull(format.formatToPlainText(tdm));
        } catch (TemplateValueFormatException e) {
            throw MessageUtils.newCantFormatDateException(format, tdmSourceExpr, e, useTempModelExc);
        }
    }

    /**
     * @param blamedDateSourceExp
     *            The blamed expression if an error occurs; only used for error messages.
     * @param blamedFormatterExp
     *            The blamed expression if an error occurs; only used for error messages.
     */
    String formatDateToPlainText(TemplateDateModel tdm, String formatString,
            ASTExpression blamedDateSourceExp, ASTExpression blamedFormatterExp,
            boolean useTempModelExc) throws TemplateException {
        Date date = _EvalUtils.modelToDate(tdm, blamedDateSourceExp);
        
        TemplateDateFormat format = getTemplateDateFormat(
                formatString, tdm.getDateType(), date.getClass(),
                blamedDateSourceExp, blamedFormatterExp,
                useTempModelExc);
        
        try {
            return _EvalUtils.assertFormatResultNotNull(format.formatToPlainText(tdm));
        } catch (TemplateValueFormatException e) {
            throw MessageUtils.newCantFormatDateException(format, blamedDateSourceExp, e, useTempModelExc);
        }
    }

    /**
     * Gets a {@link TemplateDateFormat} using the date/time/dateTime format settings and the current locale and time
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
     * the cached entries that depend on the current locale or the current time zone or the current date/time/dateTime
     * format of the {@link Environment} will be lost when those settings are changed.
     * 
     * @param formatString
     *            Like {@code "iso m"} or {@code "dd.MM.yyyy HH:mm"} or {@code "@somethingCustom"} or
     *            {@code "@somethingCustom params"}
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
    
    TemplateDateFormat getTemplateDateFormat(TemplateDateModel tdm, ASTExpression tdmSourceExpr, boolean useTempModelExc)
            throws TemplateException {
        Date date = _EvalUtils.modelToDate(tdm, tdmSourceExpr);
        
        return getTemplateDateFormat(
                tdm.getDateType(), date.getClass(), tdmSourceExpr,
                useTempModelExc);
    }

    /**
     * Same as {@link #getTemplateDateFormat(int, Class)}, but translates the exceptions to {@link TemplateException}-s.
     */
    TemplateDateFormat getTemplateDateFormat(
            int dateType, Class<? extends Date> dateClass, ASTExpression blamedDateSourceExp, boolean useTempModelExc)
                    throws TemplateException {
        try {
            return getTemplateDateFormat(dateType, dateClass);
        } catch (UnknownDateTypeFormattingUnsupportedException e) {
            throw MessageUtils.newCantFormatUnknownTypeDateException(blamedDateSourceExp, e);
        } catch (TemplateValueFormatException e) {
            String settingName;
            String settingValue;
            switch (dateType) {
            case TemplateDateModel.TIME:
                settingName = MutableProcessingConfiguration.TIME_FORMAT_KEY;
                settingValue = getTimeFormat();
                break;
            case TemplateDateModel.DATE:
                settingName = MutableProcessingConfiguration.DATE_FORMAT_KEY;
                settingValue = getDateFormat();
                break;
            case TemplateDateModel.DATE_TIME:
                settingName = MutableProcessingConfiguration.DATE_TIME_FORMAT_KEY;
                settingValue = getDateTimeFormat();
                break;
            default:
                settingName = "???";
                settingValue = "???";
            }
            
            _ErrorDescriptionBuilder desc = new _ErrorDescriptionBuilder(
                    "The value of the \"", settingName,
                    "\" FreeMarker configuration setting is a malformed date/time/dateTime format string: ",
                    new _DelayedJQuote(settingValue), ". Reason given: ",
                    e.getMessage());                    
            throw useTempModelExc ? new _TemplateModelException(e, desc) : new TemplateException(e, desc);
        }
    }

    /**
     * Same as {@link #getTemplateDateFormat(String, int, Class)}, but translates the exceptions to
     * {@link TemplateException}-s.
     */
    TemplateDateFormat getTemplateDateFormat(
            String formatString, int dateType, Class<? extends Date> dateClass,
            ASTExpression blamedDateSourceExp, ASTExpression blamedFormatterExp,
            boolean useTempModelExc)
                    throws TemplateException {
        try {
            return getTemplateDateFormat(formatString, dateType, dateClass);
        } catch (UnknownDateTypeFormattingUnsupportedException e) {
            throw MessageUtils.newCantFormatUnknownTypeDateException(blamedDateSourceExp, e);
        } catch (TemplateValueFormatException e) {
            _ErrorDescriptionBuilder desc = new _ErrorDescriptionBuilder(
                    "Can't invoke date/time/dateTime format based on format string ",
                    new _DelayedJQuote(formatString), ". Reason given: ",
                    e.getMessage())
                    .blame(blamedFormatterExp);
            throw useTempModelExc ? new _TemplateModelException(e, desc) : new TemplateException(e, desc);
        }
    }

    /**
     * Used to get the {@link TemplateDateFormat} according the date/time/dateTime format settings, for the current
     * locale and time zone. See {@link #getTemplateDateFormat(String, int, Locale, TimeZone, boolean)} for the meaning
     * of some of the parameters.
     */
    private TemplateDateFormat getTemplateDateFormat(int dateType, boolean useSQLDTTZ, boolean zonelessInput)
            throws TemplateValueFormatException {
        if (dateType == TemplateDateModel.UNKNOWN) {
            throw new UnknownDateTypeFormattingUnsupportedException();
        }
        int cacheIdx = getTemplateDateFormatCacheArrayIndex(dateType, zonelessInput, useSQLDTTZ);
        TemplateDateFormat[] cachedTempDateFormatArray = this.cachedTempDateFormatArray;
        if (cachedTempDateFormatArray == null) {
            cachedTempDateFormatArray = new TemplateDateFormat[CACHED_TDFS_LENGTH];
            this.cachedTempDateFormatArray = cachedTempDateFormatArray;
        }
        TemplateDateFormat format = cachedTempDateFormatArray[cacheIdx];
        if (format == null) {
            final String formatString;
            switch (dateType) {
            case TemplateDateModel.TIME:
                formatString = getTimeFormat();
                break;
            case TemplateDateModel.DATE:
                formatString = getDateFormat();
                break;
            case TemplateDateModel.DATE_TIME:
                formatString = getDateTimeFormat();
                break;
            default:
                throw new IllegalArgumentException("Invalid date type enum: " + Integer.valueOf(dateType));
            }

            format = getTemplateDateFormat(formatString, dateType, useSQLDTTZ, zonelessInput, false);
            
            cachedTempDateFormatArray[cacheIdx] = format;
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
                        "No custom date format was defined with name " + _StringUtils.jQuote(name));
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
     * {@link _DateUtils#dateToISO8601String(Date, boolean, boolean, boolean, int, TimeZone, DateToISO8601CalendarFactory)}
     * and {@link _DateUtils#dateToXSString(Date, boolean, boolean, boolean, int, TimeZone, DateToISO8601CalendarFactory)}
     * .
     */
    DateToISO8601CalendarFactory getISOBuiltInCalendarFactory() {
        if (isoBuiltInCalendarFactory == null) {
            isoBuiltInCalendarFactory = new _DateUtils.TrivialDateToISO8601CalendarFactory();
        }
        return isoBuiltInCalendarFactory;
    }

    /**
     * Returns the loop or macro nested content parameter variables corresponding to this variable name. Possibly null.
     * (Note that the misnomer is kept for backward compatibility: nested content parameters are not local variables
     * according to our terminology.)
     */
    // TODO [FM3] Don't return nested content params anymore (see JavaDoc)
    public TemplateModel getLocalVariable(String name) throws TemplateModelException {
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
     * <li>A nested content parameter such as {@code x} in {@code <#list xs as x>}
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
            result = configuration.getWrappedSharedVariable(name);
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
     * names of all variables in the current name-space, names of all local variables and nested content parameters. If
     * the passed root data model implements the {@link TemplateHashModelEx} interface, then all names it retrieves
     * through a call to {@link TemplateHashModelEx#keys()} method are returned as well. The method returns a new Set
     * object on each call that is completely disconnected from the Environment. That is, modifying the set will have no
     * effect on the Environment object.
     */
    public Set getKnownVariableNames() throws TemplateModelException {
        // shared vars.
        Set set = configuration.getSharedVariables().keySet();

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
     */
    static void outputInstructionStack(
            ASTElement[] instructionStackSnapshot, boolean terseMode, Writer w) {
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
                    ASTElement stackEl = instructionStackSnapshot[frameIdx];
                    final boolean nestingRelatedElement = (frameIdx > 0 && stackEl instanceof ASTDirNested)
                            || (frameIdx > 1 && instructionStackSnapshot[frameIdx - 1] instanceof ASTDirNested);
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
     */
    ASTElement[] getInstructionStackSnapshot() {
        int requiredLength = 0;
        int ln = instructionStackSize;

        for (int i = 0; i < ln; i++) {
            ASTElement stackEl = instructionStack[i];
            if (i == ln - 1 || stackEl.isShownInStackTrace()) {
                requiredLength++;
            }
        }

        if (requiredLength == 0) return null;

        ASTElement[] result = new ASTElement[requiredLength];
        int dstIdx = requiredLength - 1;
        for (int i = 0; i < ln; i++) {
            ASTElement stackEl = instructionStack[i];
            if (i == ln - 1 || stackEl.isShownInStackTrace()) {
                result[dstIdx--] = stackEl;
            }
        }

        return result;
    }

    static String instructionStackItemToString(ASTElement stackEl) {
        StringBuilder sb = new StringBuilder();
        appendInstructionStackItem(stackEl, sb);
        return sb.toString();
    }

    static void appendInstructionStackItem(ASTElement stackEl, StringBuilder sb) {
        sb.append(MessageUtils.shorten(stackEl.getDescription(), 40));

        sb.append("  [");
        ASTDirMacroOrFunction enclosingMacro = getEnclosingMacro(stackEl);
        if (enclosingMacro != null) {
            sb.append(MessageUtils.formatLocationForEvaluationError(
                    enclosingMacro, stackEl.beginLine, stackEl.beginColumn));
        } else {
            sb.append(MessageUtils.formatLocationForEvaluationError(
                    stackEl.getTemplate(), stackEl.beginLine, stackEl.beginColumn));
        }
        sb.append("]");
    }

    static private ASTDirMacroOrFunction getEnclosingMacro(ASTElement stackEl) {
        while (stackEl != null) {
            if (stackEl instanceof ASTDirMacroOrFunction) return (ASTDirMacroOrFunction) stackEl;
            stackEl = stackEl.getParent();
        }
        return null;
    }

    private void pushLocalContext(LocalContext localContext) {
        if (localContextStack == null) {
            localContextStack = new LocalContextStack();
        }
        localContextStack.push(localContext);
    }

    private void popLocalContext() {
        localContextStack.pop();
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
     * Returns the data-model (also known as the template context in some other template engines).
     */
    public TemplateHashModel getDataModel() {
        final TemplateHashModel result = new TemplateHashModel() {

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public TemplateModel get(String key) throws TemplateModelException {
                TemplateModel value = rootDataModel.get(key);
                if (value == null) {
                    value = configuration.getWrappedSharedVariable(key);
                }
                return value;
            }
        };

        if (rootDataModel instanceof TemplateHashModelEx) {
            return new TemplateHashModelEx() {

                @Override
                public boolean isEmpty() throws TemplateModelException {
                    return result.isEmpty();
                }

                @Override
                public TemplateModel get(String key) throws TemplateModelException {
                    return result.get(key);
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
            };
        }
        return result;
    }

    /**
     * Returns the read-only hash of globally visible variables. This is the correspondent of FTL <code>.globals</code>
     * hash. That is, you see the variables created with <code>&lt;#global ...&gt;</code>, and the variables of the
     * data-model. To invoke new global variables, use {@link #setGlobalVariable setGlobalVariable}.
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
                    result = configuration.getWrappedSharedVariable(key);
                }
                return result;
            }
        };
    }

    private void pushElement(ASTElement element) {
        final int newSize = ++instructionStackSize;
        ASTElement[] instructionStack = this.instructionStack;
        if (newSize > instructionStack.length) {
            final ASTElement[] newInstructionStack = new ASTElement[newSize * 2];
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

    void replaceElementStackTop(ASTElement instr) {
        instructionStack[instructionStackSize - 1] = instr;
    }

    public TemplateNodeModel getCurrentVisitorNode() {
        return currentVisitorNode;
    }

    /**
     * sets TemplateNodeModel as the current visitor node. <tt>.node</tt>
     */
    public void setCurrentVisitorNode(TemplateNodeModel node) {
        currentVisitorNode = node;
    }

    TemplateDirectiveModel getNodeProcessor(TemplateNodeModel node) throws TemplateException {
        String nodeName = node.getNodeName();
        if (nodeName == null) {
            throw new TemplateException(this, "Node name was null.");
        }
        TemplateDirectiveModel result = getNodeProcessor(nodeName, node.getNodeNamespace(), 0);

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

    private TemplateDirectiveModel getNodeProcessor(final String nodeName, final String nsURI, int startIndex)
            throws TemplateException {
        TemplateDirectiveModel result = null;
        int i;
        for (i = startIndex; i < nodeNamespaces.size(); i++) {
            Namespace ns = null;
            try {
                ns = (Namespace) nodeNamespaces.get(i);
            } catch (ClassCastException cce) {
                throw new TemplateException(this,
                        "A \"using\" clause should contain a sequence of namespaces or strings that indicate the "
                                + "location of importable macro libraries.");
            }
            result = getNodeProcessor(ns, nodeName, nsURI);
            if (result != null)
                break;
        }
        if (result != null) {
            nodeNamespaceIndex = i + 1;
            currentNodeName = nodeName;
            currentNodeNS = nsURI;
        }
        return result;
    }

    private TemplateDirectiveModel getNodeProcessor(Namespace ns, String localName, String nsURI)
            throws TemplateException {
        TemplateModel result = null;
        if (nsURI == null) {
            result = ns.get(localName);
            if (!(result instanceof TemplateDirectiveModel)) {
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
                if (!(result instanceof TemplateDirectiveModel)) {
                    result = null;
                }
            } else {
                if (nsURI.isEmpty()) {
                    result = ns.get(Template.NO_NS_PREFIX + ":" + localName);
                    if (!(result instanceof TemplateDirectiveModel)) {
                        result = null;
                    }
                }
                if (nsURI.equals(template.getDefaultNS())) {
                    result = ns.get(Template.DEFAULT_NAMESPACE_PREFIX + ":" + localName);
                    if (!(result instanceof TemplateDirectiveModel)) {
                        result = null;
                    }
                }
                if (result == null) {
                    result = ns.get(localName);
                    if (!(result instanceof TemplateDirectiveModel)) {
                        result = null;
                    }
                }
            }
        }
        return (TemplateDirectiveModel) result;
    }

    /**
     * Emulates <code>include</code> directive, except that <code>name</code> must be template root relative.
     *
     * <p>
     * It's the same as <code>include(getTemplateForInclusion(name, encoding, parse))</code>. But, you may want to
     * separately call these two methods, so you can determine the source of exceptions more precisely, and thus achieve
     * more intelligent error handling.
     *
     * @see #getTemplateForInclusion(String, boolean)
     * @see #include(Template includedTemplate)
     */
    public void include(String name, boolean parse) throws IOException, TemplateException {
        include(getTemplateForInclusion(name, parse));
    }

    /**
     * Same as {@link #getTemplateForInclusion(String, boolean)} with {@code false}
     * {@code ignoreMissing} argument.
     */
    public Template getTemplateForInclusion(String name) throws IOException {
        return getTemplateForInclusion(name, false);
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
     *            {@link TemplateResolver#toRootBasedName(String, String)} to convert paths to template root based
     *            paths.) For more details see the identical parameter of
     *            {@link Configuration#getTemplate(String, Locale, Serializable, boolean)}
     *
     * @param ignoreMissing
     *            See identical parameter of {@link Configuration#getTemplate(String, Locale, Serializable, boolean)}
     * 
     * @return Same as {@link Configuration#getTemplate(String, Locale, Serializable, boolean)}
     * @throws IOException
     *             Same as exceptions thrown by
     *             {@link Configuration#getTemplate(String, Locale, Serializable, boolean)}
     */
    public Template getTemplateForInclusion(String name, boolean ignoreMissing)
            throws IOException {
        return configuration.getTemplate(name, getLocale(), getIncludedTemplateCustomLookupCondition(), ignoreMissing);
    }

    private Serializable getIncludedTemplateCustomLookupCondition() {
        return getCurrentTemplate().getCustomLookupCondition();
    }

    /**
     * Processes a Template in the context of this <code>Environment</code>, including its output in the
     * <code>Environment</code>'s Writer.
     *
     * @param includedTemplate
     *            the template to process. Note that it does <em>not</em> need to be a template returned by
     *            {@link #getTemplateForInclusion(String, boolean)}.
     */
    public void include(Template includedTemplate)
            throws TemplateException, IOException {
        final Template prevTemplate;

        importMacros(includedTemplate);
        visit(includedTemplate.getRootASTNode());
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
     *            {@link #getTemplateForImporting(String name)}.
     * @param targetNsVarName
     *            The name of the FTL variable that will store the namespace.
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
     *            {@link TemplateResolver#toRootBasedName(String, String)} to convert paths to template root based
     *            paths.)
     */
    public Template getTemplateForImporting(String name) throws IOException {
        return getTemplateForInclusion(name, true);
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
            templateName = loadedTemplate.getLookupName();
        } else {
            lazyImport = true;
            // We can't cause a template lookup here (see TemplateLookupStrategy), as that can be expensive. We exploit
            // that (at least in 2.3.x) the name used for eager import namespace key isn't the template.sourceName, but
            // the looked up name (template.name), which we can get quickly:
            templateName = getConfiguration().getTemplateResolver().normalizeRootBasedName(templateName);
        }
        
        if (loadedLibs == null) {
            loadedLibs = new HashMap();
        }
        final Namespace existingNamespace = loadedLibs.get(templateName);
        if (existingNamespace != null) {
            if (targetNsVarName != null) {
                setVariable(targetNsVarName, existingNamespace);
                if (currentNamespace == mainNamespace) {
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
        Namespace prevNamespace = currentNamespace;
        currentNamespace = newNamespace;
        Writer prevOut = out;
        out = _NullWriter.INSTANCE;
        try {
            include(loadedTemplate);
        } finally {
            out = prevOut;
            currentNamespace = prevNamespace;
        }
    }

    /**
     * Resolves a reference to a template (like the one used in {@code #include} or {@code #import}), assuming a base
     * name. This gives a full (that is, absolute), even if non-normalized template name, that could be used for
     * {@link Configuration#getTemplate(String)}. This is mostly used when a template refers to another template.
     *
     * @param baseName
     *            The name to which relative {@code targetName}-s are relative to. Maybe {@code null} (happens when
     *            resolving names in nameless templates), which means that the base is the root "directory", and so
     *            the {@code targetName} is returned without change. Assuming the
     *            {@link Configuration#getTemplateNameFormat() templateNameFormat} is
     *            {@link DefaultTemplateNameFormat#INSTANCE}, the rules are as follows. If you want to specify a base
     *            directory here, it must end with {@code "/"}. If it doesn't end with {@code "/"}, it's treated as
     *            a file name and so its parent directory will be used as the base path. Might starts with a scheme
     *            part (like {@code "foo://"}, or even just {@code "foo:"}).
     * @param targetName
     *            The name of the template, which is either a relative or absolute name. Assuming the
     *            {@link Configuration#getTemplateNameFormat() templateNameFormat} is
     *            {@link DefaultTemplateNameFormat#INSTANCE}, the rules are as follows. If it starts with {@code "/"}
     *            or contains a scheme part separator (a {@code ":"} with no {@code "/"} anywhere before it)
     *            then it's an absolute name, otherwise it's a relative path. Relative paths are interpreted
     *            relatively to the {@code baseName}. Absolute names are simply returned as is, ignoring the {@code
     *            baseName}, except, when the {@code baseName} has scheme part while the {@code targetName} doesn't
     *            have it, then the schema of the {@code baseName} is prepended to the {@code targetName}.
     */
    public String toFullTemplateName(String baseName, String targetName)
            throws MalformedTemplateNameException {
        if (baseName == null) {
            return targetName;
        }
        return configuration.getTemplateResolver().toRootBasedName(baseName, targetName);
    }

    void importMacros(Template template) {
        for (Object macro : template.getMacros().values()) {
            visitMacroOrFunctionDefinition((ASTDirMacroOrFunction) macro);
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

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCustomState(CustomStateKey<T> customStateKey) {
        if (customStateMap == null) {
            customStateMap = new IdentityHashMap<>();
        }
        T customState = (T) customStateMap.get(customStateKey);
        if (customState == null) {
            customState = customStateKey.create();
            if (customState == null) {
                throw new IllegalStateException("CustomStateKey.create() must not return null (for key: "
                        + customStateKey + ")");
            }
            customStateMap.put(customStateKey, customState);
        }
        return customState;
    }

    /**
     * Convenience method that simply delegates to {@link Configuration#getObjectWrapper()}.
     */
    public ObjectWrapper getObjectWrapper() {
        return getConfiguration().getObjectWrapper();
    }

    public class Namespace extends SimpleHash {

        private Template template;

        // TODO [FM3] #macro etc. uses this, so the NS is associated to the main temp., even if #macro is elsewhere.
        Namespace() {
            this(Environment.this.getMainTemplate());
        }

        Namespace(Template template) {
            super(Environment.this.getObjectWrapper());
            this.template = template;
        }

        /**
         * @return the Template object with which this Namespace is associated.
         */
        public Template getTemplate() {
            return template == null ? Environment.this.getMainTemplate() : template;
        }

        /**
         * Used when initializing a lazily initialized namespace.
         */
        void setTemplate(Template template) {
            if (this.template != null) {
                throw new IllegalStateException("Can't change the template of a namespace once it was established.");
            }
            this.template = template;
        }
        
    }
    
    private enum InitializationStatus {
        UNINITIALIZED, INITIALIZING, INITIALIZED, FAILED
    }
    
    class LazilyInitializedNamespace extends Namespace {
        
        private final String templateName;
        private final Locale locale;
        private final Serializable customLookupCondition;
        
        private InitializationStatus status = InitializationStatus.UNINITIALIZED;
        
        /**
         * @param templateName
         *            Must be root relative
         */
        private LazilyInitializedNamespace(String templateName) {
            super(null);
            
            this.templateName = templateName;
            // Make snapshot of all settings that influence template resolution:
            locale = getLocale();
            customLookupCondition = getIncludedTemplateCustomLookupCondition();
        }

        private void ensureInitializedTME() throws TemplateModelException {
            if (status != InitializationStatus.INITIALIZED && status != InitializationStatus.INITIALIZING) {
                if (status == InitializationStatus.FAILED) {
                    throw new TemplateModelException(
                            "Lazy initialization of the imported namespace for "
                            + _StringUtils.jQuote(templateName)
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
                            + _StringUtils.jQuote(templateName)
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
            setTemplate(configuration.getTemplate(templateName, locale, customLookupCondition, false));
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

    /**
     * Superclass of {@link TemplateCallableModel}-s implemented in the template language.
     */
    abstract class TemplateLanguageCallable implements TemplateCallableModel, TemplateModelWithOriginName {
        final ASTDirMacroOrFunction callableDefinition;
        private final Namespace namespace;

        public TemplateLanguageCallable(ASTDirMacroOrFunction callableDefinition, Namespace namespace) {
            this.callableDefinition = callableDefinition;
            this.namespace = namespace;
        }

        @Override
        public String getOriginName() {
            String sourceName = callableDefinition.getTemplate().getSourceName();
            return sourceName != null ? sourceName + ":" + callableDefinition.getName()
                    : callableDefinition.getName();
        }

        protected void genericExecute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
                throws TemplateException, IOException {
            pushElement(callableDefinition);
            try {
                final ASTDirMacroOrFunction.Context macroCtx = callableDefinition.new Context(
                        this, callPlace, Environment.this);

                final ASTDirMacroOrFunction.Context prevMacroCtx = currentMacroContext;
                currentMacroContext = macroCtx;

                final LocalContextStack prevLocalContextStack = localContextStack;
                localContextStack = null;

                final Namespace prevNamespace = currentNamespace;
                currentNamespace = namespace;

                try {
                    // Note: Default expressions are evaluated here, so namespace, stack, etc. must be already set
                    setLocalsFromArguments(macroCtx, args);

                    visit(callableDefinition.getChildBuffer(), out);
                } catch (ASTDirReturn.Return re) {
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

        abstract boolean isFunction();

        private void setLocalsFromArguments(ASTDirMacroOrFunction.Context macroCtx, TemplateModel[] args)
                throws TemplateException {
            ASTDirMacroOrFunction.ParameterDefinition[] paramDefsByArgIdx =
                    callableDefinition.getParameterDefinitionByArgumentArrayIndex();
            for (int argIdx = 0; argIdx < args.length; argIdx++) {
                TemplateModel arg = args[argIdx];
                ASTDirMacroOrFunction.ParameterDefinition paramDef = paramDefsByArgIdx[argIdx];
                if (arg == null) { // TODO [FM3] FM2 doesn't differentiate omitted and null, but FM3 will.
                    ASTExpression defaultExp = paramDef.getDefaultExpression();
                    if (defaultExp != null) {
                        arg = defaultExp.eval(Environment.this);
                        if (arg == null) {
                            throw InvalidReferenceException.getInstance(defaultExp, Environment.this);
                        }
                    } else {
                        // TODO [FM3] Had to give different messages depending on if the argument was omitted, or if
                        // it was null, but this will be fixed with the null related refactoring.
                        throw new TemplateException(Environment.this,
                                new _ErrorDescriptionBuilder(
                                        _CallableUtils.getMessageArgumentProblem(
                                                this, argIdx,
                                                " can't be null or omitted.",
                                                isFunction())
                                )
                                .tip("If the parameter value expression on the caller side is known to "
                                        + "be legally null/missing, you may want to specify a default "
                                        + "value for it on the caller side with the \"!\" operator, like "
                                        + "paramValue!defaultValue.")
                                .tip("If the parameter was omitted on the caller side, and the omission was "
                                        + "deliberate, you may consider making the parameter optional in the macro "
                                        + "by specifying a default value for it, like <#macro macroName "
                                        + "paramName=defaultExpr>."
                                )
                        );
                    }
                }
                macroCtx.setLocalVar(paramDef.getName(), arg);
            }
        }

        ASTDirMacroOrFunction getCallableDefinition() {
            return callableDefinition;
        }

        Namespace getNamespace() {
            return namespace;
        }
    }

    /**
     * {@link TemplateDirectiveModel} implemented in the template language (such as with the {@code #macro} directive).
     * This is the value that {@code #macro} creates on runtime, not the {@code #macro} directive itself.
     */
    final class TemplateLanguageDirective extends TemplateLanguageCallable implements TemplateDirectiveModel {

        TemplateLanguageDirective(ASTDirMacroOrFunction macroDef, Namespace namespace) {
            super(macroDef, namespace);
        }

        @Override
        public void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
                throws IOException, TemplateException {
            if (getCallableDefinition() == ASTDirMacroOrFunction.PASS_MACRO) {
                return;
            }
            genericExecute(args, callPlace, out, env);
        }

        @Override
        public boolean isNestedContentSupported() {
            // TODO [FM3] We should detect if #nested is called anywhere (also maybe something like
            // `<#macro m{supportsNested[=true|false]}>`) should be added.
            return true;
        }

        @Override
        public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
            return callableDefinition.getArgumentArrayLayout();
        }

        boolean isFunction() {
            return false;
        }

    }

    /**
     * {@link TemplateFunctionModel} implemented in the template language (such as with the {@code #function}
     * directive). This is the value that {@code #function} creates on runtime, not the {@code #macro} directive itself.
     */
    final class TemplateLanguageFunction extends TemplateLanguageCallable implements TemplateFunctionModel {

        public TemplateLanguageFunction(ASTDirMacroOrFunction callableDefinition, Namespace namespace) {
            super(callableDefinition, namespace);
        }

        @Override
        public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                throws TemplateException {
            env.setLastReturnValue(null);
            try {
                genericExecute(args, callPlace, _NullWriter.INSTANCE, env);
            } catch (IOException e) {
                // Should not occur
                throw new TemplateException("Unexpected exception during function execution", e, env);
            }
            return env.getLastReturnValue();
        }

        @Override
        public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
            return callableDefinition.getArgumentArrayLayout();
        }

        boolean isFunction() {
            return true;
        }

    }

}
