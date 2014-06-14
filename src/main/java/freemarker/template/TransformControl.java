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
