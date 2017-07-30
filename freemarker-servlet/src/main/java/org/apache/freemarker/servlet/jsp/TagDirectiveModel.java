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

package org.apache.freemarker.servlet.jsp;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.io.Writer;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.IterationTag;
import javax.servlet.jsp.tagext.SimpleTag;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TryCatchFinally;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.CallPlace;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateHashModelEx2;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapts a {@link Tag}-based custom JSP tag to be a value that's callable in templates as an user-defined directive.
 * For {@link SimpleTag}-based custom JSP tags {@link SimpleTagDirectiveModel} is used instead.
 */
class TagDirectiveModel extends JspTagModelBase implements TemplateDirectiveModel {
    private static final Logger LOG = LoggerFactory.getLogger(TagDirectiveModel.class);

    private static final ArgumentArrayLayout ARGS_LAYOUT = ArgumentArrayLayout.create(
            0, false,
            null, true);

    private final boolean isBodyTag;
    private final boolean isIterationTag;
    private final boolean isTryCatchFinally;

    public TagDirectiveModel(String tagName, Class tagClass) throws IntrospectionException {
        super(tagName, tagClass);
        isIterationTag = IterationTag.class.isAssignableFrom(tagClass);
        isBodyTag = isIterationTag && BodyTag.class.isAssignableFrom(tagClass);
        isTryCatchFinally = TryCatchFinally.class.isAssignableFrom(tagClass);
    }

    @Override
    public void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
            throws TemplateException, IOException {
        try {
            Tag tag = (Tag) getTagInstance();
            FreeMarkerPageContext pageContext = PageContextFactory.getCurrentPageContext();
            Tag parentTag = (Tag) pageContext.peekTopTag(Tag.class);
            tag.setParent(parentTag);
            tag.setPageContext(pageContext);
            setupTag(tag, (TemplateHashModelEx2) args[ARGS_LAYOUT.getNamedVarargsArgumentIndex()],
                    pageContext.getObjectWrapper());
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
                out = new JspWriterAdapter(out);
                pageContext.pushWriter((JspWriter) out);
                usesAdapter = true;
            }

            // TODO [FM3] In FM2 this was done with a TemplateTransformModel, which has returned a Writer that
            // encapsulated the logic. See if there's a better solution now that we use the redesigned
            // TemplateDirectiveModel.
            TagBodyContent bodyContent = new TagBodyContent(out, tag, pageContext, usesAdapter);
            pageContext.pushTopTag(tag);
            pageContext.pushWriter(bodyContent);
            try {
                if (bodyContent.doStartTag()) {
                    do {
                        callPlace.executeNestedContent(null, bodyContent, env);
                    } while (bodyContent.doAfterBody());
                }
            } catch (Throwable e) {
                bodyContent.doCatch(e);
            } finally {
                bodyContent.close(); // Pops `topTag` and `writer`
            }
        } catch (Throwable e) {
            throw toTemplateModelExceptionOrRethrow(e);
        }
    }

    @Override
    public ArgumentArrayLayout getArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    /**
     * Implements extra methods to help mimicking JSP container behavior around the
     * {@link TemplateDirectiveModel#execute(TemplateModel[], CallPlace, Writer, Environment)} call.
     */
    class TagBodyContent extends BodyContentImpl {
        private final Tag tag;
        private final FreeMarkerPageContext pageContext;
        private boolean needPop = true;
        private final boolean needDoublePop;

        TagBodyContent(Writer out, Tag tag, FreeMarkerPageContext pageContext, boolean needDoublePop) {
            super((JspWriter) out, false);
            this.needDoublePop = needDoublePop;
            this.tag = tag;
            this.pageContext = pageContext;
        }
        
        @Override
        public String toString() {
            return "TagBodyContent for " + tag.getClass().getName() + " wrapping a " + getEnclosingWriter().toString();
        }

        Tag getTag() {
            return tag;
        }
        
        FreeMarkerPageContext getPageContext() {
            return pageContext;
        }

        /**
         * @return Whether to execute the nested content (the body, with JSP terminology)
         */
        private boolean doStartTag() throws TemplateModelException {
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
                        return false;
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
                        return true;
                    }
                    default: {
                        throw new RuntimeException("Illegal return value " + dst + " from " + tag.getClass().getName() + ".doStartTag()");
                    }
                }
            } catch (Exception e) {
                throw toTemplateModelExceptionOrRethrow(e);
            }
        }

        /**
         * @return Whether to execute the nested content again (the body, with JSP terminology)
         */
        private boolean doAfterBody() throws TemplateModelException {
            try {
                if (isIterationTag) {
                    int dab = ((IterationTag) tag).doAfterBody();
                    switch(dab) {
                        case Tag.SKIP_BODY:
                            endEvaluation();
                            return false;
                        case IterationTag.EVAL_BODY_AGAIN:
                            return true;
                        default:
                            throw new TemplateModelException("Unexpected return value " + dab + "from " + tag.getClass().getName() + ".doAfterBody()");
                    }
                }
                endEvaluation();
                return false;
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
                LOG.warn("Tag.SKIP_PAGE was ignored from a {} tag.", tag.getClass().getName());
            }
        }

        private void doCatch(Throwable t) throws Throwable {
            if (isTryCatchFinally) {
                ((TryCatchFinally) tag).doCatch(t);
            } else {
                throw t;
            }
        }

        @Override
        public void close() {
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
