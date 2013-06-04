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

package freemarker.cache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Wraps a <code>java.net.URL</code>, and implements methods required for a typical template source.
 * @author Daniel Dekany
 * @author TJ Laurenzo
 */
class URLTemplateSource {
    private final URL url;
    private URLConnection conn;
    private InputStream inputStream;

    URLTemplateSource(URL url) throws IOException {
        this.url = url;
        this.conn = url.openConnection();
    }

    public boolean equals(Object o) {
        if (o instanceof URLTemplateSource) {
            return url.equals(((URLTemplateSource) o).url);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return url.hashCode();
    }

    public String toString() {
        return url.toString();
    }
    
    long lastModified() {
        if (conn instanceof JarURLConnection) {
          // There is a bug in sun's jar url connection that causes file handle leaks when calling getLastModified()
          // Since the time stamps of jar file contents can't vary independent from the jar file timestamp, just use
          // the jar file timestamp
          URL jarURL=((JarURLConnection)conn).getJarFileURL();
          if (jarURL.getProtocol().equals("file")) {
            // Return the last modified time of the underlying file - saves some opening and closing
            return new File(jarURL.getFile()).lastModified();
          } else {
            // Use the URL mechanism
            URLConnection jarConn=null;
            try {
              jarConn=jarURL.openConnection();
              return jarConn.getLastModified();
            } catch (IOException e) {
              return -1;
            } finally {
              try {
                if (jarConn!=null) jarConn.getInputStream().close();
              } catch (IOException e) { }
            }
          }
        } else {
          long lastModified = conn.getLastModified();
          if (lastModified == -1L && url.getProtocol().equals("file")) {
              // Hack for obtaining accurate last modified time for
              // URLs that point to the local file system. This is fixed
              // in JDK 1.4, but prior JDKs returns -1 for file:// URLs.
              return new File(url.getFile()).lastModified();
          } else {
              return lastModified;
          }
        }
    }

    InputStream getInputStream() throws IOException {
        inputStream = conn.getInputStream();
        return inputStream;
    }

    void close() throws IOException {
        try {
          if (inputStream != null) {
              inputStream.close();
          } else {
              conn.getInputStream().close();
          }
        } finally {
          inputStream = null;
          conn = null;
        }
    }
}
