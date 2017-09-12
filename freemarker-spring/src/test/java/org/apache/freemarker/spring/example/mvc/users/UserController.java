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

package org.apache.freemarker.spring.example.mvc.users;

import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserController {

    private static final String DEFAULT_USER_LIST_VIEW_NAME = "example/users/userlist";

    private static final String DEFAULT_USER_EDIT_VIEW_NAME = "example/users/useredit";

    @Autowired
    private UserRepository userRepository;

    @RequestMapping(value = "/users/", method = RequestMethod.GET)
    public String listUsers(@RequestParam(value = "viewName", required = false) String viewName, Model model) {
        List<User> users = new LinkedList<>();

        for (Integer id : userRepository.getUserIds()) {
            users.add(userRepository.getUser(id));
        }

        model.addAttribute("users", users);

        return (StringUtils.hasText(viewName)) ? viewName : DEFAULT_USER_LIST_VIEW_NAME;
    }

    @RequestMapping(value = "/users/{id:\\d+}", method = RequestMethod.GET)
    public String getUser(@PathVariable("id") Integer id,
            @RequestParam(value = "viewName", required = false) String viewName, Model model) {
        User user = userRepository.getUser(id);

        if (user != null) {
            model.addAttribute("user", user);
        } else {
            model.addAttribute("errorMessage",
                    new DefaultMessageSourceResolvable(new String[] { "user.error.notfound" }, new Object[] { id }));
        }

        return (StringUtils.hasText(viewName)) ? viewName : DEFAULT_USER_EDIT_VIEW_NAME;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

}
