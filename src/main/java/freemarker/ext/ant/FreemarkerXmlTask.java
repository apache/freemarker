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

package freemarker.ext.ant;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

import freemarker.ext.dom.NodeModel;
import freemarker.ext.xml.NodeListModel;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateNodeModel;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.SecurityUtilities;


/**
 * <p>This is an <a href="http://jakarta.apache.org/ant/" target="_top">Ant</a> task for transforming
 * XML documents using FreeMarker templates. It uses the adapter class
 * {@link NodeListModel}. It will read a set of XML documents, and pass them to
 * the template for processing, building the corresponding output files in the
 * destination directory.</p>
 * <p>It makes the following variables available to the template in the data model:</p>
 * <ul>
 * <li><tt>document</tt>: <em>Deprecated!</em> The DOM tree of the currently processed XML file wrapped
      with the legacy {@link freemarker.ext.xml.NodeListModel}.
      For new projects you should use the <tt>.node</tt> instead, which initially
      contains the DOM Document wrapped with {@link freemarker.ext.dom.NodeModel}.</li>
 * <li><tt>properties</tt>: a {@link freemarker.template.SimpleHash} containing
 * properties of the project that executes the task</li>
 * <li><tt>userProperties</tt>: a {@link freemarker.template.SimpleHash} containing
 * user properties of the project that executes the task</li>
 * <li><tt>project</tt>: the DOM tree of the XML file specified by the
 * <tt>projectfile</tt>. It will not be available if you didn't specify the
 * <tt>projectfile</tt> attribute.</li>
 * <li>further custom models can be instantiated and made available to the 
 * templates using the <tt>models</tt> attribute.</li>
 * </ul>
 * <p>It supports the following attributes:</p>
 * <table style="width: auto; border-collapse: collapse" border="1" summary="FreeMarker XML ant task attributes">
 *   <tr>
 *     <th valign="top" align="left">Attribute</th>
 *     <th valign="top" align="left">Description</th>
 *     <th valign="top">Required</th>
 *   </tr>
 *   <tr>
 *     <td valign="top">basedir</td>
 *     <td valign="top">location of the XML files. Defaults to the project's
 *       basedir.</td>
 *     <td align="center" valign="top">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">destdir</td>
 *     <td valign="top">location to store the generated files.</td>
 *     <td align="center" valign="top">Yes</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">includes</td>
 *     <td valign="top">comma-separated list of patterns of files that must be
 *       included; all files are included when omitted.</td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">includesfile</td>
 *     <td valign="top">the name of a file that contains
 *       include patterns.</td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">excludes</td>
 *     <td valign="top">comma-separated list of patterns of files that must be
 *       excluded; no files (except default excludes) are excluded when omitted.</td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">excludesfile</td>
 *     <td valign="top">the name of a file that contains
 *       exclude patterns.</td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">defaultexcludes</td>
 *     <td valign="top">indicates whether default excludes should be used
 *       (<code>yes</code> | <code>no</code>); default excludes are used when omitted.</td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">extension</td>
 *     <td valign="top">extension of generated files. Defaults to .html.</td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">template</td>
 *     <td valign="top">name of the FreeMarker template file that will be
 *       applied by default to XML files</td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">templateDir</td>
 *     <td valign="top">location of the FreeMarker template(s) to be used, defaults
 *                       to the project's baseDir</td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">projectfile</td>
 *     <td valign="top">path to the project file. The poject file must be an XML file.
 *       If omitted, it will not be available to templates </td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">incremental</td>
 *     <td valign="top">indicates whether all files should be regenerated (no), or
 *       only those that are older than the XML file, the template file, or the
 *       project file (yes). Defaults to yes. </td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">encoding</td>
 *     <td valign="top">The encoding of the output files. Defaults to platform
 *       default encoding.</td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">templateEncoding</td>
 *     <td valign="top">The encoding of the template files. Defaults to platform
 *       default encoding.</td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">validation</td>
 *     <td valign="top">Whether to validate the XML input. Defaults to off.</td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">models</td>
 *     <td valign="top">A list of [name=]className pairs separated by spaces,
 *      commas, or semicolons that specifies further models that should be 
 *      available to templates. If name is omitted, the unqualified class name
 *      is used as the name. Every class that is specified must implement the
 *      TemplateModel interface and have a no-args constructor.</td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 * </table>
 * 
 * <p>It supports the following nesed elements:</p>
 * 
 * <table style="width: auto; border-collapse: collapse" border="1" summary="FreeMarker XML ant task nested elements">
 *   <tr>
 *     <th valign="top" align="left">Element</th>
 *     <th valign="top" align="left">Description</th>
 *     <th valign="top">Required</th>
 *   </tr>
 *   <tr>
 *     <td valign="top">prepareModel</td>
 *     <td valign="top">
 *      This element executes Jython script before the processing of each XML
 *      files, that you can use to modify the data model.
 *      You either enter the Jython script directly nested into this
 *      element, or specify a Jython script file with the <tt>file</tt>
 *      attribute.
 *      The following variables are added to the Jython runtime's local
 *      namespace before the script is invoked:
 *      <ul>
 *        <li><tt>model</tt>: The data model as <code>java.util.HashMap</code>.
 *           You can read and modify the data model with this variable.
 *        <li><tt>doc</tt>: The XML document as <code>org.w3c.dom.Document</code>.
 *        <li><tt>project</tt>: The project document (if used) as
 *           <code>org.w3c.dom.Document</code>.
 *      </ul>
 *      <i>If this element is used, Jython classes (tried with Jython 2.1)
 *      must be available.</i>
 *    </td>
 *    <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">prepareEnvironment</td>
 *     <td valign="top">This element executes Jython script before the processing
 *      of each XML files, that you can use to modify the freemarker environment
 *      ({@link freemarker.core.Environment}). The script is executed after the
 *      <tt>prepareModel</tt> element. The accessible Jython variables are the
 *      same as with the <tt>prepareModel</tt> element, except that there is no
 *      <tt>model</tt> variable, but there is <tt>env</tt> variable, which is
 *      the FreeMarker environment ({@link freemarker.core.Environment}).
 *      <i>If this element is used, Jython classes (tried with Jython 2.1)
 *      must be available.</i>
 *    </td>
 *    <td valign="top" align="center">No</td>
 *   </tr>
 * </table>
 * @deprecated <a href="http://fmpp.sourceforge.net">FMPP</a> is a more complete solution.
 */
