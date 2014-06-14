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

package freemarker.ext.beans;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;

public class PrallelStaticIntrospectionTest extends AbstractParallelIntrospectionTest {

    private static final String STATICS_CLASS_CONTAINER_CLASS_NAME
            = ManyStaticsOfDifferentClasses.class.getName();

    public PrallelStaticIntrospectionTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        new PrallelStaticIntrospectionTest("almostForever")
                .testReliability(Integer.MAX_VALUE);
    }
    
    protected TemplateHashModel getWrappedEntity(int clIdx)
    throws TemplateModelException {
        return (TemplateHashModel) getBeansWrapper().getStaticModels().get(
                STATICS_CLASS_CONTAINER_CLASS_NAME + "$C"
                + clIdx);
    }
    
}
