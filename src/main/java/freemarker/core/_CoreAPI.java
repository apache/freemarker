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

package freemarker.core;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateDirectiveBody;


/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */ 
public class _CoreAPI {
    
    public static final String ERROR_MESSAGE_HR = "----";

    // Can't be instantiated
    private _CoreAPI() { }
    
    public static final Set/*<String>*/ BUILT_IN_DIRECTIVE_NAMES;
    static {
        Set/*<String>*/ names = new TreeSet();
        names.add("assign");
        names.add("attempt");
        names.add("autoEsc");
        names.add("autoesc");
        names.add("break");
        names.add("call");
        names.add("case");
        names.add("comment");
        names.add("compress");
        names.add("default");
        names.add("else");
        names.add("elseif");
        names.add("elseIf");
        names.add("escape");
        names.add("fallback");
        names.add("flush");
        names.add("foreach");
        names.add("forEach");
        names.add("ftl");
        names.add("function");
        names.add("global");
        names.add("if");
        names.add("import");
        names.add("include");
        names.add("items");
        names.add("list");
        names.add("local");
        names.add("lt");
        names.add("macro");
        names.add("nested");
        names.add("noautoesc");
        names.add("noAutoEsc");
        names.add("noescape");
        names.add("noEscape");
        names.add("noparse");
        names.add("noParse");
        names.add("nt");
        names.add("outputformat");
        names.add("outputFormat");
        names.add("recover");
        names.add("recurse");
        names.add("return");
        names.add("rt");
        names.add("sep");
        names.add("setting");
        names.add("stop");
        names.add("switch");
        names.add("t");
        names.add("transform");
        names.add("visit");
        BUILT_IN_DIRECTIVE_NAMES = Collections.unmodifiableSet(names);
    }
    
    /**
     * Returns the names of the currently supported "built-ins" ({@code expr?builtin_name}-like things).
     * @return {@link Set} of {@link String}-s. 
     */
    public static Set/*<String>*/ getSupportedBuiltInNames() {
        return Collections.unmodifiableSet(BuiltIn.builtins.keySet());
    }
    
    public static void appendInstructionStackItem(TemplateElement stackEl, StringBuilder sb) {
        Environment.appendInstructionStackItem(stackEl, sb);
    }
    
    public static TemplateElement[] getInstructionStackSnapshot(Environment env) {
        return env.getInstructionStackSnapshot();
    }
    
    public static void outputInstructionStack(
            TemplateElement[] instructionStackSnapshot, boolean terseMode, Writer pw) {
        Environment.outputInstructionStack(instructionStackSnapshot, terseMode, pw);
    }

    public static Map<String, ?> getCustomAttributes(UnboundTemplate unboundTemplate) {
        return unboundTemplate.getCustomAttributes();
    }
    
    /**
     * For emulating legacy {@link Template#addMacro(Macro)}.
     */
    public static void addMacro(UnboundTemplate unboundTemplate, Macro macro) {
        final UnboundCallable unboundCallable = macroToUnboundCallable(macro);
        unboundTemplate.addUnboundCallable(unboundCallable);
    }

    /**
     * In 2.4 {@link Macro} was split to {@link BoundCallable} and {@link UnboundCallable}, but because of BC
     * constraints sometimes we can only expect a {@link Macro}, but that can always converted to
     * {@link UnboundCallable}.
     */
    private static UnboundCallable macroToUnboundCallable(Macro macro) {
        if (macro instanceof UnboundCallable) {
            // It's coming from the AST:
            return (UnboundCallable) macro;
        } else if (macro instanceof BoundCallable) {
            // It's coming from an FTL variable:
            return ((BoundCallable) macro).getUnboundCallable(); 
        } else if (macro == null) {
            return null;
        } else {
            // Impossible, Macro should have only two subclasses.
            throw new BugException();
        }
    }

    public static void addImport(UnboundTemplate unboundTemplate, LibraryLoad libLoad) {
        unboundTemplate.addImport(libLoad);
    }
    
