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
import org.wso2.healthcare.integration.v2tofhir.util.XpathEvaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data holder class to hold code system information extracted from mapping files.
 */
public class CodeSystem {
    private String code;
    private String fhirCode;
    private String display;
    private String system;
    private List<CSVRecord> recordList;
    private String parentXpath;
    protected XpathEvaluator evaluator;
    private Map<String, CSVRecord> codeMap;

    public CodeSystem() {
        this.recordList = new ArrayList<>();
        evaluator = XpathEvaluator.getInstance();
        this.codeMap = new HashMap<>();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getFhirCode() {
        return fhirCode;
    }

    public void setFhirCode(String fhirCode) {
        this.fhirCode = fhirCode;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public List<CSVRecord> getRecordList() {
        return recordList;
    }

    public void addRecord(CSVRecord record) {
        recordList.add(record);
    }

    public Map<String, CSVRecord> getCodeMap() {
        return codeMap;
    }

    public void addCode(String code, CSVRecord record) {
        codeMap.put(code, record);
    }

    public void setCodeMap(Map<String, CSVRecord> codeMap) {
        this.codeMap = codeMap;
    }

    public void setRecordList(List<CSVRecord> recordList) {
        this.recordList = recordList;
    }

    public String getParentXpath() {
        return parentXpath;
    }

    public void setParentXpath(String parentXpath) {
        this.parentXpath = parentXpath;
    }

    public CSVRecord getMappedRecord(String key) {
        return codeMap.get(key);
    }
}
