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

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import freemarker.core.FM2ASTToFM3SourceConverter;

public class FM2ToFM3ConverterCLI {

    public static int SUCCESS_EXIT_STATUS = 0;
    public static int COMMAND_LINE_FORMAT_ERROR_EXIT_STATUS = -1;
    public static int EXECUTION_ERROR_EXIT_STATUS = 1;

    private static final String DESTINATION_OPTION = "destination";
    private static final String CREATE_DESTINATION_OPTION = "create-destination";
    private static final String INCLUDE_OPTION = "include";
    private static final String EXCLUDE_OPTION = "exclude";
    private static final String FILE_EXTENSION_SUBSTITUTION = "file-ext-subst";
    private static final String NO_PREDEFINED_FILE_EXTENSION_SUBSTITUTIONS = "no-predef-file-ext-substs";
    private static final String SKIP_UNPARSEABLE_FILES = "skip-unparsable-files";
    private static final String FREEMARKER_2_SETTING_OPTION = "fm2-setting";
    private static final String HELP_OPTION = "help";
    private static final String HELP_OPTION_SHORT = "h";

    private static final Options OPTIONS = new Options()
            .addOption(Option.builder("d").longOpt(DESTINATION_OPTION)
                    .required().hasArg().argName("dir")
                    .desc("The directory where the converted files will be written. Required!")
                    .build())
            .addOption(Option.builder("p").longOpt(CREATE_DESTINATION_OPTION)
                    .desc("Whether the top level destination directory will be created if it doesn't yet exists.")
                    .build())
            .addOption(Option.builder(null).longOpt(INCLUDE_OPTION)
                    .hasArg().argName("regexp")
                    .desc("Only files that match this Java regular expression will be processed. Default matches "
                            + "files with the commonly used FreeMarker 2 file extensions (ftl, ftlh, ftlx, fm, "
                            + "case insensitive). For the file to be included, the pattern must fully match the "
                            + "path of the file relative to the source directory, that has all backslashes "
                            + "replaced with slashes (relevant on Windows).")
                    .build())
            .addOption(Option.builder(null).longOpt(EXCLUDE_OPTION)
                    .hasArg().argName("regexp")
                    .desc("Files that match the this Java regular expression will not be processed. This filters "
                            + "the files already matched by the \"include\" option. The default matches nothing "
                            + "(nothing is excluded). See the \"include\" option about the matched path.")
                    .build())
            .addOption(Option.builder("S").longOpt(FREEMARKER_2_SETTING_OPTION)
                    .hasArgs().argName("name=value").valueSeparator()
                    .desc("FreeMarker 2 configuration settings, to influence the parsing of the source. You can have "
                            + "multiple instances of this option, to set multiple settings. For the possible names "
                            + "and values see the FreeMarker 2 documentation, especially "
                            + "http://freemarker.org/docs/api/freemarker/template/Configuration.html#setSetting-java"
                            + ".lang.String-java.lang.String-"
                            + ".")
                    .build())
            .addOption(Option.builder("E").longOpt(FILE_EXTENSION_SUBSTITUTION)
                    .hasArgs().argName("old=new").valueSeparator()
                    .desc("File extensions that will be substituted (replaced). If predefined substitutions are "
                            + "allowed (by default they are), then this substitution is added to those "
                            + "(maybe replacing one).")
                    .build())
            .addOption(Option.builder(null).longOpt(NO_PREDEFINED_FILE_EXTENSION_SUBSTITUTIONS)
                    .desc("Disables the predefined file extension substitutions (i.e, \"ftl\", \"ftlh\", "
                            + "\"ftlx\" and \"fm\" are replaced with the corresponding FreeMarker 3 file extensions).")
                    .build())
            .addOption(Option.builder(null).longOpt(SKIP_UNPARSEABLE_FILES)
                    .desc("Ignore source files that aren't syntactically vaild FreeMarker 2.x templates. The problem "
                            + "will be logged as a warning into to the conversion markers file.")
                    .build())
            .addOption(Option.builder(HELP_OPTION_SHORT).longOpt(HELP_OPTION)
                    .desc("Prints command-line help.")
                    .build());

    private static final String SYNOPSIS_START = "java " + FM2ToFM3Converter.class.getName() + " <srcFileOrDir>";

    private final PrintWriter out;

    private FM2ToFM3ConverterCLI(PrintWriter out) {
        this.out = out;
    }

    public static void main(String[] args) {
        System.exit(execute(new PrintWriter(System.out), args));
    }

