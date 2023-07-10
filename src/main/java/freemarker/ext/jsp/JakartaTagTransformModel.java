/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package freemarker.ext.jsp;

import freemarker.log.Logger;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;
import freemarker.template.TransformControl;
import java.beans.IntrospectionException;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.tagext.BodyContent;
import jakarta.servlet.jsp.tagext.BodyTag;
import jakarta.servlet.jsp.tagext.IterationTag;
import jakarta.servlet.jsp.tagext.SimpleTag;
import jakarta.servlet.jsp.tagext.Tag;
import jakarta.servlet.jsp.tagext.TryCatchFinally;

/**
 * Adapts a {@link Tag}-based custom JSP tag to be a value that's callable in templates as an user-defined directive.
 * For {@link SimpleTag}-based custom JSP tags {@link SimpleTagDirectiveModel} is used instead.
 */
class JakartaTagTransformModel extends JspTagModelBase implements TemplateTransformModel {
    private static final Logger LOG = Logger.getLogger("freemarker.jsp");

    private final boolean isBodyTag;
    private final boolean isIterationTag;
    private final boolean isTryCatchFinally;

    public JakartaTagTransformModel(String tagName, Class tagClass) throws IntrospectionException {
        super(tagName, tagClass);
        isIterationTag = IterationTag.class.isAssignableFrom(tagClass);
        isBodyTag = isIterationTag && BodyTag.class.isAssignableFrom(tagClass);
        isTryCatchFinally = TryCatchFinally.class.isAssignableFrom(tagClass);
    }
    
