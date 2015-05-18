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
import java.io.Writer;
import java.util.Map;

import freemarker.template.TemplateTransformModel;

/**
 * Performs an HTML escape of a given template fragment. Specifically,
 * &lt; &gt; &quot; and &amp; are all turned into entities.
 *
 * <p>Usage:<br>
 * From java:</p>
 * <pre>
 * SimpleHash root = new SimpleHash();
 *
 * root.put( "htmlEscape", new freemarker.template.utility.HtmlEscape() );
 *
 * ...
 * </pre>
 *
 * <p>From your FreeMarker template:</p>
 * <pre>
 *
 * The following is HTML-escaped:
 * &lt;transform htmlEscape&gt;
 *   &lt;p&gt;This paragraph has all HTML special characters escaped.&lt;/p&gt;
 * &lt;/transform&gt;
 *
 * ...
 * </pre>
 *
 * @see freemarker.template.utility.XmlEscape
 */
public class HtmlEscape implements TemplateTransformModel {

    private static final char[] LT = "&lt;".toCharArray();
    private static final char[] GT = "&gt;".toCharArray();
    private static final char[] AMP = "&amp;".toCharArray();
    private static final char[] QUOT = "&quot;".toCharArray();

    public Writer getWriter(final Writer out, Map args)
    {
        return new Writer()
        {
            public void write(int c)
            throws
                IOException
            {
                switch(c)
                {
                    case '<': out.write(LT, 0, 4); break;
                    case '>': out.write(GT, 0, 4); break;
                    case '&': out.write(AMP, 0, 5); break;
                    case '"': out.write(QUOT, 0, 6); break;
                    default: out.write(c);
                }
            }

            public void write(char cbuf[], int off, int len)
            throws
                IOException
            {
                int lastoff = off;
                int lastpos = off + len;
                for (int i = off; i < lastpos; i++)
                {
                    switch (cbuf[i])
                    {
                        case '<': out.write(cbuf, lastoff, i - lastoff); out.write(LT, 0, 4); lastoff = i + 1; break;
                        case '>': out.write(cbuf, lastoff, i - lastoff); out.write(GT, 0, 4); lastoff = i + 1; break;
                        case '&': out.write(cbuf, lastoff, i - lastoff); out.write(AMP, 0, 5); lastoff = i + 1; break;
                        case '"': out.write(cbuf, lastoff, i - lastoff); out.write(QUOT, 0, 6); lastoff = i + 1; break;
                    }
                }
                int remaining = lastpos - lastoff;
                if(remaining > 0)
                {
                    out.write(cbuf, lastoff, remaining);
                }
            }
            public void flush() throws IOException {
                out.flush();
            }

            public void close() {
            }
        };
    }
}
