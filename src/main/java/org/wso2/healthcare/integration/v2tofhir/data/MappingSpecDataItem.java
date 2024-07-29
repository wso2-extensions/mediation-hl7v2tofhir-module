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

import java.util.ArrayList;
import java.util.List;

/**
 * Holds spec data from mapping specification based on the input spec type(csv, concept map etc)
 */
public class MappingSpecDataItem {

    private List<CSVRecord> recordList;
    private ConceptMap conceptMap;

    public MappingSpecDataItem(Iterable<CSVRecord> recordList) {
        this.recordList = new ArrayList<>();
        for (CSVRecord record : recordList) {
            this.recordList.add(record);
        }
    }

    public MappingSpecDataItem(ConceptMap conceptMap) {
        this.conceptMap = conceptMap;
    }

    public List<CSVRecord> getRecordList() {
        return recordList;
    }

    public ConceptMap getConceptMap() {
        return conceptMap;
    }
}
