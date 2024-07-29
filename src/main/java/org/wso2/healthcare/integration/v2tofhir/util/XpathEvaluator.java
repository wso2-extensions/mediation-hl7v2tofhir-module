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

import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.synapse.MessageContext;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;
import org.wso2.healthcare.integration.v2tofhir.V2ToFhirException;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a utility class responsible for evaluating the Xpath expressions.
 */
public class XpathEvaluator {

    private static XpathEvaluator xpathEvaluatorInstance;

    private XpathEvaluator() {
    }

    public static XpathEvaluator getInstance() {
        if (xpathEvaluatorInstance == null) {
           xpathEvaluatorInstance = new XpathEvaluator();
        }
        return xpathEvaluatorInstance;
    }

    public Object evaluate(MessageContext ctx, String xpathStr) throws V2ToFhirException {
        SynapseXPath xpath;
        try {
            List<String> resultList = new ArrayList<>();
            xpath = new SynapseXPath(xpathStr);
            Object evaluate = xpath.evaluate(ctx);
            if (evaluate instanceof ArrayList) {
                List evaluatedResults = (ArrayList)xpath.evaluate(ctx);
                if (evaluatedResults != null) {
                    for (Object evaluatedResult : evaluatedResults) {
                        if (evaluatedResult instanceof OMTextImpl) {
                            resultList.add(((OMTextImpl)evaluatedResult).getText());
                        }
                    }
                }
            }
            return resultList;
        } catch (JaxenException e) {
            throw new V2ToFhirException(e, "Error occurred while evaluating the Xpath for V2 element");
        }
    }

    public boolean evaluateCondition(MessageContext ctx, String xpathStr) throws V2ToFhirException {
        try {
            Object result = new SynapseXPath(xpathStr).evaluate(ctx);
            if (result instanceof Boolean) {
                return (boolean) result;
            } else {
                return false;
            }
        } catch (JaxenException e) {
            throw new V2ToFhirException(e, "Error occurred while evaluating the XPath condition");
        }
    }
}
