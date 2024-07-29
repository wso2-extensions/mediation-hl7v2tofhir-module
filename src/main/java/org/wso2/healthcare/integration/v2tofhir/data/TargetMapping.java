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

/**
 * Data holder class to hold target mapping information extracted from mapping files.
 */
public class TargetMapping {

    private String datatypeMap;
    private String v2Xpath;
    private String fhirpath;
    private String condition;
    private String vocabularyMap;
    private int cardinalityMin;
    private int cardinalityMax;

    public void setCardinalityMax(int cardinalityMax) {
        this.cardinalityMax = cardinalityMax;
    }

    public int getCardinalityMax() {
        return cardinalityMax;
    }

    public void setCardinalityMin(int cardinalityMin) {
        this.cardinalityMin = cardinalityMin;
    }

    public int getCardinalityMin() {
        return cardinalityMin;
    }

    public void setDatatypeMap(String datatypeMap) {
        this.datatypeMap = datatypeMap;
    }

    public String getDatatypeMap() {
        return datatypeMap;
    }

    public void setFhirpath(String fhirpath) {
        this.fhirpath = fhirpath;
    }

    public String getFhirpath() {
        return fhirpath;
    }

    public void setV2Xpath(String v2Xpath) {
        this.v2Xpath = v2Xpath;
    }

    public String getV2Xpath() {
        return v2Xpath;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getCondition() {
        return condition;
    }

    public void setVocabularyMap(String vocabularyMap) {
        this.vocabularyMap = vocabularyMap;
    }

    public String getVocabularyMap() {
        return vocabularyMap;
    }
}
