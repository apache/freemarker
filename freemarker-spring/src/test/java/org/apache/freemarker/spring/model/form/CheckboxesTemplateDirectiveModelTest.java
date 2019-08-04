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
public class CheckboxesTemplateDirectiveModelTest {

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
    public void testCheckboxesWithSequence() throws Exception {
        final User user = userRepository.getUserByEmail("john@example.com");
        mockMvc.perform(get("/users/{userId}/", user.getId()).param("viewName", "test/model/form/checkboxes-directive-usages")
                .accept(MediaType.parseMediaType("text/html"))).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html")).andDo(print())
                .andExpect(xpath("//form[@id='form1']//span/input[@type='checkbox' and @id='favoriteFoodInForm11' and @name='favoriteFood' and @checked='checked']/@value").string("Sandwich"))
                .andExpect(xpath("//form[@id='form1']//span/label[@for='favoriteFoodInForm11']").string("Sandwich"))
                .andExpect(xpath("//form[@id='form1']//span/input[@type='checkbox' and @id='favoriteFoodInForm12' and @name='favoriteFood' and @checked='checked']/@value").string("Spaghetti"))
                .andExpect(xpath("//form[@id='form1']//span/label[@for='favoriteFoodInForm12']").string("Spaghetti"))
                .andExpect(xpath("//form[@id='form1']//span/input[@type='checkbox' and @id='favoriteFoodInForm13' and @name='favoriteFood']/@value").string("Sushi"))
                .andExpect(xpath("//form[@id='form1']//span/label[@for='favoriteFoodInForm13']").string("Sushi"))
                .andExpect(xpath("//form[@id='form1']//input[@type='hidden' and @name='_favoriteFood']/@value").string("on"));
    }

    @Test
    public void testCheckboxesWithHash() throws Exception {
        final User user = userRepository.getUserByEmail("john@example.com");
        mockMvc.perform(get("/users/{userId}/", user.getId()).param("viewName", "test/model/form/checkboxes-directive-usages")
                .accept(MediaType.parseMediaType("text/html"))).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html")).andDo(print())
                .andExpect(xpath("//form[@id='form2']//span/input[@type='checkbox' and @id='favoriteFoodInForm21' and @name='favoriteFood' and @checked='checked']/@value").string("Sandwich"))
                .andExpect(xpath("//form[@id='form2']//span/label[@for='favoriteFoodInForm21']").string("Delicious sandwich"))
                .andExpect(xpath("//form[@id='form2']//span/input[@type='checkbox' and @id='favoriteFoodInForm22' and @name='favoriteFood' and @checked='checked']/@value").string("Spaghetti"))
                .andExpect(xpath("//form[@id='form2']//span/label[@for='favoriteFoodInForm22']").string("Lovely spaghetti"))
                .andExpect(xpath("//form[@id='form2']//span/input[@type='checkbox' and @id='favoriteFoodInForm23' and @name='favoriteFood']/@value").string("Sushi"))
                .andExpect(xpath("//form[@id='form2']//span/label[@for='favoriteFoodInForm23']").string("Sushi with wasabi"))
                .andExpect(xpath("//form[@id='form2']//input[@type='hidden' and @name='_favoriteFood']/@value").string("on"));
    }

    @Test
    public void testCheckboxesWithEnclosingElementNameAndDelimiter() throws Exception {
        final User user = userRepository.getUserByEmail("john@example.com");
        mockMvc.perform(get("/users/{userId}/", user.getId()).param("viewName", "test/model/form/checkboxes-directive-usages")
                .accept(MediaType.parseMediaType("text/html"))).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html")).andDo(print())
                .andExpect(xpath("//form[@id='form3']//div/input[@type='checkbox' and @id='favoriteFoodInForm31' and @name='favoriteFood' and @checked='checked']/@value").string("Sandwich"))
                .andExpect(xpath("//form[@id='form3']//div/label[@for='favoriteFoodInForm31']").string("Sandwich"))
                .andExpect(xpath("//form[@id='form3']//div[1]/br").doesNotExist())
                .andExpect(xpath("//form[@id='form3']//div/input[@type='checkbox' and @id='favoriteFoodInForm32' and @name='favoriteFood' and @checked='checked']/@value").string("Spaghetti"))
                .andExpect(xpath("//form[@id='form3']//div/label[@for='favoriteFoodInForm32']").string("Spaghetti"))
                .andExpect(xpath("//form[@id='form3']//div[2]/br").exists())
                .andExpect(xpath("//form[@id='form3']//div/input[@type='checkbox' and @id='favoriteFoodInForm33' and @name='favoriteFood']/@value").string("Sushi"))
                .andExpect(xpath("//form[@id='form3']//div/label[@for='favoriteFoodInForm33']").string("Sushi"))
                .andExpect(xpath("//form[@id='form3']//div[3]/br").exists())
                .andExpect(xpath("//form[@id='form3']//input[@type='hidden' and @name='_favoriteFood']/@value").string("on"));
    }
}
