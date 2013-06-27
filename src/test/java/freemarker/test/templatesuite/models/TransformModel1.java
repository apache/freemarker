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

package freemarker.test.templatesuite.models;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import freemarker.template.TemplateTransformModel;

/**
 * A TemplateTransformModel that includes properties. These properties can be
 * set at model construction time, or, for the purposes of this demonstration,
 * can be passed in from a wrapper TemplateMethodModel.
 */
public class TransformModel1 implements TemplateTransformModel {

    private boolean m_bAmpersands = false;
    private boolean m_bQuotes = false;
    private boolean m_bTags = false;
    private String  m_aComment = "";

    private static final int READER_BUFFER_SIZE = 4096;

    public Writer getWriter(final Writer out, 
                            final Map args) 
    {
        final StringBuilder buf = new StringBuilder();
        return new Writer(out) {
            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
            }

            public void flush() {
            }

            public void close() throws IOException {
                StringReader sr = new StringReader(buf.toString());
                StringWriter sw = new StringWriter();
                transform(sr, sw);
                out.write(sw.toString());
            }
        };
    }


    /**
     * Indicates whether we escape ampersands. This property can be set either
     * while the model is being constructed, or via a property passed in through
     * a <code>TemplateMethodModel</code>.
     */
    public void setAmpersands( boolean bAmpersands ) {
        m_bAmpersands = bAmpersands;
    }

    /**
     * Indicates whether we escape quotes. This property can be set either
     * while the model is being constructed, or via a property passed in through
     * a <code>TemplateMethodModel</code>.
     */
    public void setQuotes( boolean bQuotes ) {
        m_bQuotes = bQuotes;
    }

    /**
     * Indicates whether we escape tags. This property can be set either
     * while the model is being constructed, or via a property passed in through
     * a <code>TemplateMethodModel</code>.
     */
    public void setTags( boolean bTags ) {
        m_bTags = bTags;
    }

    /**
     * Sets a comment for this transformation. This property can be set either
     * while the model is being constructed, or via a property passed in through
     * a <code>TemplateMethodModel</code>.
     */
    public void setComment( String aComment ) {
        m_aComment = aComment;
    }

    /**
     * Performs a transformation/filter on FreeMarker output.
     *
     * @param source the input to be transformed
     * @param output the destination of the transformation
     */
    public void transform(Reader source, Writer output)
    throws IOException 
    {
        // Output the source, converting unsafe certain characters to their
        // equivalent entities.
        int n = 0;
        boolean bCommentSent = false;
        char[]  aBuffer = new char[ READER_BUFFER_SIZE ];
        int i = source.read( aBuffer );
        while (i >= 0) {
            for ( int j = 0; j < i; j++ ) {
                char c = aBuffer[j];
                switch (c) {
                    case '&':
                        if ( m_bAmpersands ) {
                            output.write("&amp;");
                        } else {
                            output.write( c );
                        }
                        break;
                    case '<':
                        if ( m_bTags ) {
                            output.write("&lt;");
                        } else {
                            output.write( c );
                        }
                        break;
                    case '>':
                        if ( m_bTags ) {
                            output.write("&gt;");
                        } else {
                            output.write( c );
                        }
                        break;
                    case '"':
                        if ( m_bQuotes ) {
                            output.write("&quot;");
                        } else {
                            output.write( c );
                        }
                        break;
                    case '\'':
                        if ( m_bQuotes ) {
                            output.write("&apos;");
                        } else {
                            output.write( c );
                        }
                        break;
                    case '*':
                        if ( ! bCommentSent ) {
                            output.write( m_aComment );
                            bCommentSent = true;
                        } else {
                            output.write( c );
                        }
                        break;
                    default:
                        output.write(c);
                }
                n++;
            }
            i = source.read( aBuffer );
        }
    }
}