    public static int execute(PrintWriter out, String... args) {
        return new FM2ToFM3ConverterCLI(out).executeInternal(args);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private int executeInternal(String... args) {
        if (args.length == 0) {
            printHelp(true);
            return COMMAND_LINE_FORMAT_ERROR_EXIT_STATUS;
        }

        if (Arrays.asList(args).contains("--" + HELP_OPTION) || Arrays.asList(args).contains("-" + HELP_OPTION_SHORT)) {
            printHelp(false);
            return SUCCESS_EXIT_STATUS;
        }

        try {
            CommandLine cl = new DefaultParser().parse(OPTIONS, args);

            if (cl.hasOption(HELP_OPTION)) {
                printHelp(false);
            } else {
                List<String> unparsedArgs = cl.getArgList();
                if (unparsedArgs.size() != 1) {
                    throw new ParseException("You must specify exactly one source file or directory.");
                }

                FM2ToFM3Converter converter = new FM2ToFM3Converter();

                converter.setSource(new File(unparsedArgs.get(0)));

                converter.setDestinationDirectory(new File(cl.getOptionValue(DESTINATION_OPTION)));

                if (cl.hasOption(CREATE_DESTINATION_OPTION)) {
                    converter.setCreateDestinationDirectory(true);
                }

                if (cl.hasOption(INCLUDE_OPTION)) {
                    converter.setInclude(getRegexpOption(cl, INCLUDE_OPTION));
                }

                if (cl.hasOption(EXCLUDE_OPTION)) {
                    converter.setExclude(getRegexpOption(cl, EXCLUDE_OPTION));
                }

                converter.setPredefinedFileExtensionSubstitutionsEnabled(
                        !cl.hasOption(NO_PREDEFINED_FILE_EXTENSION_SUBSTITUTIONS));

                converter.setFileExtensionSubstitutions((Map) Collections.unmodifiableMap(
                        cl.getOptionProperties(FILE_EXTENSION_SUBSTITUTION)));
                
                if (cl.hasOption(SKIP_UNPARSEABLE_FILES)) {
                    converter.setSkipUnparsableFiles(true);
                }

                converter.setFreeMarker2Settings(cl.getOptionProperties(FREEMARKER_2_SETTING_OPTION));
                try {
                    converter.execute();

                    if (converter.getConvertedFileCount() == 0) {
                        printWrapped("No file to convert was found.");
                    } else {
                        printWrapped("Conversion was finished successfully. "
                                + "Converted " + converter.getConvertedFileCount() + " file(s).");
                    }
                } catch (ConverterException e) {
                    printWrapped("Conversion was terminated with error. See details in the Java stack "
                            + "trace:\n");
                    printConciseStackTrace(e);
                    return EXECUTION_ERROR_EXIT_STATUS;
                }
            }
        } catch (ParseException e) {
            printWrapped("Wrong command line input: " + e.getMessage() + "\n");
            printHelp(true);

            return COMMAND_LINE_FORMAT_ERROR_EXIT_STATUS;
        }
        return SUCCESS_EXIT_STATUS;
    }

    private Pattern getRegexpOption(CommandLine cl, String optionName) throws ParseException {
        String optionValue = cl.getOptionValue(optionName);
        try {
            return Pattern.compile(optionValue);
        } catch (PatternSyntaxException e) {
            throw new ParseException(
                    "The value of the \"" + optionName + "\" is not a valid regular expression: " + optionValue);
        }
    }

    private void printConciseStackTrace(Throwable e) {
        boolean first = true;
        while (e != null) {
            printWrapped((first ? "" : "Caused by ") + e.getClass().getName() + ": " + e.getMessage());
            e = e.getCause();
            first = false;
        }
    }

    private void printHelp(boolean usageOnly) {
        HelpFormatter hf = new HelpFormatter();
        if (usageOnly) {
            hf.printUsage(out, hf.getWidth(), SYNOPSIS_START, OPTIONS);
            printWrapped("Use option -" + HELP_OPTION_SHORT + " for more help.");
        } else {
            hf.printHelp(
                    out, hf.getWidth(),
                    SYNOPSIS_START,
                    "\n"
                            + "Converts FreeMarker 2 templates to FreeMarker 3 templates, as far as it's possible automatically. "

                            + "While the output will consist of syntactically correct FreeMarker 3 templates, the "
                            + "templates will have to be reviewed by humans, due to the semantic differences (such as"
                            + " a "
                            + "different treatment of null).\n\nOptions:",
                    OPTIONS,
                    hf.getLeftPadding(), hf.getDescPadding(),
                    "\nFor more information check the Javadoc of " + FM2ASTToFM3SourceConverter.class.getName()
                            + ", for now...",
                    true);
        }
        out.flush();
    }

    private void printWrapped(String s) {
        HelpFormatter hf = new HelpFormatter();
        hf.printWrapped(out, hf.getWidth(), s);
        out.flush();
    }

}
