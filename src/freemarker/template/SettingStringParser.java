package freemarker.template;

import java.util.ArrayList;
import java.util.HashMap;

import freemarker.core.ParseException;
import freemarker.template.utility.StringUtil;

/**
 * Helper class for parsing setting values given with string.
 */
class SettingStringParser {
    private String text;
    private int p;
    private int ln;

    SettingStringParser(String text) {
        this.text = text;
        this.p = 0;
        this.ln = text.length();
    }

    ArrayList parseAsList() throws ParseException {
        char c;
        ArrayList seq = new ArrayList();
        while (true) {
            c = skipWS();
            if (c == ' ') break;
            seq.add(fetchStringValue());
            c = skipWS();
            if (c == ' ') break;
            if (c != ',') throw new ParseException(
                    "Expected \",\" or the end of text but "
                    + "found \"" + c + "\"", 0, 0);
            p++;
        }
        return seq;
    }

    HashMap parseAsImportList() throws ParseException {
        char c;
        HashMap map = new HashMap();
        while (true) {
            c = skipWS();
            if (c == ' ') break;
            String lib = fetchStringValue();

            c = skipWS();
            if (c == ' ') throw new ParseException(
                    "Unexpected end of text: expected \"as\"", 0, 0);
            String s = fetchKeyword();
            if (!s.equalsIgnoreCase("as")) throw new ParseException(
                    "Expected \"as\", but found " + StringUtil.jQuote(s), 0, 0);

            c = skipWS();
            if (c == ' ') throw new ParseException(
                    "Unexpected end of text: expected gate hash name", 0, 0);
            String ns = fetchStringValue();
            
            map.put(ns, lib);

            c = skipWS();
            if (c == ' ') break;
            if (c != ',') throw new ParseException(
                    "Expected \",\" or the end of text but "
                    + "found \"" + c + "\"", 0, 0);
            p++;
        }
        return map;
    }

    String fetchStringValue() throws ParseException {
        String w = fetchWord();
        if (w.startsWith("'") || w.startsWith("\"")) {
            w = w.substring(1, w.length() - 1);
        }
        return StringUtil.FTLStringLiteralDec(w);
    }

    String fetchKeyword() throws ParseException {
        String w = fetchWord();
        if (w.startsWith("'") || w.startsWith("\"")) {
            throw new ParseException(
                "Keyword expected, but a string value found: " + w, 0, 0);
        }
        return w;
    }

    char skipWS() {
        char c;
        while (p < ln) {
            c = text.charAt(p);
            if (!Character.isWhitespace(c)) return c;
            p++;
        }
        return ' ';
    }

    private String fetchWord() throws ParseException {
        if (p == ln) throw new ParseException(
                "Unexpeced end of text", 0, 0);

        char c = text.charAt(p);
        int b = p;
        if (c == '\'' || c == '"') {
            boolean escaped = false;
            char q = c;
            p++;
            while (p < ln) {
                c = text.charAt(p);
                if (!escaped) {
                    if (c == '\\') {
                        escaped = true;
                    } else if (c == q) {
                        break;
                    }
                } else {
                    escaped = false;
                }
                p++;
            }
            if (p == ln) {
                throw new ParseException("Missing " + q, 0, 0);
            }
            p++;
            return text.substring(b, p);
        } else {
            do {
                c = text.charAt(p);
                if (!(Character.isLetterOrDigit(c)
                        || c == '/' || c == '\\' || c == '_'
                        || c == '.' || c == '-' || c == '!'
                        || c == '*' || c == '?')) break;
                p++;
            } while (p < ln);
            if (b == p) {
                throw new ParseException("Unexpected character: " + c, 0, 0);
            } else {
                return text.substring(b, p);
            }
        }
    }
}