    public static UnboundTemplate newUnboundTemplate(Reader reader, String sourceName,
            Configuration cfg, ParserConfiguration parserCfg, String assumedEncoding) throws IOException {
        return new UnboundTemplate(reader, sourceName, cfg, parserCfg, assumedEncoding);
    }
    
    public static boolean isBoundCallable(Object obj) {
        return obj instanceof BoundCallable;
    }

    public static UnboundTemplate newPlainTextUnboundTemplate(String content, String sourceName, Configuration config) {
        return UnboundTemplate.newPlainTextUnboundTemplate(content, sourceName, config);
    }
    
    /** Used for implementing the deprecated {@link Template} method with similar name. */
    public static TemplateElement getRootTreeNode(UnboundTemplate unboundTemplate) {
        return unboundTemplate.getRootTreeNode();
    }
    
    /** Used for implementing the deprecated {@link Template} method with similar name. */
    public static Map<String, UnboundCallable> getUnboundCallables(UnboundTemplate unboundTemplate) {
        return unboundTemplate.getUnboundCallables();
    }

    /** Used for implementing the deprecated {@link Template} method with similar name. */
    public static List getImports(UnboundTemplate unboundTemplate) {
        return unboundTemplate.getImports();
    }
    
    /** Used for implementing the deprecated {@link Template} method with similar name. */
    public static void addPrefixNSMapping(UnboundTemplate unboundTemplate, String prefix, String nsURI) {
        unboundTemplate.addPrefixToNamespaceURIMapping(prefix, nsURI);
    }
    
    /** Used for implementing the deprecated {@link Template} method with similar name. */
    public static List<TemplateElement> containingElements(UnboundTemplate unboundTemplate, int column, int line) {
        return unboundTemplate.containingElements(column, line);
    }
    
    /**
     * ATTENTION: This is used by https://github.com/kenshoo/freemarker-online. Don't break backward
     * compatibility without updating that project too! 
     */
    static final public void addThreadInterruptedChecks(Template template) {
        try {
            new ThreadInterruptionSupportTemplatePostProcessor().postProcess(template);
        } catch (TemplatePostProcessorException e) {
            throw new RuntimeException("Template post-processing failed", e);
        }
    }
    
    static final public void checkHasNoNestedContent(TemplateDirectiveBody body)
            throws NestedContentNotSupportedException {
        NestedContentNotSupportedException.check(body);
    }

    public static Map<String, Macro> createAdapterMacroMapForUnboundCallables(UnboundTemplate unboundTemplate) {
        return new AdapterMacroMap(getUnboundCallables(unboundTemplate));
    }
    
    /**
     * Wraps a {@code Map<String, UnboundCallable>} as if it was a {@code Map<String, Macro>}. This is for backward
     * compatibility. The important use case is being able to put any {@link Macro} subclass into this {@code Map},
     * despite that the backing {@link Macro} can only store {@code UnboundCallable}. Reading works a bit strangely,
     * because if you put a non-{@link UnboundCallable} value in, then get it with the same key, you get the
     * corresponding {@link UnboundCallable} back instead of the original object. That's also a {@code Macro} though. 
     */
    private static class AdapterMacroMap implements Map<String, Macro> {
        
        private final Map<String, UnboundCallable> adapted;

        public AdapterMacroMap(Map<String, UnboundCallable> unboundCallableMap) {
            this.adapted = unboundCallableMap;
        }

        public int size() {
            return adapted.size();
        }

        public boolean isEmpty() {
            return adapted.isEmpty();
        }

        public boolean containsKey(Object key) {
            return adapted.containsKey(key);
        }

        public boolean containsValue(Object value) {
            return adapted.containsValue(value);
        }

        public UnboundCallable get(Object key) {
            return adapted.get(key);
        }

        public UnboundCallable put(String key, Macro value) {
            return adapted.put(key, macroToUnboundCallable(value));
        }

        public UnboundCallable remove(Object key) {
            return adapted.remove(key);
        }

