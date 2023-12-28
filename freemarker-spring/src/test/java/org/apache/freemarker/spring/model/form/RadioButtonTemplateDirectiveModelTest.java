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

package org.apache.freemarker.spring.model.form;

import org.apache.freemarker.spring.example.mvc.users.User;
import org.apache.freemarker.spring.example.mvc.users.UserRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration("classpath:META-INF/web-resources")
@ContextConfiguration(locations = { "classpath:org/apache/freemarker/spring/example/mvc/users/users-mvc-context.xml" })
public class RadioButtonTemplateDirectiveModelTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void testSingleCheckboxWithFemaleUser() throws Exception {
        final User user = userRepository.getUserByEmail("jane@example.com");
        mockMvc.perform(get("/users/{userId}", user.getId()).param("viewName", "test/model/form/radiobutton-directive-usages")
                .accept(MediaType.parseMediaType("text/html"))).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html")).andDo(print())
                .andExpect(xpath("//form[@id='form1']//input[@type='radio' and @id='gender1' and @name='gender' and @value='F']").exists())
                .andExpect(xpath("//form[@id='form1']//input[@type='radio' and @id='gender1' and @name='gender' and @value='F']/@checked").string("checked"))
                .andExpect(xpath("//form[@id='form1']//input[@type='radio' and @id='gender2' and @name='gender' and @value='M']").exists())
                .andExpect(xpath("//form[@id='form1']//input[@type='radio' and @id='gender2' and @name='gender' and @value='M']/@checked").doesNotExist())
                .andExpect(xpath("//form[@id='form1']//input[@type='radio' and @id='gender3' and @name='gender' and @value='U']").exists())
                .andExpect(xpath("//form[@id='form1']//input[@type='radio' and @id='gender3' and @name='gender' and @value='U']/@checked").doesNotExist());
    }

    @Test
    public void testSingleCheckboxWithMaleUser() throws Exception {
        final User user = userRepository.getUserByEmail("john@example.com");
        mockMvc.perform(get("/users/{userId}", user.getId()).param("viewName", "test/model/form/radiobutton-directive-usages")
                .accept(MediaType.parseMediaType("text/html"))).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html")).andDo(print())
                .andExpect(xpath("//form[@id='form1']//input[@type='radio' and @id='gender1' and @name='gender' and @value='F']").exists())
                .andExpect(xpath("//form[@id='form1']//input[@type='radio' and @id='gender1' and @name='gender' and @value='F']/@checked").doesNotExist())
                .andExpect(xpath("//form[@id='form1']//input[@type='radio' and @id='gender2' and @name='gender' and @value='M']").exists())
                .andExpect(xpath("//form[@id='form1']//input[@type='radio' and @id='gender2' and @name='gender' and @value='M']/@checked").string("checked"))
                .andExpect(xpath("//form[@id='form1']//input[@type='radio' and @id='gender3' and @name='gender' and @value='U']").exists())
                .andExpect(xpath("//form[@id='form1']//input[@type='radio' and @id='gender3' and @name='gender' and @value='U']/@checked").doesNotExist());
    }
}
