/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.template.utility;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Attila Szegedi
 */
public class OptimizerUtil
{
    private static final BigInteger INTEGER_MIN = new BigInteger(Integer.toString(Integer.MIN_VALUE));
    private static final BigInteger INTEGER_MAX = new BigInteger(Integer.toString(Integer.MAX_VALUE));
    private static final BigInteger LONG_MIN = new BigInteger(Long.toString(Long.MIN_VALUE));
    private static final BigInteger LONG_MAX = new BigInteger(Long.toString(Long.MAX_VALUE));

    private OptimizerUtil()
    {
    }
    
    public static List optimizeListStorage(List list)
    {
        switch(list.size())
        {
            case 0:
            {
                return Collections.EMPTY_LIST;
            }
            case 1:
            {
                return Collections12.singletonList(list.get(0));
            }
            default:
            {
                if(list instanceof ArrayList)
                {
                    ((ArrayList)list).trimToSize();
                }
                return list;
            }
        }
    }
    
    /**
     * This is needed to reverse the extreme conversions in arithmetic 
     * operations so that numbers can be meaningfully used with models that
     * don't know what to do with a BigDecimal. Of course, this will make
     * impossible for these models (i.e. Jython) to receive a BigDecimal even if 
     * it was originally placed as such in the data model. However, since 
     * arithmetic operations aggressively erase the information regarding the 
     * original number type, we have no other choice to ensure expected operation
     * in majority of cases.
     */
    public static Number optimizeNumberRepresentation(Number number)
    {
        if(number instanceof BigDecimal)
        {
            BigDecimal bd = (BigDecimal) number;
            if(bd.scale() == 0)
            {
                // BigDecimal -> BigInteger
                number = bd.unscaledValue();
            }
            else
            {
                double d = bd.doubleValue();
                if(d != Double.POSITIVE_INFINITY && d != Double.NEGATIVE_INFINITY)
                {
                    // BigDecimal -> Double
                    return new Double(d);
                }
            }
        }
        if(number instanceof BigInteger)
        {
            BigInteger bi = (BigInteger)number;
            if(bi.compareTo(INTEGER_MAX) <= 0 && bi.compareTo(INTEGER_MIN) >= 0)
            {
                // BigInteger -> Integer
                return new Integer(bi.intValue());
            }
            if(bi.compareTo(LONG_MAX) <= 0 && bi.compareTo(LONG_MIN) >= 0)
            {
                // BigInteger -> Long
                return new Long(bi.longValue());
            }
        }
        return number;
    }
}
