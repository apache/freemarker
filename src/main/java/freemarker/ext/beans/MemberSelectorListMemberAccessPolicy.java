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

package freemarker.ext.beans;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.NullArgumentException;

/**
 * Superclass for member-selector-list-based member access policies, like {@link WhitelistMemberAccessPolicy}.
 *
 * <p>There are two ways you can add members to the member selector list:
 * <ul>
 *     <li>Via a list of member selectors passed to the constructor
 *     <li>Via annotation (concrete type depends on subclass)
 * </ul>
 *
 * <p>Members are identified with the following data (with the example of
 * {@code com.example.MyClass.myMethod(int, int)}):
 * <ul>
 *    <li>Upper bound class ({@code com.example.MyClass} in the example)
 *    <li>Member name ({@code myMethod} in the example), except for constructors where it's unused
 *    <li>Parameter types ({@code int, int} in the example), except for fields where it's unused
 * </ul>
 *
 * <p>If a method or field is matched in the upper bound type, it will be automatically matched in all subtypes of that.
 * It's called "upper bound" type, because the member will only be matched in classes that are {@code instanceof}
 * the upper bound class. That restriction stands even if the member was inherited from another type (class or
 * interface), and it wasn't even overridden in the upper bound type; the member won't be matched in the
 * type where it was inherited from, if that type is more generic than the upper bound type.
 *
 * <p>The above inheritance rule doesn't apply to constructors. That's consistent with the fact constructors aren't
 * inherited in Java (or pretty much any other language). So for example, if you add {@code com.example.A.A()} to
 * the member selector list, and {@code B extends A}, then {@code com.example.B.B()} is still not matched by that list.
 * If you want it to be matched, you have to add {@code com.example.B.B()} to list explicitly.
 *
 * <p>Note that the return type of methods aren't used in any way. If {@code myMethod(int, int)} has multiple variants
 * with different return types (which is possible on the bytecode level) but the same parameter types, then all
 * variants of it is matched, or none is. Similarly, the type of fields isn't used either, only the name of the field
 * matters.
 *
 * @since 2.3.30
 */
public abstract class MemberSelectorListMemberAccessPolicy implements MemberAccessPolicy {
    enum ListType {
        /** Only matched members will be exposed. */
        WHITELIST,
        /** Matched members will not be exposed. */
        BLACKLIST
    }

    private final ListType listType;
    private final MethodMatcher methodMatcher;
    private final ConstructorMatcher constructorMatcher;
    private final FieldMatcher fieldMatcher;
    private final Class<? extends Annotation> matchAnnotation;

    /**
     * A condition that matches some type members. See {@link MemberSelectorListMemberAccessPolicy} documentation for more.
     * Exactly one of these will be non-{@code null}:
     * {@link #getMethod()}, {@link #getConstructor()}, {@link #getField()}.
     *
     * @since 2.3.30
     */
    public final static class MemberSelector {
        private final Class<?> upperBoundType;
        private final Method method;
        private final Constructor<?> constructor;
        private final Field field;

        /**
         * Use if you want to match methods similar to the specified one, in types that are {@code instanceof} of
         * the specified upper bound type. When methods are matched, only the name and the parameter types matter.
         */
        public MemberSelector(Class<?> upperBoundType, Method method) {
            NullArgumentException.check("upperBoundType", upperBoundType);
            NullArgumentException.check("method", method);
            this.upperBoundType = upperBoundType;
            this.method = method;
            this.constructor = null;
            this.field = null;
        }

        /**
         * Use if you want to match constructors similar to the specified one, in types that are {@code instanceof} of
         * the specified upper bound type. When constructors are matched, only the parameter types matter.
         */
        public MemberSelector(Class<?> upperBoundType, Constructor<?> constructor) {
            NullArgumentException.check("upperBoundType", upperBoundType);
            NullArgumentException.check("constructor", constructor);
            this.upperBoundType = upperBoundType;
            this.method = null;
            this.constructor = constructor;
            this.field = null;
        }

        /**
         * Use if you want to match fields similar to the specified one, in types that are {@code instanceof} of
         * the specified upper bound type. When fields are matched, only the name matters.
         */
        public MemberSelector(Class<?> upperBoundType, Field field) {
            NullArgumentException.check("upperBoundType", upperBoundType);
            NullArgumentException.check("field", field);
            this.upperBoundType = upperBoundType;
            this.method = null;
            this.constructor = null;
            this.field = field;
        }

        /**
         * Not {@code null}.
         */
        public Class<?> getUpperBoundType() {
            return upperBoundType;
        }

        /**
         * Maybe {@code null};
         * set if the selector matches methods similar to the returned one.
         */
        public Method getMethod() {
            return method;
        }

        /**
         * Maybe {@code null};
         * set if the selector matches constructors similar to the returned one.
         */
        public Constructor<?> getConstructor() {
            return constructor;
        }

