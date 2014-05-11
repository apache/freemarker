package freemarker.core;

import freemarker.template.utility.ClassUtil;

public class _DelayedShortClassName extends _DelayedConversionToString {

    public _DelayedShortClassName(Class pClass) {
        super(pClass);
    }

    protected String doConversion(Object obj) {
        return ClassUtil.getShortClassName((Class) obj, true);
    }

}
