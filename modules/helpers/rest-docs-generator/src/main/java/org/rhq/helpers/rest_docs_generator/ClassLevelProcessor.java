package org.rhq.helpers.rest_docs_generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiErrors;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.jboss.resteasy.annotations.GZIP;

/**
 * Processor for JAX-RS classes
 * @author Heiko W. Rupp
 */

@SupportedOptions({ClassLevelProcessor.TARGET_DIRECTORY,ClassLevelProcessor.VERBOSE})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes(value = {"com.wordnik.swagger.annotations.*","javax.ws.rs.*"})
public class ClassLevelProcessor extends AbstractProcessor {

    private static final String JAVAX_WS_RS = "javax.ws.rs";
    private static final String[] HTTP_METHODS = {"GET","PUT","POST","HEAD","DELETE","OPTIONS"};
    private static final String[] PARAM_SKIP_ANNOTATIONS = {"javax.ws.rs.core.UriInfo","javax.ws.rs.core.HttpHeaders","javax.servlet.http.HttpServletRequest","javax.ws.rs.core.Request"};
    private static final String API_OUT_XML = "rest-api-out.xml";
    public static final String TARGET_DIRECTORY = "targetDirectory";
    public static final String VERBOSE = "verbose";
    private static final String BODY_INDICATOR = "-body-";

    Log log = LogFactory.getLog(getClass().getName());

    private String targetDirectory;
    boolean verbose = false;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        Map<String,String>options =  processingEnv.getOptions();
        if (options.containsKey(TARGET_DIRECTORY)) {
            targetDirectory = options.get(TARGET_DIRECTORY);
        }
        if (options.containsKey(VERBOSE))
            verbose=true;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        // We are invoked twice, but do our work already in the first round
        if (roundEnv.processingOver())
            return true;

