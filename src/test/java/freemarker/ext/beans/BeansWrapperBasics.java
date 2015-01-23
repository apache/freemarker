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

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelWithAPISupport;

public class BeansWrapperBasics {

    @SuppressWarnings("boxing")
    @Test
    public void testAPIBuiltInSupport() throws TemplateModelException {
        {
            BeansWrapper bw = new BeansWrapperBuilder(Configuration.VERSION_2_3_0).build();
            assertWrappingResult(StringModel.class, bw.wrap("s"));
            assertWrappingResult(NumberModel.class, bw.wrap(1.5));
            assertWrappingResult(BooleanModel.class, bw.wrap(true));
            assertWrappingResult(CollectionModel.class, bw.wrap(ImmutableList.of(1)));
            assertWrappingResult(MapModel.class, bw.wrap(ImmutableMap.of("a", 1)));
        }
        
        {
            BeansWrapperBuilder bwb = new BeansWrapperBuilder(Configuration.VERSION_2_3_0);
            bwb.setSimpleMapWrapper(true);
            BeansWrapper bw = bwb.build();
            assertWrappingResult(SimpleMapModel.class, bw.wrap(ImmutableMap.of("a", 1)));
        }
    }

    private void assertWrappingResult(Class<? extends TemplateModel> expectedClass, TemplateModel tm)
            throws TemplateModelException {
        assertTrue(expectedClass.isInstance(tm));
        // All BeansWrapper products support `?api`:
        assertTrue(((TemplateModelWithAPISupport) tm).getAPI() instanceof APIModel);
    }

}
