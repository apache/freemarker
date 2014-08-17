package freemarker.core;

import freemarker.template.TemplateModelException;

/**
 * This exists for backward compatibly, and is used before Incompatible Improvements 2.3.21 only.
 * 
 * @since 2.3.21
 */
final class NonListableRightUnboundedRangeModel extends RightUnboundedRangeModel {

    NonListableRightUnboundedRangeModel(int begin) {
        super(begin);
    }

    public int size() throws TemplateModelException {
        return 0;
    }

}
