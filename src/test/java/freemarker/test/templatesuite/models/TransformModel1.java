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
