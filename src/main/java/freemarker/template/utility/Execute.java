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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import freemarker.template.TemplateModelException;

/**
 * <p>Gives FreeMarker the the ability to execute external commands. Will fork
 * a process, and inline anything that process sends to stdout in the template.
 * Based on a patch submitted by Peter Molettiere.</p>
 *
 * <p>BE CAREFUL! this tag, depending on use, may allow you
 * to set something up so that users of your web
 * application could run arbitrary code on your server.
 * This can only happen if you allow unchecked GET/POST
 * submissions to be used as the command string in the
 * exec tag.</p>
 *
 * <p>Usage:<br />
 * From java:</p>
 * <pre>
 * SimpleHash root = new SimpleHash();
 *
 * root.put( "exec", new freemarker.template.utility.Execute() );
 *
 * ...
 * </pre>
 *
 * <p>From your FreeMarker template:</p>
 * <pre>
 *
 * The following is executed:
 * ${exec( "/usr/bin/ls" )}
 *
 * ...
 * </pre>
 */
public class Execute implements freemarker.template.TemplateMethodModel {

    private final static int OUTPUT_BUFFER_SIZE = 1024;

    /**
     * Executes a method call.
     *
     * @param arguments a <tt>List</tt> of <tt>String</tt> objects containing the values
     * of the arguments passed to the method.
     * @return the <tt>TemplateModel</tt> produced by the method, or null.
     */
    public Object exec (List arguments) throws TemplateModelException {
        String aExecute;
        StringBuffer    aOutputBuffer = new StringBuffer();

        if( arguments.size() < 1 ) {
            throw new TemplateModelException( "Need an argument to execute" );
        }

        aExecute = (String)(arguments.get(0));

        try {
            Process exec = Runtime.getRuntime().exec( aExecute );

            // stdout from the process comes in here
            InputStream execOut = exec.getInputStream();
            Reader execReader = new InputStreamReader( execOut );

            char[] buffer = new char[ OUTPUT_BUFFER_SIZE ];
            int bytes_read = execReader.read( buffer );

            while( bytes_read > 0 ) {
                aOutputBuffer.append( buffer, 0, bytes_read );
                bytes_read = execReader.read( buffer );
            }
        }
        catch( IOException ioe ) {
            throw new TemplateModelException( ioe.getMessage() );
        }
        return aOutputBuffer.toString();
    }
}
