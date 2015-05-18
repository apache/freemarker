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

import static org.junit.Assert.*;

import java.lang.ref.Reference;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import freemarker.ext.beans.BeansWrapper.MethodAppearanceDecision;
import freemarker.ext.beans.BeansWrapper.MethodAppearanceDecisionInput;

@RunWith(JUnit4.class)
public class BeansWrapperCachesTest {

    @Test
    public void introspectionSettingChanges() {
        BeansWrapper bw = new BeansWrapper();
        ClassIntrospector ci1 = bw.getClassIntrospector();
        checkRegisteredModelFactories(ci1, bw.getStaticModels(), bw.getEnumModels(), bw.getModelCache());
        
        bw.setExposeFields(true);
        ClassIntrospector ci2 = bw.getClassIntrospector();
        assertNotSame(ci1, ci2);
        checkRegisteredModelFactories(ci1);
        checkRegisteredModelFactories(ci2, bw.getStaticModels(), bw.getEnumModels(), bw.getModelCache());
        
        bw.setExposureLevel(BeansWrapper.EXPOSE_ALL);
        ClassIntrospector ci3 = bw.getClassIntrospector();
        assertNotSame(ci2, ci3);
        checkRegisteredModelFactories(ci2);
        checkRegisteredModelFactories(ci3, bw.getStaticModels(), bw.getEnumModels(), bw.getModelCache());
        
        MethodAppearanceFineTuner maf = new MethodAppearanceFineTuner() {
            public void process(MethodAppearanceDecisionInput in, MethodAppearanceDecision out) {
                // nop
            }
        };
        bw.setMethodAppearanceFineTuner(maf);
        ClassIntrospector ci4 = bw.getClassIntrospector();
        assertNotSame(ci3, ci4);
        checkRegisteredModelFactories(ci3);
        checkRegisteredModelFactories(ci4, bw.getStaticModels(), bw.getEnumModels(), bw.getModelCache());
        
        bw.setExposeFields(true);
        assertSame(ci4, bw.getClassIntrospector());
        bw.setExposureLevel(BeansWrapper.EXPOSE_ALL);
        assertSame(ci4, bw.getClassIntrospector());
        bw.setMethodAppearanceFineTuner(maf);
        assertSame(ci4, bw.getClassIntrospector());
        checkRegisteredModelFactories(ci4, bw.getStaticModels(), bw.getEnumModels(), bw.getModelCache());
    }
    
    private void checkRegisteredModelFactories(ClassIntrospector ci, Object... expected) {
        Object[] actualRefs = ci.getRegisteredModelFactoriesSnapshot();

        scanActuals: for (Object actaulRef : actualRefs) {
            Object actualItem = ((Reference) actaulRef).get();
            for (Object expectedItem : expected) {
                if (actualItem == expectedItem) {
                    continue scanActuals;
                }
            }
            fail("Actaul item " + actualItem + " is not among the expected items");
        }
        
        scanExpecteds: for (Object expectedItem : expected) {
            for (Object ref : actualRefs) {
                Object actualItem = ((Reference) ref).get();
                if (actualItem == expectedItem) {
                    continue scanExpecteds;
                }
            }
            fail("Expected item " + expectedItem + " is not among the actual items");
        }
    }
    
}