        public void putAll(Map<? extends String, ? extends Macro> t) {
            for (Map.Entry<? extends String, ? extends Macro> ent : t.entrySet()) {
                put(ent.getKey(), ent.getValue());
            }
        }

        public void clear() {
            adapted.clear();
        }

        public Set<String> keySet() {
            return adapted.keySet();
        }

        public Collection<Macro> values() {
            // According the Map API, this Collection doesn't allows adding elements, it's safe to treat as a
            // Collection<Macro>.
            return (Collection) adapted.values();
        }

        public Set<Entry<String, Macro>> entrySet() {
            // According the Map API, this set doesn't allows adding elements, but, the Map.Entry-s are still
            // modifiable.
            return new AdapterMacroMapEntrySet(adapted.entrySet());
        }

        @Override
        public boolean equals(Object o) {
            return adapted.equals(o);
        }

        @Override
        public int hashCode() {
            return adapted.hashCode();
        }
        
    }
    
    /** Helper for {@link AdapterMacroMap}. */
    private static class AdapterMacroMapEntrySet implements Set<Map.Entry<String, Macro>> {
        
        private final Set<Map.Entry<String, UnboundCallable>> adapted;

        public AdapterMacroMapEntrySet(Set<Entry<String, UnboundCallable>> adapted) {
            this.adapted = adapted;
        }

        public int size() {
            return adapted.size();
        }

        public boolean isEmpty() {
            return adapted.isEmpty();
        }

        public boolean contains(Object o) {
            return adapted.contains(o);
        }

        public Iterator<Entry<String, Macro>> iterator() {
            return new AdapterMacroMapEntrySetIterator(adapted.iterator());
        }

        public Object[] toArray() {
            return adapted.toArray();
        }

        public <T> T[] toArray(T[] a) {
            return adapted.toArray(a);
        }

        public boolean add(Entry<String, Macro> o) {
            // Won't be allowed anyway
            return adapted.add((Entry) o);
        }

        public boolean remove(Object o) {
            return adapted.remove(o);
        }

        public boolean containsAll(Collection<?> c) {
            return adapted.containsAll(c);
        }

        public boolean addAll(Collection<? extends Entry<String, Macro>> c) {
            // Won't be allowed anyway
            return adapted.addAll((Collection) c);
        }

        public boolean retainAll(Collection<?> c) {
            return adapted.retainAll(c);
        }

        public boolean removeAll(Collection<?> c) {
            return adapted.removeAll(c);
        }

        public void clear() {
            adapted.clear();
        }

        @Override
        public boolean equals(Object o) {
            return adapted.equals(o);
        }

        @Override
        public int hashCode() {
            return adapted.hashCode();
        }
        
    }
    
    /** Helper for {@link AdapterMacroMap}. */
    private static class AdapterMacroMapEntrySetIterator implements Iterator<Map.Entry<String, Macro>> {
        
        private final Iterator<Map.Entry<String, UnboundCallable>> adapted;

        public AdapterMacroMapEntrySetIterator(Iterator<Entry<String, UnboundCallable>> adapted) {
            this.adapted = adapted;
        }

        public boolean hasNext() {
            return adapted.hasNext();
        }

        public Entry<String, Macro> next() {
            return new AdapterMacroMapEntry(adapted.next());
        }

        public void remove() {
            adapted.remove();
        }
        
    }
    
    /** Helper for {@link AdapterMacroMap}. */
    private static class AdapterMacroMapEntry implements Map.Entry<String, Macro> {
        
        private final Map.Entry<String, UnboundCallable> adapted;

        public AdapterMacroMapEntry(Entry<String, UnboundCallable> adapted) {
            this.adapted = adapted;
        }

        public String getKey() {
            return adapted.getKey();
        }

        public UnboundCallable getValue() {
            return adapted.getValue();
        }

        public UnboundCallable setValue(Macro value) {
            return adapted.setValue(macroToUnboundCallable(value));
        }

        @Override
        public boolean equals(Object o) {
            return adapted.equals(o);
        }

        @Override
        public int hashCode() {
            return adapted.hashCode();
        }
        
    }
    
}
