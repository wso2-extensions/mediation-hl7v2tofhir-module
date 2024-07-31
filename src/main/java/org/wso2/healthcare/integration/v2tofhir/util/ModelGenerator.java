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

package org.wso2.healthcare.integration.v2tofhir.util;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Property;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Type;
import org.wso2.healthcare.integration.fhir.FHIRConnectException;
import org.wso2.healthcare.integration.fhir.config.FHIRConnectorContext;
import org.wso2.healthcare.integration.fhir.model.HolderFHIRResource;
import org.wso2.healthcare.integration.fhir.utils.FHIRDataTypeUtils;
import org.wso2.healthcare.integration.v2tofhir.V2ToFhirException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Utility class related to FHIR resource model generation.
 */
public class ModelGenerator {

    private static final List<String> primitiveDataTypes = Arrays
            .asList("boolean", "integer", "string", "decimal", "uri", "url", "canonical", "base64Binary", "instant",
                    "date", "dateTime", "time", "code", "oid", "id", "markdown", "unsignedInt", "positiveInt", "uuid");

    public static IBaseResource createResource(String type) throws V2ToFhirException {
        IBaseResource resource;
        try {
            Class<?> resourceClass = Class.forName("org.hl7.fhir.r4.model." + type);
            resource = (IBaseResource) resourceClass.getConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new V2ToFhirException(e, "Error occurred while creating fhir model.");
        }
        return resource;
    }

    public static void saveFhirResourceToContext(String type, Resource fhirResource,
            org.wso2.healthcare.integration.fhir.model.Resource targetResource) throws V2ToFhirException {
        if (targetResource instanceof HolderFHIRResource) {
            ((HolderFHIRResource) targetResource).setHolderResource(fhirResource);
            return;
        }
        try {
            Method fhirSetResourceMethod = targetResource.getClass()
                    .getDeclaredMethod("setFhir" + type, fhirResource.getClass());
            fhirSetResourceMethod.invoke(targetResource, fhirResource);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new V2ToFhirException(e, "Error occurred while setting generated fhir resource to target resource");
        }
    }

    public static boolean isPrimitiveDataType(String dataType) {
        return primitiveDataTypes.contains(dataType);
    }

