package freemarker.core;

import java.util.List;
import java.util.StringTokenizer;

import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.StringUtil;

class BuiltInsForStringsBasic {

    static class cap_firstBI extends BuiltInForString {
        TemplateModel calculateResult(String s, Environment env) {
            int i = 0;
            int ln = s.length();
            while (i < ln  &&  Character.isWhitespace(s.charAt(i))) {
                i++;
            }
            if (i < ln) {
                StringBuffer b = new StringBuffer(s);
                b.setCharAt(i, Character.toUpperCase(s.charAt(i)));
                s = b.toString();
            }
            return new SimpleScalar(s);
        }
    }

    static class capitalizeBI extends BuiltInForString {
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(StringUtil.capitalize(s));
        }
    }

    static class chop_linebreakBI extends BuiltInForString {
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(StringUtil.chomp(s));
        }
    }

    static class containsBI extends BuiltIn {
        
        private class BIMethod implements TemplateMethodModelEx {
            
            private final String s;
    
            private BIMethod(String s) {
                this.s = s;
            }
    
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                return s.indexOf(getStringMethodArg(args, 0)) != -1
                        ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
            }
        }
    
        TemplateModel _eval(Environment env) throws TemplateException {
            return new BIMethod(target.evalAndCoerceToString(env,
                    "For sequences/collections (lists and such) use \"?seq_contains\" instead."));
        }
    }

    static class ends_withBI extends BuiltInForString {
    
        private class BIMethod implements TemplateMethodModelEx {
            private String s;
    
            private BIMethod(String s) {
                this.s = s;
            }
    
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                return s.endsWith(getStringMethodArg(args, 0)) ?
                        TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
            }
        }
    
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
        }
    }

    static class index_ofBI extends BuiltIn {
        
        private class BIMethod implements TemplateMethodModelEx {
            
            private final String s;
            
            private BIMethod(String s) {
                this.s = s;
            }
            
            public Object exec(List args) throws TemplateModelException {
                int argCnt = args.size();
                checkMethodArgCount(argCnt, 1, 2);
                String subStr = getStringMethodArg(args, 0);
                if (argCnt > 1) {
                    int startIdx = getNumberMethodArg(args, 1).intValue();
                    return new SimpleNumber(findLast ? s.lastIndexOf(subStr, startIdx) : s.indexOf(subStr, startIdx));
                } else {
                    return new SimpleNumber(findLast ? s.lastIndexOf(subStr) : s.indexOf(subStr));
                }
            }
        }
        
        private final boolean findLast;
    
        index_ofBI(boolean findLast) {
            this.findLast = findLast;
        }
        
        TemplateModel _eval(Environment env) throws TemplateException {
            return new BIMethod(target.evalAndCoerceToString(env,
                    "For sequences/collections (lists and such) use \"?seq_index_of\" instead."));
        } 
    }

    static class lengthBI extends BuiltInForString {
    
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new SimpleNumber(s.length());
        }
        
    }

    static class lower_caseBI extends BuiltInForString {
        TemplateModel calculateResult(String s, Environment env)
        {
            return new SimpleScalar(s.toLowerCase(env.getLocale()));
        }
    }

    static class padBI extends BuiltInForString {
        
        private class BIMethod implements TemplateMethodModelEx {
            
            private final String s;
    
            private BIMethod(String s) {
                this.s = s;
            }
    
            public Object exec(List args) throws TemplateModelException {
                int argCnt  = args.size();
                checkMethodArgCount(argCnt, 1, 2);
    
                int width = getNumberMethodArg(args, 0).intValue();
    
                if (argCnt > 1) {
                    String filling = getStringMethodArg(args, 1);
                    try {
                        return new SimpleScalar(
                                leftPadder
                                        ? StringUtil.leftPad(s, width, filling)
                                        : StringUtil.rightPad(s, width, filling));
                    } catch (IllegalArgumentException e) {
                        if (filling.length() == 0) {
                            throw new _TemplateModelException(new Object[] {
                                    "?", key, "(...) argument #2 can't be a 0-length string." });
                        } else {
                            throw new _TemplateModelException(e, new Object[] {
                                    "?", key, "(...) failed: ", e });
                        }
                    }
                } else {
                    return new SimpleScalar(leftPadder ? StringUtil.leftPad(s, width) : StringUtil.rightPad(s, width));
                }
            }
        }
    
        private final boolean leftPadder;
    
        padBI(boolean leftPadder) {
            this.leftPadder = leftPadder;
        }
    
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
        }
    }

    static class starts_withBI extends BuiltInForString {
    
        private class BIMethod implements TemplateMethodModelEx {
            private String s;
    
            private BIMethod(String s) {
                this.s = s;
            }
    
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                return s.startsWith(getStringMethodArg(args, 0)) ?
                        TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
            }
        }
    
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
        }
    }

    static class substringBI extends BuiltInForString {
        
        TemplateModel calculateResult(final String s, final Environment env) throws TemplateException {
            return new TemplateMethodModelEx() {
                
                public Object exec(java.util.List args) throws TemplateModelException {
                    int argCount = args.size();
                    checkMethodArgCount(argCount, 1, 2);
    
                    int beginIdx = getNumberMethodArg(args, 0).intValue();
    
                    final int len = s.length();
    
                    if (beginIdx < 0) {
                        throw newIndexLessThan0Exception(0, beginIdx);
                    } else if (beginIdx > len) {
                        throw newIndexGreaterThanLengthException(0, beginIdx, len);
                    }
    
                    if (argCount > 1) {
                        int endIdx = getNumberMethodArg(args, 1).intValue();
                        if (endIdx < 0) {
                            throw newIndexLessThan0Exception(1, endIdx);
                        } else if (endIdx > len) {
                            throw newIndexGreaterThanLengthException(1, endIdx, len);
                        }
                        if (beginIdx > endIdx) {
                            throw MessageUtil.newMethodArgsInvalidValueException(
                                    "?" + key, new Object[] {
                                            "The begin index argument, ", new Integer(beginIdx),
                                            ", shouldn't be greater than the end index argument, ",
                                            new Integer(endIdx), "." });
                        }
                        return new SimpleScalar(s.substring(beginIdx, endIdx));
                    } else {
                        return new SimpleScalar(s.substring(beginIdx));
                    }
                }
    
                private TemplateModelException newIndexGreaterThanLengthException(
                        int argIdx, int idx, final int len) throws TemplateModelException {
                    return MessageUtil.newMethodArgInvalidValueException(
                            "?" + key, argIdx, new Object[] {
                                    "The index mustn't be greater than the length of the string, ",
                                    new Integer(len),
                                    ", but it was ", new Integer(idx), "." });
                }
    
                private TemplateModelException newIndexLessThan0Exception(
                        int argIdx, int idx) throws TemplateModelException {
                    return MessageUtil.newMethodArgInvalidValueException(
                            "?" + key, argIdx, new Object[] {
                                    "The index must be at least 0, but was ",
                                    new Integer(idx), "." });
                }
                
            };
        }
    }

    static class trimBI extends BuiltInForString {
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(s.trim());
        }
    }

    static class uncap_firstBI extends BuiltInForString {
        TemplateModel calculateResult(String s, Environment env) {
            int i = 0;
            int ln = s.length();
            while (i < ln  &&  Character.isWhitespace(s.charAt(i))) {
                i++;
            }
            if (i < ln) {
                StringBuffer b = new StringBuffer(s);
                b.setCharAt(i, Character.toLowerCase(s.charAt(i)));
                s = b.toString();
            }
            return new SimpleScalar(s);
        }
    }

    static class upper_caseBI extends BuiltInForString {
        TemplateModel calculateResult(String s, Environment env)
        {
            return new SimpleScalar(s.toUpperCase(env.getLocale()));
        }
    }

    static class word_listBI extends BuiltInForString {
        TemplateModel calculateResult(String s, Environment env) {
            SimpleSequence result = new SimpleSequence();
            StringTokenizer st = new StringTokenizer(s);
            while (st.hasMoreTokens()) {
               result.add(st.nextToken());
            }
            return result;
        }
    }

    // Can't be instantiated
    private BuiltInsForStringsBasic() { }
    
}
