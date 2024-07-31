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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.synapse.MessageContext;
import org.hl7.fhir.r4.model.Type;
import org.wso2.healthcare.integration.fhir.FHIRConnectException;
import org.wso2.healthcare.integration.v2tofhir.V2SpecParser;
import org.wso2.healthcare.integration.v2tofhir.V2ToFhirException;
import org.wso2.healthcare.integration.v2tofhir.util.ModelGenerator;
import org.wso2.healthcare.integration.v2tofhir.util.XpathEvaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data holder class to hold data type information extracted from mapping files.
 */
public class DataType {

    private List<CSVRecord> recordList;
    private Map<Long, String> dataTypeMap;
    private Map<Long, TargetMapping> targetMappingMap;
    private String parentXpath;
    private String targetFhirDataType;
    private Type fhirType;
    protected XpathEvaluator evaluator;

    public DataType() {
        this.recordList = new ArrayList<>();
        this.dataTypeMap = new HashMap<>();
        evaluator = XpathEvaluator.getInstance();
        targetMappingMap = new HashMap<>();
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

    public Map<Long, String> getDataTypeMap() {
        return dataTypeMap;
    }

    public void setDataTypeMap(Map<Long, String> dataTypeMap) {
        this.dataTypeMap = dataTypeMap;
    }

    public String getParentXpath() {
        return parentXpath;
    }

    public void setParentXpath(String parentXpath) {
        this.parentXpath = parentXpath;
    }

    public String getTargetFhirDataType() {
        return targetFhirDataType;
    }

    public void setTargetFhirDataType(String targetFhirDataType) {
        this.targetFhirDataType = targetFhirDataType;
    }

    public Type setFhirType(Type fhirType) {
        this.fhirType = fhirType;
        return this.fhirType;
    }

    public Type getFhirType() {
        return fhirType;
    }

    public Type createAndPopulateDataType(MessageContext messageContext, String parentXpath, String vocabularyMap) throws V2ToFhirException {
        Map<String, String> params = new HashMap<>();
        List<ImmutablePair<String, Type>> multipleParams = new ArrayList<>();
        Type fhirType = null;
        if (targetFhirDataType != null) {
            for (CSVRecord record : recordList) {
                String effectiveXpath = record.get("v2Xpath");
                String condition = record.get("condition");
                if (StringUtils.isNotBlank(condition)) {
                    if (parentXpath != null) {
                        condition = condition.replaceAll("<parentXpath>", parentXpath);
                    } else {
                        condition = condition.replaceAll("<parentXpath>","");
                    }
                    //evaluating the condition
                    boolean doProceed = evaluator.evaluateCondition(messageContext, condition);
                    if (!doProceed) {
                        continue;
                    }
                }
                String xpathEvaluatedValue = "";
                if (parentXpath != null && effectiveXpath != null) {
                    effectiveXpath = parentXpath + effectiveXpath.substring(1);
                    List<String> evaluatedResult = (List<String>) evaluator.evaluate(messageContext, effectiveXpath);
                    //TODO handle multiple results
                    if (evaluatedResult.size() > 0) {
                        xpathEvaluatedValue = evaluatedResult.get(0);
                        if (StringUtils.isNotBlank(record.get("vocabularyMap"))) {
                            CSVRecord codeRecord = V2SpecParser.getCodeSystemMap().get(record.get("vocabularyMap"))
                                    .getMappedRecord(xpathEvaluatedValue);
                            if (StringUtils.isNotBlank(codeRecord.get("fhirCode"))) {
                                xpathEvaluatedValue = codeRecord.get("fhirCode");
                            } else {
                                xpathEvaluatedValue = "";
                            }
                        } else if ( StringUtils.isNotBlank(vocabularyMap)) {
                            CSVRecord codeRecord = V2SpecParser.getCodeSystemMap().get(vocabularyMap)
                                    .getMappedRecord(xpathEvaluatedValue);
                            if (StringUtils.isNotBlank(codeRecord.get("fhirCode"))) {
                                xpathEvaluatedValue = codeRecord.get("fhirCode");
                            } else {
                                xpathEvaluatedValue = "";
                            }
                        }
                    }
                }
                if (record.get("fhirattr") != null) {
                    if (dataTypeMap.get(record.getRecordNumber()) != null) {
                        DataType nestedDataType = V2SpecParser.getDataTypesMap()
                                .get(dataTypeMap.get(record.getRecordNumber()));
                        nestedDataType.createAndPopulateDataType(messageContext, effectiveXpath, vocabularyMap);
                        if (nestedDataType.getTargetFhirDataType().equals(targetFhirDataType)) {
                            fhirType = nestedDataType.getFhirType();
                        } else {
                            if (nestedDataType.getFhirType() != null) {
                                //used for child datatypes with a different type than the parent data type
                                ImmutablePair<String, Type> keyVal = new ImmutablePair<>(record.get("fhirattr"),
                                        nestedDataType.getFhirType());
                                multipleParams.add(keyVal);
                            }
                        }
                    } else {
                        if (params.containsKey(record.get("fhirattr"))) {
                            Map<String, String> multipleParamMap = new HashMap<>();
                            multipleParamMap.put("value", xpathEvaluatedValue);
                            try {
                                if (StringUtils.isNotBlank(xpathEvaluatedValue)) {
                                    Type dataType = ModelGenerator
                                            .createDataType(record.get("dataType"), multipleParamMap, null);
                                    ImmutablePair<String, Type> keyVal = new ImmutablePair<>(record.get("fhirattr"), dataType);
                                    multipleParams.add(keyVal);
                                }
                            } catch (FHIRConnectException e) {
                                throw new V2ToFhirException(e, "Error occurred while creating fhir datatype");
                            }
                        } else {
                            params.put(record.get("fhirattr"), xpathEvaluatedValue);
                        }
                    }
                }
                //TODO handle when there's a datatype mapping available
            }
            try {
                Type copiedType = null;
                if (fhirType != null) {
                    copiedType = fhirType.copy();
                }
                fhirType = ModelGenerator.createDataType(targetFhirDataType, params, null);

                ModelGenerator.copyTypeAttributes(fhirType, copiedType);
                if (fhirType != null) {
                    for (ImmutablePair<String, Type> multipleParam : multipleParams) {
                        if (multipleParam.getValue() != null) {
                            fhirType.setProperty(multipleParam.getKey(), multipleParam.getValue());
                        }
                    }
                }
            } catch (FHIRConnectException e) {
                throw new V2ToFhirException(e, "Error while creating fhir data types from V2 element.");
            }
        }
        return fhirType;
    }
}
