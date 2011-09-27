package org.rhq.enterprise.server.remote;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.management.MBeanServer;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceClient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.invocation.NameBasedInvocation;

import org.rhq.core.server.ExternalizableStrategy;
import org.rhq.enterprise.server.safeinvoker.HibernateDetachUtility;

public class RemoteWsInvocationHandler implements ServerInvocationHandler {

    private static final Log log = LogFactory.getLog(RemoteWsInvocationHandler.class);
    private static final Map<String, Class<?>> PRIMITIVE_CLASSES;
    private static Map<String, Class<?>> WS_MAP_CLASSES = new HashMap<String, Class<?>>();
    private static Object objFactory = null;

    //Stores references to jaxb static types that have been refelectively located.
    private static HashMap<String, Object> remoteList = new HashMap<String, Object>();

    static {
        PRIMITIVE_CLASSES = new HashMap<String, Class<?>>();
        PRIMITIVE_CLASSES.put(Short.TYPE.getName(), Short.TYPE);
        PRIMITIVE_CLASSES.put(Integer.TYPE.getName(), Integer.TYPE);
        PRIMITIVE_CLASSES.put(Long.TYPE.getName(), Long.TYPE);
        PRIMITIVE_CLASSES.put(Float.TYPE.getName(), Float.TYPE);
        PRIMITIVE_CLASSES.put(Double.TYPE.getName(), Double.TYPE);
        PRIMITIVE_CLASSES.put(Boolean.TYPE.getName(), Boolean.TYPE);
        PRIMITIVE_CLASSES.put(Character.TYPE.getName(), Character.TYPE);
        PRIMITIVE_CLASSES.put(Byte.TYPE.getName(), Byte.TYPE);
    }

    public void addListener(InvokerCallbackHandler arg0) {
    }

