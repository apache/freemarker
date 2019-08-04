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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.apache.freemarker.spring.example.mvc.users.UserRepository;
import org.hamcrest.Matchers;
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

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration("classpath:META-INF/web-resources")
@ContextConfiguration(locations = { "classpath:org/apache/freemarker/spring/example/mvc/users/users-mvc-context.xml" })
public class ErrorsTemplateDirectiveModelTest {

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
    public void testBasicUsages() throws Exception {
        final Long userId = userRepository.getUserIds().iterator().next();
        mockMvc.perform(post("/users/", userId).param("viewName", "test/model/form/errors-directive-usages")
                .param("firstName", "").param("lastName", "").param("email", "")
                .accept(MediaType.parseMediaType("text/html"))).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html")).andDo(print())
                .andExpect(xpath("string(//form[@id='form1']//div[@id='formErrors1']/ul)").string(Matchers.containsString("First name")))
                .andExpect(xpath("string(//form[@id='form1']//div[@id='formErrors1']/ul)").string(Matchers.containsString("Last name")))
                .andExpect(xpath("string(//form[@id='form1']//div[@id='formErrors1']/ul)").string(Matchers.containsString("E-Mail")))
                .andExpect(xpath("string(//form[@id='form1']//div[@id='formErrors2']/p)").string(Matchers.containsString("some errors")))
                .andExpect(xpath("//form[@id='form1']//span[@class='errorFirstName']/text()").string(Matchers.containsString("First name")))
                .andExpect(xpath("//form[@id='form1']//span[@class='errorLastName']/text()").string(Matchers.containsString("Last name")))
                .andExpect(xpath("//form[@id='form1']//div[@class='errorEmail']/text()").string(Matchers.containsString("E-Mail")));
    }
}
