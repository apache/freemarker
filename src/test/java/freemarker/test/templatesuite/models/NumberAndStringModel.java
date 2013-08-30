package freemarker.test.templatesuite.models;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;

public class NumberAndStringModel implements TemplateNumberModel,
		TemplateScalarModel {
	
	private final String s;
	
	public NumberAndStringModel(String s) {
		super();
		this.s = s;
	}

	public String getAsString() throws TemplateModelException {
		return s;
	}

	@SuppressWarnings("boxing")
    public Number getAsNumber() throws TemplateModelException {
		return s.length();
	}

}
