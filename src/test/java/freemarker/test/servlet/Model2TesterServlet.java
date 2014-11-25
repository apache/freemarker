package freemarker.test.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import freemarker.ext.servlet.FreemarkerServlet;

/**
 * MVC controller servlet used for {@link FreemarkerServlet} JUnit tests. It understands these request parameters:
 * <ul>
 *   <li>{@value #VIEW_PARAM_NAME}: The servlet-relative path of the URL to which we forward after the action.
 *       Required unless the action will return with non-{@code null}.
 *   <li>{@value #ACTION_PARAM_NAME}: The full qualified name of the {@link Model2Action} implementation used as the
 *       action. Optional, by default it will be {@link DefaultModel2TesterAction#INSTANCE}. 
 * </ul>
 */
public class Model2TesterServlet extends HttpServlet {
    
    private static final Logger LOG = Logger.getLogger(Model2TesterServlet.class);
    
    public static final String VIEW_PARAM_NAME = "view";
    public static final String ACTION_PARAM_NAME = "action";

    private static final long serialVersionUID = 1L;

    private static final String CHARSET = "UTF-8";

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
            IOException {
        req.setCharacterEncoding(CHARSET);
        resp.setCharacterEncoding(CHARSET);

        final Model2Action action;
        {
            final String actionClassName = req.getParameter(ACTION_PARAM_NAME);
            if (actionClassName != null) {
                try {
                    action = (Model2Action) Class.forName(actionClassName).newInstance();
                } catch (final Throwable e) {
                    final String msg = "Failed to instantiate action: " + actionClassName;
                    resp.sendError(
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            msg + ".\nException (see log for trace): " + e);
                    LOG.error(msg, e);
                    return;
                }
            } else {
                action = DefaultModel2TesterAction.INSTANCE;
            }
        }
        
        final String actionViewPath;
        try {
            actionViewPath = action.execute(req, resp);
        } catch (final Throwable e) {
            final String msg = action.getClass().getName() + ".execute has thrown exception";
            resp.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    msg + ".\nException (see log for trace): " + e);
            LOG.error(msg, e);
            return;
        }

        final String paramViewPath = req.getParameter(VIEW_PARAM_NAME);
        final String viewPath = removeStartingSlash(actionViewPath != null ? actionViewPath : paramViewPath);
        
        if (viewPath == null) {
            resp.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "No view was specified as parameter or as action return value.");
            return;
        }
        
        req.getRequestDispatcher(viewPath).forward(req, resp);
    }

    private String removeStartingSlash(final String s) {
        if (s == null) return null;
        return s.startsWith("/") ? s.substring(1) : s;
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
            IOException {
        super.doGet(req, resp);
    }

}
