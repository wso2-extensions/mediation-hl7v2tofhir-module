/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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

package org.wso2.healthcare.integration.v2tofhir.operations;

import org.apache.axis2.AxisFault;
import org.apache.commons.lang3.StringUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.wso2.healthcare.integration.fhir.FHIRConnectException;
import org.wso2.healthcare.integration.fhir.FHIRConnectorBase;
import org.wso2.healthcare.integration.fhir.config.FHIRConnectorContext;
import org.wso2.healthcare.integration.fhir.model.Bundle;
import org.wso2.healthcare.integration.fhir.model.HolderFHIRResource;
import org.wso2.healthcare.integration.fhir.template.util.MsgCtxUtil;
import org.wso2.healthcare.integration.v2tofhir.V2SpecParser;
import org.wso2.healthcare.integration.v2tofhir.V2ToFhirException;
import org.wso2.healthcare.integration.v2tofhir.resources.Resource;
import org.wso2.healthcare.integration.v2tofhir.util.ModelGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class is responsible for creating FHIR resources(given) from HL7V2 message.
 */
public class CreateResources extends FHIRConnectorBase {
    public CreateResources() {
        V2SpecParser.init();
    }

    @Override
    protected void execute(MessageContext messageContext, FHIRConnectorContext fhirConnectorContext,
            HashMap<String, String> configuredParams) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Starting execution of CreateResources operation.");
            }
            String resourceTypes = configuredParams.get("resourceTypes");
            if (StringUtils.isEmpty(resourceTypes)) {
                Set<String> resourceNames = V2SpecParser.getResourcesMap().keySet();
                if (resourceNames.isEmpty()) {
                    throw new V2ToFhirException("No resources found in the loaded V2 to FHIR spec.");
                }
                resourceTypes = "";
                for (String resourceName : resourceNames) {
                    resourceTypes = resourceTypes.concat(resourceName + ",");
                }
                resourceTypes = resourceTypes.substring(0, resourceTypes.length() - 1);
            }
            String[] resourceTypeArr = resourceTypes.split(",");
            //setting up bundle resource
            Bundle bundle = new Bundle(null, configuredParams,
                    fhirConnectorContext);
            bundle.setFhirBundle(new org.hl7.fhir.r4.model.Bundle());
            bundle.getFhirBundle().setType(org.hl7.fhir.r4.model.Bundle.BundleType.MESSAGE);
            MsgCtxUtil.removeNamespacesFromXmlMessage(messageContext);
            for (String resourceType : resourceTypeArr) {
                Map<String, String> resourceParams = new HashMap<>();

                Resource convertedFhirResource = V2SpecParser.getResource(resourceType);
                //TODO: need to generate id for the resource based on the segment mappings
                org.wso2.healthcare.integration.fhir.model.Resource resourceWSO2Model = new HolderFHIRResource("", new HashMap<>());
                fhirConnectorContext.createResource(resourceWSO2Model);
                if (fhirConnectorContext.getContainerResource() == null) {
                    fhirConnectorContext.createResource(bundle);
                }
                if (convertedFhirResource == null) {
                    throw new V2ToFhirException("provided resource type:" + resourceType +" is not supported.");
                }
                org.hl7.fhir.r4.model.Resource updatedResource = convertedFhirResource
                        .createAndUpdateResource(messageContext);
                ModelGenerator
                        .saveFhirResourceToContext(StringUtils.capitalize(resourceType), updatedResource, resourceWSO2Model);
                if (updatedResource.isEmpty()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Resource: " + updatedResource.fhirType() + " is empty. Skipping adding to the bundle.");
                    }
                    continue;
                }
                org.hl7.fhir.r4.model.Bundle.BundleEntryComponent bundleEntry = new org.hl7.fhir.r4.model.Bundle.BundleEntryComponent();
                if (log.isDebugEnabled()) {
                    log.debug("Adding resource to the bundle:" + updatedResource.fhirType());
                }
                bundleEntry.setProperty("resource", updatedResource);
                bundle.getFhirBundle().addEntry(bundleEntry);
                JsonUtil.getNewJsonPayload(((Axis2MessageContext) messageContext).getAxis2MessageContext(), bundle.serializeToJSON(), true, true);
            }
        } catch (FHIRConnectException | V2ToFhirException | AxisFault e) {
            handleException("Error occurred while creating resources from hl7v2 message. ", e, messageContext);
        }
    }

    @Override
    public String getOperationName() {
        return "createResources";
    }
}
