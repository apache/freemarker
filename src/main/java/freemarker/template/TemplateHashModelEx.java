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

/**
 * Corresponds to the "extended hash" template language data-type; extends {@link TemplateHashModel} by allowing
 * iterating through its keys and values.
 * 
 * <p>If a class
 * implements this interface, then the built-in operators <code>?size</code>,
 * <code>?keys</code>, and <code>?values</code> can be applied to its
 * instances in the template.</p>
 *
 * <p>As of version 2.2.2, the engine will automatically wrap the
 * collections returned by <code>keys</code> and <code>values</code> to
 * present them as sequences to the template.  For performance, you may
 * wish to return objects that implement both TemplateCollectionModel
 * and {@link TemplateSequenceModel}. Note that the wrapping to sequence happens
 * on demand; if the template does not try to use the variable returned by
 * <code>?keys</code> or <code>?values</code> as sequence (<code>theKeys?size</code>, or <code>theKeys[x]</code>,
 * or <code>theKeys?sort</code>, etc.), just iterates over the variable
 * (<code>&lt;#list foo?keys as k>...</code>), then no wrapping to
 * sequence will happen, thus there will be no overhead. 
 * 
 * @author <a href="mailto:jon@revusky.com">Jonathan Revusky</a>
 * @see SimpleHash
 */
public interface TemplateHashModelEx extends TemplateHashModel {

    /**
     * @return the number of key/value mappings in the hash.
     */
    int size() throws TemplateModelException;

    /**
     * @return a collection containing the keys in the hash. Every element of 
     * the returned collection must implement the {@link TemplateScalarModel}
     * (as the keys of hashes are always strings).
     */
    TemplateCollectionModel keys() throws TemplateModelException;

    /**
     * @return a collection containing the values in the hash.
     */
    TemplateCollectionModel values() throws TemplateModelException;
}
