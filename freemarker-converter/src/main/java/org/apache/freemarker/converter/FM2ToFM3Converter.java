/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.converter;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.freemarker.converter.ConversionMarkers.Type;
import org.apache.freemarker.core.util._NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.StrongCacheStorage;
import freemarker.cache.TemplateLoader;
import freemarker.cache.TemplateLookupStrategy;
import freemarker.core.CSSOutputFormat;
import freemarker.core.FM2ASTToFM3SourceConverter;
import freemarker.core.HTMLOutputFormat;
import freemarker.core.JSONOutputFormat;
import freemarker.core.JavaScriptOutputFormat;
import freemarker.core.MarkupOutputFormat;
import freemarker.core.OutputFormat;
import freemarker.core.ParseException;
import freemarker.core.PlainTextOutputFormat;
import freemarker.core.RTFOutputFormat;
import freemarker.core.UndefinedOutputFormat;
import freemarker.core.XHTMLOutputFormat;
import freemarker.core.XMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template._TemplateAPI;

/**
 * Converts FreeMarker 2 templates to FreeMarker 3 templates, as far as it's possible automatically. While the output
 * will contain syntactically correct FreeMarker 3 templates, the templates will have to be reviewed by humans,
 * due to the semantic differences (such as a different treatment of {@code null}).
 * <p>
 * This is work in progress... new conversion are mostly only added when the syntactical change was
 * already implemented. Current conversions:
 * <ul>
 *     <li>All FreeMarker defined names are converted camel case
 *     <li>Renamed setting names in the {@code ftl} heading and in {@code #setting} tags are replaced with the new name.
 *     <li>Renamed built-in variables are replaced with the new name
 *     <li>From {@code #else}, {@code #elseif} and {@code #recover} tags that end with {@code "/>"} or {@code "/]"} the
 *         "/" characters is removed (as it's now illegal)
 *     <li>{@code #else}, {@code #elseif} and {@code #recover} tags that end with {@code "/>"} or {@code "/]"} the
 *         "/" characters is removed (as it's now illegal)
 *     <li>The last tag in {@code <#attempt>...<#recover>...</#recover>} is replaced with {@code </#attempt>}
 * </ul>
 */
public class FM2ToFM3Converter extends Converter {

    public static final Pattern DEFAULT_INCLUDE = Pattern.compile("(?i).*\\.(fm|ftl(x|h)?)");

    public static final Map<String, String> PREDEFINED_FILE_EXTENSION_SUBSTITUTIONS
            = new ImmutableMap.Builder<String,String>()
                    .put("ftl", "f3ac")
                    .put("ftlh", "f3ah")
                    .put("ftlx", "f3ax")
                    .put("fm", "f3ac")
                    .build();

    private static final Logger LOG = LoggerFactory.getLogger(Converter.class);
    
    private boolean predefinedFileExtensionSubstitutionsEnabled;
    private Map<String, String> fileExtensionSubstitutions = PREDEFINED_FILE_EXTENSION_SUBSTITUTIONS;
    private Properties freeMarker2Settings;
    private Configuration fm2Cfg;
    private StringTemplateLoader stringTemplateLoader;
    private boolean validateOutput = true;
    private boolean skipUnparsableFiles;

    @Override
    protected Pattern getDefaultInclude() {
        return DEFAULT_INCLUDE;
    }