        Document doc;
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = documentBuilder.newDocument();
        }
        catch (Exception e) {
            log.error(e);
            return false;
        }

        Element root = doc.createElement("api");
        doc.appendChild(root);

        // Loop over all classes
        for (javax.lang.model.element.Element t : roundEnv.getRootElements()) { // classes to process
            processClass(doc, root, (TypeElement)t);
        }

        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 2); // xml indent 2 spaces
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); // do xml indent

            // We initialize here for String writing to be able to also see the result on stdout
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);

            String xmlString = result.getWriter().toString();
            if (verbose)
                System.out.println(xmlString);

            File f ;
            if (targetDirectory!=null) {
                File targetDir = new File(targetDirectory);
                if (!targetDir.exists())
                    targetDir.mkdirs();

                f = new File(targetDir, API_OUT_XML);
            }
            else
                f = new File(API_OUT_XML);

            String path = f.getAbsolutePath();
            String s = "..... writing to [" + path + "] ......";
            if (verbose)
                System.out.println(s);
            else
                log.info(s);

            try {
                FileWriter fw = new FileWriter(f);
                fw.write(xmlString);
                fw.flush();
                fw.close();
            } catch (IOException e) {
                log.error(e);
            }

        } catch (TransformerException e) {
            log.error(e);
        }
        return true;
    }

    private void processClass(Document doc, Element xmlRoot, TypeElement classElementIn) {

        log.debug("Looking at " + classElementIn.getQualifiedName().toString());
        Path basePath = classElementIn.getAnnotation(Path.class);
        if (basePath==null || basePath.value().isEmpty()) {
            log.debug("No @Path found on " + classElementIn.getQualifiedName() + " - skipping");
            return;
        }

        Element classElement = doc.createElement("class");
        String className = classElementIn.toString();
        classElement.setAttribute("name",className);
        String value = basePath.value();
        value = cleanOutPath(value);
        classElement.setAttribute("path", value);
        Api api = classElementIn.getAnnotation(Api.class);
        if (api!=null) {
            String shortDescription = api.value();
            setOptionalAttribute(classElement, "shortDesc", shortDescription);
            String longDescription = api.description();
            setOptionalAttribute(classElement, "description", longDescription);
            String basePathAttr = api.basePath();
            setOptionalAttribute(classElement, "basePath",basePathAttr);
        }
        Produces produces = classElementIn.getAnnotation(Produces.class);
        if (produces!=null) {
            String[] types = produces.value();
            Element pElement = doc.createElement("produces");
            classElement.appendChild(pElement);
            for (String type : types) {
                Element tElement = doc.createElement("type");
                pElement.appendChild(tElement);
                tElement.setTextContent(type);
            }
        }

        xmlRoot.appendChild(classElement);

        // Loop over the methods on this class
        for (ExecutableElement m : ElementFilter.methodsIn(classElementIn.getEnclosedElements())) {
            processMethods(doc, m, classElement);
        }
    }


    private void processMethods(Document doc, ExecutableElement td, Element classElement) {

        log.debug("  Looking at method " + td.getSimpleName().toString());

        Path pathAnnotation = td.getAnnotation(Path.class);
        if (pathAnnotation==null) {
            return;
        }
        String path = pathAnnotation.value();
        path = cleanOutPath(path);

        Element methodElement = doc.createElement("method");
        methodElement.setAttribute("path",path);
        GZIP gzip = td.getAnnotation(GZIP.class);
        if (gzip!=null) {
            methodElement.setAttribute("gzip","true");
        }
        classElement.appendChild(methodElement);
        Name elementName = td.getSimpleName();
        methodElement.setAttribute("name", elementName.toString());
        String httpMethod = getHttpMethod(td.getAnnotationMirrors());
        methodElement.setAttribute("method",httpMethod);
        ApiOperation apiOperation = td.getAnnotation(ApiOperation.class);
        if (apiOperation!=null) {
        String description = apiOperation.value();
        setOptionalAttribute(methodElement, "description", description);

        String responseClass = !apiOperation.responseClass().equals("ok")? apiOperation.responseClass() : null;

        setOptionalAttribute(methodElement,"returnType",responseClass,td.getReturnType().toString());
        }

        // Loop over the parameters
        processParams(doc, td, methodElement);

        processErrors(doc,td, methodElement);

    }

    /**
     * Process the parameters of a method.
     * @param doc Xml Document to add the output to
     * @param methodElement Method to look for parameters
     * @param parent The parent xml element to tack the results on
     */
    private void processParams(Document doc, ExecutableElement methodElement, Element parent) {
        for (VariableElement paramElement : methodElement.getParameters()) {
            TypeMirror t = paramElement.asType();
            if (skipParamType(t))
                continue;
            Element element = doc.createElement("param");
            parent.appendChild(element);
            // determine name
            String name;
            String paramType= BODY_INDICATOR;
            PathParam pp = paramElement.getAnnotation(PathParam.class);
            QueryParam qp = paramElement.getAnnotation(QueryParam.class);
            ApiParam ap = paramElement.getAnnotation(ApiParam.class);
            if (pp != null) {
                name = pp.value();
                paramType="Path";
            }
            else if (qp!=null) {
                name = qp.value();
                paramType="Query";
            }
            else if (ap!=null)
                name = ap.name();
            else {
                Name nameElement = paramElement.getSimpleName();
                name = nameElement.toString();
            }

            element.setAttribute("name", name);
            element.setAttribute("paramType",paramType);
            ApiParam apiParam = paramElement.getAnnotation(ApiParam.class);
            if (apiParam!=null) {
                String description = apiParam.value();
                setOptionalAttribute(element, "description", description);
                String required = String.valueOf(apiParam.required());
                if (pp!=null || paramType.equals(BODY_INDICATOR)) // PathParams are always required
                    required="true";

                setOptionalAttribute(element, "required", required, "false");
                String allowedValues = apiParam.allowableValues();
                setOptionalAttribute(element, "allowableValues", allowedValues, "all");
            }
            String defaultValue;
            DefaultValue dva = paramElement.getAnnotation(DefaultValue.class);
            if (dva!=null)
                defaultValue = dva.value();
            else if (ap!=null)
                defaultValue = ap.defaultValue();
            else
                defaultValue = "-none-";

            if (defaultValue!=null)
                element.setAttribute("defaultValue",defaultValue);


            element.setAttribute("type", t.toString());
        }
    }

    /**
     * Look at the ApiError(s) annotations and populate the output
     * @param doc XML Document to add
     * @param methodElement Method declaration to look at
     * @param parent The parent xml element to attach the result to
     */
    private void processErrors(Document doc, ExecutableElement methodElement, Element parent) {
        ApiError ae = methodElement.getAnnotation(ApiError.class);
        processError(doc,ae,parent);
        ApiErrors aes = methodElement.getAnnotation(ApiErrors.class);
        if (aes != null) {
            for (ApiError ae2 : aes.value()) {
                processError(doc,ae2,parent);
            }
        }
    }

    /**
     * Process a single @ApiError
     * @param doc XML Document to add
     * @param ae ApiError annotation to evaluate
     * @param parent Parent XML element to tack the ApiError data on
     */
    private void processError(Document doc, ApiError ae, Element parent) {
        if (ae==null)
            return;

        Element element = doc.createElement("error");
        parent.appendChild(element);
        element.setAttribute("code", String.valueOf(ae.code()));
        element.setAttribute("reason",ae.reason());
    }

    /**
     * Determine if the passed mirror belongs to an annotation that denotes a parameter to be skipped
     * @param t Type to analyze
     * @return True if the type matches the blacklist
     */
    private boolean skipParamType(TypeMirror t) {
        String name = t.toString();
        boolean skip=false;
        for (String toSkip : PARAM_SKIP_ANNOTATIONS) {
            if (toSkip.equals(name)) {
                skip=true;
                break;
            }
        }
        return skip;
    }

    /**
     * Determine the http method (@GET, @PUT etc.) from the list of annotations on the method
     * @param annotationMirrors mirrors for the method
     * @return The http method string or null if it can not be determined
     */
    private String getHttpMethod(Collection<? extends AnnotationMirror> annotationMirrors) {
        for (AnnotationMirror am : annotationMirrors) {
            javax.lang.model.element.Element element = am.getAnnotationType().asElement();
            String pName = element.getEnclosingElement().toString();
            String cName = element.getSimpleName().toString();
            if (pName.startsWith(JAVAX_WS_RS)) {
                for (String name : HTTP_METHODS) {
                    if (cName.equals(name)) {
                        return name;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Set the passed text as attribute name on the passed xmlElement if the text is not empty
     * @param xmlElement Element to set the attribute on
     * @param name The name of the attribute
     * @param text The text to set
     */
    private void setOptionalAttribute(Element xmlElement, String name, String text) {
        if (text!=null && !text.isEmpty()) {
            xmlElement.setAttribute(name, text);
        }
    }

    /**
     * Set the passed text as attribute name on the passed xmlElement if the text is not empty
     * @param xmlElement Element to set the attribute on
     * @param name The name of the attribute
     * @param text The text to set
     * @param defaultValue Value to set if text is null or empty
     */
    private void setOptionalAttribute(Element xmlElement, String name, String text,String defaultValue) {
        if (text!=null && !text.isEmpty()) {
            xmlElement.setAttribute(name, text);
        }
        else {
            xmlElement.setAttribute(name,defaultValue);
        }
    }


    private String cleanOutPath(String in) {
        if (in.equals("/"))
            return "";

        if (in.startsWith("/"))
            in = in.substring(1);
        if (in.endsWith("/"))
            in = in.substring(0,in.length()-1);

        return in;
    }

}
