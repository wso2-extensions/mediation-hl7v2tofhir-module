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

package org.wso2.healthcare.integration.v2tofhir;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.wso2.healthcare.integration.fhir.config.FHIRConnectorConfig;
import org.wso2.healthcare.integration.fhir.utils.FHIRParserUtils;
import org.wso2.healthcare.integration.v2tofhir.data.CodeSystem;
import org.wso2.healthcare.integration.v2tofhir.data.ConceptMap;
import org.wso2.healthcare.integration.v2tofhir.data.DataType;
import org.wso2.healthcare.integration.v2tofhir.data.MappingSpecDataItem;
import org.wso2.healthcare.integration.v2tofhir.data.Segment;
import org.wso2.healthcare.integration.v2tofhir.data.TargetMapping;
import org.wso2.healthcare.integration.v2tofhir.resources.Resource;
import org.wso2.healthcare.integration.common.OpenHealthcareException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for parsing the v2 to fhir mapping specification files.
 */
public class V2SpecParser {

    private static Map<String, MappingSpecDataItem> messageProfiles = new HashMap<>();
    private static Map<String, MappingSpecDataItem> segmentProfiles = new HashMap<>();
    private static Map<String, MappingSpecDataItem> datatypeProfiles = new HashMap<>();
    private static Map<String, MappingSpecDataItem> codesystemProfiles = new HashMap<>();
    private static Map<String, Resource> resourcesMap;
    private static Map<String, Segment> segmentsMap;
    private static Map<String, DataType> dataTypesMap;
    private static Map<String, CodeSystem> codeSystemMap;

    private static final Log LOG = LogFactory.getLog(V2SpecParser.class);

    public static void parseSpecFiles() throws IOException {

        String[] headers = { "v2Xpath", "cardinalityMin", "cardinalityMax", "condition", "targetResource",
                "segmentMap" };
        parseDefaultMappingCsvFiles(headers);
        parseDefaultConceptMappingFiles();
        String profilePath  = System.getProperty("fhir.connector.v2tofhir.profiles");
        if (profilePath == null) {
            profilePath = System.getProperty("carbon.home") + File.separatorChar + "fhir" + File.separatorChar
                    + "v2-to-fhir";
        }
        File profilesDir = new File(profilePath);
        if (profilesDir.exists() && profilesDir.isDirectory()) {
            messageProfiles.putAll(getCsvProfiles(profilePath + File.separatorChar + "messages", headers));

            headers = new String[] { "sortOrder","v2Xpath", "cardinalityMin", "cardinalityMax", "condition", "fhirpath",
                    "datatypeMap", "vocabularyMap", "targetResource", "assignment"};
            segmentProfiles.putAll(getCsvProfiles(profilePath + File.separatorChar + "segments", headers));

            headers = new String[] { "v2Xpath", "cardinalityMin", "cardinalityMax", "condition", "fhirattr",
                    "datatypeMap", "vocabularyMap","targetDataType", "dataType"};
            datatypeProfiles.putAll(getCsvProfiles(profilePath + File.separatorChar + "datatypes", headers));

            headers = new String[] { "code", "condition", "fhirCode", "display", "system" };
            codesystemProfiles.putAll(getCsvProfiles(profilePath + File.separatorChar + "codesystems", headers));
        }
    }

    private static Map<String, MappingSpecDataItem> getCsvProfiles(String path, String[] headers) throws IOException {
        Map<String, MappingSpecDataItem> recordsMap = new HashMap<>();
        File profilesDir = new File(path);
        if (profilesDir.exists() && profilesDir.isDirectory()) {
            File[] profileFiles = profilesDir.listFiles();
            if (profileFiles != null) {
                for (File profileFile : profileFiles) {
                    Reader in = new FileReader(profileFile);
                    if (profileFile.getName().endsWith(".csv")) {
                        recordsMap.put(profileFile.getName().substring(0, profileFile.getName().indexOf(".csv")),
                                new MappingSpecDataItem(CSVFormat.DEFAULT.withHeader(headers).withFirstRecordAsHeader().parse(in)));
                    }
                }
            }
        }
        return recordsMap;
    }

    private static void processMessages() {
        for (String msgName : messageProfiles.keySet()) {
            MappingSpecDataItem specDataItem = messageProfiles.get(msgName);
            for (CSVRecord csvRecord : specDataItem.getRecordList()) {
                //TODO process messages from the spec
            }
        }
    }

