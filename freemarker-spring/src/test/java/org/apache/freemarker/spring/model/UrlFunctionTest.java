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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

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
public class UrlFunctionTest {

    private static final String TEMPLATE_NUMBER_FORMAT = "00000000";

    private static final String TEMPLATE_DATE_FORMAT = "yyyy-MM-dd";

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
        final String formattedUerId = new DecimalFormat(TEMPLATE_NUMBER_FORMAT).format(userId);
        final User user = userRepository.getUser(userId);
        mockMvc.perform(get("/users/").param("viewName", "test/model/url-function-basic-usages")
                .accept(MediaType.parseMediaType("text/html"))).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html")).andDo(print())
                .andExpect(xpath("//h2[@id='usersListHeader']//a/@href").string("/users/"))
                .andExpect(xpath("//h3[@id='usersListHeaderWithSortParams']//a/@href")
                        .string("/users/?sortField=birthDate&sortDirection=descending"))
                .andExpect(xpath("//h2[@id='otherAppsUsersListHeader']//a/@href").string("/otherapp/users/"))
                .andExpect(xpath("//h3[@id='otherAppsUsersListHeaderWithSortParams']//a/@href")
                        .string("/otherapp/users/?sortField=birthDate&sortDirection=descending"))
                .andExpect(xpath("//div[@id='user-%s']//a[@class='userIdLink']/@href", formattedUerId)
                        .string("/users/" + userId + "/"))
                .andExpect(xpath("//div[@id='user-%s']//a[@class='userNameLink']/@href", formattedUerId)
                        .string("/users/" + formattedUerId + "/"))
                .andExpect(xpath("//div[@id='user-%s']//a[@class='badUserBirthDateLink']/@href", formattedUerId)
                        .doesNotExist())
                .andExpect(xpath("//div[@id='user-%s']//a[@class='goodUserBirthDateLink']/@href", formattedUerId)
                        .string("/users/" + userId + "/?birthDate="
                                + new SimpleDateFormat(TEMPLATE_DATE_FORMAT).format(user.getBirthDate())))
                .andExpect(xpath("//div[@id='listLinkTest']//a[@class='badListLink']/@href").doesNotExist())
                .andExpect(xpath("//div[@id='listLinkTest']//a[@class='goodListLink']/@href")
                        .string("/users/?items=101_102"))
                .andExpect(xpath("//div[@id='mapLinkTest']//a[@class='badMapLink']/@href").doesNotExist())
                .andExpect(xpath("//div[@id='mapLinkTest']//a[@class='goodMapLink']/@href")
                        .string("/users/?items=101_102"))
                .andExpect(xpath("//div[@id='freeMarkerManualUrl']//a/@href")
                        .string("http://freemarker.org/docs/index.html"));
    }
}
