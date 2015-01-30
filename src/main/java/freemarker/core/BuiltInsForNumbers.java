/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import freemarker.template.SimpleDate;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.utility.NumberUtil;
import freemarker.template.utility.StringUtil;

/**
 * A holder for builtins that operate exclusively on number left-hand value.
 */
class BuiltInsForNumbers {

    private static abstract class abcBI extends BuiltInForNumber {

        TemplateModel calculateResult(Number num, TemplateModel model) throws TemplateModelException {
            final int n;
            try {
                n = NumberUtil.toIntExact(num);
            } catch (ArithmeticException e) {
                throw new _TemplateModelException(target, new Object[] {
                        "The left side operand value isn't compatible with ?", key, ": ",
                        e.getMessage() });
         
            }
            if (n <= 0) {
                throw new _TemplateModelException(target, new Object[] {
                        "The left side operand of to ?", key, " must be at least 1, but was ",
                        new Integer(n), "." });
            }
            return new SimpleScalar(toABC(n));
        }

        protected abstract String toABC(int n);
        
    }

    static class lower_abcBI extends abcBI {

        protected String toABC(int n) {
            return StringUtil.toLowerABC(n);
        }
        
    }

    static class upper_abcBI extends abcBI {

        protected String toABC(int n) {
            return StringUtil.toUpperABC(n);
        }
        
    }
    
    static class absBI extends BuiltInForNumber {
        TemplateModel calculateResult(Number num, TemplateModel model) throws TemplateModelException {
            if (num instanceof Integer) {
                int n = ((Integer) num).intValue();
                if (n < 0) {
                    return new SimpleNumber(-n);
                } else {
                    return model;
                }
            } else if (num instanceof BigDecimal) {
                BigDecimal n = (BigDecimal) num;
                if (n.signum() < 0) {
                    return new SimpleNumber(n.negate());
                } else {
                    return model;
                }
            } else if (num instanceof Double) {
                double n = ((Double) num).doubleValue();
                if (n < 0) {
                    return new SimpleNumber(-n);
                } else {
                    return model;
                }
            } else if (num instanceof Float) {
                float n = ((Float) num).floatValue();
                if (n < 0) {
                    return new SimpleNumber(-n);
                } else {
                    return model;
                }
            } else if (num instanceof Long) {
                long n = ((Long) num).longValue();
                if (n < 0) {
                    return new SimpleNumber(-n);
                } else {
                    return model;
                }
            } else if (num instanceof Short) {
                short n = ((Short) num).shortValue();
                if (n < 0) {
                    return new SimpleNumber(-n);
                } else {
                    return model;
                }
            } else if (num instanceof Byte) {
                byte n = ((Byte) num).byteValue();
                if (n < 0) {
                    return new SimpleNumber(-n);
                } else {
                    return model;
                }
            } else if (num instanceof BigInteger) {
                BigInteger n = (BigInteger) num;
                if (n.signum() < 0) {
                    return new SimpleNumber(n.negate());
                } else {
                    return model;
                }
            } else {
                throw new _TemplateModelException(new Object[] {
                        "Unsupported number class: ", num.getClass() });
            }            
        }
    }
    
    static class byteBI extends BuiltInForNumber {
        TemplateModel calculateResult(Number num, TemplateModel model) {
            if (num instanceof Byte) {
                return model;
            }
            return new SimpleNumber(new Byte(num.byteValue()));
        }
    }

    static class ceilingBI extends BuiltInForNumber {
        TemplateModel calculateResult(Number num, TemplateModel model) {
            return new SimpleNumber(new BigDecimal(num.doubleValue()).divide(BIG_DECIMAL_ONE, 0, BigDecimal.ROUND_CEILING));
        }
    }

    static class doubleBI extends BuiltInForNumber {
        TemplateModel calculateResult(Number num, TemplateModel model) {
            if (num instanceof Double) {
                return model;
            }
            return new SimpleNumber(num.doubleValue());
        }
    }

    static class floatBI extends BuiltInForNumber {
        TemplateModel calculateResult(Number num, TemplateModel model) {
            if (num instanceof Float) {
                return model;
            }
            return new SimpleNumber(num.floatValue());
        }
    }

    static class floorBI extends BuiltInForNumber {
        TemplateModel calculateResult(Number num, TemplateModel model) {
            return new SimpleNumber(new BigDecimal(num.doubleValue()).divide(BIG_DECIMAL_ONE, 0, BigDecimal.ROUND_FLOOR));
        }
    }

    static class intBI extends BuiltInForNumber {
        TemplateModel calculateResult(Number num, TemplateModel model) {
            if (num instanceof Integer) {
                return model;
            }
            return new SimpleNumber(num.intValue());
        }
    }