    private static void processSegments() {
        for (String segmentName : segmentProfiles.keySet()) {
            String targetResource =  null;
            ConceptMap conceptMap = segmentProfiles.get(segmentName).getConceptMap();
            Segment segment = new Segment();
            if (conceptMap != null) {
                if (conceptMap.getTargetResource() != null) {
                    targetResource = conceptMap.getTargetResource();
                    segment.setTargetResource(targetResource);
                }
                Map<Integer, TargetMapping> targetMappingMap = conceptMap.getTargetMappingMap();
                for (Integer sortOrder : targetMappingMap.keySet()) {
                    TargetMapping targetMapping = targetMappingMap.get(sortOrder);
                    if (targetMapping.getDatatypeMap() != null &&
                            datatypeProfiles.get(targetMapping.getDatatypeMap()) != null) {
                        segment.getDataTypeMap().put((long) sortOrder, targetMapping.getDatatypeMap());
                    }
                    if (targetMapping.getFhirpath() != null) {
                        segment.getFhirPathMap().put((long) sortOrder, targetMapping.getFhirpath());
                    }
                    segment.getTargetMappingMap().put((long) sortOrder, targetMapping);
                }
            }
            //processing customizations for segments
            List<CSVRecord> csvRecords = segmentProfiles.get(segmentName).getRecordList();
            int count = 0;
            if (csvRecords != null) {
                for (CSVRecord csvRecord : csvRecords) {
                    if (count == 0) {
                        targetResource = csvRecord.get("targetResource");
                        segment.setTargetResource(targetResource);
                    }
                    segment.addRecord(csvRecord);
                    if (datatypeProfiles.get(csvRecord.get("datatypeMap")) != null) {
                        segment.getDataTypeMap().put(csvRecord.getRecordNumber(), csvRecord.get("datatypeMap"));
                    }
                    segment.getFhirPathMap().put(csvRecord.getRecordNumber(), csvRecord.get("fhirpath"));
                    count ++;
                }
            }
            if (resourcesMap.containsKey(targetResource)) {
                Resource resource = resourcesMap.get(targetResource);
                resource.addSegment(segment);
            } else {
                Resource resource = new Resource();
                resource.addSegment(segment);
                resourcesMap.put(targetResource, resource);
            }
            segmentsMap.put(segmentName, segment);
        }
    }

    private static void processDataTypes() {
        for (String dataTypeName : datatypeProfiles.keySet()) {
            String targetDataType;
            ConceptMap conceptMap = datatypeProfiles.get(dataTypeName).getConceptMap();
            DataType dataType = new DataType();
            if (conceptMap != null) {
                if (conceptMap.getTargetDataType() != null) {
                    targetDataType = conceptMap.getTargetDataType();
                    dataType.setTargetFhirDataType(targetDataType);
                }
                Map<Integer, TargetMapping> targetMappingMap = conceptMap.getTargetMappingMap();
                for (Integer sortOrder : targetMappingMap.keySet()) {
                    TargetMapping targetMapping = targetMappingMap.get(sortOrder);
                    if (targetMapping.getDatatypeMap() != null &&
                            datatypeProfiles.get(targetMapping.getDatatypeMap()) != null) {
                        dataType.getDataTypeMap().put((long) sortOrder, targetMapping.getDatatypeMap());
                    }
                }
            }
            //processing customizations for data types
            List<CSVRecord> csvRecords = datatypeProfiles.get(dataTypeName).getRecordList();
            int count = 0;
            if (csvRecords == null) {
                continue;
            }
            for (CSVRecord csvRecord : csvRecords) {
                if (count == 0) {
                    targetDataType = csvRecord.get("targetDataType");
                    dataType.setTargetFhirDataType(targetDataType);
                }
                dataType.addRecord(csvRecord);
                if (StringUtils.isNotBlank(csvRecord.get("datatypeMap"))) {
                    dataType.getDataTypeMap().put(csvRecord.getRecordNumber(), csvRecord.get("datatypeMap"));
                }
                count++;
            }
            dataTypesMap.put(dataTypeName, dataType);
        }
    }

    private static void processCodeSystems() {
        for (String codeSystemName : codesystemProfiles.keySet()) {
            List<CSVRecord> csvRecords = codesystemProfiles.get(codeSystemName).getRecordList();
            CodeSystem codeSystem = new CodeSystem();
            if (csvRecords == null) {
                continue;
            }
            for (CSVRecord csvRecord : csvRecords) {
                codeSystem.addCode(csvRecord.get("code"), csvRecord);
            }
            codeSystemMap.put(codeSystemName, codeSystem);
        }
    }

    public static void init() {
        resourcesMap = new HashMap<>();
        segmentsMap = new HashMap<>();
        dataTypesMap = new HashMap<>();
        codeSystemMap = new HashMap<>();
        try {
            parseSpecFiles();
            processMessages();
            processSegments();
            processDataTypes();
            processCodeSystems();
        } catch (IOException e) {
            LOG.error("Error occurred while processing the csv descriptor files", e);
        }
    }

