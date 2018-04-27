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
import org.apache.freemarker.spring.model.ElementAttributeMatcher;
import org.apache.freemarker.spring.model.MissingElementAttributeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.XpathResultMatchers;
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
public class SelectTemplateDirectiveModelTest {

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
        final String favoriteSport = user.getFavoriteSport();

        final ResultActions resultAcctions =
                mockMvc.perform(get("/users/{userId}/", userId).param("viewName", "test/model/form/select-directive-usages")
                .accept(MediaType.parseMediaType("text/html"))).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html")).andDo(print());

        XpathResultMatchers xPathMatchers = xpath("//form[@id='form1']//select[@name='favoriteSport']");
        resultAcctions.andExpect(xPathMatchers.node(new MissingElementAttributeMatcher("size")));
        resultAcctions.andExpect(xPathMatchers.node(new MissingElementAttributeMatcher("multiple")));

        for (int i = 0; i < UserRepository.INDOOR_SPORTS.size(); i++) {
            final String sport = UserRepository.INDOOR_SPORTS.get(i);
            xPathMatchers = xpath("//form[@id='form1']//select[@name='favoriteSport']//option[" + (i + 1) + "]");
            resultAcctions.andExpect(xPathMatchers.string(sport));

            if (sport.equals(favoriteSport)) {
                resultAcctions.andExpect(xPathMatchers.node(new ElementAttributeMatcher("selected", "selected")));
            } else {
                resultAcctions.andExpect(xPathMatchers.node(new MissingElementAttributeMatcher("selected")));
            }
        }

        xPathMatchers = xpath("//form[@id='form2']//select[@name='favoriteSport']");
        resultAcctions.andExpect(xPathMatchers.node(new MissingElementAttributeMatcher("size")));
        resultAcctions.andExpect(xPathMatchers.node(new MissingElementAttributeMatcher("multiple")));

        for (int i = 0; i < UserRepository.OUTDOOR_SPORTS.size(); i++) {
            final String sport = UserRepository.OUTDOOR_SPORTS.get(i);
            xPathMatchers = xpath("//form[@id='form2']//select[@name='favoriteSport']//option[" + (i + 1) + "]");
            resultAcctions.andExpect(xPathMatchers.string(sport));

            if (sport.equals(favoriteSport)) {
                resultAcctions.andExpect(xPathMatchers.node(new ElementAttributeMatcher("selected", "selected")));
            } else {
                resultAcctions.andExpect(xPathMatchers.node(new MissingElementAttributeMatcher("selected")));
            }
        }

        xPathMatchers = xpath("//form[@id='form3']//select[@name='favoriteSport']");
        resultAcctions.andExpect(xPathMatchers.node(new ElementAttributeMatcher("size", "3")));
        resultAcctions.andExpect(xPathMatchers.node(new ElementAttributeMatcher("multiple", "multiple")));

        for (int i = 0; i < UserRepository.ALL_SPORTS.size(); i++) {
            final String sport = UserRepository.ALL_SPORTS.get(i);
            xPathMatchers = xpath("//form[@id='form3']//select[@name='favoriteSport']//option[" + (i + 1) + "]");
            resultAcctions.andExpect(xPathMatchers.string(sport));

            if (sport.equals(favoriteSport)) {
                resultAcctions.andExpect(xPathMatchers.node(new ElementAttributeMatcher("selected", "selected")));
            } else {
                resultAcctions.andExpect(xPathMatchers.node(new MissingElementAttributeMatcher("selected")));
            }
        }

        xPathMatchers = xpath("//form[@id='form4']//select[@name='favoriteSport']");
        resultAcctions.andExpect(xPathMatchers.node(new MissingElementAttributeMatcher("size")));
        resultAcctions.andExpect(xPathMatchers.node(new MissingElementAttributeMatcher("multiple")));

        xPathMatchers = xpath("//form[@id='form4']//select[@name='favoriteSport']//option[1]");
        resultAcctions.andExpect(xPathMatchers.string("--- Select ---"));

        for (int i = 0; i < UserRepository.OUTDOOR_SPORTS.size(); i++) {
            final String sport = UserRepository.OUTDOOR_SPORTS.get(i);
            xPathMatchers = xpath("//form[@id='form4']//select[@name='favoriteSport']//option[" + (i + 2) + "]");
            resultAcctions.andExpect(xPathMatchers.string(sport));

            if (sport.equals(favoriteSport)) {
                resultAcctions.andExpect(xPathMatchers.node(new ElementAttributeMatcher("selected", "selected")));
            } else {
                resultAcctions.andExpect(xPathMatchers.node(new MissingElementAttributeMatcher("selected")));
            }
        }

        xPathMatchers = xpath("//form[@id='form5']//select[@name='favoriteSport']//option[1]");
        resultAcctions.andExpect(xPathMatchers.node(new ElementAttributeMatcher("disabled", "disabled")));
    }
}
