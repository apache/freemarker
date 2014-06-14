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