        /**
         * Maybe {@code null};
         * set if the selector matches fields similar to the returned one.
         */
        public Field getField() {
            return field;
        }

        /**
         * Parses a member selector that was specified with a string.
         *
         * @param classLoader
         *      Used to resolve class names in the member selectors. Generally you want to pick a class that belongs to
         *      you application (not to a 3rd party library, like FreeMarker), and then call
         *      {@link Class#getClassLoader()} on that. Note that the resolution of the classes is not lazy, and so the
         *      {@link ClassLoader} won't be stored after this method returns.
         * @param memberSelectorString
         *      Describes the member (method, constructor, field) which you want to whitelist. Starts with the full
         *      qualified name of the member, like {@code com.example.MyClass.myMember}. Unless it's a field, the
         *      name is followed by comma separated list of the parameter types inside parentheses, like in
         *      {@code com.example.MyClass.myMember(java.lang.String, boolean)}. The parameter type names must be
         *      also full qualified names, except primitive type names. Array types must be indicated with one or
         *      more {@code []}-s after the type name. Varargs arguments shouldn't be marked with {@code ...}, but with
         *      {@code []}. In the member name, like {@code com.example.MyClass.myMember}, the class refers to the so
         *      called "upper bound class". Regarding that and inheritance rules see the class level documentation.
         *
         * @throws ClassNotFoundException If a type referred in the member selector can't be found.
         * @throws NoSuchMethodException If the method or constructor referred in the member selector can't be found.
         * @throws NoSuchFieldException If the field referred in the member selector can't be found.
         */
        public static MemberSelector parse(String memberSelectorString, ClassLoader classLoader) throws
                ClassNotFoundException, NoSuchMethodException, NoSuchFieldException {
            if (memberSelectorString.contains("<") || memberSelectorString.contains(">")
                    || memberSelectorString.contains("...") || memberSelectorString.contains(";")) {
                throw new IllegalArgumentException(
                        "Malformed whitelist entry (shouldn't contain \"<\", \">\", \"...\", or \";\"): "
                                + memberSelectorString);
            }
            String cleanedStr = memberSelectorString.trim().replaceAll("\\s*([\\.,\\(\\)\\[\\]])\\s*", "$1");

            int postMemberNameIdx;
            boolean hasArgList;
            {
                int openParenIdx = cleanedStr.indexOf('(');
                hasArgList = openParenIdx != -1;
                postMemberNameIdx = hasArgList ? openParenIdx : cleanedStr.length();
            }

            final int postClassDotIdx = cleanedStr.lastIndexOf('.', postMemberNameIdx);
            if (postClassDotIdx == -1) {
                throw new IllegalArgumentException("Malformed whitelist entry (missing dot): " + memberSelectorString);
            }

            Class<?> upperBoundClass;
            String upperBoundClassStr = cleanedStr.substring(0, postClassDotIdx);
            if (!isWellFormedClassName(upperBoundClassStr)) {
                throw new IllegalArgumentException("Malformed whitelist entry (malformed upper bound class name): "
                        + memberSelectorString);
            }
            upperBoundClass = classLoader.loadClass(upperBoundClassStr);

            String memberName = cleanedStr.substring(postClassDotIdx + 1, postMemberNameIdx);
            if (!isWellFormedJavaIdentifier(memberName)) {
                throw new IllegalArgumentException(
                        "Malformed whitelist entry (malformed member name): " + memberSelectorString);
            }

            if (hasArgList) {
                if (cleanedStr.charAt(cleanedStr.length() - 1) != ')') {
                    throw new IllegalArgumentException("Malformed whitelist entry (should end with ')'): "
                            + memberSelectorString);
                }
                String argsSpec = cleanedStr.substring(postMemberNameIdx + 1, cleanedStr.length() - 1);
                StringTokenizer tok = new StringTokenizer(argsSpec, ",");
                int argCount = tok.countTokens();
                Class<?>[] argTypes = new Class[argCount];
                for (int i = 0; i < argCount; i++) {
                    String argClassName = tok.nextToken();
                    int arrayDimensions = 0;
                    while (argClassName.endsWith("[]")) {
                        arrayDimensions++;
                        argClassName = argClassName.substring(0, argClassName.length() - 2);
                    }
                    Class<?> argClass;
                    Class<?> primArgClass = ClassUtil.resolveIfPrimitiveTypeName(argClassName);
                    if (primArgClass != null) {
                        argClass = primArgClass;
                    } else {
                        if (!isWellFormedClassName(argClassName)) {
                            throw new IllegalArgumentException(
                                    "Malformed whitelist entry (malformed argument class name): " + memberSelectorString);
                        }
                        argClass = classLoader.loadClass(argClassName);
                    }
                    argTypes[i] = ClassUtil.getArrayClass(argClass, arrayDimensions);
                }
                return memberName.equals(upperBoundClass.getSimpleName())
                        ? new MemberSelector(upperBoundClass, upperBoundClass.getConstructor(argTypes))
                        : new MemberSelector(upperBoundClass, upperBoundClass.getMethod(memberName, argTypes));
            } else {
                return new MemberSelector(upperBoundClass, upperBoundClass.getField(memberName));
            }
        }