    public static Type createDataType(String type, Map<String, String> configuredParams,
            FHIRConnectorContext fhirConnectorContext) throws FHIRConnectException {
        String prefix = "value";
        switch (type) {
        case "Coding":
            return FHIRDataTypeUtils.getCoding("", configuredParams, fhirConnectorContext);
        case "CodeableConcept":
            String code = configuredParams.get("code");
            String valueSet = configuredParams.get("valueSet");
            if (code != null && valueSet != null) {
                // populating the code and valueSet that will be read by the FHIRDataTypeUtils.getCoding
                configuredParams.put("coding.code", code);
                configuredParams.put("coding.valueSet", valueSet);
            }
            return FHIRDataTypeUtils.getCodeableConcept("",configuredParams, fhirConnectorContext);
        case "Identifier":
            return FHIRDataTypeUtils.getIdentifier("",configuredParams, fhirConnectorContext);
        case "HumanName":
            return FHIRDataTypeUtils.getHumanName("",configuredParams, fhirConnectorContext);
        case "Address":
            return FHIRDataTypeUtils.getAddress("",configuredParams, fhirConnectorContext);
        case "ContactPoint":
            return FHIRDataTypeUtils.getContactPoint("",configuredParams, fhirConnectorContext);
        case "Attachment":
            return FHIRDataTypeUtils.getAttachment("",configuredParams, fhirConnectorContext);
        case "Period":
            return FHIRDataTypeUtils.getPeriod("",configuredParams, fhirConnectorContext);
        case "Quantity":
            return FHIRDataTypeUtils.getQuantity("",configuredParams, fhirConnectorContext);
        case "SimpleQuantity":
            return FHIRDataTypeUtils.getSimpleQuantity("",configuredParams, fhirConnectorContext);
        case "Range":
            return FHIRDataTypeUtils.getRange("",configuredParams, fhirConnectorContext);
        case "Ratio":
            return FHIRDataTypeUtils.getRatio("",configuredParams, fhirConnectorContext);
        case "Timing":
            return FHIRDataTypeUtils.getTiming("",configuredParams, fhirConnectorContext);
        case "Money":
            return FHIRDataTypeUtils.getMoney("",configuredParams, fhirConnectorContext);
        case "Signature":
            return FHIRDataTypeUtils.getSignature("",configuredParams, fhirConnectorContext);
        case "Annotation":
            return FHIRDataTypeUtils.getAnnotation("",configuredParams, fhirConnectorContext);
        case "SampledData":
            return FHIRDataTypeUtils.getSampledData("", configuredParams, fhirConnectorContext);
        case "Age":
            return FHIRDataTypeUtils.getAge("", configuredParams, fhirConnectorContext);
        case "Distance":
            return FHIRDataTypeUtils.getDistance("", configuredParams, fhirConnectorContext);
        case "Duration":
            return FHIRDataTypeUtils.getDuration("", configuredParams, fhirConnectorContext);
        case "Count":
            return FHIRDataTypeUtils.getCount("", configuredParams, fhirConnectorContext);
        case "MoneyQuantity":
            return FHIRDataTypeUtils.getMoneyQuantity("", configuredParams, fhirConnectorContext);
        case "ContactDetail":
            return FHIRDataTypeUtils.getContactDetail("", configuredParams, fhirConnectorContext);
        case "UsageContext":
            return FHIRDataTypeUtils.getUsageContext("", configuredParams, fhirConnectorContext);
        case "RelatedArtifact":
            return FHIRDataTypeUtils.getRelatedArtifact("", configuredParams, fhirConnectorContext);
        case "Contributor":
            return FHIRDataTypeUtils.getContributor("", configuredParams, fhirConnectorContext);
        case "DataRequirement":
            return FHIRDataTypeUtils.getDataRequirement("", configuredParams, fhirConnectorContext);
        case "ParameterDefinition":
            return FHIRDataTypeUtils.getParameterDefinition("", configuredParams, fhirConnectorContext);
        case "Expression":
            return FHIRDataTypeUtils.getExpression("", configuredParams, fhirConnectorContext);
        case "TriggerDefinition":
            return FHIRDataTypeUtils.getTriggerDefinition("", configuredParams, fhirConnectorContext);
        case "Reference":
            return FHIRDataTypeUtils.getReference("", configuredParams, fhirConnectorContext);
        case "Meta":
            return FHIRDataTypeUtils.getMeta("", configuredParams, fhirConnectorContext);
        case "Dosage":
            return FHIRDataTypeUtils.getDosage("", configuredParams, fhirConnectorContext);
        case "Narrative":
            return FHIRDataTypeUtils.getNarrative("", configuredParams, fhirConnectorContext);
        case "Extension":
            return FHIRDataTypeUtils.getExtension("", configuredParams, fhirConnectorContext);
        case "ElementDefinition":
            return FHIRDataTypeUtils.getElementDefinition("", configuredParams, fhirConnectorContext);
        case "boolean":
            return FHIRDataTypeUtils.getBooleanType(prefix, configuredParams, fhirConnectorContext);
        case "integer":
            return FHIRDataTypeUtils.getIntegerType(prefix, configuredParams, fhirConnectorContext);
        case "string":
            return FHIRDataTypeUtils.getStringType(prefix, configuredParams, fhirConnectorContext);
        case "decimal":
            return FHIRDataTypeUtils.getDecimalType(prefix, configuredParams, fhirConnectorContext);
        case "uri":
            return FHIRDataTypeUtils.getUriType(prefix, configuredParams, fhirConnectorContext);
        case "url":
            return FHIRDataTypeUtils.getUrlType(prefix, configuredParams, fhirConnectorContext);
        case "canonical":
            return FHIRDataTypeUtils.getCanonicalType(prefix, configuredParams, fhirConnectorContext);
        case "base64Binary":
            return FHIRDataTypeUtils.getBase64BinaryType(prefix, configuredParams, fhirConnectorContext);
        case "instant":
            return FHIRDataTypeUtils.getInstantType(prefix, configuredParams, fhirConnectorContext);
        case "date":
            String dateval = processDTMType(configuredParams.get(prefix));
            if (!dateval.isEmpty()) {
                configuredParams.put(prefix, processDTMType(configuredParams.get(prefix)));
            }
            return FHIRDataTypeUtils.getDateType(prefix, configuredParams, fhirConnectorContext);
        case "dateTime":
            String dateTimeVal = processDTMType(configuredParams.get(prefix));
            if (!dateTimeVal.isEmpty()) {
                configuredParams.put(prefix, processDTMType(configuredParams.get(prefix)));
            }
            return FHIRDataTypeUtils.getDateTimeType(prefix, configuredParams, fhirConnectorContext);
        case "time":
            return FHIRDataTypeUtils.getTimeType(prefix, configuredParams, fhirConnectorContext);
        case "code":
            return FHIRDataTypeUtils.getCodeType(prefix, configuredParams, fhirConnectorContext);
        case "oid":
            return FHIRDataTypeUtils.getOidType(prefix, configuredParams, fhirConnectorContext);
        case "id":
            return FHIRDataTypeUtils.getIdType(prefix, configuredParams, fhirConnectorContext);
        case "markdown":
            return FHIRDataTypeUtils.getMarkdownType(prefix, configuredParams, fhirConnectorContext);
        case "unsignedInt":
            return FHIRDataTypeUtils.getUnsignedIntType(prefix, configuredParams, fhirConnectorContext);
        case "positiveInt":
            return FHIRDataTypeUtils.getPositiveIntType(prefix, configuredParams, fhirConnectorContext);
        case "uuid":
            return FHIRDataTypeUtils.getUuidType(prefix, configuredParams, fhirConnectorContext);
        default:
            return null;
        }
    }

    public static void copyTypeAttributes(Type originalType, Type copiedType) {
        if (copiedType != null) {
            List<Property> childProperties = copiedType.children();
            for (Property childProperty : childProperties) {
                List<Base> values = childProperty.getValues();
                for (Base value : values) {
                    originalType.setProperty(childProperty.getName(), value);
                }
            }
        }
    }

    private static String processDTMType(String dtmStr) {
        if (dtmStr == null) {
            return "";
        }
        switch (dtmStr.length()) {
        case 4:
            return dtmStr;
        case 6:
            return dtmStr.substring(0, 3) + "-" + dtmStr.charAt(4);
        case 8:
            return dtmStr.substring(0,3) + "-" + dtmStr.charAt(4) + "-" + dtmStr.charAt(6);
        default:
            return "";
        }
    }
}