    public Object invoke(InvocationRequest invocationRequest) throws Throwable {

        System.out.println("In WS Invocation handler ...:" + invocationRequest);

        if (invocationRequest == null) {
            throw new IllegalArgumentException("WS-InvocationRequest was null.");
        }

        String methodName = null;
        boolean successful = false; // we will flip this to true when we know we were successful
        Object result = null;

        long time = System.currentTimeMillis();

        try {
            InitialContext ic = new InitialContext();

            //Figure out which method was called
            NameBasedInvocation nbi = ((NameBasedInvocation) invocationRequest.getParameter());
            if (null == nbi) {
                throw new IllegalArgumentException("WS-InvocationRequest did not supply method.");
            }

            //Split the protocol and figure out how to handle.
            methodName = nbi.getMethodName();
            String[] methodInfo = methodName.split(":");

            // Lookup the remote first, if it doesn't exist exit with error.
            // This prevents remote clients from accessing the locals.
            String jndiName = "rhq/" + methodInfo[0];
            Object target = ic.lookup(jndiName + "/remote");
            target = ic.lookup(jndiName + "/local");

            //run down the method to invoke
            String[] signature = nbi.getSignature();
            int signatureLength = signature.length;
            Class<?>[] sig = new Class[signatureLength];
            for (int i = 0; i < signatureLength; i++) {
                sig[i] = getClass(signature[i]);
            }

            //RETAIN all of the above from original remoteInvocationHandler as JBossWs gets us into server
            // side ...

            //            //following functionality is meant to be replicated via static types generated.
            Method m = target.getClass().getMethod(methodInfo[1], sig);
            result = m.invoke(target, nbi.getParameters());
            //            successful = true;
            //to be commented above when all WS methods work correctly

            //Attempt to create static types and make the call reflectively
            System.out.println("echo curr meth SIGNATURE:" + signature + ":size:" + signature.length);
            for (int i = 0; i < signature.length; i++) {
                System.out.println("SIG:" + signature[i] + ":");
            }

            try {//wrap everything in exceptions and handle accordingly

                /*Remote webservice object reference that the command is to be called against. In other words there is a
                 * static JAXB type that the ObjectFactory creates that we'll then run the command against. 
                 */
                Object remoteWsRef = null;
                // nbi.getMethodName() ex. [SubjectManagerBean:login]
                //String remoteClassKey = "org.rhq.enterprise.server.ws." + methodInfo[0] + "Service";
                // Ex. SubjectManagerBeanService

                //                String remoteClassKey = methodInfo[0] + "Service";
                //this is hard coded now
                String remoteClassKey = "WebservicesManagerBeanService";

                //locate the WsRemote references
                if (!remoteList.containsKey(remoteClassKey)) {//if not there lazily instantiate
                    //locate Service
                    Class<?> located = Class.forName("org.rhq.enterprise.server.ws." + remoteClassKey);
                    System.out.println("Located service ref:" + located);
                    //Generate URL: is on local host as well.
                    URL sUrl = generateRemoteWebserviceURL(located, "127.0.0.1", 7080, false);
                    //Generate QName
                    QName sQName = generateRemoteWebserviceQName(located);
                    //locate constructor and call it
                    Constructor<?> constructor = located.getConstructor(URL.class, QName.class);
                    System.out.println("Constructor for service located:" + constructor);
                    //instantiate
                    Object servInstance = constructor.newInstance(sUrl, sQName);
                    System.out.println("ServiceInstance:" + servInstance);
                    //Finally get the remote instance A.K.A port
                    //find method Ex. smService.getSubjectManagerBeanPort();
                    //                    Method portMethod = servInstance.getClass().getMethod("get" + methodInfo[0] + "Port", null);
                    Method portMethod = servInstance.getClass().getMethod("getWebservicesManagerBeanPort", null);

                    //call method
                    Object remoteInst = portMethod.invoke(servInstance, null);
                    System.out.println("Remote Instance:" + remoteInst);
                    remoteWsRef = remoteInst;
                    remoteList.put(remoteClassKey, remoteInst);//store away
                    System.out.println("Added key:" + remoteClassKey + " value:" + remoteInst);
                    //now cache remote for later user with key.
                } else { //retrieve from cache
                    remoteWsRef = remoteList.get(remoteClassKey);
                }

                //Now make call to the method passed in
                //so need to translate rhq-signature into static jaxb type signature objects
                Class<?>[] statSignature = new Class<?>[nbi.getSignature().length];
                int index = 0;

                for (String type : nbi.getSignature()) {
                    System.out.println("Original signature element:" + type + ":");
                    //assume type names are unique and only need terminal string. 
                    //Ex. org.rhq.core.domain.auth.Subject -> org.rhq.enterprise.server.ws.Subject 
                    StringTokenizer bag = new StringTokenizer(type, ".");
                    System.out.println("Bag:" + bag + ":size:" + bag.countTokens());
                    String terminal = "";
                    while (bag.hasMoreTokens()) {
                        terminal = bag.nextToken();
                    }
                    System.out.println("terminal bagToken:" + terminal + ":");
                    //pull existing type information and use if it's non-RHQ or jaxb type
                    Class<?> newSignatureInstance = getClass(type);
                    Class<?> locatedWsClass = null;
                    if (!WS_MAP_CLASSES.containsKey(terminal)) {//check for signature elements with mappings
                        System.out.println("Map Key '" + terminal + "' is not located");
                        //Check for class in ws.* package and if it exists then return it and store it.
                        try {
                            locatedWsClass = Class.forName("org.rhq.enterprise.server.ws." + terminal);
                            //overwrite with one from WS class
                            newSignatureInstance = locatedWsClass;
                        } catch (ClassNotFoundException cnfe) {
                            //do nothing
                        }
                        System.out.println("Located Class is :" + locatedWsClass + ":");
                        if (locatedWsClass != null) {
                            WS_MAP_CLASSES.put(terminal, locatedWsClass);
                        }

                    } else {
                        newSignatureInstance = WS_MAP_CLASSES.get(terminal);
                    }
                    statSignature[index++] = newSignatureInstance;
                }
                System.out.println("Static signature Map:" + statSignature);
                for (int kl = 0; kl < statSignature.length; kl++) {
                    System.out.println("Signature :" + statSignature[kl] + ":");
                }

                //TODO: SP not sure if need to non rhq/jaxb types in here .. Ex. java.util.Set -> java.util.List. Revisit if signature mismatch occurs
                //look up method
                System.out.println("Meth lookup for:" + methodInfo[1] + ":" + statSignature + ":");
                Method methodCall = remoteWsRef.getClass().getMethod(methodInfo[1], statSignature);
                System.out.println("Successfully found Method call:" + methodCall);

                //translate param values content: 
                Object[] values = new Object[nbi.getParameters().length];
                //iterate over params and translate/copy if neccesary
                for (int k = 0; k < nbi.getParameters().length; k++) {
                    Object passedIn = nbi.getParameters()[k];
                    System.out.println("Object passed in :" + passedIn + ":class:" + passedIn.getClass());
                    Object translated = copyValue(passedIn);
                    System.out.println("Object translated:" + translated + ":class:" + translated.getClass());
                    //now pass in the new values
                    values[k] = translated;
                }
                //finally call the jaxb static ws method with parameters
                Object wsResult = methodCall.invoke(remoteWsRef, values);
                System.out.println("Type Returned is:" + wsResult + ":class:" + wsResult.getClass());
                System.out.println("OrigType Returned is:" + result + ":class:" + result.getClass());

                //Now translate back and override return type appropriately
                Object updated = updateRhqType(wsResult, result);
                System.out.println("Updated type returned:" + updated);
                //now over write the result to be returned
                result = updated;
                successful = true;

            } catch (Exception ex) {
                //TODO: SP wrap these exception in correct type for jbossremoting? 
                System.out.println("Exception [" + methodInfo[0] + "Service]" + " is:" + ex);
                throw new InvocationTargetException(ex);
            }

        } catch (InvocationTargetException e) {
            log.error("WS-Failed to invoke remote request", e);
            return e.getTargetException();
        } catch (Exception e) {
            log.error("WS-Failed to invoke remote request", e);
            return e;
        } finally {
            if (result != null) {
                // set the strategy guiding how the return information is serialized
                ExternalizableStrategy.setStrategy(ExternalizableStrategy.Subsystem.REFLECTIVE_SERIALIZATION);

                // scrub the return data if Hibernate proxies
                try {
                    HibernateDetachUtility.nullOutUninitializedFields(result,
                        HibernateDetachUtility.SerializationType.SERIALIZATION);
                } catch (Exception e) {
                    log.error("Failed to null out uninitialized fields", e);
                    //                    this.metrics.addData(methodName, System.currentTimeMillis() - time, false);
                    return e;
                }
            }

            // want to calculate this after the hibernate util so we take that into account too
            long executionTime = System.currentTimeMillis() - time;
            //            this.metrics.addData(methodName, executionTime, successful);
            if (log.isDebugEnabled()) {
                log.debug("Remote request [" + methodName + "] execution time (ms): " + executionTime);
            }
        }

        return result;
    }

