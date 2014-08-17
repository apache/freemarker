package freemarker.core;

import freemarker.template.SimpleNumber;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;

abstract class RangeModel  implements TemplateSequenceModel, java.io.Serializable {
    
    private final int begin;

    public RangeModel(int begin) {
        this.begin = begin;
    }

    final int getBegining() {
        return begin;
    }
    
    final public TemplateModel get(int index) throws TemplateModelException {
        if (index < 0 || index >= size()) {
            throw new _TemplateModelException(new Object[] {
                    "Range item index ", new Integer(index), " is out of bounds." });
        }
        long value = (long) begin + getStep() * index;
        return value <= Integer.MAX_VALUE ? new SimpleNumber((int) value) : new SimpleNumber(value);
    }
    
    /**
     * @return {@code 1} or {@code -1}; other return values need not be properly handled until FTL supports other steps.
     */
    abstract int getStep();
    
    abstract boolean isRightUnbounded();
    
}
