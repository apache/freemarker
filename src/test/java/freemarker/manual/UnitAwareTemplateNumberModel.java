package freemarker.manual;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

public class UnitAwareTemplateNumberModel implements TemplateNumberModel {

    private final Number value;
    private final String unit;
    
    public UnitAwareTemplateNumberModel(Number value, String unit) {
        this.value = value;
        this.unit = unit;
    }

    @Override
    public Number getAsNumber() throws TemplateModelException {
        return value;
    }

    public String getUnit() {
        return unit;
    }

}
