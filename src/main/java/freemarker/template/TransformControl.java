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

import java.io.IOException;

/**
 * An interface that can be implemented by writers returned from
 * {@link TemplateTransformModel#getWriter(java.io.Writer, java.util.Map)}. The
 * methods on this
 * interfaces are callbacks that will be called by the template engine and that
 * give the writer a chance to better control the evaluation of the transform
 * body. The writer can instruct the engine to skip or to repeat body 
 * evaluation, and gets notified about exceptions that are thrown during the
 * body evaluation.
 *
 * @author Attila Szegedi
 */
public interface TransformControl
{
    /**
     * Constant returned from {@link #afterBody()} that tells the
     * template engine to repeat transform body evaluation and feed
     * it again to the transform.
     */
    public static final int REPEAT_EVALUATION = 0;

    /**
     * Constant returned from {@link #afterBody()} that tells the
     * template engine to end the transform and close the writer.
     */
    public static final int END_EVALUATION = 1;
 
    /**
     * Constant returned from {@link #onStart()} that tells the
     * template engine to skip evaluation of the body.
     */
    public static final int SKIP_BODY = 0;
    
    /**
     * Constant returned from {@link #onStart()} that tells the
     * template engine to evaluate the body.
     */
    public static final int EVALUATE_BODY = 1;

    /**
     * Called before the body is evaluated for the first time.
     * @return 
     * <ul>
     * <li><tt>SKIP_BODY</tt> if the transform wants to ignore the body. In this
     * case, only {@link java.io.Writer#close()} is called next and processing ends.</li>
     * <li><tt>EVALUATE_BODY</tt> to normally evaluate the body of the transform
     * and feed it to the writer</li>
     * </ul>
     */
    public int onStart() throws TemplateModelException, IOException;
    
    /**
     * Called after the body has been evaluated.
     * @return
     * <ul>
     * <li><tt>END_EVALUATION</tt> if the transformation should be ended.</li>
     * <li><tt>REPEAT_EVALUATION</tt> to have the engine re-evaluate the 
     * transform body and feed it again to the writer.</li>
     * </ul>
     */
    public int afterBody() throws TemplateModelException, IOException;
    
    /**
     * Called if any exception occurs during the transform between the
     * {@link TemplateTransformModel#getWriter(java.io.Writer, java.util.Map)} call
     * and the {@link java.io.Writer#close()} call.
     * @param t the throwable that represents the exception. It can be any 
     * non-checked throwable, as well as {@link TemplateException} and 
     * {@link java.io.IOException}.
     * 
     * @throws Throwable is recommended that the methods rethrow the received 
     * throwable. If the method wants to throw another throwable, it should
     * either throw a non-checked throwable, or an instance of 
     * {@link TemplateException} and {@link java.io.IOException}. Throwing any
     * other checked exception will cause the engine to rethrow it as
     * a {@link java.lang.reflect.UndeclaredThrowableException}.
     */
    public void onError(Throwable t) throws Throwable;
}
