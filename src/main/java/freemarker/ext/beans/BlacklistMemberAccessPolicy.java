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

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Blacklist-based member access policy, that is, members that are matched by the listing will not be accessible, all
 * others will be. Note that {@link BeansWrapper} and its subclasses doesn't discover all members on the first place,
 * and the {@link MemberAccessPolicy} just removes from that set of members, never adds to it.
 *
 * <p>This class is rarely useful in itself, and mostly meant to be used when composing a {@link MemberAccessPolicy}
 * from other {@link MemberAccessPolicy}-es. If you are serious about security, never use this alone; consider using
 * {@link WhitelistMemberAccessPolicy} as part of your solution.
 *
 * <p>See more about the rules at {@link MemberSelectorListMemberAccessPolicy}. Unlike
 * {@link WhitelistMemberAccessPolicy}, {@link BlacklistMemberAccessPolicy} doesn't have annotations that can be used
 * to add members to the member selector list.
 *
 * @since 2.3.30
 */
public class BlacklistMemberAccessPolicy extends MemberSelectorListMemberAccessPolicy {

    private final boolean toStringAlwaysExposed;

    /**
     * @param memberSelectors
     *      List of member selectors; see {@link MemberSelectorListMemberAccessPolicy} class-level documentation for
     *      more.
     */
    public BlacklistMemberAccessPolicy(Collection<? extends MemberSelector> memberSelectors) {
        super(memberSelectors, ListType.BLACKLIST, null);

        boolean toStringBlacklistedAnywhere = false;
        for (MemberSelector memberSelector : memberSelectors) {
            Method method = memberSelector.getMethod();
            if (method != null && method.getName().equals("toString") && method.getParameterTypes().length == 0) {
                toStringBlacklistedAnywhere = true;
                break;
            }
        }
        toStringAlwaysExposed = !toStringBlacklistedAnywhere;
    }

    @Override
    public boolean isToStringAlwaysExposed() {
        return toStringAlwaysExposed;
    }

}
