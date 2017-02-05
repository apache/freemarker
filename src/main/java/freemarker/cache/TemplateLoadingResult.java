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
package freemarker.cache;

import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.Date;

import freemarker.core.TemplateConfiguration;
import freemarker.template.Configuration;
import freemarker.template.utility.NullArgumentException;

/**
 * Return value of {@link TemplateLoader#load(String, TemplateLoadingSource, Serializable, TemplateLoaderSession)}
 */
public final class TemplateLoadingResult {
    private final TemplateLoadingResultStatus status;
    private final TemplateLoadingSource source;
    private final Serializable version;
    private final Reader reader;
    private final InputStream inputStream;
    private final TemplateConfiguration templateConfiguration; 

    public static final TemplateLoadingResult NOT_FOUND = new TemplateLoadingResult(
            TemplateLoadingResultStatus.NOT_FOUND);
    public static final TemplateLoadingResult NOT_MODIFIED = new TemplateLoadingResult(
            TemplateLoadingResultStatus.NOT_MODIFIED);

    /**
     * Creates an instance with status {@link TemplateLoadingResultStatus#OPENED}, for a storage mechanism that
     * naturally returns the template content as sequence of {@code char}-s as opposed to a sequence of {@code byte}-s.
     * This is the case for example when you store the template in a database in a varchar or CLOB. Do <em>not</em> use
     * this constructor for stores that naturally return binary data instead (like files, class loader resources,
     * BLOB-s, etc.), because using this constructor will disable FreeMarker's charset selection mechanism.
     * 
     * @param source
     *            See {@link #getSource()}
     * @param version
     *            See {@link #getVersion()} for the meaning of this. Can be {@code null}, but use that only if the
     *            backing storage mechanism doesn't know this information.
     * @param reader
     *            Gives the content of the template
     * @param templateConfiguration
     *            Usually {@code null}, as usually the backing storage mechanism doesn't store such information;
     *            see {@link #getTemplateConfiguration()}.
     */
    public TemplateLoadingResult(TemplateLoadingSource source, Serializable version, Reader reader,
            TemplateConfiguration templateConfiguration) {
        NullArgumentException.check("templateSource", source);
        NullArgumentException.check("reader", reader);
        this.status = TemplateLoadingResultStatus.OPENED;
        this.source = source;
        this.version = version;
        this.reader = reader;
        this.inputStream = null;
        this.templateConfiguration = templateConfiguration; 
    }

    /**
     * Creates an instance with status {@link TemplateLoadingResultStatus#OPENED}, for a storage mechanism that
     * naturally returns the template content as sequence of {@code byte}-s as opposed to a sequence of {@code char}-s.
     * This is the case for example when you store the template in a file, classpath resource, or BLOB. Do <em>not</em>
     * use this constructor for stores that naturally return text instead (like database varchar and CLOB columns).
     * 
     * @param source
     *            See {@link #getSource()}
     * @param version
     *            See {@link #getVersion()} for the meaning of this. Can be {@code null}, but use that only if the
     *            backing storage mechanism doesn't know this information.
     * @param inputStream
     *            Gives the content of the template
     * @param templateConfiguration
     *            Usually {@code null}, as usually the backing storage mechanism doesn't store such information; see
     *            {@link #getTemplateConfiguration()}. The most probable application is supplying the charset (encoding)
     *            used by the {@link InputStream} (via {@link TemplateConfiguration#setEncoding(String)}), but only
     *            do that if the storage mechanism really knows what the charset is.
     */
    public TemplateLoadingResult(TemplateLoadingSource source, Serializable version, InputStream inputStream,
            TemplateConfiguration templateConfiguration) {
        NullArgumentException.check("templateSource", source);
        NullArgumentException.check("inputStream", inputStream);
        this.status = TemplateLoadingResultStatus.OPENED;
        this.source = source;
        this.version = version;
        this.reader = null;
        this.inputStream = inputStream;
        this.templateConfiguration = templateConfiguration; 
    }

    /**
     * Used internally for creating the singleton instances which has a state where all other fields are {@code null}.
     */
    private TemplateLoadingResult(TemplateLoadingResultStatus status) {
        this.status = status;
        this.source = null;
        this.version = null;
        this.reader = null;
        this.inputStream = null;
        this.templateConfiguration = null;
    }

