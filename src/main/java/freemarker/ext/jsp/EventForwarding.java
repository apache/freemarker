/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.ext.jsp;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import freemarker.log.Logger;

/**
 * An instance of this class should be registered as a <tt>&lt;listener></tt> in
 * the <tt>web.xml</tt> descriptor in order to correctly dispatch events to
 * event listeners that are specified in TLD files.
 * @author Attila Szegedi
 */
public class EventForwarding
    implements
        ServletContextAttributeListener,
        ServletContextListener,
        HttpSessionListener,
        HttpSessionAttributeListener
{
    private static final Logger logger = Logger.getLogger("freemarker.jsp");
    
    private static final String ATTR_NAME = EventForwarding.class.getName();
    
    private final List servletContextAttributeListeners = new ArrayList();
    private final List servletContextListeners = new ArrayList();
    private final List httpSessionAttributeListeners = new ArrayList();
    private final List httpSessionListeners = new ArrayList();

    void addListeners(List listeners)
    {
        for (Iterator iter = listeners.iterator(); iter.hasNext();)
        {
            addListener((EventListener)iter.next());
        }
    }
    
    private void addListener(EventListener listener)
    {
        boolean added = false;
        if(listener instanceof ServletContextAttributeListener)
        {
            addListener(servletContextAttributeListeners, listener);
            added = true;
        }
        if(listener instanceof ServletContextListener)
        {
            addListener(servletContextListeners, listener);
            added = true;
        }
        if(listener instanceof HttpSessionAttributeListener)
        {
            addListener(httpSessionAttributeListeners, listener);
            added = true;
        }
        if(listener instanceof HttpSessionListener)
        {
            addListener(httpSessionListeners, listener);
            added = true;
        }
        if(!added) {
            logger.warn(
                "Listener of class " + listener.getClass().getName() +
                "wasn't registered as it doesn't implement any of the " +
                "recognized listener interfaces.");
        }
    }

    static EventForwarding getInstance(ServletContext context)
    {
        return (EventForwarding)context.getAttribute(ATTR_NAME);
    }
    private void addListener(List listeners, EventListener listener)
    {
        synchronized(listeners)
        {
            listeners.add(listener);
        }
    }
    
    public void attributeAdded(ServletContextAttributeEvent arg0)
    {
        synchronized(servletContextAttributeListeners)
        {
            int s = servletContextAttributeListeners.size();
            for(int i = 0; i < s; ++i)
            {
                ((ServletContextAttributeListener)servletContextAttributeListeners.get(i)).attributeAdded(arg0);
            }
        }
    }

    public void attributeRemoved(ServletContextAttributeEvent arg0)
    {
        synchronized(servletContextAttributeListeners)
        {
            int s = servletContextAttributeListeners.size();
            for(int i = 0; i < s; ++i)
            {
                ((ServletContextAttributeListener)servletContextAttributeListeners.get(i)).attributeRemoved(arg0);
            }
        }
    }

    public void attributeReplaced(ServletContextAttributeEvent arg0)
    {
        synchronized(servletContextAttributeListeners)
        {
            int s = servletContextAttributeListeners.size();
            for(int i = 0; i < s; ++i)
            {
                ((ServletContextAttributeListener)servletContextAttributeListeners.get(i)).attributeReplaced(arg0);
            }
        }
    }

    public void contextInitialized(ServletContextEvent arg0)
    {
        arg0.getServletContext().setAttribute(ATTR_NAME, this);
        
        synchronized(servletContextListeners)
        {
            int s = servletContextListeners.size();
            for(int i = 0; i < s; ++i)
            {
                ((ServletContextListener)servletContextListeners.get(i)).contextInitialized(arg0);
            }
        }
    }

    public void contextDestroyed(ServletContextEvent arg0)
    {
        synchronized(servletContextListeners)
        {
            int s = servletContextListeners.size();
            for(int i = s - 1; i >= 0; --i)
            {
                ((ServletContextListener)servletContextListeners.get(i)).contextDestroyed(arg0);
            }
        }
    }

    public void sessionCreated(HttpSessionEvent arg0)
    {
        synchronized(httpSessionListeners)
        {
            int s = httpSessionListeners.size();
            for(int i = 0; i < s; ++i)
            {
                ((HttpSessionListener)httpSessionListeners.get(i)).sessionCreated(arg0);
            }
        }
    }

    public void sessionDestroyed(HttpSessionEvent arg0)
    {
        synchronized(httpSessionListeners)
        {
            int s = httpSessionListeners.size();
            for(int i = s - 1; i >= 0; --i)
            {
                ((HttpSessionListener)httpSessionListeners.get(i)).sessionDestroyed(arg0);
            }
        }
    }

    public void attributeAdded(HttpSessionBindingEvent arg0)
    {
        synchronized(httpSessionAttributeListeners)
        {
            int s = httpSessionAttributeListeners.size();
            for(int i = 0; i < s; ++i)
            {
                ((HttpSessionAttributeListener)httpSessionAttributeListeners.get(i)).attributeAdded(arg0);
            }
        }
    }

    public void attributeRemoved(HttpSessionBindingEvent arg0)
    {
        synchronized(httpSessionAttributeListeners)
        {
            int s = httpSessionAttributeListeners.size();
            for(int i = 0; i < s; ++i)
            {
                ((HttpSessionAttributeListener)httpSessionAttributeListeners.get(i)).attributeRemoved(arg0);
            }
        }
    }

    public void attributeReplaced(HttpSessionBindingEvent arg0)
    {
        synchronized(httpSessionAttributeListeners)
        {
            int s = httpSessionAttributeListeners.size();
            for(int i = 0; i < s; ++i)
            {
                ((HttpSessionAttributeListener)httpSessionAttributeListeners.get(i)).attributeReplaced(arg0);
            }
        }
    }
}
