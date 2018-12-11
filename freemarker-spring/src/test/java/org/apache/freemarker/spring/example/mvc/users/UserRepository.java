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

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    public static final List<String> INDOOR_SPORTS = Collections
            .unmodifiableList(Arrays.asList("bowling", "gymnastics", "handball"));

    public static final List<String> OUTDOOR_SPORTS = Collections
            .unmodifiableList(Arrays.asList("baseball", "football", "marathon"));

    public static final List<String> ALL_SPORTS = Collections
            .unmodifiableList(Arrays.asList("bowling", "gymnastics", "handball", "baseball", "football", "marathon"));

    private Map<Long, User> usersMap = new ConcurrentHashMap<>();
    {
        Long id = 101L;
        User user = new User(id);
        user.setEmail("john@example.com");
        user.setPassword("johnpass");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setGender("M");
        Calendar birthDate = Calendar.getInstance();
        birthDate.set(Calendar.YEAR, 1973);
        birthDate.set(Calendar.MONTH, Calendar.JANUARY);
        birthDate.set(Calendar.DATE, 5);
        user.setBirthDate(birthDate.getTime());
        user.setDescription("Lorem ipsum dolor sit amet, \r\nconsectetur adipiscing elit, \r\n"
                + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
        user.setFavoriteSport("baseball");
        user.setReceiveNewsletter(false);
        user.setFavoriteFood(new String[] { "Sandwich", "Spaghetti" });
        usersMap.put(id, user);

        id = 102L;
        user = new User(id);
        user.setEmail("jane@example.com");
        user.setPassword("janepass");
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setGender("F");
        birthDate = Calendar.getInstance();
        birthDate.set(Calendar.YEAR, 1970);
        birthDate.set(Calendar.MONTH, Calendar.FEBRUARY);
        birthDate.set(Calendar.DATE, 7);
        user.setBirthDate(birthDate.getTime());
        user.setDescription("Ut enim ad minim veniam, \r\n"
                + "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.");
        user.setFavoriteSport("marathon");
        user.setReceiveNewsletter(true);
        user.setFavoriteFood(new String[] { "Sandwich", "Sushi" });
        usersMap.put(id, user);
    }

    public synchronized Set<Long> getUserIds() {
        return new TreeSet<>(usersMap.keySet());
    }

    public synchronized User getUser(final Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must be non-null.");
        }

        User user = usersMap.get(id);

        if (user != null) {
            return cloneUser(user, user.getId());
        }

        return null;
    }

    public synchronized User getUserByEmail(final String email) {
        if (email == null) {
            throw new IllegalArgumentException("E-Mail must be non-null.");
        }

        for (User user : usersMap.values()) {
            if (email.equals(user.getEmail())) {
                return cloneUser(user, user.getId());
            }
        }

        return null;
    }

    public synchronized User addOrUpdateUser(final User user) {
        final Long id = user.getId();
        User newUser = cloneUser(user, id);
        usersMap.put(id, newUser);
        return cloneUser(newUser, id);
    }

    public synchronized boolean deleteUser(final String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username must be non-null.");
        }

        final User user = usersMap.remove(username);
        return user != null;
    }

    private User cloneUser(final User source, final Long id) {
        User clone = new User(id);
        clone.setPassword(source.getPassword());
        clone.setEmail(source.getEmail());
        clone.setFirstName(source.getFirstName());
        clone.setLastName(source.getLastName());
        clone.setGender(source.getGender());
        clone.setBirthDate(source.getBirthDate());
        clone.setDescription(source.getDescription());
        clone.setFavoriteSport(source.getFavoriteSport());
        clone.setReceiveNewsletter(source.isReceiveNewsletter());
        clone.setFavoriteFood(source.getFavoriteFood());
        return clone;
    }
}
