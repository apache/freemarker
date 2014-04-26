package freemarker.ext.beans;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Flag values and masks for "type flags". "Type flags" is a set of bits that store information about the possible
 * destination types at a parameter position of overloaded methods. 
 */
class TypeFlags {

    /**
     * Indicates that the unwrapping hint will not be a specific numerical type; it must not be set if there's no
     * numerical type at the given parameter index.
     */
    static final int WIDENED_NUMERICAL_UNWRAPPING_HINT = 1;
    
    static final int BYTE = 4;
    static final int SHORT = 8;
    static final int INTEGER = 16;
    static final int LONG = 32;
    static final int FLOAT = 64;
    static final int DOUBLE = 128;
    static final int BIG_INTEGER = 256;
    static final int BIG_DECIMAL = 512;
    static final int UNKNOWN_NUMERICAL_TYPE = 1024;

    static final int ACCEPTS_NUMBER = 0x800;
    static final int ACCEPTS_DATE = 0x1000;
    static final int ACCEPTS_STRING = 0x2000;
    static final int ACCEPTS_BOOLEAN = 0x4000;
    static final int ACCEPTS_MAP = 0x8000;
    static final int ACCEPTS_LIST = 0x10000;
    static final int ACCEPTS_SET = 0x20000;
    static final int ACCEPTS_ARRAY = 0x40000;
    static final int ACCEPTS_CHAR = 0x80000;
    
    static final int ACCEPTS_ANY_OBJECT = ACCEPTS_NUMBER | ACCEPTS_DATE | ACCEPTS_STRING | ACCEPTS_BOOLEAN
            | ACCEPTS_MAP | ACCEPTS_LIST | ACCEPTS_SET | ACCEPTS_ARRAY | ACCEPTS_CHAR;
    
    static final int MASK_KNOWN_INTEGERS = BYTE | SHORT | INTEGER | LONG | BIG_INTEGER;
    static final int MASK_KNOWN_NONINTEGERS = FLOAT | DOUBLE | BIG_DECIMAL;
    static final int MASK_ALL_KNOWN_NUMERICALS = MASK_KNOWN_INTEGERS | MASK_KNOWN_NONINTEGERS;
    static final int MASK_ALL_NUMERICALS = MASK_ALL_KNOWN_NUMERICALS | UNKNOWN_NUMERICAL_TYPE;
    
    static int classToTypeFlags(Class pClass) {
        // We start with the most frequent cases  
        if (pClass == Object.class) {
            return ACCEPTS_ANY_OBJECT;
        } else if (pClass == String.class) {
            return ACCEPTS_STRING;
        } else if (pClass.isPrimitive()) {
            if (pClass == Integer.TYPE) return INTEGER | ACCEPTS_NUMBER;
            else if (pClass == Long.TYPE) return LONG | ACCEPTS_NUMBER;
            else if (pClass == Double.TYPE) return DOUBLE | ACCEPTS_NUMBER;
            else if (pClass == Float.TYPE) return FLOAT | ACCEPTS_NUMBER;
            else if (pClass == Byte.TYPE) return BYTE | ACCEPTS_NUMBER;
            else if (pClass == Short.TYPE) return SHORT | ACCEPTS_NUMBER;
            else if (pClass == Character.TYPE) return ACCEPTS_CHAR;
            else if (pClass == Boolean.TYPE) return ACCEPTS_BOOLEAN;
            else return 0;
        } else if (Number.class.isAssignableFrom(pClass)) {
            if (pClass == Integer.class) return INTEGER | ACCEPTS_NUMBER;
            else if (pClass == Long.class) return LONG | ACCEPTS_NUMBER;
            else if (pClass == Double.class) return DOUBLE | ACCEPTS_NUMBER;
            else if (pClass == Float.class) return FLOAT | ACCEPTS_NUMBER;
            else if (pClass == Byte.class) return BYTE | ACCEPTS_NUMBER;
            else if (pClass == Short.class) return SHORT | ACCEPTS_NUMBER;
            else if (BigDecimal.class.isAssignableFrom(pClass)) return BIG_DECIMAL | ACCEPTS_NUMBER;
            else if (BigInteger.class.isAssignableFrom(pClass)) return BIG_INTEGER | ACCEPTS_NUMBER;
            else return UNKNOWN_NUMERICAL_TYPE | ACCEPTS_NUMBER;
        } else if (pClass.isArray()) {
            return ACCEPTS_ARRAY;
        } else {
            int flags = 0;
            if (pClass.isAssignableFrom(String.class)) {
                flags |= ACCEPTS_STRING;
            }
            if (pClass.isAssignableFrom(Date.class)) {
                flags |= ACCEPTS_DATE;
            }
            if (pClass.isAssignableFrom(Boolean.class)) {
                flags |= ACCEPTS_BOOLEAN;
            }
            if (pClass.isAssignableFrom(Map.class)) {
                flags |= ACCEPTS_MAP;
            }
            if (pClass.isAssignableFrom(List.class)) {
                flags |= ACCEPTS_LIST;
            }
            if (pClass.isAssignableFrom(Set.class)) {
                flags |= ACCEPTS_SET;
            }
            if (pClass.isAssignableFrom(Character.class)) {
                flags |= ACCEPTS_CHAR;
            }
            return flags;
        } 
    }

}
