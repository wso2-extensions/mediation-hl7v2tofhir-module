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

package org.wso2.healthcare.integration.v2tofhir.resources;

import org.apache.synapse.MessageContext;
import org.wso2.healthcare.integration.v2tofhir.V2ToFhirException;
import org.wso2.healthcare.integration.v2tofhir.data.Segment;
import org.wso2.healthcare.integration.v2tofhir.util.ModelGenerator;
import org.wso2.healthcare.integration.v2tofhir.util.XpathEvaluator;

import java.util.ArrayList;
import java.util.List;

public class Resource {

    protected XpathEvaluator evaluator;
    private org.hl7.fhir.r4.model.Resource resource;
    private List<Segment> segmentList;

    public Resource() {
        evaluator = XpathEvaluator.getInstance();
        segmentList = new ArrayList<>();
    }

    public org.hl7.fhir.r4.model.Resource getResource() {
        return resource;
    }

    public void setResource(org.hl7.fhir.r4.model.Resource resource) {
        this.resource = resource;
    }

    public org.hl7.fhir.r4.model.Resource createAndUpdateResource(MessageContext messageContext)
            throws V2ToFhirException {
        org.hl7.fhir.r4.model.Resource resource = null;
        if (segmentList != null) {
            for (Segment segment : segmentList) {
                if (resource == null) {
                    resource = (org.hl7.fhir.r4.model.Resource) ModelGenerator
                            .createResource(segment.getTargetResource());
                }
                resource = segment.createAndPopulateResource(resource, messageContext);
            }
        }
        return resource;
    }

    public List<Segment> getSegmentList() {
        return segmentList;
    }

    public void addSegment(Segment segment) {
        segmentList.add(segment);
    }

    public void setSegmentList(List<Segment> segmentList) {
        this.segmentList = segmentList;
    }
}
