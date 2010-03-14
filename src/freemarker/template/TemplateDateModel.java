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

package freemarker.template;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Date values in a template data model must implement this interface.
 * Contrary to Java, FreeMarker actually distinguishes values that represent
 * only a time, only a date, or a combined date and time. All three are
 * represented using this single interface, however there's a method that
 *
 * @author Attila Szegedi
 *
 * @version $Id: TemplateDateModel.java,v 1.10 2004/03/13 13:05:09 ddekany Exp $
 */
public interface TemplateDateModel extends TemplateModel
{
    /**
     * It is not known whether the date model represents a time-only,
     * a date-only, or a datetime value.
     */
    public static final int UNKNOWN = 0;

    /**
     * The date model represents a time-only value.
     */
    public static final int TIME = 1;

    /**
     * The date model represents a date-only value.
     */
    public static final int DATE = 2;

    /**
     * The date model represents a datetime value.
     */
    public static final int DATETIME = 3;
    
    public static final List TYPE_NAMES =
        Collections.unmodifiableList(
            Arrays.asList(
                new String[] {
                    "UNKNOWN", "TIME", "DATE", "DATETIME"
                }));
    /**
     * Returns the date value. The return value must not be null.
     * @return the {@link Date} instance associated with this date model.
     */
    public Date getAsDate() throws TemplateModelException;

    /**
     * Returns the type of the date. It can be any of <tt>TIME</tt>, 
     * <tt>DATE</tt>, or <tt>DATETIME</tt>.
     */
    public int getDateType();
}
