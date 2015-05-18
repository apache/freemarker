/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.test.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface Model2Action {

    /**
     * The web application relative path of the view JSP or FTL, or {@code null} if we expect that to be
     * specified in the URL. The architecture meant to be similar to JSP Model 2, this, the FreeMarker data-model
     * variables meant to be created as servlet scope (request, session, etc.) attributes.
     * 
     * @return The servlet-relative path to forward to, or {@code null} if we expect it to be specified with the
     *          {@value Model2TesterServlet#VIEW_PARAM_NAME} request parameter.
     */
    public String execute(final HttpServletRequest req, final HttpServletResponse resp) throws Exception;

}
