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

import java.util.List;

/**
 * Data holder class to hold message information extracted from mapping files.
 */
public class Message {
    private String v2Xpath;
    private String cardinalityMin;
    private String cardinalityMax;
    private String condition;
    private String targetResource;
    private String segmentMap;
    private List<Segment> segmentList;

    public String getV2Xpath() {
        return v2Xpath;
    }

    public void setV2Xpath(String v2Xpath) {
        this.v2Xpath = v2Xpath;
    }

    public String getCardinalityMin() {
        return cardinalityMin;
    }

    public void setCardinalityMin(String cardinalityMin) {
        this.cardinalityMin = cardinalityMin;
    }

    public String getCardinalityMax() {
        return cardinalityMax;
    }

    public void setCardinalityMax(String cardinalityMax) {
        this.cardinalityMax = cardinalityMax;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getTargetResource() {
        return targetResource;
    }

    public void setTargetResource(String targetResource) {
        this.targetResource = targetResource;
    }

    public String getSegmentMap() {
        return segmentMap;
    }

    public void setSegmentMap(String segmentMap) {
        this.segmentMap = segmentMap;
    }

    public List<Segment> getSegmentList() {
        return segmentList;
    }

    public void addSegment(Segment segment) {
        this.segmentList.add(segment);
    }

    public void setSegmentList(List<Segment> segmentList) {
        this.segmentList = segmentList;
    }
}
