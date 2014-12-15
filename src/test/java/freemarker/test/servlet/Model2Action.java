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
