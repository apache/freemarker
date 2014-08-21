package freemarker.core;

abstract class RightUnboundedRangeModel extends RangeModel {
    
    RightUnboundedRangeModel(int begin) {
        super(begin);
    }

    final int getStep() {
        return 1;
    }

    final boolean isRightUnbounded() {
        return true;
    }
    
    final boolean isRightAdaptive() {
        return true;
    }

    final boolean isAffactedByStringSlicingBug() {
        return false;
    }
    
}
