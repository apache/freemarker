package freemarker.core;

/** Don't use this; used internally by FreeMarker, might changes without notice. */
public class _DelayedAOrAn extends _DelayedConversionToString {

    public _DelayedAOrAn(Object object) {
        super(object);
    }

    protected String doConversion(Object obj) {
        String s = obj.toString();
        return MessageUtil.getAOrAn(s) + " " + s;
    }

}