    @Override
    public Writer getWriter(Writer out, Map args) throws TemplateModelException {
        try {
            Tag tag = (Tag) getTagInstance();
            JakartaFreeMarkerPageContext pageContext = JakartaPageContextFactory.getCurrentPageContext();
            Tag parentTag = (Tag) pageContext.peekTopTag(Tag.class);
            tag.setParent(parentTag);
            tag.setPageContext(pageContext);
            setupTag(tag, args, pageContext.getObjectWrapper());
            // If the parent of this writer is not a JspWriter itself, use
            // a little Writer-to-JspWriter adapter...
            boolean usesAdapter;
            if (out instanceof JspWriter) {
                // This is just a sanity check. If it were JDK 1.4-only,
                // we'd use an assert.
                if (out != pageContext.getOut()) {
                    throw new TemplateModelException(
                        "out != pageContext.getOut(). Out is " + 
                        out + " pageContext.getOut() is " +
                        pageContext.getOut());
                }
                usesAdapter = false;
            } else {                
                out = new JakartaJspWriterAdapter(out);
                pageContext.pushWriter((JspWriter) out);
                usesAdapter = true;
            }
            JspWriter w = new TagWriter(out, tag, pageContext, usesAdapter);
            pageContext.pushTopTag(tag);
            pageContext.pushWriter(w);
            return w;
        } catch (Exception e) {
            throw toTemplateModelExceptionOrRethrow(e);
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

        @Override
        public void flush() throws IOException {
            if (buf == null) {
                getEnclosingWriter().flush();
            }
        }

        @Override
        public void clear() throws IOException {
            if (buf != null) {
                buf = new CharArrayWriter();
            } else {
                throw new IOException("Can't clear");
            }
        }

        @Override
        public void clearBuffer() throws IOException {
            if (buf != null) {
                buf = new CharArrayWriter();
            } else {
                throw new IOException("Can't clear");
            }
        }

        @Override
        public int getRemaining() {
            return Integer.MAX_VALUE;
        }

        @Override
        public void newLine() throws IOException {
            write(JakartaJspWriterAdapter.NEWLINE);
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public void print(boolean arg0) throws IOException {
            write(arg0 ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
        }

        @Override
        public void print(char arg0) throws IOException {
            write(arg0);
        }

        @Override
        public void print(char[] arg0) throws IOException {
            write(arg0);
        }

        @Override
        public void print(double arg0) throws IOException {
            write(Double.toString(arg0));
        }

        @Override
        public void print(float arg0) throws IOException {
            write(Float.toString(arg0));
        }

        @Override
        public void print(int arg0) throws IOException {
            write(Integer.toString(arg0));
        }

        @Override
        public void print(long arg0) throws IOException {
            write(Long.toString(arg0));
        }

        @Override
        public void print(Object arg0) throws IOException {
            write(arg0 == null ? "null" : arg0.toString());
        }

        @Override
        public void print(String arg0) throws IOException {
            write(arg0);
        }

        @Override
        public void println() throws IOException {
            newLine();
        }

        @Override
        public void println(boolean arg0) throws IOException {
            print(arg0);
            newLine();
        }

        @Override
        public void println(char arg0) throws IOException {
            print(arg0);
            newLine();
        }

        @Override
        public void println(char[] arg0) throws IOException {
            print(arg0);
            newLine();
        }

        @Override
        public void println(double arg0) throws IOException {
            print(arg0);
            newLine();
        }

        @Override
        public void println(float arg0) throws IOException {
            print(arg0);
            newLine();
        }

        @Override
        public void println(int arg0) throws IOException {
            print(arg0);
            newLine();
        }

        @Override
        public void println(long arg0) throws IOException {
            print(arg0);
            newLine();
        }

        @Override
        public void println(Object arg0) throws IOException {
            print(arg0);
            newLine();
        }

        @Override
        public void println(String arg0) throws IOException {
            print(arg0);
            newLine();
        }

        @Override
        public void write(int c) throws IOException {
            if (buf != null) {
                buf.write(c);
            } else {
                getEnclosingWriter().write(c);
            }
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            if (buf != null) {
                buf.write(cbuf, off, len);
            } else {
                getEnclosingWriter().write(cbuf, off, len);
            }
        }

        @Override
        public String getString() {
            return buf.toString();
        }

        @Override
        public Reader getReader() {
            return new CharArrayReader(buf.toCharArray());
        }

        @Override
        public void writeOut(Writer out) throws IOException {
            buf.writeTo(out);
        }

    }

    class TagWriter extends BodyContentImpl implements TransformControl {
        private final Tag tag;
        private final JakartaFreeMarkerPageContext pageContext;
        private boolean needPop = true;
        private final boolean needDoublePop;
        private boolean closed = false;
        
        TagWriter(Writer out, Tag tag, JakartaFreeMarkerPageContext pageContext, boolean needDoublePop) {
            super((JspWriter) out, false);
            this.needDoublePop = needDoublePop;
            this.tag = tag;
            this.pageContext = pageContext;
        }
        
        @Override
        public String toString() {
            return "TagWriter for " + tag.getClass().getName() + " wrapping a " + getEnclosingWriter().toString();
        }

        Tag getTag() {
            return tag;
        }

        JakartaFreeMarkerPageContext getPageContext() {
            return pageContext;
        }
        
        @Override
        public int onStart()
        throws TemplateModelException {
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
                        if (isBodyTag) {
                            initBuffer();
                            BodyTag btag = (BodyTag) tag;
                            btag.setBodyContent(this);
                            btag.doInitBody();
                        } else {
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
            } catch (Exception e) {
                throw toTemplateModelExceptionOrRethrow(e);
            }
        }
        
        @Override
        public int afterBody()
        throws TemplateModelException {
            try {
                if (isIterationTag) {
                    int dab = ((IterationTag) tag).doAfterBody();
                    switch(dab) {
                        case Tag.SKIP_BODY:
                            endEvaluation();
                            return END_EVALUATION;
                        case IterationTag.EVAL_BODY_AGAIN:
                            return REPEAT_EVALUATION;
                        default:
                            throw new TemplateModelException("Unexpected return value " + dab + "from " + tag.getClass().getName() + ".doAfterBody()");
                    }
                }
                endEvaluation();
                return END_EVALUATION;
            } catch (Exception e) {
                throw toTemplateModelExceptionOrRethrow(e);
            }
        }
        
        private void endEvaluation() throws JspException {
            if (needPop) {
                pageContext.popWriter();
                needPop = false;
            }
            if (tag.doEndTag() == Tag.SKIP_PAGE) {
                LOG.warn("Tag.SKIP_PAGE was ignored from a " + tag.getClass().getName() + " tag.");
            }
        }
        
        @Override
        public void onError(Throwable t) throws Throwable {
            if (isTryCatchFinally) {
                ((TryCatchFinally) tag).doCatch(t);
            } else {
                throw t;
            }
        }
        
        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            
            if (needPop) {
                pageContext.popWriter();
            }
            pageContext.popTopTag();
            try {
                if (isTryCatchFinally) {
                    ((TryCatchFinally) tag).doFinally();
                }
                // No pooling yet
                tag.release();
            } finally {
                if (needDoublePop) {
                    pageContext.popWriter();
                }
            }
        }
        
    }
}