    static class is_infiniteBI extends BuiltInForNumber {
        TemplateModel calculateResult(Number num, TemplateModel model) throws TemplateModelException {
            return NumberUtil.isInfinite(num) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }


    static class is_nanBI extends BuiltInForNumber {
        TemplateModel calculateResult(Number num, TemplateModel model) throws TemplateModelException {
            return NumberUtil.isNaN(num) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    // Does both someNumber?long and someDate?long, thus it doesn't extend NumberBuiltIn
    static class longBI extends BuiltIn {
        TemplateModel _eval(Environment env)
                throws TemplateException
        {
            TemplateModel model = target.eval(env);
            if (!(model instanceof TemplateNumberModel)
                    && model instanceof TemplateDateModel) {
                Date date = EvalUtil.modelToDate((TemplateDateModel) model, target);
                return new SimpleNumber(date.getTime());
            } else {
                Number num = target.modelToNumber(model, env);
                if (num instanceof Long) {
                    return model;
                }
                return new SimpleNumber(num.longValue());
            }
        }
    }

    static class number_to_dateBI extends BuiltInForNumber {
        
        private final int dateType;
        
        number_to_dateBI(int dateType) {
            this.dateType = dateType;
        }
        
        TemplateModel calculateResult(Number num, TemplateModel model)
        throws TemplateModelException {
            return new SimpleDate(new Date(safeToLong(num)), dateType);
        }
    }

    static class roundBI extends BuiltInForNumber {
        private static final BigDecimal half = new BigDecimal("0.5");
        TemplateModel calculateResult(Number num, TemplateModel model) {
            return new SimpleNumber(new BigDecimal(num.doubleValue()).add(half).divide(BIG_DECIMAL_ONE, 0, BigDecimal.ROUND_FLOOR));
        }
    }
    
    static class shortBI extends BuiltInForNumber {
        TemplateModel calculateResult(Number num, TemplateModel model) {
            if (num instanceof Short) {
                return model;
            }
            return new SimpleNumber(new Short(num.shortValue()));
        }
    }

    private static final long safeToLong(Number num) throws TemplateModelException {
        if (num instanceof Double) {
            double d = Math.round(((Double) num).doubleValue());
            if (d > Long.MAX_VALUE || d < Long.MIN_VALUE) {
                throw new _TemplateModelException(new Object[] {
                        "Number doesn't fit into a 64 bit signed integer (long): ", new Double(d) });
            } else {
                return (long) d;
            }
        } else if (num instanceof Float) {
            float f = Math.round(((Float) num).floatValue());
            if (f > Long.MAX_VALUE || f < Long.MIN_VALUE) {
                throw new _TemplateModelException(new Object[] {
                        "Number doesn't fit into a 64 bit signed integer (long): ", new Float(f) });
            } else {
                return (long) f;
            }
        } else if (num instanceof BigDecimal) {
            BigDecimal bd = ((BigDecimal) num).setScale(0, BigDecimal.ROUND_HALF_UP);
            if (bd.compareTo(BIG_DECIMAL_LONG_MAX) > 0 || bd.compareTo(BIG_DECIMAL_LONG_MIN) < 0) {
                throw new _TemplateModelException(new Object[] {
                        "Number doesn't fit into a 64 bit signed integer (long): ", bd });
            } else {
                return bd.longValue();
            }
        } else if (num instanceof BigInteger) {
            BigInteger bi = (BigInteger) num;
            if (bi.compareTo(BIG_INTEGER_LONG_MAX) > 0 || bi.compareTo(BIG_INTEGER_LONG_MIN) < 0) {
                throw new _TemplateModelException(new Object[] {
                        "Number doesn't fit into a 64 bit signed integer (long): ", bi });
            } else {
                return bi.longValue();
            }
        } else if (num instanceof Long || num instanceof Integer || num instanceof Byte || num instanceof Short) {
            return num.longValue();
        } else {
            // Should add Atomic* types in 2.4...
            throw new _TemplateModelException(new Object[] { "Unsupported number type: ", num.getClass() });
        }
    }
    
    private static final BigDecimal BIG_DECIMAL_ONE = new BigDecimal("1");
    private static final BigDecimal BIG_DECIMAL_LONG_MIN = BigDecimal.valueOf(Long.MIN_VALUE); 
    private static final BigDecimal BIG_DECIMAL_LONG_MAX = BigDecimal.valueOf(Long.MAX_VALUE);
    private static final BigInteger BIG_INTEGER_LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE); 
    
    private static final BigInteger BIG_INTEGER_LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    
    // Can't be instantiated
    private BuiltInsForNumbers() { }
    
}
