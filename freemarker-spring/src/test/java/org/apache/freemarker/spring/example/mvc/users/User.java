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

import java.util.Date;

public class User {

    private Long id;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private Date birthDate;
    private String description;
    private String favoriteSport;
    private boolean receiveNewsletter;
    private String[] favoriteFood;

    public User() {
    }

    public User(final Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFavoriteSport() {
        return favoriteSport;
    }

    public void setFavoriteSport(String favoriteSport) {
        this.favoriteSport = favoriteSport;
    }

    public boolean isReceiveNewsletter() {
        return receiveNewsletter;
    }

    public void setReceiveNewsletter(boolean receiveNewsletter) {
        this.receiveNewsletter = receiveNewsletter;
    }

    public String[] getFavoriteFood() {
        if (favoriteFood == null) {
            return null;
        }

        String[] cloned = new String[favoriteFood.length];
        System.arraycopy(favoriteFood, 0, cloned, 0, favoriteFood.length);
        return cloned;
    }

    public void setFavoriteFood(String[] favoriteFood) {
        this.favoriteFood = favoriteFood;
    }

    @Override
    public String toString() {
        return super.toString() + " {id=" + id + ", firstName='" + firstName + "', lastName='" + lastName + "', email='"
                + email + "', birthDate='" + birthDate + "', description='" + description + "', favoriteSport='"
                + favoriteSport + "', receiveNewsletter=" + receiveNewsletter + ", favoriteFood=" + favoriteFood + "}";
    }
}
