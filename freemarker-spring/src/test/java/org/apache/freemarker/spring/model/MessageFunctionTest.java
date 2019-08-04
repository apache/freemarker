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

package org.apache.freemarker.spring.model;

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
public class MessageFunctionTest {

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
        mockMvc.perform(get("/users/{userId}/", userId).param("viewName", "test/model/message-function-basic-usages")
                .accept(MediaType.parseMediaType("text/html"))).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html")).andDo(print())
                .andExpect(xpath("//div[@id='userId']/text()").string(wac.getMessage("user.id", null, null)))
                .andExpect(xpath("//div[@id='userEmail']/text()").string(wac.getMessage("user.email", null, null)))
                .andExpect(xpath("//div[@id='userInfoWithArgs']/text()").string(wac.getMessage("user.form.message",
                        new Object[] { user.getFirstName(), user.getLastName(), user.getEmail() }, null)));
    }

    @Test
    public void testWithMessageSourceResolvable() throws Exception {
        final Long nonExistingUserId = 0L;
        mockMvc.perform(
                get("/users/{userId}/", nonExistingUserId).param("viewName", "test/model/message-function-basic-usages")
                        .accept(MediaType.parseMediaType("text/html")))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/html")).andDo(print())
                .andExpect(xpath("//div[@id='errorMessage']/text()")
                        .string(wac.getMessage("user.error.notfound", new Object[] { nonExistingUserId }, null)));
    }
}
