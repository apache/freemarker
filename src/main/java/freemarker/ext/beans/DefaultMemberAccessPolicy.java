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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import freemarker.ext.beans.MemberSelectorListMemberAccessPolicy.MemberSelector;
import freemarker.template.Version;
import freemarker.template._TemplateAPI;

/**
 * Member access policy, used  to implement default behavior that's mostly compatible with pre-2.3.30 versions, but is
 * somewhat safer; it still can't provide safety in practice, if you allow untrusted users to edit templates! Use
 * {@link WhitelistMemberAccessPolicy} if you need stricter control.
 *
 * @since 2.3.30
 */
public final class DefaultMemberAccessPolicy implements MemberAccessPolicy {

    private static final DefaultMemberAccessPolicy INSTANCE = new DefaultMemberAccessPolicy();

    private final Set<Class<?>> whitelistRuleFinalClasses;
    private final Set<Class<?>> whitelistRuleNonFinalClasses;
    private final WhitelistMemberAccessPolicy whitelistMemberAccessPolicy;
    private final BlacklistMemberAccessPolicy blacklistMemberAccessPolicy;
    private final boolean toStringAlwaysExposed;

    /**
     * Returns the singleton that's compatible with the given incompatible improvements version.
     */
    public static DefaultMemberAccessPolicy getInstance(Version incompatibleImprovements) {
        _TemplateAPI.checkVersionNotNullAndSupported(incompatibleImprovements);
        // All breakpoints here must occur in ClassIntrospectorBuilder.normalizeIncompatibleImprovementsVersion!
        // Though currently we don't have any.
        return INSTANCE;
    }

    private DefaultMemberAccessPolicy() {
        try {
            ClassLoader classLoader = DefaultMemberAccessPolicy.class.getClassLoader();

            whitelistRuleFinalClasses = new HashSet<>();
            whitelistRuleNonFinalClasses = new HashSet<>();
            Set<Class<?>> typesWithBlacklistUnlistedRule = new HashSet<>();
            List<MemberSelector> whitelistMemberSelectors = new ArrayList<>();
            for (String line : loadMemberSelectorFileLines()) {
                line = line.trim();
                if (!MemberSelector.isIgnoredLine(line)) {
                    if (line.startsWith("@")) {
                        String[] lineParts = line.split("\\s+");
                        if (lineParts.length != 2) {
                            throw new IllegalStateException("Malformed @ line: " + line);
                        }
                        String typeName = lineParts[1];
                        Class<?> upperBoundType;
                        try {
                            upperBoundType = classLoader.loadClass(typeName);
                        } catch (ClassNotFoundException e) {
                            upperBoundType = null;
                        }
                        String rule = lineParts[0].substring(1);
                        if (rule.equals("whitelistPolicyIfAssignable")) {
                            if (upperBoundType != null) {
                                Set<Class<?>> targetSet =
                                        (upperBoundType.getModifiers() & Modifier.FINAL) != 0
                                                ? whitelistRuleFinalClasses
                                                : whitelistRuleNonFinalClasses;
                                targetSet.add(upperBoundType);
                            }
                        } else if (rule.equals("blacklistUnlistedMembers")) {
                            if (upperBoundType != null) {
                                typesWithBlacklistUnlistedRule.add(upperBoundType);
                            }
                        } else {
                            throw new IllegalStateException("Unhandled rule: " + rule);
                        }
                    } else {
                        MemberSelector memberSelector;
                        try {
                            memberSelector = MemberSelector.parse(line, classLoader);
                        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
                            // Can happen if we run on an older Java than the list was made for
                            memberSelector = null;
                        }
                        if (memberSelector != null) {
                            Class<?> upperBoundType = memberSelector.getUpperBoundType();
                            if (upperBoundType != null) {
                                if (!whitelistRuleFinalClasses.contains(upperBoundType)
                                        && !whitelistRuleNonFinalClasses.contains(upperBoundType)
                                        && !typesWithBlacklistUnlistedRule.contains(upperBoundType)) {
                                    throw new IllegalStateException("Type without rule: " + upperBoundType.getName());
                                }
                                // We always do the same, as "blacklistUnlistedMembers" is also defined via a whitelist:
                                whitelistMemberSelectors.add(memberSelector);
                            }
                        }
                    }
                }
            }

            whitelistMemberAccessPolicy = new WhitelistMemberAccessPolicy(whitelistMemberSelectors);

            // Generate blacklists based on the whitelist and the members of "blacklistUnlistedMembers" types:
            List<MemberSelector> blacklistMemberSelectors = new ArrayList<>();
            for (Class<?> blacklistUnlistedRuleType : typesWithBlacklistUnlistedRule) {
                ClassMemberAccessPolicy classPolicy = whitelistMemberAccessPolicy.forClass(blacklistUnlistedRuleType);
                for (Method method : blacklistUnlistedRuleType.getMethods()) {
                    if (!classPolicy.isMethodExposed(method)) {
                        blacklistMemberSelectors.add(new MemberSelector(blacklistUnlistedRuleType, method));
                    }
                }
                for (Constructor<?> constructor : blacklistUnlistedRuleType.getConstructors()) {
                    if (!classPolicy.isConstructorExposed(constructor)) {
                        blacklistMemberSelectors.add(new MemberSelector(blacklistUnlistedRuleType, constructor));
                    }
                }
                for (Field field : blacklistUnlistedRuleType.getFields()) {
                    if (!classPolicy.isFieldExposed(field)) {
                        blacklistMemberSelectors.add(new MemberSelector(blacklistUnlistedRuleType, field));
                    }
                }
            }
            blacklistMemberAccessPolicy = new BlacklistMemberAccessPolicy(blacklistMemberSelectors);

            toStringAlwaysExposed =
                    whitelistMemberAccessPolicy.isToStringAlwaysExposed()
                    && blacklistMemberAccessPolicy.isToStringAlwaysExposed();
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't init " + this.getClass().getName() + " instance", e);
        }
    }

    private static List<String> loadMemberSelectorFileLines() throws IOException {
        List<String> whitelist = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        DefaultMemberAccessPolicy.class.getResourceAsStream("DefaultMemberAccessPolicy-rules"),
                        "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                whitelist.add(line);
            }
        }

        return whitelist;
    }

    @Override
    public ClassMemberAccessPolicy forClass(Class<?> contextClass) {
        if (isTypeWithWhitelistRule(contextClass)) {
            return whitelistMemberAccessPolicy.forClass(contextClass);
        } else {
            return blacklistMemberAccessPolicy.forClass(contextClass);
        }
    }

    @Override
    public boolean isToStringAlwaysExposed() {
        return toStringAlwaysExposed;
    }

    private boolean isTypeWithWhitelistRule(Class<?> contextClass) {
        if (whitelistRuleFinalClasses.contains(contextClass)) {
            return true;
        }
        for (Class<?> nonFinalClass : whitelistRuleNonFinalClasses) {
            if (nonFinalClass.isAssignableFrom(contextClass)) {
                return true;
            }
        }
        return false;
    }

}
