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

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration("classpath:META-INF/web-resources")
@ContextConfiguration(locations = { "classpath:org/apache/freemarker/spring/example/mvc/users/users-mvc-context.xml" })
public class FormTemplateDirectiveModelTest {

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
        final User user = userRepository.getUser(userId);
        mockMvc.perform(get("/users/{userId}/", userId).param("viewName", "test/model/form/form-directive-usages")
                .accept(MediaType.parseMediaType("text/html"))).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html")).andDo(print())
                .andExpect(xpath("//form[@id='form1']/@method").string("post"))
                .andExpect(xpath("//form[@id='form1']//input[@name='firstName']/@value").string(user.getFirstName()))
                .andExpect(xpath("//form[@id='form1']//input[@name='lastName']/@value").string(user.getLastName()));
    }

    @Test
    public void testDefaultAttributes() throws Exception {
        final Long userId = userRepository.getUserIds().iterator().next();
        final User user = userRepository.getUser(userId);
        mockMvc.perform(get("/users/{userId}/", userId).param("viewName", "test/model/form/form-directive-usages")
                .accept(MediaType.parseMediaType("text/html"))).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html")).andDo(print())
                .andExpect(xpath("//form[@id='form2']/@class").string("my_cssClass"))
                .andExpect(xpath("//form[@id='form2']/@style").string("my_cssStyle"))
                .andExpect(xpath("//form[@id='form2']/@lang").string("my_lang"))
                .andExpect(xpath("//form[@id='form2']/@title").string("my_title"))
                .andExpect(xpath("//form[@id='form2']/@dir").string("my_dir"))
                .andExpect(xpath("//form[@id='form2']/@tabindex").string("my_tabindex"))
                .andExpect(xpath("//form[@id='form2']/@onclick").string("my_onclick()"))
                .andExpect(xpath("//form[@id='form2']/@ondblclick").string("my_ondblclick()"))
                .andExpect(xpath("//form[@id='form2']/@onmousedown").string("my_onmousedown()"))
                .andExpect(xpath("//form[@id='form2']/@onmouseup").string("my_onmouseup()"))
                .andExpect(xpath("//form[@id='form2']/@onmouseover").string("my_onmouseover()"))
                .andExpect(xpath("//form[@id='form2']/@onmousemove").string("my_onmousemove()"))
                .andExpect(xpath("//form[@id='form2']/@onmouseout").string("my_onmouseout()"))
                .andExpect(xpath("//form[@id='form2']/@onkeypress").string("my_onkeypress()"))
                .andExpect(xpath("//form[@id='form2']/@onkeyup").string("my_onkeyup()"))
                .andExpect(xpath("//form[@id='form2']/@onkeydown").string("my_onkeydown()"))
                .andExpect(xpath("//form[@id='form2']/@action").string("my_action"))
                .andExpect(xpath("//form[@id='form2']/@method").string("post"))
                .andExpect(xpath("//form[@id='form2']/@target").string("my_target"))
                .andExpect(xpath("//form[@id='form2']/@enctype").string("my_enctype"))
                .andExpect(xpath("//form[@id='form2']/@acceptCharset").string("my_acceptCharset"))
                .andExpect(xpath("//form[@id='form2']/@onsubmit").string("my_onsubmit()"))
                .andExpect(xpath("//form[@id='form2']/@onreset").string("my_onreset()"))
                .andExpect(xpath("//form[@id='form2']/@autocomplete").string("my_autocomplete"))
                .andExpect(xpath("//form[@id='form2']/@name").string("my_name"))
                .andExpect(xpath("//form[@id='form2']/@autocomplete").string("my_autocomplete"));
    }

}
