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

package org.wso2.healthcare.integration.v2tofhir.data;

import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;
import org.wso2.healthcare.integration.v2tofhir.MappingType;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Data holder class to hold concept map information extracted from mapping files.
 */
public class ConceptMap {

    private org.hl7.fhir.r4.model.ConceptMap conceptMap;
    private MappingType type;
    private String targetResource;
    private String targetDataType;
    private Map<Integer, TargetMapping> targetMappingMap;

    public ConceptMap(MappingType type, org.hl7.fhir.r4.model.ConceptMap conceptMap) {
        this.conceptMap = conceptMap;
        this.type = type;
        targetMappingMap = new HashMap<>();
        initialize();
    }

    public String getTargetResource() {
        return targetResource;
    }

    public String getTargetDataType() {
        return targetDataType;
    }

    public Map<Integer, TargetMapping> getTargetMappingMap() {
        return targetMappingMap;
    }

    private void initialize() {
        String sourceUri = null;
        if (conceptMap != null && conceptMap.hasTarget()) {
            if (type.equals(MappingType.SEGMENT)) {
                Type target = conceptMap.getTarget();
                this.targetResource = ((UriType) target).getValue();
            } else if (type.equals(MappingType.DATATYPE)) {
                Type target = conceptMap.getTarget();
                this.targetDataType = ((UriType) target).getValue();
            }
            sourceUri = ((UriType)conceptMap.getSource()).getValue();
        }
        int sortOrder = 0;
        if (conceptMap != null && conceptMap.hasGroup()) {
            for (org.hl7.fhir.r4.model.ConceptMap.ConceptMapGroupComponent group : conceptMap.getGroup()) {
                for (org.hl7.fhir.r4.model.ConceptMap.SourceElementComponent element : group.getElement()) {
                    TargetMapping targetMapping = new TargetMapping();
                    targetMappingMap.putIfAbsent(sortOrder, targetMapping);
                    if (sourceUri != null) {
                        String elementName = element.getCode();
                        if (elementName.contains("-")) {
                            elementName = elementName.replace("-", ".");
                        }
                        targetMapping.setV2Xpath("//" + sourceUri + "/" + elementName + "/text()");
                    }
                    element.getTarget().forEach(target -> {
                        String fhirpath = target.getCode();
                        // Skip extensions
                        // TODO: support extensions
                        if (fhirpath.contains("extension")) {
                            return;
                        }
                        // Regex to identify if a fhirpath starts with an index
                        String regex = "^\\[\\d+\\]\\.";
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(fhirpath);
                        if (matcher.find()) {
                            targetMapping.setFhirpath(fhirpath.substring(0, matcher.end()));
                        } else {
                            targetMapping.setFhirpath(fhirpath);
                        }
                        target.getExtension().forEach(extension -> {
                            if ("http://hl7.org/fhir/uv/v2mappings/StructureDefinition/TypeInfo".equals(extension.getUrl())) {
                                if (extension.getExtensionByUrl("type") != null &&
                                        extension.getExtensionByUrl("type").getValue() != null) {
                                    Type targetDataType = extension.getExtensionByUrl("type").getValue();
                                    this.targetDataType = targetDataType.toString();
                                    Extension cardinalityMinExt = extension.getExtensionByUrl("cardinalityMin");
                                    Extension cardinalityMaxExt = extension.getExtensionByUrl("cardinalityMax");
                                    if (cardinalityMinExt != null) {
                                        targetMapping.setCardinalityMin(((IntegerType) cardinalityMinExt.getValue()).getValue());
                                    }
                                    if (cardinalityMaxExt != null) {
                                        targetMapping.setCardinalityMax(((IntegerType) cardinalityMaxExt.getValue()).getValue());
                                    }
                                }
                            }
                        });
                        if (target.hasDependsOn()) {
                            target.getDependsOn().forEach(dependsOn -> {
                                if (dependsOn.getProperty().equals("data-type-map")) {
                                    targetMapping.setDatatypeMap(dependsOn.getValue());
                                    if (targetMapping.getV2Xpath().endsWith("/text()")) {
                                        targetMapping.setV2Xpath(targetMapping.getV2Xpath().replace("/text()", ""));
                                    }
                                }
                            });
                        }
                    });
                    sortOrder++;
                }
            }
        }
    }
}
