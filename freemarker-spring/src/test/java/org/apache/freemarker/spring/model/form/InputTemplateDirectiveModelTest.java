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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration("classpath:META-INF/web-resources")
@ContextConfiguration(locations = { "classpath:org/apache/freemarker/spring/example/mvc/users/users-mvc-context.xml" })
public class InputTemplateDirectiveModelTest {

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
        mockMvc.perform(get("/users/{userId}/", userId).param("viewName", "test/model/form/input-directive-usages")
                .accept(MediaType.parseMediaType("text/html"))).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html")).andDo(print())
                .andExpect(xpath("//form[@id='form1']//input[@id='customEmailId' and @name='email']/@value").string(user.getEmail()))
                .andExpect(xpath("//form[@id='form1']//input[@id='firstName' and @name='firstName']/@value").string(user.getFirstName()))
                .andExpect(xpath("//form[@id='form1']//input[@id='lastName' and @name='lastName']/@value").string(user.getLastName()));
    }

    @Test
    public void testDefaultAttributes() throws Exception {
        final Long userId = userRepository.getUserIds().iterator().next();
        mockMvc.perform(get("/users/{userId}/", userId).param("viewName", "test/model/form/input-directive-usages")
                .accept(MediaType.parseMediaType("text/html"))).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html")).andDo(print())
                .andExpect(xpath("//form[@id='form2']//input/@name").string("firstName"))
                .andExpect(xpath("//form[@id='form2']//input/@class").string("my_cssClass"))
                .andExpect(xpath("//form[@id='form2']//input/@style").string("my_cssStyle"))
                .andExpect(xpath("//form[@id='form2']//input/@lang").string("my_lang"))
                .andExpect(xpath("//form[@id='form2']//input/@title").string("my_title"))
                .andExpect(xpath("//form[@id='form2']//input/@dir").string("my_dir"))
                .andExpect(xpath("//form[@id='form2']//input/@tabindex").string("my_tabindex"))
                .andExpect(xpath("//form[@id='form2']//input/@onclick").string("my_onclick()"))
                .andExpect(xpath("//form[@id='form2']//input/@ondblclick").string("my_ondblclick()"))
                .andExpect(xpath("//form[@id='form2']//input/@onmousedown").string("my_onmousedown()"))
                .andExpect(xpath("//form[@id='form2']//input/@onmouseup").string("my_onmouseup()"))
                .andExpect(xpath("//form[@id='form2']//input/@onmouseover").string("my_onmouseover()"))
                .andExpect(xpath("//form[@id='form2']//input/@onmousemove").string("my_onmousemove()"))
                .andExpect(xpath("//form[@id='form2']//input/@onmouseout").string("my_onmouseout()"))
                .andExpect(xpath("//form[@id='form2']//input/@onkeypress").string("my_onkeypress()"))
                .andExpect(xpath("//form[@id='form2']//input/@onkeyup").string("my_onkeyup()"))
                .andExpect(xpath("//form[@id='form2']//input/@onkeydown").string("my_onkeydown()"))
                .andExpect(xpath("//form[@id='form2']//input/@onfocus").string("my_onfocus()"))
                .andExpect(xpath("//form[@id='form2']//input/@onblur").string("my_onblur()"))
                .andExpect(xpath("//form[@id='form2']//input/@onchange").string("my_onchange()"))
                .andExpect(xpath("//form[@id='form2']//input/@accesskey").string("my_accesskey"))
                .andExpect(xpath("//form[@id='form2']//input/@disabled").string("disabled"))
                .andExpect(xpath("//form[@id='form2']//input/@readonly").string("readonly"))
                .andExpect(xpath("//form[@id='form2']//input/@size").string("my_size"))
                .andExpect(xpath("//form[@id='form2']//input/@maxlength").string("my_maxlength"))
                .andExpect(xpath("//form[@id='form2']//input/@alt").string("my_alt"))
                .andExpect(xpath("//form[@id='form2']//input/@onselect").string("my_onselect()"))
                .andExpect(xpath("//form[@id='form2']//input/@autocomplete").string("my_autocomplete"));
    }
}
