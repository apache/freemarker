package freemarker.core;

import java.util.Properties;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Version;

/**
 * Don't use this; used internally by FreeMarker, might changes without notice.
 * The runtime environment used during the evaluation of configuration {@link Properties}.
 */
public class _SettingEvaluationEnvironment {
    
    private static final ThreadLocal CURRENT = new ThreadLocal();

    private BeansWrapper objectWrapper;
    
    public static _SettingEvaluationEnvironment getCurrent() {
        Object r = CURRENT.get();
        if (r != null) {
            return (_SettingEvaluationEnvironment) r;
        }
        return new _SettingEvaluationEnvironment();
    }
    
    public static _SettingEvaluationEnvironment startScope() {
        Object previous = CURRENT.get();
        CURRENT.set(new _SettingEvaluationEnvironment());
        return (_SettingEvaluationEnvironment) previous;
    }
    
    public static void endScope(_SettingEvaluationEnvironment previous) {
        CURRENT.set(previous);
    }

    public BeansWrapper getObjectWrapper() {
        if (objectWrapper == null) {
            objectWrapper = new BeansWrapper(new Version(2, 3, 21));
        }
        return objectWrapper;
    }
    
}
