package freemarker.core;

public class Internal_DelayedAOrAn extends Internal_DelayedConversionToString {

    public Internal_DelayedAOrAn(Object object) {
        super(object);
    }

    @Override
    protected String doConversion(Object obj) {
        String s = obj.toString();
        return MessageUtil.getAOrAn(s) + " " + s;
    }

}