    /**
     * Returns non-{@code null} exactly if {@link #getStatus()} is {@link TemplateLoadingResultStatus#OPENED} and the
     * backing store mechanism returns content as {@code byte}-s, as opposed to as {@code chars}-s. See also
     * {@link #TemplateLoadingResult(TemplateLoadingSource, Serializable, InputStream, TemplateConfiguration)}. It's the
     * responsibility of the caller (which is {@link TemplateCache} usually) to {@code close()} the {@link InputStream}.
     * The return value is always the same instance, no mater when and how many times this method is called.
     * 
     * <p>
     * The return {@code InputStream} should use buffering if that's useful considering the backing storage mechanism.
     * TODO Is it really needed?
     * 
     * @return {@code null} or a {@code InputStream} to read the template content; see method description for more.
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Tells what kind of result this is; see the documentation of {@link TemplateLoadingResultStatus}.
     */
    public TemplateLoadingResultStatus getStatus() {
        return status;
    }

    /**
     * Identifies the source on the level of the storage mechanism; stored in the cache together with the version
     * ({@link #getVersion()}). When checking if a cache entry is up to date, the sources are compared, and only if they
     * are equal are the versions compared. See more at {@link TemplateLoadingSource}.
     */
    public TemplateLoadingSource getSource() {
        return source;
    }

    /**
     * If the result status is {@link TemplateLoadingResultStatus#OPENED} and the backing storage stores such
     * information, the version (usually the last modification time) of the loaded template, otherwise {@code null}. The
     * version is some kind of value which changes when the template in the backing storage is updated. Usually, it's
     * the last modification time (a {@link Date} or {@link Long}), though that can be problematic if the template can
     * change twice within the granularity of the clock used by the storage. Thus some storages may use a revision
     * number instead that's always increased when the template is updated, or the cryptographic hash of the template
     * content as the version. Version objects are compared with each other with their {@link Object#equals(Object)}
     * method, to see if a cache entry is outdated (though only when the source objects ({@link #getSource()}) are
     * equal). Thus, the version object must have proper {@link Object#equals(Object)} and {@link Object#hashCode()}
     * methods.
     */
    public Serializable getVersion() {
        return version;
    }

    /**
     * Returns non-{@code null} exactly if {@link #getStatus()} is {@link TemplateLoadingResultStatus#OPENED} and the
     * backing store mechanism returns content as {@code char}-s, as opposed to as {@code byte}-s. See also
     * {@link #TemplateLoadingResult(TemplateLoadingSource, Serializable, Reader, TemplateConfiguration)}. It's the
     * responsibility of the caller (which is {@link TemplateCache} usually) to {@code close()} the {@link Reader}. The
     * return value is always the same instance, no mater when and how many times this method is called.
     * 
     * @return {@code null} or a {@code Reader} to read the template content; see method description for more.
     */
    public Reader getReader() {
        return reader;
    }

    /**
     * If {@link #getStatus()} is {@link TemplateLoadingResultStatus#OPENED}, and the template loader stores such
     * information (which is rare) then it returns the {@link TemplateConfiguration} applicable to the template,
     * otherwise it returns {@code null}. If {@link #getStatus()} is not {@link TemplateLoadingResultStatus#OPENED},
     * then this should always return {@code null}. If there are {@link TemplateConfiguration}-s coming from other
     * sources, such as from {@link Configuration#getTemplateConfigurations()}, this won't replace them, but will be
     * merged with them, with properties coming from the returned {@link TemplateConfiguration} having the highest
     * priority.
     * 
     * @return {@code null}, or a {@link TemplateConfiguration}. The parent configuration of the
     *         {@link TemplateConfiguration} need not be set. The returned {@link TemplateConfiguration} won't be
     *         modified. (If the caller needs to modify it, such as to call
     *         {@link TemplateConfiguration#setParentConfiguration(Configuration)}, it has to copy it first.)
     */
    public TemplateConfiguration getTemplateConfiguration() {
        return templateConfiguration;
    }
    
}