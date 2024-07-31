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

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.synapse.MessageContext;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Property;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Type;
import org.wso2.healthcare.integration.fhir.FHIRConnectException;
import org.wso2.healthcare.integration.fhir.core.Query;
import org.wso2.healthcare.integration.fhir.model.HolderFHIRResource;
import org.wso2.healthcare.integration.fhir.model.type.BaseType;
import org.wso2.healthcare.integration.fhir.model.type.GeneralPurposeDataType;
import org.wso2.healthcare.integration.fhir.utils.QueryUtils;
import org.wso2.healthcare.integration.v2tofhir.V2SpecParser;
import org.wso2.healthcare.integration.v2tofhir.V2ToFhirException;
import org.wso2.healthcare.integration.v2tofhir.util.ModelGenerator;
import org.wso2.healthcare.integration.v2tofhir.util.XpathEvaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data holder class to hold segment information extracted from mapping files.
 */
public class Segment {

    private Map<Long, String> dataTypeMap;
    private Map<Long, String> fhirPathMap;
    private Map<Long, TargetMapping> targetMappingMap;
    //handles multiple data elements with same fhirpath
    private Map<String, Type> typeMap;
    private List<CSVRecord> recordList;
    private String targetResource;
    private Resource fhirResource;
    private XpathEvaluator evaluator;

    public Segment() {
        this.dataTypeMap = new HashMap<>();
        this.fhirPathMap = new HashMap<>();
        this.targetMappingMap = new HashMap<>();
        this.typeMap = new HashMap<>();
        this.recordList = new ArrayList<>();
        this.evaluator = XpathEvaluator.getInstance();
    }

    public Map<Long, String> getDataTypeMap() {
        return dataTypeMap;
    }

    public void setDataTypeMap(Map<Long, String> dataTypeMap) {
        this.dataTypeMap = dataTypeMap;
    }

    public List<CSVRecord> getRecordList() {
        return recordList;
    }

    public void addRecord(CSVRecord record) {
        recordList.add(record);
    }

    public void setRecordList(List<CSVRecord> recordList) {
        this.recordList = recordList;
    }

    public Map<Long, String> getFhirPathMap() {
        return fhirPathMap;
    }

    public void setFhirPathMap(Map<Long, String> fhirPathMap) {
        this.fhirPathMap = fhirPathMap;
    }

    public Map<Long, TargetMapping> getTargetMappingMap() {
        return targetMappingMap;
    }

    public void setTargetResource(String targetResource) {
        this.targetResource = targetResource;
    }

    public String getTargetResource() {
        return targetResource;
    }

    public Resource getFhirResource() {
        return fhirResource;
    }

    public void setFhirResource(Resource fhirResource) {
        this.fhirResource = fhirResource;
    }

    public Resource createAndPopulateResource(IBaseResource resource, MessageContext messageContext)
            throws V2ToFhirException {
        for (Long pos : fhirPathMap.keySet()) {
            String path = fhirPathMap.get(pos);
            if (dataTypeMap.get(pos) == null ) {
                String fhirAttributeName;
                Type dataType;
                String childTypeAttribute;
                String typeName;
                if (path.contains("[") && path.contains("].")) {
                    fhirAttributeName = path.substring(0, path.indexOf("["));
                    typeName = path.substring(0, path.indexOf("]") + 1);
                    dataType = typeMap.get(typeName);
                    childTypeAttribute = path.substring(path.indexOf("].") + 2);
                } else {
                    fhirAttributeName = path;
                    dataType = typeMap.get(path);
                    childTypeAttribute = path;
                    typeName = path;
                }
                Property property = ((Resource) resource).getNamedProperty(fhirAttributeName);

                String xpath = targetMappingMap.containsKey(pos) ? targetMappingMap.get(pos).getV2Xpath() :
                        recordList.get((int) (pos - 1)).get("v2Xpath");

                List<String> evaluatedResult = (List<String>) evaluator.evaluate(messageContext, xpath);
                Map<String, String> paramMap = new HashMap<>();
                if (evaluatedResult.size() > 0) {
                    if (ModelGenerator.isPrimitiveDataType(property.getTypeCode())) {
                        paramMap.put("value", evaluatedResult.get(0));
                    } else {
                        paramMap.put(childTypeAttribute, evaluatedResult.get(0));
                    }
                    try {
                        Type copiedType = null;
                        if (dataType != null) {
                            copiedType = dataType.copy();
                        }
                        dataType = ModelGenerator.createDataType(property.getTypeCode(), paramMap, null);
                        if (dataType != null && copiedType != null) {
                            ModelGenerator.copyTypeAttributes(dataType, copiedType);
                        }
                    } catch (FHIRConnectException e) {
                        throw new V2ToFhirException(e, "Error while creating fhir datatype from v2 segment.");
                    }
                }
                typeMap.put(typeName, dataType);
            }
        }
        for (Long pos : targetMappingMap.keySet()) {
            TargetMapping targetMapping = targetMappingMap.get(pos);
            populateDatatype(messageContext, resource, targetMapping.getV2Xpath(),
                    targetMapping.getCondition(), targetMapping.getVocabularyMap(), pos);
        }
        //loading custom mappings loaded from the csv for datatypes
        for (CSVRecord record : recordList) {
            populateDatatype(messageContext, resource, record.get("v2Xpath"), record.get("condition"),
                    record.get("vocabularyMap"), record.getRecordNumber());
        }
        for (String type : typeMap.keySet()) {
            if (typeMap.get(type) != null) {
                if (type.contains("[")) {
                    ((Resource) resource).setProperty(type.substring(0, type.indexOf("[")), typeMap.get(type));
                } else {
                    ((Resource) resource).setProperty(type, typeMap.get(type));
                }
            }
        }
        return (Resource) resource;
    }

    private void populateDatatype(MessageContext messageContext, IBaseResource resource, String v2Xpath,
                                  String condition, String vocabularyMap, long sortOrder) throws V2ToFhirException {
        if (dataTypeMap.get(sortOrder) != null) {
            DataType dataType = V2SpecParser.getDataTypesMap().get(dataTypeMap.get(sortOrder));
            if (StringUtils.isNotBlank(condition)) {
                //evaluating the condition
                boolean doProceed = evaluator.evaluateCondition(messageContext, condition);
                if (!doProceed) {
                    return;
                }
            }
            Type fhirType = dataType.createAndPopulateDataType(messageContext, v2Xpath, vocabularyMap);
            if (fhirType != null) {
                String fhirAttributeWithIndex = fhirPathMap.get(sortOrder);
                if (typeMap.containsKey(fhirAttributeWithIndex)) {
                    ModelGenerator.copyTypeAttributes(fhirType, typeMap.get(fhirAttributeWithIndex));
                    typeMap.remove(fhirAttributeWithIndex);
                }
                Query fhirQuery = new Query();
                try {
                    HolderFHIRResource resourceModel = new HolderFHIRResource("", new HashMap<>());
                    resourceModel.setHolderResource((Resource) resource);
                    fhirQuery.setSrcResource(resourceModel);
                    fhirQuery.setFhirPath(targetResource + "." + fhirAttributeWithIndex);
                    QueryUtils.evaluateQueryAndPlace(fhirQuery, new BaseType(new GeneralPurposeDataType(null, fhirType)));
                } catch (FHIRConnectException e) {
                    throw new V2ToFhirException(e);
                }
            }
        }
    }
}
