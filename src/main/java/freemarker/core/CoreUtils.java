package freemarker.core;

import freemarker.template.utility.StringUtil;

final class CoreUtils {

    private CoreUtils() {
        // No meant to be instantiated
    }

    static String toFTLAfterDotIdentifierReference(String name) {
        return backslashEscapeIdentifier(name);
    }

    static String toFTLTopLevelIdentifierReference(String name) {
        return backslashEscapeIdentifier(name);
    }

    static String toFTLTopLevelTragetIdentifier(final String name) {
        char quotationType = 0;
        scanForQuotationType: for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (!(i == 0 ? Character.isJavaIdentifierStart(c) : Character.isJavaIdentifierPart(c)) && c != '@') {
                if ((quotationType == 0 || quotationType == '\\') && (c == '-' || c == '.' || c == ':')) {
                    quotationType = '\\';
                } else {
                    quotationType = '"';
                    break scanForQuotationType;
                }
            }
        }
        switch (quotationType) {
        case 0:
            return name;
        case '"':
            return StringUtil.ftlQuote(name);
        case '\\':
            return backslashEscapeIdentifier(name);
        default:
            throw new BugException();
        }
    }

    private static String backslashEscapeIdentifier(String name) {
        return StringUtil.replace(StringUtil.replace(StringUtil.replace(name, "-", "\\-"), ".", "\\."), ":", "\\:");
    }

}
