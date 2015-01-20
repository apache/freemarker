package freemarker.cache;

import freemarker.template.Configuration;
import freemarker.template.TemplateNotFoundException;
import freemarker.template.Version;
import freemarker.template.utility.StringUtil;

public abstract class TemplateNameFormat {

    private static final String TERMINATING_PARENT_DIR_PATH = "/..";
    private static final String SCHEME_SEPARATOR = "://";
    private static final String CURRENT_DIR_PATH_PREFIX = "./";
    private static final String CURRENT_DIR_PATH = "/./";
    private static final String PARENT_DIR_PATH_PREFIX = "../";
    private static final String PARENT_DIR_PATH = "/../";
    private static final char SLASH = '/';
    
    private TemplateNameFormat() {
       // Can't be instantiated from outside 
    }
    
    /**
     * The default template name format when {@link Configuration#Configuration(Version) incompatible_improvements} is
     * below 2.4.0. As of FreeMarker 2.4.0, the default {@code incompatible_improvements} is still {@code 2.3.0}, and it
     * will certainly remain so for a very long time. In new projects it's highly recommended to use
     * {@link #DEFAULT_2_4_0} instead.
     */
    public static final TemplateNameFormat DEFAULT_2_3_0 = new TemplateNameFormat() {

        String toAbsoluteName(String baseName, String targetName) {
            if (targetName.indexOf(SCHEME_SEPARATOR) > 0) {
                return targetName;
            } else if (targetName.startsWith("/"))  {
                int schemeSepIdx = baseName.indexOf(SCHEME_SEPARATOR);
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

        String normalizeAbsoluteName(String name) {
            // Disallow 0 for security reasons.
            if (name.indexOf(0) != -1) return null;
            
            for(;;) {
                int parentDirPathLoc = name.indexOf(PARENT_DIR_PATH);
                if(parentDirPathLoc == 0) {
                    // If it starts with /../, then it reaches outside the template
                    // root.
                    return null;
                }
                if(parentDirPathLoc == -1) {
                    if(name.startsWith(PARENT_DIR_PATH_PREFIX)) {
                        // Another attempt to reach out of template root.
                        return null;
                    }
                    break;
                }
                int previousSlashLoc = name.lastIndexOf(SLASH, parentDirPathLoc - 1);
                name = name.substring(0, previousSlashLoc + 1) +
                       name.substring(parentDirPathLoc + PARENT_DIR_PATH.length());
            }
            for(;;) {
                int currentDirPathLoc = name.indexOf(CURRENT_DIR_PATH);
                if(currentDirPathLoc == -1) {
                    if(name.startsWith(CURRENT_DIR_PATH_PREFIX)) {
                        name = name.substring(CURRENT_DIR_PATH_PREFIX.length());
                    }
                    break;
                }
                name = name.substring(0, currentDirPathLoc) +
                       name.substring(currentDirPathLoc + CURRENT_DIR_PATH.length() - 1);
            }
            // Editing can leave us with a leading slash; strip it.
            if(name.length() > 1 && name.charAt(0) == SLASH) {
                name = name.substring(1);
            }
            return name;
        } 
        
    };
    
    /**
     * The default template name format only when {@link Configuration#Configuration(Version) incompatible_improvements}
     * is set to 2.4.0 (or higher). This is not the out-of-the-box default format of FreeMarker 2.4.x, because the
     * default {@code incompatible_improvements} is still 2.3.0 there.
     * 
     * <p>
     * Differences from the {@link #DEFAULT_2_3_0} format:
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
     * <li>{@code ":"} is not allowed in template names, except as the scheme separator.
     * 
     * <li>Template names might end with {@code /}, like {@code "foo/"}, and the presence or lack of the terminating
     * {@code /} is seen as significant. While their actual interpretation is up to the {@link TemplateLoader},
     * operations that manipulate templates name assume that the last step refers to a "directory" as opposed to a
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
    public static final TemplateNameFormat DEFAULT_2_4_0 = new TemplateNameFormat() {

        String toAbsoluteName(String baseName, String targetName) {
            if (findSchemeSectionEnd(targetName) != 0) {
                return targetName;
            } else if (targetName.startsWith("/"))  {
                final String targetNameAsRelative = targetName.substring(1);
                final int schemeSectionEnd = findSchemeSectionEnd(baseName);
                if (schemeSectionEnd == 0) {
                    return targetNameAsRelative;
                } else {
                    // Prepend the scheme of baseName:
                    return baseName.substring(0, schemeSectionEnd) + targetNameAsRelative;
                }
            } else {  // targetName is a relativespath
                if (!baseName.endsWith("/")) {
                    // Not a directory name => get containing directory name
                    int baseEnd = baseName.lastIndexOf("/") + 1;
                    if (baseEnd == 0) {
                        baseEnd = findSchemeSectionEnd(baseName);
                    }
                    baseName = baseName.substring(0, baseEnd);
                }
                return baseName + targetName;
            }
        }

        String normalizeAbsoluteName(final String name) {
            // Disallow 0 for security reasons.
            if (name.indexOf(0) != -1) {
                return null;
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
                return null;
            }
            
            path = removeRedundantSlashes(path);
            // path now doesn't start with "/"
            
            path = removeDotSteps(path);
            
            path = resolveDotDotSteps(path);
            if (path == null) {
                return null;
            }

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

        private String removeRedundantSlashes(String name) {
            String prevName;
            do {
                prevName = name;
                name = StringUtil.replace(name, "//", "/");
            } while (prevName != name);
            return name.startsWith("/") ? name.substring(1) : name;
        }

        private String removeDotSteps(String name) {
            int nextFromIdx = name.length() - 1;
            findDotSteps: while (true) {
                final int dotIdx = name.lastIndexOf('.', nextFromIdx);
                if (dotIdx < 0) {
                    return name;
                }
                nextFromIdx = dotIdx - 1;
                
                if (dotIdx != 0 && name.charAt(dotIdx - 1) != SLASH) {
                    // False alarm
                    continue findDotSteps;
                }
                
                final boolean slashRight;
                if (dotIdx + 1 == name.length()) {
                    slashRight = false;
                } else if (name.charAt(dotIdx + 1) == SLASH) {
                    slashRight = true;
                } else {
                    // False alarm
                    continue findDotSteps;
                }
                
                if (slashRight) { // "foo/./bar" or "./bar" 
                    name = name.substring(0, dotIdx) + name.substring(dotIdx + 2);
                } else { // "foo/." or "."
                    name = name.substring(0, name.length() - 1);
                }
            }
        }

        private String resolveDotDotSteps(String name) {
            int nextFromIdx = 0;
            findDotDotSteps: while (true) {
                final int dotDotIdx = name.indexOf("..", nextFromIdx);
                if (dotDotIdx < 0) {
                    return name;
                }

                if (dotDotIdx == 0) {
                    return null;
                } else if (name.charAt(dotDotIdx - 1) != SLASH) {
                    // False alarm
                    nextFromIdx = dotDotIdx + 3;
                    continue findDotDotSteps;
                }
                // Here we know that it has a preceding "/".
                
                final boolean slashRight;
                if (dotDotIdx + 2 == name.length()) {
                    slashRight = false;
                } else if (name.charAt(dotDotIdx + 2) == SLASH) {
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
                            return null;
                        }
                        previousSlashIdx = name.lastIndexOf(SLASH, searchSlashBacwardsFrom);
                        if (previousSlashIdx == -1) {
                            if (searchSlashBacwardsFrom == 0 && name.charAt(0) == '*') {
                                // "*/.."
                                return null;
                            }
                            break scanBackwardsForSlash;
                        }
                        if (name.charAt(previousSlashIdx + 1) == '*' && name.charAt(previousSlashIdx + 2) == '/') {
                            skippedStarStep = true;
                            searchSlashBacwardsFrom = previousSlashIdx - 1; 
                        } else {
                            break scanBackwardsForSlash;
                        }
                    }
                }
                
                // Note: previousSlashIdx is possibly -1
                // Removed part in {}: "a/{b/*/../}c" or "a/{b/*/..}"
                name = name.substring(0, previousSlashIdx + 1)
                        + (skippedStarStep ? "*/" : "")
                        + name.substring(dotDotIdx + (slashRight ? 3 : 2));
                nextFromIdx = previousSlashIdx + 1;
            }
        }

        private String removeRedundantStarSteps(String name) {
            String prevName;
            removeDoubleStarSteps: do {
                int supiciousIdx = name.indexOf("*/*");
                if (supiciousIdx == -1) {
                    break removeDoubleStarSteps;
                }
        
                prevName = name;
                
                // Is it delimited on both sided by "/" or by the string boundaires? 
                if ((supiciousIdx == 0 || name.charAt(supiciousIdx - 1) == '/')
                        && (supiciousIdx + 3 == name.length() || name.charAt(supiciousIdx + 3) == '/')) {
                    name = name.substring(0, supiciousIdx) + name.substring(supiciousIdx + 2); 
                }
            } while (prevName != name);
            
            // An initial "*" step is redundant:
            if (name.startsWith("*")) {
                if (name.length() == 1) {
                    name = "";
                } else if (name.charAt(1) == '/') {
                    name = name.substring(2); 
                }
                // else: it's wasn't a "*" step.
            }
            
            return name;
        }
        
    };
    
    abstract String toAbsoluteName(String baseName, String targetName);
    
    abstract String normalizeAbsoluteName(String name);

}
