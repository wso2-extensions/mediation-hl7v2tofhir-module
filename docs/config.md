# Supported operations

Below is the list of supported operations from the v2tofhir module. Click on each operation name to see its details.

# createResources Operation

The createResources operation converts HL7 V2 messages to FHIR resources. You can specify the FHIR resource type(s) to be created in the operation configuration.

## Properties


| Property Name                      | Property Description                                                    |
|------------------------------------|--------------------------------------------------------------------------|
| `objectId`                         | The unique identifier for the object to which the entry is being added. |
| `resourceTypes`                    | The FHIR resource type(s) to be created.                                |