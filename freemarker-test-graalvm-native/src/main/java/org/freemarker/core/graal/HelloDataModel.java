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

package org.freemarker.core.graal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import freemarker.ext.beans.TemplateAccessible;

public class HelloDataModel {

    public enum Status {
        ACTIVE,
        PENDING,
        ARCHIVED
    }

    public record Item(String title, BigDecimal price, boolean inStock, List<String> tags) {
    }

    public String exposedNote = "public-field-ok";

    private String name;

    private String version;

    private Status status = Status.ACTIVE;

    private List<Item> items = new ArrayList<>();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @TemplateAccessible
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void setItems(List<Item> items) {
        this.items = new ArrayList<>(items);
    }

    public String formatItem(String label, int quantity) {
        return label + " x" + quantity;
    }

    public String formatItem(String label, String note) {
        return label + " (" + note + ")";
    }

    @TemplateAccessible
    public String whitelistedAction() {
        return "allowed:" + name;
    }

    public String getRestrictedSecret() {
        return "top-secret";
    }

    public static String staticGreeting(String target) {
        return "Hello from static, " + target + "!";
    }

}