    private Object copyValue(Object passedIn) throws ClassNotFoundException, SecurityException, NoSuchMethodException,
        IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        System.out.println("In copyValue the translation method:");
        Object translated = passedIn; //start out with untranslated .. may not need to.

        //check for passed in class type
        String type = passedIn.getClass().getName();
        System.out.println("Type passed in is:" + type + ":");
        StringTokenizer bag = new StringTokenizer(type, ".");
        System.out.println("Bag:" + bag + ":size:" + bag.countTokens());
        String terminal = "";
        while (bag.hasMoreTokens()) {
            terminal = bag.nextToken();
        }

        System.out.println("Terminal value is :" + terminal);
        System.out.println("Does map contain key:" + WS_MAP_CLASSES.containsKey(terminal) + ":");
        //check to see if parameter passed needs JAXB translation
        if (WS_MAP_CLASSES.containsKey(terminal)) {
            //create the comparable jaxb type but need objectFactory
            if (!WS_MAP_CLASSES.containsKey("objectFactory")) {
                // Ex.         objFactory = new ObjectFactory();
                if (objFactory == null) {
                    //Get Object from classpath
                    Class<?> ofRef = Class.forName("org.rhq.enterprise.server.ws.ObjectFactory");
                    System.out.println("Located object factory class:" + ofRef + ":");
                    Constructor<?> ofConstructor = ofRef.getConstructor(null);
                    // Ex.                   objFactory = (Class) ofConstructor.newInstance(null);
                    Object retValue = ofConstructor.newInstance(null);
                    System.out.println("ObjectFactory instantiated:" + retValue + ":");
                    objFactory = retValue;
                }
            }

            //Ex.
            //            if (terminal.equals(Subject.class.getSimpleName())) {
            //                org.rhq.enterprise.server.ws.Subject to = objFactory.createSubject();
            //                Subject from = (Subject) passedIn;
            //                //manually copy... ugh!
            //                to.setDepartment(from.getDepartment());
            //                to.setEmailAddress(from.getEmailAddress());
            //                to.setFactive(from.getFactive());
            //                to.setFirstName(from.getFirstName());
            //                to.setFsystem(from.getFsystem());
            //                to.setId(from.getId());
            //                to.setLastName(from.getLastName());
            //                to.setName(from.getName());
            //                to.setPhoneNumber(from.getPhoneNumber());
            //                to.setSessionId(from.getSessionId());
            //                to.setSmsAddress(from.getSmsAddress());
            //                //                to.setUserConfiguration(from.getUserConfiguration());
            //                translated = to;
            //            }

            //create Type
            //locate Method to create type Ex. org.rhq.enterprise.server.ws.ObjectFactory.createSubject()
            Method mJxbType = objFactory.getClass().getMethod("create" + terminal, null);
            System.out.println("Located CreateMethod for type passed in:" + mJxbType + ":");
            //instantiate jaxb type
            Object jaxbType = mJxbType.invoke(objFactory, null);
            System.out.println("Jaxb Type created:" + jaxbType + ":");

            //get all 'set' methods for jaxbType. Seems to be sufficient TODO: verify assumption here for generated types.
            Method[] jxbTypeMethList = jaxbType.getClass().getDeclaredMethods();
            //retrieve rhqTypeMethods
            Method[] rhqTypeMethList = passedIn.getClass().getDeclaredMethods();
            //            HashMap<String, Method> getRhqMethmap = createReferenceMap(rhqTypeMethList, "get");
            HashMap<String, Method> getRhqMethmap = createReferenceMap(rhqTypeMethList, "");

            //iterate over rhqType get methods calling with values
            for (int m = 0; m < jxbTypeMethList.length; m++) {
                //only work on 'set'
                String meth = jxbTypeMethList[m].getName();
                //TODO: put in special handling for non-trivial types. 
                if (meth.startsWith("set")) {
                    System.out.println("Working on method:" + meth + ":obj:" + jxbTypeMethList[m] + ":"
                        + meth.substring(3) + ":");
                    //lookup get method on rhqType
                    Method getMethod = getRhqMethmap.get("get" + meth.substring(3));
                    System.out.println("Just located coreMeth:" + getMethod + ":");
                    if (getMethod == null) {//non set method detected
                        //look for 'is' prefixed getters as well
                        getMethod = getRhqMethmap.get("is" + meth.substring(3));
                        System.out.println("2nd Attempt: Looked up coreMeth for rhq:" + getMethod + ": meth:"
                            + meth.substring(3) + ":F:" + meth);
                        //if still null check 'fetch'
                        if (getMethod == null) {
                            //look for 'fetch' prefixed getters as well
                            getMethod = getRhqMethmap.get("fetch" + meth.substring(3));
                            System.out.println("3rd Attempt: Looked up coreMeth for rhq:" + getMethod + ": meth:"
                                + meth.substring(3) + ":F:" + meth);
                        }
                    }

                    System.out.println("ABOUT to RUN with in/out:" + jxbTypeMethList[m].getParameterTypes()[0]
                        + " # with input " + getMethod.getReturnType());
                    Class<?> inParam = jxbTypeMethList[m].getParameterTypes()[0];
                    Class<?> outParam = getMethod.getReturnType();
                    if (inParam.getCanonicalName().equals(outParam.getCanonicalName())) {
                        System.out.println("In/out parms equal.jx:" + jaxbType + " ## passedIn:" + passedIn);
                        //wrap get in set, aka... do the transfer
                        jxbTypeMethList[m].invoke(jaxbType, getMethod.invoke(passedIn, null));
                    } else {//deal with the mismatch
                        System.out.println("@@@@@ copyVal in/out NOT MATCHING @@@@@@@@@@@");
                        System.out.println("inP:" + inParam.getCanonicalName());
                        System.out.println("outParam:" + outParam.getCanonicalName());
                        //get return value out
                        Object returnValue = getMethod.invoke(passedIn, null);
                        if (returnValue == null) {
                            System.out.println("NULL VALUE DETECTED .. bailing .. not transfer necessary.");
                        } else {
                            //create input type it's supposed to be ex. org.rhq.enterprise.server.ws.InventoryStatus
                            String terminalString = locateTerminalString(inParam.getCanonicalName());
                            System.out.println("Term Str:" + terminalString + " # meth: " + "create" + terminalString);
                            //CHECK for ENUM .. different approach.
                            if (inParam.getEnumConstants() == null) {
                                Method obFacMethod = objFactory.getClass().getMethod("create" + terminalString, null);
                                System.out.println("Located method:" + obFacMethod);
                                //TODO: need to finish this impl for non-enum or non-trivial types. See below.
                                System.out.println("UNFINISHED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                            } else {
                                System.out.println("LOCATED an ENUM:... processing");
                                //get instance of the passed in type .. aka returned value
                                Object rtype = getMethod.invoke(passedIn, null);
                                System.out.println("Input value evaluated:" + rtype + ":running :" + getMethod);
                                //get jaxbtype then do jaxbtype.valueOf(rtype);
                                Method valueOfMethod = inParam.getMethod("valueOf", String.class);
                                String value = rtype.toString();
                                //                                Method valueMethod = rtype.getClass().getMethod("value", null);
                                System.out.println("RETRIEVED ENUM methods:" + valueOfMethod + "##"
                                    + rtype.getClass().getCanonicalName() + "ValueAsString:" + value);
                                //assign to jaxbType
                                //                                jaxbType = valueOfMethod.invoke(inParam, valueOfMethod.invoke(rtype, String.class));
                                //                                jaxbType = valueOfMethod.invoke(inParam, valueOfMethod.invoke(rtype, value));
                                jaxbType = valueOfMethod.invoke(inParam, value);
                                System.out.println("Completed assignment...should be good:" + jaxbType);
                            }
                        }
                        //                        Method obFacMethod = objFactory.getClass().getMethod("create" + terminalString, null);
                        //                        System.out.println("Located method:" + obFacMethod);
                        //                        Object jaxbInst = obFacMethod.invoke(objFactory, null);
                        //                        System.out.println("Instantiated jaxbType:" + jaxbInst);
                        //                        //TODO: now copy over .. recurse?
                        //                        Object updated = updateRhqType(returnValue, jaxbInst);
                        //                        jxbTypeMethList[m].invoke(jaxbType, updated);
                    }

                    //                    //wrap get in set, aka... do the transfer
                    //                    jxbTypeMethList[m].invoke(jaxbType, getMethod.invoke(passedIn, null));

                    //                    //split out the return type for easier type checking
                    //                    Object input = getMethod.invoke(passedIn, null);
                    //                    //now check to make sure types(input/output) are compatible
                    //                    if (methodOutputInputSame(jxbTypeMethList[m].getParameterTypes(), getMethod.getDeclaringClass())) {
                    //                        System.out.println("$$$$$$$$$ COPYVALUE TYPE mismatch detected $$$$$$$$$$$$");
                    //                        //therefor operate on input value to fix. recursive?
                    //                    }
                    //                    jxbTypeMethList[m].invoke(jaxbType, input);
                }
            }
            translated = jaxbType;
        }
        System.out.println("Exiting translation method.");
        return translated;
    }

