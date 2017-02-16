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
package org.apache.freemarker.core.templateresolver.impl;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Version;
import org.apache.freemarker.core.templateresolver.MalformedTemplateNameException;
import org.apache.freemarker.core.templateresolver.TemplateNameFormat;

/**
 * The default template name format when {@link Configuration#Configuration(Version) incompatible_improvements} is
 * below 2.4.0. As of FreeMarker 2.4.0, the default {@code incompatible_improvements} is still {@code 2.3.0}, and it
 * will certainly remain so for a very long time. In new projects it's highly recommended to use
 * {@link DefaultTemplateNameFormat#INSTANCE} instead.
 * 
 * @deprecated [FM3] Remove
 */
@Deprecated
public final class DefaultTemplateNameFormatFM2 extends TemplateNameFormat {
    
    public static final DefaultTemplateNameFormatFM2 INSTANCE = new DefaultTemplateNameFormatFM2();
    
    private DefaultTemplateNameFormatFM2() {
        //
    }
    
    @Override
    public String toRootBasedName(String baseName, String targetName) {
        if (targetName.indexOf("://") > 0) {
            return targetName;
        } else if (targetName.startsWith("/")) {
            int schemeSepIdx = baseName.indexOf("://");
            if (schemeSepIdx > 0) {
                return baseName.substring(0, schemeSepIdx + 2) + targetName;
            } else {
                return targetName.substring(1);
            }
        } else {
            if (!baseName.endsWith("/")) {
                baseName = baseName.substring(0, baseName.lastIndexOf("/") + 1);
            }
            return baseName + targetName;
        }
    }

    @Override
    public String normalizeRootBasedName(final String name) throws MalformedTemplateNameException {
        // Disallow 0 for security reasons.
        checkNameHasNoNullCharacter(name);
        
        // The legacy algorithm haven't considered schemes, so the name is in effect a path.
        // Also, note that `path` will be repeatedly replaced below, while `name` is final.
        String path = name;
        
        for (; ; ) {
            int parentDirPathLoc = path.indexOf("/../");
            if (parentDirPathLoc == 0) {
                // If it starts with /../, then it reaches outside the template
                // root.
                throw newRootLeavingException(name);
            }
            if (parentDirPathLoc == -1) {
                if (path.startsWith("../")) {
                    throw newRootLeavingException(name);
                }
                break;
            }
            int previousSlashLoc = path.lastIndexOf('/', parentDirPathLoc - 1);
            path = path.substring(0, previousSlashLoc + 1) +
                   path.substring(parentDirPathLoc + "/../".length());
        }
        for (; ; ) {
            int currentDirPathLoc = path.indexOf("/./");
            if (currentDirPathLoc == -1) {
                if (path.startsWith("./")) {
                    path = path.substring("./".length());
                }
                break;
            }
            path = path.substring(0, currentDirPathLoc) +
                   path.substring(currentDirPathLoc + "/./".length() - 1);
        }
        // Editing can leave us with a leading slash; strip it.
        if (path.length() > 1 && path.charAt(0) == '/') {
            path = path.substring(1);
        }
        return path;
    }
    
}