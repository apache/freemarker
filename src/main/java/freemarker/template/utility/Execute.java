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
 * <p>Usage:<br>
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
            try {
                Reader execReader = new InputStreamReader( execOut );
    
                char[] buffer = new char[ OUTPUT_BUFFER_SIZE ];
                int bytes_read = execReader.read( buffer );
                while( bytes_read > 0 ) {
                    aOutputBuffer.append( buffer, 0, bytes_read );
                    bytes_read = execReader.read( buffer );
                }
            } finally {
                execOut.close();
            }
        }
        catch( IOException ioe ) {
            throw new TemplateModelException( ioe.getMessage() );
        }
        return aOutputBuffer.toString();
    }
}