    private String locateTerminalString(String canonicalName) {
        String terminal = "";
        if ((canonicalName != null) && (canonicalName.trim().length() > 0)) {
            StringTokenizer bag = new StringTokenizer(canonicalName, ".");
            while (bag.hasMoreTokens()) {
                terminal = bag.nextToken();
            }
        }
        return terminal;
    }

    private Object updateRhqType(Object jaxbType, Object rhqType) throws ClassNotFoundException, SecurityException,
        NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException,
        InvocationTargetException {

        System.out.println("In updateRhqType the translation method:");
        System.out.println("JAXBTYPE:" + jaxbType + ":class:" + jaxbType.getClass() + ":canon:"
            + jaxbType.getClass().getCanonicalName());
        System.out.println("RHQTYPE:" + rhqType + ":class:" + rhqType.getClass() + ":canon:"
            + rhqType.getClass().getCanonicalName());
        Object updated = rhqType;

        System.out.println("Jaxb Type passed in:" + jaxbType + ":");
        //get all 'get' methods for jaxbType: TODO: add 'is" to this list for boolean values.
        Method[] jaxbTypeMethList = jaxbType.getClass().getDeclaredMethods();
        //retrieve rhqTypeMethod
        Method[] rhqTypeMethList = rhqType.getClass().getDeclaredMethods();
        //        //make sure method list is coming from right class level
        //        if (jaxbType.getClass().getCanonicalName().indexOf("java.lang.Class") > -1) {
        //            jaxbTypeMethList = ((Class) (jaxbType)).getDeclaredMethods();
        //            rhqTypeMethList = ((Class) (rhqType)).getDeclaredMethods();
        //        }

        //TODO: put in the correct filters 'get'set is, etc...
        HashMap<String, Method> rMap = createReferenceMap(rhqTypeMethList, "");
        HashMap<String, Method> map = createReferenceMap(jaxbTypeMethList, "");
        System.out.println("Ref r-map created:" + rMap + ":");
        System.out.println("Ref j-map created:" + map + ":");

        for (Method method : jaxbTypeMethList) {
            //only work on methods that have a 'get' in jaxbtype
            String meth = method.getName();
            //now look up Rhq set method
            Method rhqSetMethod = rMap.get("set" + meth.substring(3));
            System.out.println("Looked up set method for rhq:" + rhqSetMethod + ": meth:" + meth.substring(3) + ":F:"
                + meth);
            //TODO: what about other method names
            if (rhqSetMethod == null) {//non set method detected
                //look for 'is' prefixed getters as well
                rhqSetMethod = rMap.get("set" + meth.substring(2));
                System.out.println("2nd Attempt: Looked up set method for rhq:" + rhqSetMethod + ": meth:"
                    + meth.substring(2) + ":F:" + meth);
            }

            //TODO: put in special handling for non-trivial types. 
            //            if (meth.startsWith("get")) {
            if (meth.startsWith("get") || meth.startsWith("is")) {
                System.out.println("Working on j-get-method:" + meth + ":obj:" + method + ":" + meth.substring(3)
                    + ":f:" + meth);
                System.out.println("Working on r-set-method:" + meth + ":obj:" + rhqSetMethod + ":" + meth.substring(3)
                    + ":f:" + meth);
                //wrap get in set

                //                rhqSetMethod.invoke(rhqType, method.invoke(jaxbType, null));
                // insert check for parameter type mismatch
                if (methodOutputInputSame(rhqSetMethod.getParameterTypes(), method.getReturnType())) {
                    rhqSetMethod.invoke(rhqType, method.invoke(jaxbType, null));
                } else {// extract param and translate then invoke 
                    System.out.println("$$$$$$$$$$$ PROBLEM@@@ type mismatch ");
                    String from = method.getReturnType().getCanonicalName();
                    String to = rhqSetMethod.getParameterTypes()[0].getCanonicalName();

                    //copy data over
                    System.out.println("moving:" + method.getReturnType() + ": data to "
                        + rhqSetMethod.getParameterTypes()[0]);
                    if (from.equals("java.util.List") && to.equals("java.util.Set")) {//This is common enough case just hard code.
                        Set input = new HashSet();
                        //retrieve return type data as object
                        Object jxbReturnValue = method.invoke(jaxbType, null);
                        //copy all data over as target type
                        input.addAll((Collection) jxbReturnValue);
                        //now make the call with translated type
                        rhqSetMethod.invoke(rhqType, input);
                        System.out.println("Completed List to Set translation.");
                    } else {
                        System.out.println("Assuming a JAXB -> RHQ copy needs to occur");
                        //recursive call to method to handle non-trivial types...
                        //                        Object updatedRhqValue = updateRhqType(method.getReturnType(),
                        //                            rhqSetMethod.getParameterTypes()[0]);
                        System.out.println("JAXBtype:" + jaxbType + ":method:" + meth + ":");
                        Object jxbNonTrivType = method.invoke(jaxbType, null);
                        System.out.println("Retrieved non-trivial jxb type:" + jxbNonTrivType + ":");
                        if (jxbNonTrivType != null) {
                            //Now get instance of type from RHQtype .. otherwise transfer not possible
                            //locate method to instantiate/retrieve it
                            Method rhqNonTrivTypeMethod = rMap.get("get" + rhqSetMethod.getName().substring(3));
                            System.out.println("Retrieved Meth for non-triv type:" + rhqNonTrivTypeMethod + ":");
                            Object rhqNonTrivTypeInst = rhqNonTrivTypeMethod.invoke(rhqType, null);
                            //now recurse ...
                            Object updatedRhqValue = updateRhqType(jxbNonTrivType, rhqNonTrivTypeInst);
                            //now make the call with translated type
                            rhqSetMethod.invoke(rhqType, updatedRhqValue);
                            System.out.println("Completed RHQ type update.");
                        } else {
                            System.out.println("NULL value detected. Not copying result of method:" + method);
                        }
                    }

                    //                  Object[] translated = translateNonTrivialTypeToObjectArray(method.invoke(jaxbType,null),); 
                }
            }
        }

        updated = rhqType;
        //    }
        System.out.println("Exiting update method.");
        return updated;
    }

