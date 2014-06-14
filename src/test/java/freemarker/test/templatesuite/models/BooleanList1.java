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

import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;

/**
 * Model for testing the impact of isEmpty() on template list models. Every
 * other method simply delegates to a SimpleList model.
 */
public class BooleanList1 implements TemplateSequenceModel {

    private LegacyList  cList;

    /** Creates new BooleanList1 */
    public BooleanList1() {
        cList = new LegacyList();
        cList.add( "false" );
        cList.add( "0" );
        cList.add(TemplateBooleanModel.FALSE);
        cList.add(TemplateBooleanModel.TRUE);
        cList.add(TemplateBooleanModel.TRUE);
        cList.add(TemplateBooleanModel.TRUE);
        cList.add(TemplateBooleanModel.FALSE);
    }

    /**
     * @return true if there is a next element.
     */
    public boolean hasNext() {
        return cList.hasNext();
    }

    /**
     * @return the next element in the list.
     */
    public TemplateModel next() throws TemplateModelException {
        return cList.next();
    }

    /**
     * @return true if the cursor is at the beginning of the list.
     */
    public boolean isRewound() {
        return cList.isRewound();
    }

    /**
     * @return the specified index in the list
     */
    public TemplateModel get(int i) throws TemplateModelException {
        return cList.get(i);
    }

    /**
     * Resets the cursor to the beginning of the list.
     */
    public void rewind() {
        cList.rewind();
    }

    public int size() {
        return cList.size();
    }

}
