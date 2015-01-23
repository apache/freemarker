package freemarker.test;

import java.util.ArrayList;
import java.util.List;

import freemarker.cache.StringTemplateLoader;

public class MonitoredTemplateLoader extends StringTemplateLoader {
    
    private final List<String> templatesTried = new ArrayList<String>();
    
    @Override
    public Object findTemplateSource(String name) {
        templatesTried.add(name);
        return super.findTemplateSource(name);
    }

    public List<String> getTemplatesTried() {
        return templatesTried;
    }
    
    public void clear() {
        templatesTried.clear();
    }
    
    
}