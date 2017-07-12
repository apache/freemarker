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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.freemarker.core.NamingConvention;
import org.apache.freemarker.core.util._NullArgumentException;

import com.google.common.collect.ImmutableMap;

import freemarker.core.CSSOutputFormat;
import freemarker.core.FM2ASTToFM3SourceConverter;
import freemarker.core.HTMLOutputFormat;
import freemarker.core.JSONOutputFormat;
import freemarker.core.JavaScriptOutputFormat;
import freemarker.core.MarkupOutputFormat;
import freemarker.core.OutputFormat;
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

    public static final Map<String, String> DEFAULT_FILE_EXTENSION_SUBSTITUTIONS
            = new ImmutableMap.Builder<String,String>()
                    .put("ftl", "fm3")
                    .put("ftlh", "fm3h")
                    .put("ftlx", "fm3x")
                    .put("fm", "fm3")
                    .build();

    private Map<String, String> fileExtensionSubtitutions = DEFAULT_FILE_EXTENSION_SUBSTITUTIONS;
    private Properties freeMarker2Settings;
    private Configuration fm2Cfg;

    @Override
    protected Pattern getDefaultInclude() {
        return DEFAULT_INCLUDE;
    }

    @Override
    protected void prepare() throws ConverterException {
        super.prepare();
        fm2Cfg = new Configuration(Configuration.VERSION_2_3_19 /* To fix ignored initial unknown tags */);
        fm2Cfg.setWhitespaceStripping(false);
        fm2Cfg.setTabSize(1);
        fm2Cfg.setRecognizeStandardFileExtensions(true);
        _TemplateAPI.setPreventStrippings(fm2Cfg, true);
        if (freeMarker2Settings != null) {
            try {
                fm2Cfg.setSettings(freeMarker2Settings);
            } catch (Exception e) {
                throw new ConverterException("Error while configuring FreeMarker 2", e);
            }
        }
    }

    private String getDestinationFileName(Template template) throws ConverterException {
        String srcFileName = template.getName();
        int lastDotIdx = srcFileName.lastIndexOf('.');
        if (lastDotIdx == -1) {
            return srcFileName;
        }

        String ext = srcFileName.substring(lastDotIdx + 1);

        String replacementExt = getFileExtensionSubtitutions().get(ext);
        if (replacementExt == null) {
            replacementExt = getFileExtensionSubtitutions().get(ext.toLowerCase());
        }
        if (replacementExt == null) {
            return srcFileName;
        }

        if (template.getActualTagSyntax() == Configuration.SQUARE_BRACKET_TAG_SYNTAX) {
            replacementExt = replacementExt.replace("3", "3s");
        }

        return srcFileName.substring(0, lastDotIdx + 1) + replacementExt;
    }
    
    @Override
    protected void convertFile(FileConversionContext fileTransCtx) throws ConverterException, IOException {
        String src = IOUtils.toString(fileTransCtx.getSourceStream(), StandardCharsets.UTF_8);
        FM2ASTToFM3SourceConverter.Result result = FM2ASTToFM3SourceConverter.convert(
                fileTransCtx.getSourceFile().getName(), src, fm2Cfg, fileTransCtx.getConversionMarkers()
        );
        fileTransCtx.setDestinationFileName(getDestinationFileName(result.getFM2Template()));
        fileTransCtx.getDestinationStream().write(
                result.getFM3Content().getBytes(getTemplateEncoding(result.getFM2Template())));

        try {
            org.apache.freemarker.core.Configuration fm3Config = new org.apache.freemarker.core.Configuration
                    .Builder(org.apache.freemarker.core.Configuration.getVersion() /* highest possible by design */)
                    .namingConvention(NamingConvention.CAMEL_CASE)
                    .outputFormat(converOutputFormat(result.getFM2Template().getOutputFormat()))
                    .build();
            new org.apache.freemarker.core.Template(null, result.getFM3Content(), fm3Config);
        } catch (Exception e) {
            throw new ConverterException(
                    "The result of the conversion wasn't valid FreeMarker 3 template; see cause exception and "
                    + fileTransCtx.getDestinationFile(), e);
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

    public Map<String, String> getFileExtensionSubtitutions() {
        return fileExtensionSubtitutions;
    }

    public void setFileExtensionSubtitutions(Map<String, String> fileExtensionSubtitutions) {
        _NullArgumentException.check("fileExtensionSubtitutions", fileExtensionSubtitutions);
        this.fileExtensionSubtitutions = fileExtensionSubtitutions;
    }

    public Properties getFreeMarker2Settings() {
        return freeMarker2Settings;
    }

    public void setFreeMarker2Settings(Properties freeMarker2Settings) {
        _NullArgumentException.check("freeMarker2Settings", freeMarker2Settings);
        this.freeMarker2Settings = freeMarker2Settings;
    }

}