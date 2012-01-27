package org.rhq.modules.plugins.jbossas7;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonNode;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenResources;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Deal with deployments
 * @author Heiko W. Rupp
 */
public class DeploymentComponent extends BaseComponent implements OperationFacet,  ContentFacet {

    private boolean verbose = ASConnection.verbose;

    @Override
    public AvailabilityType getAvailability() {
        Operation op = new ReadAttribute(getAddress(),"enabled");
        Result res = getASConnection().execute(op);
        if (!res.isSuccess())
            return AvailabilityType.DOWN;

        if (res.getResult()== null || !(Boolean)(res.getResult()))
            return AvailabilityType.DOWN;

        return AvailabilityType.UP;
    }

    @Override
    public OperationResult invokeOperation(String name,
                                           Configuration parameters) throws InterruptedException, Exception {

        String action;
        if (name.equals("enable")) {
            action = "deploy";
        } else if (name.equals("disable")) {
            action = "undeploy";
        } else {
            return super.invokeOperation(name, parameters);
        }

        Operation op = new Operation(action,getAddress());
        Result res = getASConnection().execute(op);
        OperationResult result = new OperationResult();
        if (res.isSuccess())
            result.setSimpleResult("Success");
        else
            result.setErrorMessage(res.getFailureDescription());

        return result;
    }


    @Override
    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        return new ArrayList<DeployPackageStep>();
    }

    @Override
    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages,
                                                 ContentServices contentServices) {

        log.info("Starting deployment..");
        DeployPackagesResponse response = new DeployPackagesResponse();

        if (packages.size()!=1) {
            response.setOverallRequestResult(ContentResponseResult.FAILURE);
            response.setOverallRequestErrorMessage("Can only deploy one package at a time");
            log.warn("deployPackages can only deploy one package at a time");
        }

        ResourcePackageDetails detail = packages.iterator().next();

        ASUploadConnection uploadConnection = new ASUploadConnection(getASConnection());
        OutputStream out = uploadConnection.getOutputStream(detail.getFileName());
        ResourceType resourceType = context.getResourceType();

        log.info("trying deployment of" + resourceType.getName() + ", key=" + detail.getKey() );

        contentServices.downloadPackageBits(context.getContentContext(),
                detail.getKey(), out, true);

        JsonNode uploadResult = uploadConnection.finishUpload();
        if (verbose)
            log.info(uploadResult);

        if (ASUploadConnection.isErrorReply(uploadResult)) {
            response.setOverallRequestResult(ContentResponseResult.FAILURE);
            response.setOverallRequestErrorMessage(ASUploadConnection.getFailureDescription(uploadResult));

            return response;
        }
        JsonNode resultNode = uploadResult.get("result");
        String hash = resultNode.get("BYTES_VALUE").getTextValue();


        CreateResourceReport report1 = new CreateResourceReport("", resourceType, new Configuration(),
                new Configuration(), detail);
        //CreateResourceReport report = runDeploymentMagicOnServer(report1,detail.getKey().getName(),hash, hash);

        try {
            redeployOnServer(detail.getKey().getName(), hash);
            response.setOverallRequestResult(ContentResponseResult.SUCCESS);
            DeployIndividualPackageResponse packageResponse = new DeployIndividualPackageResponse(detail.getKey(), ContentResponseResult.SUCCESS);
                    response.addPackageResponse(packageResponse);

        }
        catch (Exception e) {
            response.setOverallRequestResult(ContentResponseResult.FAILURE);
        }

        log.info(".. result is " + response);

        return response;
    }

    private void redeployOnServer(String name, String hash) throws Exception {

        Operation op = new Operation("full-replace-deployment", new Address());
        op.addAdditionalProperty("name",name);
        List<Object> content = new ArrayList<Object>(1);
        Map<String,Object> contentValues = new HashMap<String,Object>();
        contentValues.put("hash",new PROPERTY_VALUE("BYTES_VALUE",hash));
        content.add(contentValues);
        op.addAdditionalProperty("content",content);
        Result result = getASConnection().execute(op);
        if (result.isRolledBack())
            throw new Exception(result.getFailureDescription());
    }

    @Override
    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {

        Set<ResourcePackageDetails> details = new HashSet<ResourcePackageDetails>();

        // PackageType = "Deployment"
        Address address1 = getAddress().getParent();
        Operation op = new ReadChildrenResources(address1,"deployment");
        ComplexResult cres = getASConnection().executeComplex(op);

        if (!cres.isSuccess())
            return details;

        Map<String,Object> deployments = cres.getResult();
        for (String key : deployments.keySet()) {
            Map<String,Object> deployment = (Map<String, Object>) deployments.get(key);
            log.info("Discover package [" + key + "] for type [" + type + "]");

            List<Map> contentList = (List<Map>) deployment.get("content"); // deployments on SG or ManagedServer level have no hash
            Map<String,Map> hashMap = contentList.get(0);
            Map<String,String> bvMap = hashMap.get("hash");
            String content = bvMap.get("BYTES_VALUE");
            PackageDetailsKey pdKey = new PackageDetailsKey(key,
                    content, // no way to obtain the user defined version from the server
                    type.getName(),
                    "noarch"
            );
            ResourcePackageDetails detail = new ResourcePackageDetails(pdKey);
            detail.setSHA256(content);

            details.add(detail);
        }

        return details;
    }

    @Override
    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        return null;  // TODO: Customise this generated block
    }
}
