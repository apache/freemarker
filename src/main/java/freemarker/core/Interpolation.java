package freemarker.core;

abstract class Interpolation extends TemplateElement {

    protected abstract String dump(boolean canonical, boolean inStringLiteral);

    protected final String dump(boolean canonical) {
        return dump(canonical, false);
    }
    
    final String getCanonicalFormInStringLiteral() {
        return dump(true, true);
    }

}
