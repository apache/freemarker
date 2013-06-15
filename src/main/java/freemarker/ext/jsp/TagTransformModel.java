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

package freemarker.ext.jsp;

import java.beans.IntrospectionException;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.IterationTag;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TryCatchFinally;

import freemarker.log.Logger;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;
import freemarker.template.TransformControl;

/**
 * @author Attila Szegedi
 */
class TagTransformModel extends JspTagModelBase implements TemplateTransformModel
{
    private static final Logger logger = Logger.getLogger("freemarker.jsp");
    
    private final boolean isBodyTag;
    private final boolean isIterationTag;
    private final boolean isTryCatchFinally;
            
    public TagTransformModel(Class tagClass) throws IntrospectionException {
        super(tagClass);
        isIterationTag = IterationTag.class.isAssignableFrom(tagClass);
        isBodyTag = isIterationTag && BodyTag.class.isAssignableFrom(tagClass);
        isTryCatchFinally = TryCatchFinally.class.isAssignableFrom(tagClass);
    }
    
    public Writer getWriter(Writer out, Map args) throws TemplateModelException
    {
        try {
            Tag tag = (Tag)getTagInstance();
            FreeMarkerPageContext pageContext = PageContextFactory.getCurrentPageContext();
            Tag parentTag = (Tag)pageContext.peekTopTag(Tag.class);
            tag.setParent(parentTag);
            tag.setPageContext(pageContext);
            setupTag(tag, args, pageContext.getObjectWrapper());
            // If the parent of this writer is not a JspWriter itself, use
            // a little Writer-to-JspWriter adapter...
            boolean usesAdapter;
            if(out instanceof JspWriter) {
                // This is just a sanity check. If it were JDK 1.4-only,
                // we'd use an assert.
                if(out != pageContext.getOut()) {
                    throw new TemplateModelException(
                        "out != pageContext.getOut(). Out is " + 
                        out + " pageContext.getOut() is " +
                        pageContext.getOut());
                }
                usesAdapter = false;
            }
            else {                
                out = new JspWriterAdapter(out);
                pageContext.pushWriter((JspWriter)out);
                usesAdapter = true;
            }
            JspWriter w = new TagWriter(out, tag, pageContext, usesAdapter);
            pageContext.pushTopTag(tag);
            pageContext.pushWriter(w);
            return w;
        }
        catch(TemplateModelException e) {
            throw e;
        }
        catch(RuntimeException e) {
            throw e;
        }
        catch(Exception e) {
            throw new TemplateModelException(e);
        }
    }

    /**
     * An implementation of BodyContent that buffers it's input to a char[].
     */
    static class BodyContentImpl extends BodyContent {
        private CharArrayWriter buf;

        BodyContentImpl(JspWriter out, boolean buffer) {
            super(out);
            if (buffer) initBuffer();
        }

        void initBuffer() {
            buf = new CharArrayWriter();
        }

        public void flush() throws IOException {
            if(buf == null) {
                getEnclosingWriter().flush();
            }
        }

        public void clear() throws IOException {
            if(buf != null) {
                buf = new CharArrayWriter();
            }
            else {
                throw new IOException("Can't clear");
            }
        }

        public void clearBuffer() throws IOException {
            if(buf != null) {
                buf = new CharArrayWriter();
            }
            else {
                throw new IOException("Can't clear");
            }
        }

        public int getRemaining() {
            return Integer.MAX_VALUE;
        }

        public void newLine() throws IOException {
            write(JspWriterAdapter.NEWLINE);
        }

        public void close() throws IOException {
        }

        public void print(boolean arg0) throws IOException {
            write(arg0 ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
        }

        public void print(char arg0) throws IOException
        {
            write(arg0);
        }

        public void print(char[] arg0) throws IOException
        {
            write(arg0);
        }

        public void print(double arg0) throws IOException
        {
            write(Double.toString(arg0));
        }

        public void print(float arg0) throws IOException
        {
            write(Float.toString(arg0));
        }

        public void print(int arg0) throws IOException
        {
            write(Integer.toString(arg0));
        }

        public void print(long arg0) throws IOException
        {
            write(Long.toString(arg0));
        }

        public void print(Object arg0) throws IOException
        {
            write(arg0 == null ? "null" : arg0.toString());
        }

        public void print(String arg0) throws IOException
        {
            write(arg0);
        }

        public void println() throws IOException
        {
            newLine();
        }

        public void println(boolean arg0) throws IOException
        {
            print(arg0);
            newLine();
        }

        public void println(char arg0) throws IOException
        {
            print(arg0);
            newLine();
        }

        public void println(char[] arg0) throws IOException
        {
            print(arg0);
            newLine();
        }

        public void println(double arg0) throws IOException
        {
            print(arg0);
            newLine();
        }

        public void println(float arg0) throws IOException
        {
            print(arg0);
            newLine();
        }

        public void println(int arg0) throws IOException
        {
            print(arg0);
            newLine();
        }

        public void println(long arg0) throws IOException
        {
            print(arg0);
            newLine();
        }

        public void println(Object arg0) throws IOException
        {
            print(arg0);
            newLine();
        }

        public void println(String arg0) throws IOException
        {
            print(arg0);
            newLine();
        }

        public void write(int c) throws IOException
        {
            if(buf != null) {
                buf.write(c);
            }
            else {
                getEnclosingWriter().write(c);
            }
        }

        public void write(char[] cbuf, int off, int len) throws IOException
        {
            if(buf != null) {
                buf.write(cbuf, off, len);
            }
            else {
                getEnclosingWriter().write(cbuf, off, len);
            }
        }

        public String getString() {
            return buf.toString();
        }

        public Reader getReader() {
            return new CharArrayReader(buf.toCharArray());
        }

        public void writeOut(Writer out) throws IOException {
            buf.writeTo(out);
        }

    }

