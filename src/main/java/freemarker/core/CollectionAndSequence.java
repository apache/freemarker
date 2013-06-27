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

package freemarker.core;

import java.io.Serializable;
import java.util.ArrayList;

import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateSequenceModel;

/**
 * Add sequence capabilities to an existing collection, or
 * vice versa. Used by ?keys and ?values built-ins.
 */
final public class CollectionAndSequence
implements TemplateCollectionModel, TemplateSequenceModel, Serializable
{
    private TemplateCollectionModel collection;
    private TemplateSequenceModel sequence;
    private ArrayList data;

    public CollectionAndSequence(TemplateCollectionModel collection) {
        this.collection = collection;
    }

    public CollectionAndSequence(TemplateSequenceModel sequence) {
        this.sequence = sequence;
    }

    public TemplateModelIterator iterator() throws TemplateModelException {
        if (collection != null) {
            return collection.iterator();
        } else {
            return new SequenceIterator(sequence);
        }
    }

    public TemplateModel get(int i) throws TemplateModelException {
        if (sequence != null) {
            return sequence.get(i);
        } else {
            initSequence();
            return (TemplateModel)data.get(i);
        }
    }

    public int size() throws TemplateModelException {
        if (sequence != null) {
            return sequence.size();
        } else {
            initSequence();
            return data.size();
        }
    }

    private void initSequence() throws TemplateModelException {
        if (data == null) {
            data = new ArrayList();
            TemplateModelIterator it = collection.iterator();
            while (it.hasNext()) {
                data.add(it.next());
            }
        }
    }

    private static class SequenceIterator
    implements TemplateModelIterator
    {
        private final TemplateSequenceModel sequence;
        private final int size;
        private int index = 0;

        SequenceIterator(TemplateSequenceModel sequence) throws TemplateModelException {
            this.sequence = sequence;
            this.size = sequence.size();
            
        }
        public TemplateModel next() throws TemplateModelException {
            return sequence.get(index++);
        }

        public boolean hasNext() {
            return index < size;
        }
    }
}
