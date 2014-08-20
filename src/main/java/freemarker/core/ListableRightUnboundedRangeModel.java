package freemarker.core;

import java.math.BigInteger;

import freemarker.template.SimpleNumber;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;

/**
 * This is the model used for right-unbounded ranges since Incompatible Improvements 2.3.21.
 * 
 * @since 2.3.21
 */
final class ListableRightUnboundedRangeModel extends RightUnboundedRangeModel implements TemplateCollectionModel {

    ListableRightUnboundedRangeModel(int begin) {
        super(begin);
    }

    public int size() throws TemplateModelException {
        return Integer.MAX_VALUE;
    }

    public TemplateModelIterator iterator() throws TemplateModelException {
        return new TemplateModelIterator() {
            boolean needInc;
            int nextType = 1;
            int nextInt = getBegining();
            long nextLong;
            BigInteger nextBigInteger;

            public TemplateModel next() throws TemplateModelException {
                if (needInc) {
                    switch (nextType) {
                    case 1:
                        if (nextInt < Integer.MAX_VALUE) {
                            nextInt++;
                        } else {
                            nextType = 2;
                            nextLong = nextInt + 1L;
                        }
                        break;
                        
                    case 2:
                        if (nextLong < Long.MAX_VALUE) {
                            nextLong++;
                        } else {
                            nextType = 3;
                            nextBigInteger = BigInteger.valueOf(nextLong);
                            nextBigInteger = nextBigInteger.add(BigInteger.ONE);
                        }
                        break;
                        
                    default: // 3
                        nextBigInteger = nextBigInteger.add(BigInteger.ONE);
                    }
                }
                needInc = true;
                return nextType == 1 ? new SimpleNumber(nextInt)
                        : (nextType == 2 ? new SimpleNumber(nextLong)
                        : new SimpleNumber(nextBigInteger)); 
            }

            public boolean hasNext() throws TemplateModelException {
                return true;
            }
            
        };
        
    }
    
}
