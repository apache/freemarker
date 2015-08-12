package freemarker.core;

import freemarker.template.TemplateOutputModel;

class RawTemplateOutputModel implements TemplateOutputModel<RawTemplateOutputModel> {

    public OutputFormat<RawTemplateOutputModel> getOutputFormat() {
        return RawOutputFormat.INSTANCE;
    }

}
