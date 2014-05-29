package freemarker.core;

/** 1 to "1st", 2 to "2nd", etc. */
public class _DelayedOrdinal extends _DelayedConversionToString {

    public _DelayedOrdinal(Object object) {
        super(object);
    }

    protected String doConversion(Object obj) {
        if (obj instanceof Number) {
            long n = ((Number) obj).longValue();
            if (n % 10 == 1 && n % 100 != 11) {
                return n + "st";
            } else if (n % 10 == 2 && n % 100 != 12) {
                return n + "nd";
            } else if (n % 10 == 3 && n % 100 != 13) {
                return n + "rd";
            } else {
                return n + "th";
            }
        } else {
            return "" + obj;
        }
    }
    
}
