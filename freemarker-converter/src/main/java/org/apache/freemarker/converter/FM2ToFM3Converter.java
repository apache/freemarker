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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.freemarker.core.util._NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.core.FM2ASTToFM3SourceConverter;
import freemarker.template.Configuration;
import freemarker.template.Template;

public class FM2ToFM3Converter extends Converter {

    public static final Logger LOG = LoggerFactory.getLogger(Converter.class);

    private static final Map<String, String> DEFAULT_REPLACED_FILE_EXTENSIONS;
    static {
        DEFAULT_REPLACED_FILE_EXTENSIONS = new HashMap<>();
        DEFAULT_REPLACED_FILE_EXTENSIONS.put("ftl", "fm3");
        DEFAULT_REPLACED_FILE_EXTENSIONS.put("fm", "fm3");
        DEFAULT_REPLACED_FILE_EXTENSIONS.put("ftlh", "fm3h");
        DEFAULT_REPLACED_FILE_EXTENSIONS.put("fmh", "fm3h");
        DEFAULT_REPLACED_FILE_EXTENSIONS.put("ftlx", "fm3x");
        DEFAULT_REPLACED_FILE_EXTENSIONS.put("fmx", "fm3x");
    }

    private Map<String, String> outputFileExtensions = DEFAULT_REPLACED_FILE_EXTENSIONS;
    private Properties freeMarker2Settings;
    private Configuration fm2Cfg;

    @Override
    protected void prepare() throws ConverterException {
        super.prepare();
        fm2Cfg = new Configuration(Configuration.VERSION_2_3_19 /* To fix ignored initial unknown tags */);
        fm2Cfg.setWhitespaceStripping(false);
        fm2Cfg.setTabSize(1);
        try {
            fm2Cfg.setSettings(freeMarker2Settings);
        } catch (Exception e) {
            throw new ConverterException("Error while configuring FreeMarker 2", e);
        }
    }

    private String getDestinationFileName(Template template) throws ConverterException {
        String srcFileName = template.getName();
        int lastDotIdx = srcFileName.lastIndexOf('.');
        if (lastDotIdx == -1) {
            return srcFileName;
        }

        String ext = srcFileName.substring(lastDotIdx + 1);

        String replacementExt = getOutputFileExtensions().get(ext);
        if (replacementExt == null) {
            replacementExt = getOutputFileExtensions().get(ext.toLowerCase());
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
        Template template;
        try {
            template = new Template(fileTransCtx.getSourceFile().getName(), src, fm2Cfg);
        } catch (Exception e) {
            throw new ConverterException("Failed to load FreeMarker 2.3.x template", e);
        }

        fileTransCtx.setDestinationFileName(getDestinationFileName(template));
        fileTransCtx.getDestinationStream().write(
                FM2ASTToFM3SourceConverter.convert(template, src).getBytes(getTemplateEncoding(template)));
    }

    private String getTemplateEncoding(Template template) {
        String encoding = template.getEncoding();
        return encoding != null ? encoding : fm2Cfg.getEncoding(template.getLocale());
    }

    public Map<String, String> getOutputFileExtensions() {
        return outputFileExtensions;
    }

    public void setOutputFileExtensions(Map<String, String> outputFileExtensions) {
        _NullArgumentException.check("outputFileExtensions", outputFileExtensions);
        this.outputFileExtensions = outputFileExtensions;
    }

    public Properties getFreeMarker2Settings() {
        return freeMarker2Settings;
    }

    public void setFreeMarker2Settings(Properties freeMarker2Settings) {
        _NullArgumentException.check("freeMarker2Settings", freeMarker2Settings);
        this.freeMarker2Settings = freeMarker2Settings;
    }

}