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

package freemarker.cache;

import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateNotFoundException;
import freemarker.template.Version;
import freemarker.template.utility.StringUtil;

/**
 * Symbolized template name format. The API of this class isn't exposed as it's too immature, so custom
 * template name formats aren't possible yet.
 *
 * @since 2.3.22
 */
public abstract class TemplateNameFormat {

    private TemplateNameFormat() {
       // Currently can't be instantiated from outside 
    }
    
    /**
     * The default template name format when {@link Configuration#Configuration(Version) incompatible_improvements} is
     * below 2.4.0. As of FreeMarker 2.4.0, the default {@code incompatible_improvements} is still {@code 2.3.0}, and it
     * will certainly remain so for a very long time. In new projects it's highly recommended to use
     * {@link #DEFAULT_2_4_0} instead.
     */
    public static final TemplateNameFormat DEFAULT_2_3_0 = new Default020300();
    
    /**
     * The default template name format only when {@link Configuration#Configuration(Version) incompatible_improvements}
     * is set to 2.4.0 (or higher). This is not the out-of-the-box default format of FreeMarker 2.4.x, because the
     * default {@code incompatible_improvements} is still 2.3.0 there.
     * 
     * <p>
     * Differences to the {@link #DEFAULT_2_3_0} format:
     * 
     * <ul>
     * 
     * <li>The scheme and the path need not be separated with {@code "://"} anymore, only with {@code ":"}. This makes
     * template names like {@code "classpath:foo.ftl"} interpreted as an absolute name with scheme {@code "classpath"}
     * and absolute path "foo.ftl". The scheme name before the {@code ":"} can't contain {@code "/"}, or else it's
     * treated as a malformed name. The scheme part can be separated either with {@code "://"} or just {@code ":"} from
     * the path. Hence, {@code myschme:/x} is normalized to {@code myschme:x}, while {@code myschme:///x} is normalized
     * to {@code myschme://x}, but {@code myschme://x} or {@code myschme:/x} aren't changed by normalization. It's up
     * the {@link TemplateLoader} to which the normalized names are passed to decide which of these scheme separation
     * conventions are valid (maybe both).</li>
     * 
     * <li>{@code ":"} is not allowed in template names, except as the scheme separator (see previous point).
     * 
     * <li>Malformed paths throw {@link MalformedTemplateNameException} instead of acting like if the template wasn't
     * found.
     * 
     * <li>{@code "\"} (backslash) is not allowed in template names, and causes {@link MalformedTemplateNameException}.
     * With {@link #DEFAULT_2_3_0} you would certainly end up with a {@link TemplateNotFoundException} (or worse,
     * it would work, but steps like {@code ".."} wouldn't be normalized by FreeMarker).
     * 
     * <li>Template names might end with {@code /}, like {@code "foo/"}, and the presence or lack of the terminating
     * {@code /} is seen as significant. While their actual interpretation is up to the {@link TemplateLoader},
     * operations that manipulate template names assume that the last step refers to a "directory" as opposed to a
     * "file" exactly if the terminating {@code /} is present. Except, the empty name is assumed to refer to the root
     * "directory" (despite that it doesn't end with {@code /}).
     *
     * <li>{@code //} is normalized to {@code /}, except of course if it's in the scheme name terminator. Like
     * {@code foo//bar///baaz.ftl} is normalized to {@code foo/bar/baaz.ftl}. (In general, 0 long step names aren't
     * possible anymore.)</li>
     * 
     * <li>The {@code ".."} bugs of the legacy normalizer are fixed: {@code ".."} steps has removed the preceding
     * {@code "."} or {@code "*"} or scheme steps, not treating them specially as they should be. Now these work as
     * expected. Examples: {@code "a/./../c"} has become to {@code "a/c"}, now it will be {@code "c"}; {@code "a/b/*}
     * {@code /../c"} has become to {@code "a/b/c"}, now it will be {@code "a/*}{@code /c"}; {@code "scheme://.."} has
     * become to {@code "scheme:/"}, now it will be {@code null} ({@link TemplateNotFoundException}) for backing out of
     * the root directory.</li>
     * 
     * <li>As now directory paths has to be handled as well, it recognizes terminating, leading, and lonely {@code ".."}
     * and {@code "."} steps. For example, {@code "foo/bar/.."} now becomes to {@code "foo/"}</li>
     * 
     * <li>Multiple consecutive {@code *} steps are normalized to one</li>
     * 
     * </ul>
     */
    public static final TemplateNameFormat DEFAULT_2_4_0 = new Default020400();
    
    /**
     * @param baseName Maybe {@code null}, maybe a "file" name instead of a "directory" name.
     * @param targetName No {@code null}. Maybe relative, maybe absolute.
     */
    abstract String toAbsoluteName(String baseName, String targetName) throws MalformedTemplateNameException;
    
    /**
     * @return For backward compatibility only, {@code null} is allowed and will be treated as if the template doesn't
     *         exist (despite that a normalizer doesn't access the storage, so it's not its duty to decide that).
     */
    abstract String normalizeAbsoluteName(String name) throws MalformedTemplateNameException;