    private boolean methodOutputInputSame(Class<?>[] rhqParamTypeList, Class<?> paramTypeList) {
        boolean paramsEqual = true;
        //iterate over parameters in order and
        if (rhqParamTypeList.length > 1) {
            System.out.println("##### parameter count mismatch!!! ATTENTION REQUIRED.");
            return false;
        }
        for (int i = 0; i < rhqParamTypeList.length; i++) {
            //            System.out.println("PARAM-R-" + i + ":" + rhqParamTypeList[i].getCanonicalName());
            //            System.out.println("PARAM-J-" + i + ":" + paramTypeList.getCanonicalName());
            if (!rhqParamTypeList[i].getCanonicalName().equals(paramTypeList.getCanonicalName())) {
                paramsEqual = false;
                System.out.println("###########Mismatch detected####################");
                System.out.println("PARAM-R-" + i + ":" + rhqParamTypeList[i].getCanonicalName());
                System.out.println("PARAM-J-" + i + ":" + paramTypeList.getCanonicalName());
            }
        }
        return paramsEqual;
    }

    //TODO: modify prefix to be a list of comma separated filter prefixes
    private HashMap<String, Method> createReferenceMap(Method[] rhqTypeMethList, String prefix) {
        HashMap<String, Method> map = new HashMap<String, Method>();
        for (int i = 0; i < rhqTypeMethList.length; i++) {
            if ((prefix != null) && (prefix.trim().length() > 0)) {
                if (rhqTypeMethList[i].getName().startsWith(prefix)) {
                    map.put(rhqTypeMethList[i].getName(), rhqTypeMethList[i]);
                }
            } else { //else add all methods
                map.put(rhqTypeMethList[i].getName(), rhqTypeMethList[i]);
            }
        }
        return map;
    }