public class FreemarkerXmlTask
extends
    MatchingTask
{
    private JythonAntTask prepareModel;
    private JythonAntTask prepareEnvironment;
    private final DocumentBuilderFactory builderFactory;
    private DocumentBuilder builder;
    
    /** the {@link Configuration} used by this task. */
    private Configuration cfg = new Configuration();
    
    /** the destination directory */
    private File destDir;

    /** the base directory */
    private File baseDir;

    //Where the templates live
    
    private File templateDir;
    
    /** the template= attribute */
    private String templateName;

    /** The template in its parsed form */
    private Template parsedTemplate;

    /** last modified of the template sheet */
    private long templateFileLastModified = 0;

    /** the projectFile= attribute */
    private String projectAttribute = null;

    private File projectFile = null;

    /** The DOM tree of the project wrapped into FreeMarker TemplateModel */
    private TemplateModel projectTemplate;
    // The DOM tree wrapped using the freemarker.ext.dom wrapping.
    private TemplateNodeModel projectNode;
    private TemplateModel propertiesTemplate;
    private TemplateModel userPropertiesTemplate;

    /** last modified of the project file if it exists */
    private long projectFileLastModified = 0;

    /** check the last modified date on files. defaults to true */
    private boolean incremental = true;

    /** the default output extension is .html */
    private String extension = ".html";

    private String encoding = SecurityUtilities.getSystemProperty("file.encoding", "utf-8");
    private String templateEncoding = encoding;
    private boolean validation = false;

    private String models = "";
    private final Map modelsMap = new HashMap();
    
    
    
    /**
     * Constructor creates the SAXBuilder.
     */
    public FreemarkerXmlTask()
    {
        builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
    }

    /**
     * Set the base directory. Defaults to <tt>.</tt>
     */
    public void setBasedir(File dir)
    {
        baseDir = dir;
    }

    /**
     * Set the destination directory into which the generated
     * files should be copied to
     * @param dir the name of the destination directory
     */
    public void setDestdir(File dir)
    {
        destDir = dir;
    }

    /**
     * Set the output file extension. <tt>.html</tt> by default.
     */
    public void setExtension(String extension)
    {
        this.extension = extension;
    }

    public void setTemplate(String templateName) {
        this.templateName = templateName;
    }
    
    public void setTemplateDir(File templateDir) throws BuildException {
        this.templateDir = templateDir;
        try {
            cfg.setDirectoryForTemplateLoading(templateDir);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    /**
     * Set the path to the project XML file
     */
    public void setProjectfile(String projectAttribute)
    {
        this.projectAttribute = projectAttribute;
    }

    /**
     * Turn on/off incremental processing. On by default
     */
    public void setIncremental(String incremental)
    {
        this.incremental = !(incremental.equalsIgnoreCase("false") || incremental.equalsIgnoreCase("no") || incremental.equalsIgnoreCase("off"));
    }

    /**
     * Set encoding for generated files. Defaults to platform default encoding.
     */
    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }

    public void setTemplateEncoding(String inputEncoding)
    {
        this.templateEncoding = inputEncoding;
    }
    
    /**
     * Sets whether to validate the XML input.
     */
    public void setValidation(boolean validation) 
    {
        this.validation = validation;
    }

    public void setModels(String models)
    {
        this.models = models;
    }
    
    public void execute() throws BuildException
    {
        DirectoryScanner scanner;
        String[]         list;

        if (baseDir == null)
        {
            baseDir = getProject().getBaseDir();
        }
        if (destDir == null )
        {
            String msg = "destdir attribute must be set!";
            throw new BuildException(msg, getLocation());
        }
        
        File templateFile = null;

        if (templateDir == null) {
            if (templateName != null) {
                templateFile = new File(templateName);
                if (!templateFile.isAbsolute()) {
                    templateFile = new File(getProject().getBaseDir(), templateName);
                }
                templateDir = templateFile.getParentFile();
                templateName = templateFile.getName();
            }
            else {
                templateDir = baseDir;
            }
            setTemplateDir(templateDir);
        } else if (templateName != null) {
            if (new File(templateName).isAbsolute()) {
                throw new BuildException("Do not specify an absolute location for the template as well as a templateDir");
            }
            templateFile = new File(templateDir, templateName);
        }
        if (templateFile != null) {
            templateFileLastModified = templateFile.lastModified();
        }

        try {
            if (templateName != null) {
                parsedTemplate = cfg.getTemplate(templateName, templateEncoding);
            }
        }
        catch (IOException ioe) {
            throw new BuildException(ioe.toString());
        }
        // get the last modification of the template
        log("Transforming into: " + destDir.getAbsolutePath(), Project.MSG_INFO);

        // projectFile relative to baseDir
        if (projectAttribute != null && projectAttribute.length() > 0)
        {
            projectFile = new File(baseDir, projectAttribute);
            if (projectFile.isFile())
                projectFileLastModified = projectFile.lastModified();
            else
            {
                log ("Project file is defined, but could not be located: " +
                     projectFile.getAbsolutePath(), Project.MSG_INFO );
                projectFile = null;
            }
        }

        generateModels();
        
        // find the files/directories
        scanner = getDirectoryScanner(baseDir);

        propertiesTemplate = wrapMap(project.getProperties());
        userPropertiesTemplate = wrapMap(project.getUserProperties());

        builderFactory.setValidating(validation);
        try
        {
            builder = builderFactory.newDocumentBuilder();
        }
        catch(ParserConfigurationException e)
        {
            throw new BuildException("Could not create document builder", e, getLocation());
        }

        // get a list of files to work on
        list = scanner.getIncludedFiles();
        
        
        for (int i = 0;i < list.length; ++i)
        {
            process(baseDir, list[i], destDir);
        }
    }
    
    public void addConfiguredJython(JythonAntTask jythonAntTask) {
        this.prepareEnvironment = jythonAntTask;
    }

    public void addConfiguredPrepareModel(JythonAntTask prepareModel) {
        this.prepareModel = prepareModel;
    }

    public void addConfiguredPrepareEnvironment(JythonAntTask prepareEnvironment) {
        this.prepareEnvironment = prepareEnvironment;
    }
    
    /**
     * Process an XML file using FreeMarker
     */
    private void process(File baseDir, String xmlFile, File destDir)
    throws BuildException
    {
        File outFile=null;
        File inFile=null;
        try
        {
            // the current input file relative to the baseDir
            inFile = new File(baseDir,xmlFile);
            // the output file relative to basedir
            outFile = new File(destDir,
                               xmlFile.substring(0,
                                                 xmlFile.lastIndexOf('.')) + extension);

            // only process files that have changed
            if (!incremental ||
                (inFile.lastModified() > outFile.lastModified() ||
                 templateFileLastModified > outFile.lastModified() ||
                 projectFileLastModified > outFile.lastModified()))
            {
                ensureDirectoryFor(outFile);

                //-- command line status
                log("Input:  " + xmlFile, Project.MSG_INFO );
                
                if (projectTemplate == null && projectFile != null) {
                    Document doc = builder.parse(projectFile);
                    projectTemplate = new NodeListModel(builder.parse(projectFile));
                    projectNode = NodeModel.wrap(doc);
                }

                // Build the file DOM
                Document docNode = builder.parse(inFile);
                
                TemplateModel document = new NodeListModel(docNode);
                TemplateNodeModel docNodeModel = NodeModel.wrap(docNode);
                HashMap root = new HashMap();
                root.put("document", document);
                insertDefaults(root);

                // Process the template and write out
                // the result as the outFile.
                Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), encoding));
                try
                {
                    if (parsedTemplate == null) {
                        throw new BuildException("No template file specified in build script or in XML file");
                    }
                    if (prepareModel != null) {
                        Map vars = new HashMap();
                        vars.put("model", root);
                        vars.put("doc", docNode);
                        if (projectNode != null) {
                            vars.put("project", ((NodeModel) projectNode).getNode());
                        }
                        prepareModel.execute(vars);
                    }
                    freemarker.core.Environment env = parsedTemplate.createProcessingEnvironment(root, writer);
                    env.setCurrentVisitorNode(docNodeModel);
                    if (prepareEnvironment != null) {
                        Map vars = new HashMap();
                        vars.put("env", env);
                        vars.put("doc", docNode);
                        if (projectNode != null) {
                            vars.put("project", ((NodeModel) projectNode).getNode());
                        }
                        prepareEnvironment.execute(vars);
                    }
                    env.process();
                    writer.flush();
                }
                finally
                {
                    writer.close();
                }

                log("Output: " + outFile, Project.MSG_INFO );
                
            }
        }
        catch (SAXParseException spe)
        {
            Throwable rootCause = spe;
            if (spe.getException() != null)
                rootCause = spe.getException();
            log("XML parsing error in " + inFile.getAbsolutePath(), Project.MSG_ERR);
            log("Line number " + spe.getLineNumber());
            log("Column number " + spe.getColumnNumber());
            throw new BuildException(rootCause, getLocation());
        }
        catch (Throwable e)
        {
            if (outFile != null ) {
                if(!outFile.delete() && outFile.exists()) {
                    log("Failed to delete " + outFile, Project.MSG_WARN);
                }
            }
            e.printStackTrace();
            throw new BuildException(e, getLocation());
        }
    }

    private void generateModels()
    {
        StringTokenizer modelTokenizer = new StringTokenizer(models, ",; ");
        while(modelTokenizer.hasMoreTokens())
        {
            String modelSpec = modelTokenizer.nextToken();
            String name = null;
            String clazz = null;
            
            int sep = modelSpec.indexOf('=');
            if(sep == -1)
            {
                // No explicit name - use unqualified class name
                clazz = modelSpec;
                int dot = clazz.lastIndexOf('.');
                if(dot == -1)
                {
                    // clazz in the default package
                    name = clazz;
                }
                else
                {
                    name = clazz.substring(dot + 1);
                }
            }
            else
            {
                name = modelSpec.substring(0, sep);
                clazz = modelSpec.substring(sep + 1);
            }
            try
            {
                modelsMap.put(name, ClassUtil.forName(clazz).newInstance());
            }
            catch(Exception e)
            {
                throw new BuildException(e);
            }
        }
    }
    
    /**
     * create directories as needed
     */
    private void ensureDirectoryFor( File targetFile ) throws BuildException
    {
        File directory = new File( targetFile.getParent() );
        if (!directory.exists())
        {
            if (!directory.mkdirs())
            {
                throw new BuildException("Unable to create directory: "
                                         + directory.getAbsolutePath(), getLocation());
            }
        }
    }

    private static TemplateModel wrapMap(Map table)
    {
        SimpleHash model = new SimpleHash();
        for (Iterator it = table.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry entry = (Map.Entry)it.next();
            model.put(String.valueOf(entry.getKey()), new SimpleScalar(String.valueOf(entry.getValue())));
        }
        return model;
    }

    protected void insertDefaults(Map root) 
    {
        root.put("properties", propertiesTemplate);
        root.put("userProperties", userPropertiesTemplate);
        if (projectTemplate != null) {
            root.put("project", projectTemplate);
            root.put("project_node", projectNode);
        }
        if(modelsMap.size() > 0)
        {
            for (Iterator it = modelsMap.entrySet().iterator(); it.hasNext();)
            {
                Map.Entry entry = (Map.Entry) it.next();
                root.put(entry.getKey(), entry.getValue());
            }
        }
    }
    
}