    /**
     * This function will read the default mapping files shipped in the v2-to-fhir-mappings directory.
     * @param headers csv file headers
     */
    public static void parseDefaultMappingCsvFiles(String[] headers) {

        try (InputStream manifestFileInputStream = FHIRConnectorConfig.class.getResourceAsStream("/v2-to-fhir-mappings/mapping-manifest.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(manifestFileInputStream))) {
            String filePath;
            while ((filePath = reader.readLine()) != null) {
                if (!filePath.endsWith(".csv")) {
                    continue;
                }
                String relativeFilePath = "/v2-to-fhir-mappings/" + filePath;
                try (InputStream fileStream = FHIRConnectorConfig.class.getResourceAsStream(relativeFilePath)) {
                    if (fileStream != null) {
                        String dirName = filePath.substring(0, filePath.indexOf("/"));
                        String specFileName = filePath.substring(filePath.indexOf("/") + 1, filePath.indexOf(".csv"));
                        switch (dirName) {
                            case "segments":
                                segmentProfiles.putIfAbsent(specFileName, new MappingSpecDataItem(CSVFormat.DEFAULT.withHeader(headers).
                                        withFirstRecordAsHeader().parse(new BufferedReader(new InputStreamReader(fileStream)))));
                                break;
                            case "datatypes":
                                datatypeProfiles.putIfAbsent(specFileName, new MappingSpecDataItem(CSVFormat.DEFAULT.withHeader(headers).
                                        withFirstRecordAsHeader().parse(new BufferedReader(new InputStreamReader(fileStream)))));
                                break;
                            case "codesystems":
                                codesystemProfiles.putIfAbsent(specFileName, new MappingSpecDataItem(CSVFormat.DEFAULT.withHeader(headers).
                                        withFirstRecordAsHeader().parse(new BufferedReader(new InputStreamReader(fileStream)))));
                                break;
                            case "messages":
                                messageProfiles.putIfAbsent(specFileName, new MappingSpecDataItem(CSVFormat.DEFAULT.withHeader(headers).
                                        withFirstRecordAsHeader().parse(new BufferedReader(new InputStreamReader(fileStream)))));
                                break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Error occurred while reading hl7v2 to fhir mapper files", e);
        }
    }

    /**
     * This function will read the default concept mapping files shipped in the v2-to-fhir-mappings directory.
     */
    public static void parseDefaultConceptMappingFiles() {

        try (InputStream manifestFileInputStream = FHIRConnectorConfig.class.getResourceAsStream("/v2-to-fhir-mappings/mapping-manifest.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(manifestFileInputStream))) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Reading v2tofhir concept mapping files started.");
            }
            String filePath;
            while ((filePath = reader.readLine()) != null) {
                String relativeFilePath = "/v2-to-fhir-mappings/" + filePath;
                if (relativeFilePath.contains("/conceptmaps/")) {
                    try (InputStream fileStream = FHIRConnectorConfig.class.getResourceAsStream(relativeFilePath)) {
                        if (fileStream != null) {
                            IBaseResource resource = FHIRParserUtils.parseFHIRResource(fileStream);
                            if (resource instanceof org.hl7.fhir.r4.model.ConceptMap) {
                                org.hl7.fhir.r4.model.ConceptMap conceptMap = (org.hl7.fhir.r4.model.ConceptMap) resource;
                                String conceptMapId = conceptMap.getId();
                                String conceptMapName = conceptMap.getName();
                                if (conceptMapId.startsWith("ConceptMap/datatype-")) {
                                    datatypeProfiles.putIfAbsent(conceptMapName, new MappingSpecDataItem(new ConceptMap(
                                            MappingType.DATATYPE, conceptMap)));
                                } else if (conceptMapId.startsWith("ConceptMap/segment-")) {
                                    segmentProfiles.putIfAbsent(conceptMapName, new MappingSpecDataItem(new ConceptMap(
                                            MappingType.SEGMENT, conceptMap)));
                                } else if (conceptMapId.startsWith("ConceptMap/codesystem-")) {
                                    codesystemProfiles.putIfAbsent(conceptMapName, new MappingSpecDataItem(new ConceptMap(
                                            MappingType.CODESYSTEM, conceptMap)));
                                } else if (conceptMapId.startsWith("ConceptMap/message-")) {
                                    messageProfiles.putIfAbsent(conceptMapName, new MappingSpecDataItem(new ConceptMap(
                                            MappingType.MESSAGE, conceptMap)));
                                }
                            }
                        }
                    }
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Reading v2tofhir concept mapping files completed.");
            }
        } catch (IOException e) {
            LOG.error("Error occurred while reading v2tofhir concept mapping files", e);
        } catch (OpenHealthcareException e) {
            LOG.error("Error occurred while parsing v2tofhir concept mapping files", e);
        }
    }

    public static Map<String, DataType> getDataTypesMap() {
        return dataTypesMap;
    }

    public static void setDataTypesMap(Map<String, DataType> dataTypesMap) {
        V2SpecParser.dataTypesMap = dataTypesMap;
    }

    public static Map<String, Resource> getResourcesMap() {
        return resourcesMap;
    }

    public static void setResourcesMap(Map<String, Resource> resourcesMap) {
        V2SpecParser.resourcesMap = resourcesMap;
    }

    public static Resource getResource(String resourceName) {
        return resourcesMap.get(resourceName);
    }

    public static Map<String, CodeSystem> getCodeSystemMap() {
        return codeSystemMap;
    }
}