    private static final class Default020300 extends TemplateNameFormat {
        String toAbsoluteName(String baseName, String targetName) {
            if (baseName == null) {
                return targetName;
            }
            
            if (targetName.indexOf("://") > 0) {
                return targetName;
            } else if (targetName.startsWith("/"))  {
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
    
        String normalizeAbsoluteName(final String name) throws MalformedTemplateNameException {
            // Disallow 0 for security reasons.
            checkNameHasNoNullCharacter(name);
            
            // The legacy algorithm haven't considered schemes, so the name is in effect a path.
            // Also, note that `path` will be repeatedly replaced below, while `name` is final.
            String path = name;
            
            for(;;) {
                int parentDirPathLoc = path.indexOf("/../");
                if(parentDirPathLoc == 0) {
                    // If it starts with /../, then it reaches outside the template
                    // root.
                    throw newRootLeavingException(name);
                }
                if(parentDirPathLoc == -1) {
                    if(path.startsWith("../")) {
                        throw newRootLeavingException(name);
                    }
                    break;
                }
                int previousSlashLoc = path.lastIndexOf('/', parentDirPathLoc - 1);
                path = path.substring(0, previousSlashLoc + 1) +
                       path.substring(parentDirPathLoc + "/../".length());
            }
            for(;;) {
                int currentDirPathLoc = path.indexOf("/./");
                if(currentDirPathLoc == -1) {
                    if(path.startsWith("./")) {
                        path = path.substring("./".length());
                    }
                    break;
                }
                path = path.substring(0, currentDirPathLoc) +
                       path.substring(currentDirPathLoc + "/./".length() - 1);
            }
            // Editing can leave us with a leading slash; strip it.
            if(path.length() > 1 && path.charAt(0) == '/') {
                path = path.substring(1);
            }
            return path;
        }
        
        public String toString() {
            return "TemplateNameFormat.DEFAULT_2_3_0";
        }
        
    }

    private static final class Default020400 extends TemplateNameFormat {
        String toAbsoluteName(String baseName, String targetName) {
            if (baseName == null) {
                return targetName;
            }
            
            if (findSchemeSectionEnd(targetName) != 0) {
                return targetName;
            } else if (targetName.startsWith("/"))  {  // targetName is an absolute path
                final String targetNameAsRelative = targetName.substring(1);
                final int schemeSectionEnd = findSchemeSectionEnd(baseName);
                if (schemeSectionEnd == 0) {
                    return targetNameAsRelative;
                } else {
                    // Prepend the scheme of baseName:
                    return baseName.substring(0, schemeSectionEnd) + targetNameAsRelative;
                }
            } else {  // targetName is a relative path
                if (!baseName.endsWith("/")) {
                    // Not a directory name => get containing directory name
                    int baseEnd = baseName.lastIndexOf("/") + 1;
                    if (baseEnd == 0) {
                        // For something like "classpath:t.ftl", must not remove the scheme part:
                        baseEnd = findSchemeSectionEnd(baseName);
                    }
                    baseName = baseName.substring(0, baseEnd);
                }
                return baseName + targetName;
            }
        }
    
        String normalizeAbsoluteName(final String name) throws MalformedTemplateNameException {
            // Disallow 0 for security reasons.
            checkNameHasNoNullCharacter(name);
    
            if (name.indexOf('\\') != -1) {
                throw new MalformedTemplateNameException(
                        name,
                        "Backslash (\"\\\") is not allowed in template names. Use slash (\"/\") instead.");
            }
            
            // Split name to a scheme and a path:
            final String scheme;
            String path;
            {
                int schemeSectionEnd = findSchemeSectionEnd(name);
                if (schemeSectionEnd == 0) {
                    scheme = null;
                    path = name;
                } else {
                    scheme = name.substring(0, schemeSectionEnd);
                    path = name.substring(schemeSectionEnd);
                }
            }
            
            if (path.indexOf(':') != -1) {
                throw new MalformedTemplateNameException(name,
                        "The ':' character can only be used after the scheme name (if there's any), "
                        + "not in the path part");
            }
            
            path = removeRedundantSlashes(path);
            // path now doesn't start with "/"
            
            path = removeDotSteps(path);
            
            path = resolveDotDotSteps(path, name);
    
            path = removeRedundantStarSteps(path);
            
            return scheme == null ? path : scheme + path;
        }

        private int findSchemeSectionEnd(String name) {
            int schemeColonIdx = name.indexOf(":");
            if (schemeColonIdx == -1 || name.lastIndexOf('/', schemeColonIdx - 1) != -1) {
                return 0;
            } else {
                // If there's a following "//", it's treated as the part of the scheme section:
                if (schemeColonIdx + 2 < name.length()
                        && name.charAt(schemeColonIdx + 1) == '/' && name.charAt(schemeColonIdx + 2) == '/') {
                    return schemeColonIdx + 3;
                } else {
                    return schemeColonIdx + 1;
                }
            }
        }
    
        private String removeRedundantSlashes(String path) {
            String prevName;
            do {
                prevName = path;
                path = StringUtil.replace(path, "//", "/");
            } while (prevName != path);
            return path.startsWith("/") ? path.substring(1) : path;
        }
    
        private String removeDotSteps(String path) {
            int nextFromIdx = path.length() - 1;
            findDotSteps: while (true) {
                final int dotIdx = path.lastIndexOf('.', nextFromIdx);
                if (dotIdx < 0) {
                    return path;
                }
                nextFromIdx = dotIdx - 1;
                
                if (dotIdx != 0 && path.charAt(dotIdx - 1) != '/') {
                    // False alarm
                    continue findDotSteps;
                }
                
                final boolean slashRight;
                if (dotIdx + 1 == path.length()) {
                    slashRight = false;
                } else if (path.charAt(dotIdx + 1) == '/') {
                    slashRight = true;
                } else {
                    // False alarm
                    continue findDotSteps;
                }
                
                if (slashRight) { // "foo/./bar" or "./bar" 
                    path = path.substring(0, dotIdx) + path.substring(dotIdx + 2);
                } else { // "foo/." or "."
                    path = path.substring(0, path.length() - 1);
                }
            }
        }
    
        /**
         * @param name The original name, needed for exception error messages.
         */
        private String resolveDotDotSteps(String path, final String name) throws MalformedTemplateNameException {
            int nextFromIdx = 0;
            findDotDotSteps: while (true) {
                final int dotDotIdx = path.indexOf("..", nextFromIdx);
                if (dotDotIdx < 0) {
                    return path;
                }
    
                if (dotDotIdx == 0) {
                    throw newRootLeavingException(name);
                } else if (path.charAt(dotDotIdx - 1) != '/') {
                    // False alarm
                    nextFromIdx = dotDotIdx + 3;
                    continue findDotDotSteps;
                }
                // Here we know that it has a preceding "/".
                
                final boolean slashRight;
                if (dotDotIdx + 2 == path.length()) {
                    slashRight = false;
                } else if (path.charAt(dotDotIdx + 2) == '/') {
                    slashRight = true;
                } else {
                    // False alarm
                    nextFromIdx = dotDotIdx + 3;
                    continue findDotDotSteps;
                }
                
                int previousSlashIdx;
                boolean skippedStarStep = false;
                {
                    int searchSlashBacwardsFrom = dotDotIdx - 2; // before the "/.."
                    scanBackwardsForSlash: while (true) {
                        if (searchSlashBacwardsFrom == -1) {
                            throw newRootLeavingException(name);
                        }
                        previousSlashIdx = path.lastIndexOf('/', searchSlashBacwardsFrom);
                        if (previousSlashIdx == -1) {
                            if (searchSlashBacwardsFrom == 0 && path.charAt(0) == '*') {
                                // "*/.."
                                throw newRootLeavingException(name);
                            }
                            break scanBackwardsForSlash;
                        }
                        if (path.charAt(previousSlashIdx + 1) == '*' && path.charAt(previousSlashIdx + 2) == '/') {
                            skippedStarStep = true;
                            searchSlashBacwardsFrom = previousSlashIdx - 1; 
                        } else {
                            break scanBackwardsForSlash;
                        }
                    }
                }
                
                // Note: previousSlashIdx is possibly -1
                // Removed part in {}: "a/{b/*/../}c" or "a/{b/*/..}"
                path = path.substring(0, previousSlashIdx + 1)
                        + (skippedStarStep ? "*/" : "")
                        + path.substring(dotDotIdx + (slashRight ? 3 : 2));
                nextFromIdx = previousSlashIdx + 1;
            }
        }
    
        private String removeRedundantStarSteps(String path) {
            String prevName;
            removeDoubleStarSteps: do {
                int supiciousIdx = path.indexOf("*/*");
                if (supiciousIdx == -1) {
                    break removeDoubleStarSteps;
                }
        
                prevName = path;
                
                // Is it delimited on both sided by "/" or by the string boundaires? 
                if ((supiciousIdx == 0 || path.charAt(supiciousIdx - 1) == '/')
                        && (supiciousIdx + 3 == path.length() || path.charAt(supiciousIdx + 3) == '/')) {
                    path = path.substring(0, supiciousIdx) + path.substring(supiciousIdx + 2); 
                }
            } while (prevName != path);
            
            // An initial "*" step is redundant:
            if (path.startsWith("*")) {
                if (path.length() == 1) {
                    path = "";
                } else if (path.charAt(1) == '/') {
                    path = path.substring(2); 
                }
                // else: it's wasn't a "*" step.
            }
            
            return path;
        }
        
        public String toString() {
            return "TemplateNameFormat.DEFAULT_2_4_0";
        }
    }

    private static void checkNameHasNoNullCharacter(final String name) throws MalformedTemplateNameException {
        if (name.indexOf(0) != -1) {
            throw new MalformedTemplateNameException(name,
                    "Null character (\\u0000) in the name; possible attack attempt");
        }
    }
    
    private static MalformedTemplateNameException newRootLeavingException(final String name) {
        return new MalformedTemplateNameException(name, "Backing out from the root directory is not allowed");
    }
    
}