    Class<?> getClass(String name) throws ClassNotFoundException {
        // TODO GH: Doesn't support arrays
        if (PRIMITIVE_CLASSES.containsKey(name)) {
            return PRIMITIVE_CLASSES.get(name);
        } else {
            return Class.forName(name);
        }
    }

    /**Dynamically builds the WSDL URL to connect to a remote server.
    *
    * @param remote class correctly annotated with Webservice reference.
    * @return valid URL
    * @throws MalformedURLException
    */
    public static URL generateRemoteWebserviceURL(Class remote, String host, int port, boolean useHttps)
        throws MalformedURLException {

        URL wsdlLocation = null;
        //TODO: what to do about exceptions/messaging? throw illegalArgs?
        //insert checks for host, port
        if ((host == null) || (host.trim().length() == 0) || (port <= 0)) {
            return wsdlLocation;
        }

        //check for reference for right annotations
        if ((remote != null) && remote.isAnnotationPresent(WebServiceClient.class)) {
            String beanName = remote.getSimpleName();
            String protocol = "https://";
            if (!useHttps) {
                protocol = "http://";
            }
            wsdlLocation = new URL(protocol + host + ":" + port + "/rhq-rhq-enterprise-server-ejb3/"
                + beanName.substring(0, beanName.length() - "Service".length()) + "?wsdl");
        }
        return wsdlLocation;

    }

    public static QName generateRemoteWebserviceQName(Class remote) {

        QName generated = null;
        //check for reference with right annotation
        if ((remote != null) && (remote.isAnnotationPresent(WebServiceClient.class))) {
            String annotatedQnameValue = "";
            Annotation annot = remote.getAnnotation(WebServiceClient.class);
            WebServiceClient annotated = (WebServiceClient) annot;
            annotatedQnameValue = annotated.targetNamespace();
            String beanName = remote.getSimpleName();

            generated = new QName(annotatedQnameValue, beanName);
        }
        return generated;

    }

    public void removeListener(InvokerCallbackHandler arg0) {
    }

    public void setInvoker(ServerInvoker arg0) {
    }

    public void setMBeanServer(MBeanServer arg0) {
    }

}