        /**
         * Convenience method to parse all member selectors in the collection (see {@link #parse(String, ClassLoader)}),
         * while also filtering out blank and comment lines; see {@link #parse(String, ClassLoader)},
         * and {@link #isIgnoredLine(String)}.
         *
         * @param ignoreMissingClassOrMember If {@code true}, members selectors that throw exceptions because the
         *      referred type or member can't be found will be silently ignored. If {@code true}, same exceptions
         *      will be just thrown by this method.
         */
        public static List<MemberSelector> parse(
                Collection<String> memberSelectors, boolean ignoreMissingClassOrMember, ClassLoader classLoader)
                throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException {
            List<MemberSelector> parsedMemberSelectors = new ArrayList<>(memberSelectors.size());
            for (String memberSelector : memberSelectors) {
                if (!isIgnoredLine(memberSelector)) {
                    try {
                        parsedMemberSelectors.add(parse(memberSelector, classLoader));
                    } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException e) {
                        if (!ignoreMissingClassOrMember) {
                            throw e;
                        }
                    }
                }
            }
            return parsedMemberSelectors;
        }

        /**
         * A line is ignored if it's blank or a comment. A line is be blank if it doesn't contain non-whitespace
         * character. A line is a comment if it starts with {@code #}, or {@code //} (ignoring any amount of
         * preceding whitespace).
         */
        public static boolean isIgnoredLine(String line) {
            String trimmedLine = line.trim();
            return trimmedLine.length() == 0 || trimmedLine.startsWith("#") || trimmedLine.startsWith("//");
        }
    }

    /**
     * @param memberSelectors
     *      List of member selectors; see {@link MemberSelectorListMemberAccessPolicy} class-level documentation for
     *      more.
     * @param listType
     *      Decides the "color" of the list
     * @param matchAnnotation
     */
    MemberSelectorListMemberAccessPolicy(
            Collection<? extends MemberSelector> memberSelectors, ListType listType,
            Class<? extends Annotation> matchAnnotation) {
        this.listType = listType;
        this.matchAnnotation = matchAnnotation;

        methodMatcher = new MethodMatcher();
        constructorMatcher = new ConstructorMatcher();
        fieldMatcher = new FieldMatcher();
        for (MemberSelector memberSelector : memberSelectors) {
            Class<?> upperBoundClass = memberSelector.upperBoundType;
            if (memberSelector.constructor != null) {
                constructorMatcher.addMatching(upperBoundClass, memberSelector.constructor);
            } else if (memberSelector.method != null) {
                methodMatcher.addMatching(upperBoundClass, memberSelector.method);
            } else if (memberSelector.field != null) {
                fieldMatcher.addMatching(upperBoundClass, memberSelector.field);
            } else {
                throw new AssertionError();
            }
        }
    }

    @Override
    public final ClassMemberAccessPolicy forClass(final Class<?> contextClass) {
        return new ClassMemberAccessPolicy() {
            @Override
            public boolean isMethodExposed(Method method) {
                return matchResultToIsExposedResult(
                        methodMatcher.matches(contextClass, method)
                        || matchAnnotation != null
                                && _MethodUtil.getInheritableAnnotation(contextClass, method, matchAnnotation)
                                        != null);
            }

            @Override
            public boolean isConstructorExposed(Constructor<?> constructor) {
                return matchResultToIsExposedResult(
                        constructorMatcher.matches(contextClass, constructor)
                        || matchAnnotation != null
                                && _MethodUtil.getInheritableAnnotation(contextClass, constructor, matchAnnotation)
                                        != null);
            }

            @Override
            public boolean isFieldExposed(Field field) {
                return matchResultToIsExposedResult(
                        fieldMatcher.matches(contextClass, field)
                        || matchAnnotation != null
                                && _MethodUtil.getInheritableAnnotation(contextClass, field, matchAnnotation)
                                        != null);
            }
        };
    }

    private boolean matchResultToIsExposedResult(boolean matches) {
        if (listType == ListType.WHITELIST) {
            return matches;
        }
        if (listType == ListType.BLACKLIST) {
            return !matches;
        }
        throw new AssertionError();
    }

    private static boolean isWellFormedClassName(String s) {
        if (s.length() == 0) {
            return false;
        }
        int identifierStartIdx = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (i == identifierStartIdx) {
                if (!Character.isJavaIdentifierStart(c)) {
                    return false;
                }
            } else if (c == '.' && i != s.length() - 1) {
                identifierStartIdx = i + 1;
            } else {
                if (!Character.isJavaIdentifierPart(c)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isWellFormedJavaIdentifier(String s) {
        if (s.length() == 0) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(s.charAt(0))) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

}
