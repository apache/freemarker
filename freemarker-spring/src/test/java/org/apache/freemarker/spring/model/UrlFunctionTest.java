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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

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
public class UrlFunctionTest {

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
    public void testThemeFunctionBasicUsages() throws Exception {
        final Integer userId = userRepository.getUserIds().iterator().next();
        mockMvc.perform(get("/users/").param("viewName", "test/model/url-function-basic-usages")
                .accept(MediaType.parseMediaType("text/html"))).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html")).andDo(print())
                .andExpect(xpath("//h2[@id='usersListHeader']//a/@href", userId).string("/users/"))
                .andExpect(xpath("//h3[@id='usersListHeaderWithSortParams']//a/@href", userId)
                        .string("/users/?sortField=birthDate&sortDirection=descending"))
                .andExpect(xpath("//h2[@id='otherAppsUsersListHeader']//a/@href", userId).string("/otherapp/users/"))
                .andExpect(xpath("//h3[@id='otherAppsUsersListHeaderWithSortParams']//a/@href", userId)
                        .string("/otherapp/users/?sortField=birthDate&sortDirection=descending"))
                .andExpect(xpath("//div[@id='user-%s']//a[@class='userIdLink']/@href", userId).string("/users/" + userId + "/"))
                .andExpect(xpath("//div[@id='user-%s']//a[@class='userNameLink']/@href", userId).string("/users/" + userId + "/"))
                .andExpect(xpath("//div[@id='freeMarkerManualUrl']//a/@href", userId)
                        .string("http://freemarker.org/docs/index.html"));
    }
}
