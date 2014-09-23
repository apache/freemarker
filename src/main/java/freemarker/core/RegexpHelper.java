package freemarker.core;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import freemarker.cache.MruCacheStorage;
import freemarker.log.Logger;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.StringUtil;

/**
 * Helper for language features (like built-ins) that use regular expressions. 
 */
final class RegexpHelper {

    private static final Logger LOG = Logger.getLogger("freemarker.runtime");
    
    private static volatile boolean flagWarningsEnabled = LOG.isWarnEnabled();
    private static final int MAX_FLAG_WARNINGS_LOGGED = 25;
    private static final Object flagWarningsCntSync = new Object();
    private static int flagWarningsCnt;
    
    private static final MruCacheStorage patternCache = new MruCacheStorage(50, 150);

    static private long intFlagToLong(int flag) {
        return flag & 0x0000FFFFL;
    }

    // Standard regular expression flags converted to long:
    static final long RE_FLAG_CASE_INSENSITIVE = intFlagToLong(Pattern.CASE_INSENSITIVE);

    static final long RE_FLAG_MULTILINE = intFlagToLong(Pattern.MULTILINE);

    static final long RE_FLAG_COMMENTS = intFlagToLong(Pattern.COMMENTS);

    static final long RE_FLAG_DOTALL = intFlagToLong(Pattern.DOTALL);

    // FreeMarker-specific regular expression flags (using the higher 32 bits):
    static final long RE_FLAG_REGEXP = 0x100000000L;

    static final long RE_FLAG_FIRST_ONLY = 0x200000000L;
    
    // Can't be instantiated
    private RegexpHelper() { }

    static Pattern getPattern(String patternString, int flags)
    throws TemplateModelException {
        PatternCacheKey patternKey = new PatternCacheKey(patternString, flags);
        
        Pattern result;
        
        synchronized (patternCache) {
            result = (Pattern) patternCache.get(patternKey);
        }
        if (result != null) {
            return result;
        }
        
        try {
            result = Pattern.compile(patternString, flags);
        } catch (PatternSyntaxException e) {
            throw new _TemplateModelException(e, new Object[] {
                    "Malformed regular expression: ", new _DelayedGetMessage(e) });
        }
        synchronized (patternCache) {
            patternCache.put(patternKey, result);
        }
        return result;
    };

    private static class PatternCacheKey {
        private final String patternString;
        private final int flags;
        private final int hashCode;
        
        public PatternCacheKey(String patternString, int flags) {
            this.patternString = patternString;
            this.flags = flags;
            hashCode = patternString.hashCode() + 31 * flags;
        }
        
        public boolean equals(Object that) {
            if (that instanceof PatternCacheKey) {
                PatternCacheKey thatPCK = (PatternCacheKey) that; 
                return thatPCK.flags == flags
                        && thatPCK.patternString.equals(patternString);
            } else {
                return false;
            }
        }

        public int hashCode() {
            return hashCode;
        }
        
    }

    static long parseFlagString(String flagString) {
        long flags = 0;
        for (int i = 0; i < flagString.length(); i++) {
            char c = flagString.charAt(i);
            switch (c) {
                case 'i':
                    flags |= RE_FLAG_CASE_INSENSITIVE;
                    break;
                case 'm':
                    flags |= RE_FLAG_MULTILINE;
                    break;
                case 'c':
                    flags |= RE_FLAG_COMMENTS;
                    break;
                case 's':
                    flags |= RE_FLAG_DOTALL;
                    break;
                case 'r':
                    flags |= RE_FLAG_REGEXP;
                    break;
                case 'f':
                    flags |= RE_FLAG_FIRST_ONLY;
                    break;
                default:
                    if (flagWarningsEnabled) {
                        RegexpHelper.logFlagWarning(
                                "Unrecognized regular expression flag: "
                                + StringUtil.jQuote(String.valueOf(c)) + ".");
                    }
            }  // switch
        }
        return flags;
    }

    /**
     * Logs flag warning for a limited number of times. This is used to prevent
     * log flooding.
     */
    static void logFlagWarning(String message) {
        if (!flagWarningsEnabled) return;
        
        int cnt;
        synchronized (flagWarningsCntSync) {
            cnt = flagWarningsCnt;
            if (cnt < MAX_FLAG_WARNINGS_LOGGED) {
                flagWarningsCnt++;
            } else {
                flagWarningsEnabled = false;
                return;
            }
        }
        message += " This will be an error in FreeMarker 2.4!";
        if (cnt + 1 == MAX_FLAG_WARNINGS_LOGGED) {
            message += " [Will not log more regular expression flag problems until restart!]";
        }
        LOG.warn(message);
    }

    static void checkNonRegexpFlags(String biName, long flags) {
        if (!flagWarningsEnabled) return;
        
        if ((flags & RE_FLAG_MULTILINE) != 0) {
            logFlagWarning("?" + biName + " doesn't support the \"m\" flag "
                    + "without the \"r\" flag.");
        }
        if ((flags & RE_FLAG_DOTALL) != 0) {
            logFlagWarning("?" + biName + " doesn't support the \"s\" flag "
                    + "without the \"r\" flag.");
        }
        if ((flags & RE_FLAG_COMMENTS) != 0) {
            logFlagWarning("?" + biName + " doesn't support the \"c\" flag "
                    + "without the \"r\" flag.");
        }
    }
    
}
