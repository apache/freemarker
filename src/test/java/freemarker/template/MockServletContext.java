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
package freemarker.template;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.FilterRegistration.Dynamic;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.descriptor.JspConfigDescriptor;

public class MockServletContext implements ServletContext {

    public Object getAttribute(String arg0) {
        return null;
    }

    public Enumeration getAttributeNames() {
        return null;
    }

    public ServletContext getContext(String arg0) {
        return null;
    }

    public String getContextPath() {
        return "/myapp";
    }

    public String getInitParameter(String arg0) {
        return null;
    }

    public Enumeration getInitParameterNames() {
        return null;
    }

    public int getMajorVersion() {
        return 0;
    }

    public String getMimeType(String arg0) {
        return null;
    }

    public int getMinorVersion() {
        return 0;
    }

    public RequestDispatcher getNamedDispatcher(String arg0) {
        return null;
    }

    public String getRealPath(String arg0) {
        return null;
    }

    public RequestDispatcher getRequestDispatcher(String arg0) {
        return null;
    }

    public URL getResource(String arg0) throws MalformedURLException {
        return null;
    }

    public InputStream getResourceAsStream(String arg0) {
        return null;
    }

    public Set getResourcePaths(String arg0) {
        return null;
    }

    public String getServerInfo() {
        return null;
    }

    public Servlet getServlet(String arg0) throws ServletException {
        return null;
    }

    public String getServletContextName() {
        return "MyApp";
    }

    public Enumeration getServletNames() {
        return null;
    }

    public Enumeration getServlets() {
        return null;
    }

    public void log(String arg0) {
        
    }

    public void log(Exception arg0, String arg1) {
        
    }

    public void log(String arg0, Throwable arg1) {
        
    }

    public void removeAttribute(String arg0) {
    }

    public void setAttribute(String arg0, Object arg1) {
    }

	@Override
	public Dynamic addFilter(String arg0, String arg1) {
		return null;
	}

	@Override
	public Dynamic addFilter(String arg0, Filter arg1) {
		return null;
	}

	@Override
	public Dynamic addFilter(String arg0, Class<? extends Filter> arg1) {
		return null;
	}

	@Override
	public jakarta.servlet.ServletRegistration.Dynamic addJspFile(String arg0, String arg1) {
		return null;
	}

	@Override
	public void addListener(String arg0) {
	}

	@Override
	public <T extends EventListener> void addListener(T arg0) {
	}

	@Override
	public void addListener(Class<? extends EventListener> arg0) {
	}

	@Override
	public jakarta.servlet.ServletRegistration.Dynamic addServlet(String arg0, String arg1) {
		return null;
	}

	@Override
	public jakarta.servlet.ServletRegistration.Dynamic addServlet(String arg0, Servlet arg1) {
		return null;
	}

	@Override
	public jakarta.servlet.ServletRegistration.Dynamic addServlet(String arg0, Class<? extends Servlet> arg1) {
		return null;
	}

	@Override
	public <T extends Filter> T createFilter(Class<T> arg0) throws ServletException {
		return null;
	}

	@Override
	public <T extends EventListener> T createListener(Class<T> arg0) throws ServletException {
		return null;
	}

	@Override
	public <T extends Servlet> T createServlet(Class<T> arg0) throws ServletException {
		return null;
	}

	@Override
	public void declareRoles(String... arg0) {
	}

	@Override
	public ClassLoader getClassLoader() {
		return null;
	}

	@Override
	public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
		return null;
	}

	@Override
	public int getEffectiveMajorVersion() {
		return 0;
	}

	@Override
	public int getEffectiveMinorVersion() {
		return 0;
	}

	@Override
	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
		return null;
	}

	@Override
	public FilterRegistration getFilterRegistration(String arg0) {
		return null;
	}

	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		return null;
	}

	@Override
	public JspConfigDescriptor getJspConfigDescriptor() {
		return null;
	}

	@Override
	public String getRequestCharacterEncoding() {
		return null;
	}

	@Override
	public String getResponseCharacterEncoding() {
		return null;
	}

	@Override
	public ServletRegistration getServletRegistration(String arg0) {
		return null;
	}

	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		return null;
	}

	@Override
	public SessionCookieConfig getSessionCookieConfig() {
		return null;
	}

	@Override
	public int getSessionTimeout() {
		return 0;
	}

	@Override
	public String getVirtualServerName() {
		return null;
	}

	@Override
	public boolean setInitParameter(String arg0, String arg1) {
		return false;
	}

	@Override
	public void setRequestCharacterEncoding(String arg0) {
	}

	@Override
	public void setResponseCharacterEncoding(String arg0) {
	}

	@Override
	public void setSessionTimeout(int arg0) {
	}

	@Override
	public void setSessionTrackingModes(Set<SessionTrackingMode> arg0) {
	}

}
