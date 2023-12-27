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
package org.apache.freemarker.servlet;

import jakarta.servlet.*;
import jakarta.servlet.ServletRegistration.Dynamic;
import jakarta.servlet.descriptor.JspConfigDescriptor;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

public class DummyMockServletContext implements ServletContext {

    @Override
    public Object getAttribute(String arg0) {
        return null;
    }

    @Override
    public Enumeration getAttributeNames() {
        return null;
    }

    @Override
    public ServletContext getContext(String arg0) {
        return null;
    }

    @Override
    public String getContextPath() {
        return "/myapp";
    }

    @Override
    public String getInitParameter(String arg0) {
        return null;
    }

    @Override
    public Enumeration getInitParameterNames() {
        return null;
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public String getMimeType(String arg0) {
        return null;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String arg0) {
        return null;
    }

    @Override
    public String getRealPath(String arg0) {
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String arg0) {
        return null;
    }

    @Override
    public URL getResource(String arg0) throws MalformedURLException {
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String arg0) {
        return null;
    }

    @Override
    public Set getResourcePaths(String arg0) {
        return null;
    }

    @Override
    public String getServerInfo() {
        return null;
    }

    @Override
    public String getServletContextName() {
        return "MyApp";
    }

    @Override
    public void log(String arg0) {
        
    }

    @Override
    public void log(String arg0, Throwable arg1) {
        
    }

    @Override
    public void removeAttribute(String arg0) {
    }

    @Override
    public void setAttribute(String arg0, Object arg1) {
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return false;
    }

    @Override
    public Dynamic addServlet(String servletName, String className)
            throws IllegalArgumentException, IllegalStateException {
        return null;
    }

    @Override
    public Dynamic addServlet(String servletName, Servlet servlet)
            throws IllegalArgumentException, IllegalStateException {
        return null;
    }

    @Override
    public Dynamic addServlet(String servletName, Class<? extends Servlet> clazz)
            throws IllegalArgumentException, IllegalStateException {
        return null;
    }

    @Override
    public Dynamic addJspFile(String s, String s1) {
        return null;
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return null;
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return null;
    }

    @Override
    public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className)
            throws IllegalArgumentException, IllegalStateException {
        return null;
    }

    @Override
    public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter)
            throws IllegalArgumentException, IllegalStateException {
        return null;
    }

    @Override
    public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass)
            throws IllegalArgumentException, IllegalStateException {
        return null;
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return null;
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return null;
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
    }

    @Override
    public void addListener(String className) {
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public void declareRoles(String... roleNames) {
    }

    @Override
    public String getVirtualServerName() {
        return null;
    }

    @Override
    public int getSessionTimeout() {
        return 0;
    }

    @Override
    public void setSessionTimeout(int i) {

    }

    @Override
    public String getRequestCharacterEncoding() {
        return null;
    }

    @Override
    public void setRequestCharacterEncoding(String s) {

    }

    @Override
    public String getResponseCharacterEncoding() {
        return null;
    }

    @Override
    public void setResponseCharacterEncoding(String s) {

    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return null;
    }

    @Override
    public int getEffectiveMajorVersion() throws UnsupportedOperationException {
        return 3;
    }

    @Override
    public int getEffectiveMinorVersion() throws UnsupportedOperationException {
        return 0;
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.getClassLoader();
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

}