    class TagWriter extends BodyContentImpl implements TransformControl
    {
        private final Tag tag;
        private final FreeMarkerPageContext pageContext;
        private boolean needPop = true;
        private final boolean needDoublePop;
        
        TagWriter(Writer out, Tag tag, FreeMarkerPageContext pageContext, boolean needDoublePop)
        {
            super((JspWriter)out, false);
            this.needDoublePop = needDoublePop;
            this.tag = tag;
            this.pageContext = pageContext;
        }
        
        public String toString() {
            return "TagWriter for " + tag.getClass().getName() + " wrapping a " + getEnclosingWriter().toString();
        }

        Tag getTag()
        {
            return tag;
        }
        
        FreeMarkerPageContext getPageContext()
        {
            return pageContext;
        }
        
        public int onStart()
        throws
            TemplateModelException
        {
            try {
                int dst = tag.doStartTag();
                switch(dst) {
                    case Tag.SKIP_BODY:
                    // EVAL_PAGE is illegal actually, but some taglibs out there
                    // use it, and it seems most JSP compilers allow them to and
                    // treat it identically to SKIP_BODY, so we're going with 
                    // the flow and we allow it too, altough strictly speaking
                    // it's in violation of the spec.
                    case Tag.EVAL_PAGE: {
                        endEvaluation();
                        return TransformControl.SKIP_BODY;
                    }
                    case BodyTag.EVAL_BODY_BUFFERED: {
                        if(isBodyTag) {
                            initBuffer();
                            BodyTag btag = (BodyTag)tag;
                            btag.setBodyContent(this);
                            btag.doInitBody();
                        }
                        else {
                            throw new TemplateModelException("Can't buffer body since " + tag.getClass().getName() + " does not implement BodyTag.");
                        }
                        // Intentional fall-through
                    }
                    case Tag.EVAL_BODY_INCLUDE: {
                        return TransformControl.EVALUATE_BODY;
                    }
                    default: {
                        throw new RuntimeException("Illegal return value " + dst + " from " + tag.getClass().getName() + ".doStartTag()");
                    }
                }
            }
            catch(JspException e) {
                throw new TemplateModelException(e.getMessage(), e);
            }
        }
        
        public int afterBody()
        throws
            TemplateModelException
        {
            try {
                if(isIterationTag) {
                    int dab = ((IterationTag)tag).doAfterBody();
                    switch(dab) {
                        case Tag.SKIP_BODY: {
                            endEvaluation();
                            return END_EVALUATION;
                        }
                        case IterationTag.EVAL_BODY_AGAIN: {
                            return REPEAT_EVALUATION;
                        }
                        default: {
                            throw new TemplateModelException("Unexpected return value " + dab + "from " + tag.getClass().getName() + ".doAfterBody()");
                        }
                    }
                }
                endEvaluation();
                return END_EVALUATION;
            }
            catch(JspException e) {
                throw new TemplateModelException(e);
            }
        }
        
        private void endEvaluation() throws JspException {
            if(needPop) {
                pageContext.popWriter();
                needPop = false;
            }
            if(tag.doEndTag() == Tag.SKIP_PAGE) {
                logger.warn("Tag.SKIP_PAGE was ignored from a " + tag.getClass().getName() + " tag.");
            }
        }
        
        public void onError(Throwable t) throws Throwable {
            if(isTryCatchFinally) {
                ((TryCatchFinally)tag).doCatch(t);
            }
            else {
                throw t;
            }
        }
        
        public void close() {
            if(needPop) {
                pageContext.popWriter();
            }
            pageContext.popTopTag();
            try {
                if(isTryCatchFinally) {
                    ((TryCatchFinally)tag).doFinally();
                }
                // No pooling yet
                tag.release();
            }
            finally {
                if(needDoublePop) {
                    pageContext.popWriter();
                }
            }
        }
        
    }
}