    @Override
    protected void prepare() throws ConverterException {
        super.prepare();
        fm2Cfg = new Configuration(Configuration.VERSION_2_3_19 /* To fix ignored initial unknown tags */);
        fm2Cfg.setRecognizeStandardFileExtensions(true);
        if (freeMarker2Settings != null) {
            try {
                fm2Cfg.setSettings(freeMarker2Settings);
            } catch (Exception e) {
                throw new ConverterException("Error while configuring FreeMarker 2", e);
            }
        }

        // From now on we will overwrite settings that the user has set with freeMarker2Settings.

        fm2Cfg.setTabSize(1);
        _TemplateAPI.setPreventStrippings(fm2Cfg, true);
        fm2Cfg.setTemplateLookupStrategy(TemplateLookupStrategy.DEFAULT_2_3_0);
        fm2Cfg.setLocalizedLookup(false);
        fm2Cfg.setCacheStorage(new StrongCacheStorage());

        stringTemplateLoader = new StringTemplateLoader();
        try {
            fm2Cfg.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[] {
                    stringTemplateLoader,
                    new FileTemplateLoader(getSource().isDirectory() ? getSource() : getSource().getParentFile())
                }
            ));
        } catch (IOException e) {
            throw new ConverterException("Failed to create template loader", e);
        }
    }

    private String getDestinationFileName(Template template) throws ConverterException {
        String srcFileName = template.getName();
        int lastSlashIdx = srcFileName.lastIndexOf('/');
        if (lastSlashIdx != -1) {
            srcFileName = srcFileName.substring(lastSlashIdx + 1);
        }

        int lastDotIdx = srcFileName.lastIndexOf('.');
        if (lastDotIdx == -1) {
            return srcFileName;
        }

        String ext = srcFileName.substring(lastDotIdx + 1);

        String replacementExt = getFileExtensionSubstitutions().get(ext);
        if (replacementExt == null) {
            replacementExt = getFileExtensionSubstitutions().get(ext.toLowerCase());
        }
        if (replacementExt == null && getPredefinedFileExtensionSubstitutionsEnabled()) {
            replacementExt = PREDEFINED_FILE_EXTENSION_SUBSTITUTIONS.get(ext.toLowerCase());
        }
        if (replacementExt == null) {
            return srcFileName;
        }

        if (template.getActualTagSyntax() == Configuration.SQUARE_BRACKET_TAG_SYNTAX) {
            replacementExt = replacementExt.replace("3a", "3s");
        }

        return srcFileName.substring(0, lastDotIdx + 1) + replacementExt;
    }
    
    @Override
    protected void convertFile(FileConversionContext fileTransCtx) throws ConverterException, IOException {
        Template template = null;
        try {
            template = fm2Cfg.getTemplate(fileTransCtx.getRelativeSourcePathWithSlashes());
            fm2Cfg.clearTemplateCache();
        } catch (Exception e) {
            if (getSkipUnparsableFiles() && e instanceof ParseException) {
                ParseException pe = (ParseException) e;
                fileTransCtx.getConversionMarkers().markInSource(
                        pe.getLineNumber(), pe.getColumnNumber(), Type.WARN, "Skipped file due to parse error: "
                        + pe.getEditorMessage());
                LOG.debug("Skipped file due to parsing error: {}", fileTransCtx.getRelativeSourcePathWithSlashes());
                return; //!
            } else {
                throw new ConverterException("Failed to load FreeMarker 2.3.x template", e);
            }
        }

        FM2ASTToFM3SourceConverter.Result result = FM2ASTToFM3SourceConverter.convert(
                template, fm2Cfg, stringTemplateLoader, fileTransCtx.getConversionMarkers()
        );
        fileTransCtx.setDestinationFileName(getDestinationFileName(result.getFM2Template()));
        fileTransCtx.getDestinationStream().write(
                result.getFM3Content().getBytes(getTemplateEncoding(result.getFM2Template())));

        if (validateOutput) {
            try {
                org.apache.freemarker.core.Configuration fm3Config = new org.apache.freemarker.core.Configuration
                        .Builder(org.apache.freemarker.core.Configuration.getVersion() /* highest possible by design */)
                        .outputFormat(converOutputFormat(result.getFM2Template().getOutputFormat()))
                        .build();
                new org.apache.freemarker.core.Template(null, result.getFM3Content(), fm3Config);
            } catch (Exception e) {
                throw new ConverterException(
                        "The result of the conversion wasn't valid FreeMarker 3 template; see cause exception and "
                                + fileTransCtx.getDestinationFile(), e);
            }
        }
    }

    private org.apache.freemarker.core.outputformat.OutputFormat converOutputFormat(OutputFormat outputFormat) {
        return outputFormat == HTMLOutputFormat.INSTANCE
                        ? org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat.INSTANCE
                : outputFormat == XHTMLOutputFormat.INSTANCE
                        ? org.apache.freemarker.core.outputformat.impl.XHTMLOutputFormat.INSTANCE
                : outputFormat == XMLOutputFormat.INSTANCE
                        ? org.apache.freemarker.core.outputformat.impl.XMLOutputFormat.INSTANCE
                : outputFormat == RTFOutputFormat.INSTANCE
                        ? org.apache.freemarker.core.outputformat.impl.RTFOutputFormat.INSTANCE
                : outputFormat == PlainTextOutputFormat.INSTANCE
                        ? org.apache.freemarker.core.outputformat.impl.PlainTextOutputFormat.INSTANCE
                : outputFormat == UndefinedOutputFormat.INSTANCE
                        ? org.apache.freemarker.core.outputformat.impl.UndefinedOutputFormat.INSTANCE
                : outputFormat == JavaScriptOutputFormat.INSTANCE
                        ? org.apache.freemarker.core.outputformat.impl.JavaScriptOutputFormat.INSTANCE
                : outputFormat == JSONOutputFormat.INSTANCE
                        ? org.apache.freemarker.core.outputformat.impl.JSONOutputFormat.INSTANCE
                : outputFormat == CSSOutputFormat.INSTANCE
                        ? org.apache.freemarker.core.outputformat.impl.CSSOutputFormat.INSTANCE
                : getSimilarOutputFormat(outputFormat);
    }

    private org.apache.freemarker.core.outputformat.OutputFormat getSimilarOutputFormat(OutputFormat outputFormat) {
        if (outputFormat instanceof MarkupOutputFormat) {
            return org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat.INSTANCE;
        } else {
            return org.apache.freemarker.core.outputformat.impl.PlainTextOutputFormat.INSTANCE;
        }
    }

    private String getTemplateEncoding(Template template) {
        String encoding = template.getEncoding();
        return encoding != null ? encoding : fm2Cfg.getEncoding(template.getLocale());
    }

    /**
     * Getter pair of {@link #setPredefinedFileExtensionSubstitutionsEnabled(boolean)}
     */
    public boolean getPredefinedFileExtensionSubstitutionsEnabled() {
        return predefinedFileExtensionSubstitutionsEnabled;
    }

    /**
     * Whether to use {@link #PREDEFINED_FILE_EXTENSION_SUBSTITUTIONS} when {@link #getFileExtensionSubstitutions()}
     * contains no mapping for the source file extension; defaults to {@code true}.
     */
    public void setPredefinedFileExtensionSubstitutionsEnabled(boolean predefinedFileExtensionSubstitutionsEnabled) {
        this.predefinedFileExtensionSubstitutionsEnabled = predefinedFileExtensionSubstitutionsEnabled;
    }

    /**
     * Getter pair of {@link #setFileExtensionSubstitutions(Map)} .
     */
    public Map<String, String> getFileExtensionSubstitutions() {
        return fileExtensionSubstitutions;
    }

    /**
     * Defines source file file extensions to destination file extensions mappings, in additionally to the
     * {@linkplain #setPredefinedFileExtensionSubstitutionsEnabled(boolean) predefined file extension substitutions}.
     * It's recommended to use lower case file extensions as keys, as if there's no hit with case sensitive lookup,
     * it will be retried with the source file extension converted to lower case (so it will be a case insensitive
     * lookup in effect). Mappings given here has higher priority than those coming from the
     * {@linkplain #setPredefinedFileExtensionSubstitutionsEnabled(boolean) predefined file extension substitutions}.
     */
    public void setFileExtensionSubstitutions(Map<String, String> fileExtensionSubstitutions) {
        _NullArgumentException.check("fileExtensionSubstitutions", fileExtensionSubstitutions);
        this.fileExtensionSubstitutions = fileExtensionSubstitutions;
    }

    public Properties getFreeMarker2Settings() {
        return freeMarker2Settings;
    }

    public void setFreeMarker2Settings(Properties freeMarker2Settings) {
        _NullArgumentException.check("freeMarker2Settings", freeMarker2Settings);
        this.freeMarker2Settings = freeMarker2Settings;
    }

    public boolean getValidateOutput() {
        return validateOutput;
    }

    public void setValidateOutput(boolean validateOutput) {
        this.validateOutput = validateOutput;
    }
    
    /**
     * Getter pair of {@link #setSkipUnparsableFiles(boolean)}.
     */
    public boolean getSkipUnparsableFiles() {
        return skipUnparsableFiles;
    }

    /**
     * Sets whether source files that syntactically aren't valid FreeMarker 2 templates should be ignored.
     * The problem will be logged as a warning into to the conversion markers file.
     * Defaults to {@code false}. 
     */
    public void setSkipUnparsableFiles(boolean skipUnparsableFiles) {
        this.skipUnparsableFiles = skipUnparsableFiles;
    }

}