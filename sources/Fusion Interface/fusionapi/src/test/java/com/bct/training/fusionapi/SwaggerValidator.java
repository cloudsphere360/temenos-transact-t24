/**
 * ﻿    Copyright © Temenos Headquarters SA 2018.  All rights reserved.
 */
package com.bct.training.fusionapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.temenos.irf.vocab.Issue;
import com.temenos.irf.vocab.ResourceValidator;
import com.temenos.vocab.VocabularyArray;
import com.temenos.vocab.VocabularyLoader;

import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.*;

/**
 * Class that validates the Swagger document(swagger 2.0)
 */
public class SwaggerValidator extends Validator {

    /**
     * validate the swagger document.
     *
     * @param swagger Object of swagger
     * @return
     */
    public Map<String, List<Issue>> validate(Swagger swagger) {

        Map<String, List<Issue>> fieldIssues = new HashMap<>();
        Map<String, Path> paths = swagger.getPaths();
        Map<String, Model> definitions = swagger.getDefinitions();
        for (Map.Entry<String, Path> path : paths.entrySet()) {
            String domain = getDomain();
            List<Operation> operationsList = path.getValue().getOperations();
            for (Operation op : operationsList) {
                List<Issue> issues = new ArrayList<>();
                String operationId = op.getOperationId();
                ResourceValidator.validateOperation(operationId, domain, issues);
                if (!issues.isEmpty()) {
                    fieldIssues.put(operationId, issues);
                }
                List<Issue> parameterIssueList = getSwaggerParameterIssues(definitions, op, domain);
                List<Issue> urlIssuesList = getUrlIssues(path.getKey(), domain);
                addToAllIssues(fieldIssues, urlIssuesList, operationId);
                addToAllIssues(fieldIssues, parameterIssueList, operationId);
            }
        }
        return fieldIssues;
    }

    /**
     * Method to get the swagger Parameter Issues.
     *
     * @param definitions
     * @param operation
     * @param domain
     * @return
     */
    private List<Issue> getSwaggerParameterIssues(Map<String, Model> definitions, Operation operation, String domain) {
        List<Issue> parameterIssueList = new ArrayList<>();
        Map<String,Property> finalMap = new HashMap<>();
        for (Parameter parameter : operation.getParameters()) {

            if (parameter.getName().equalsIgnoreCase("payload") && parameter.getIn().equalsIgnoreCase("body") && parameter.getRequired()) {
                BodyParameter bodyParameter = (BodyParameter) parameter;
                if (bodyParameter.getSchema() != null && bodyParameter.getSchema().getReference() != null) {
                    String requestBodySchemaName = getRequestBodySchemaName(bodyParameter);

                    if (definitions.get(requestBodySchemaName) != null && definitions.get(requestBodySchemaName).getProperties() != null) {
                        getBodySchemeParameters(definitions, finalMap, requestBodySchemaName);

                        finalMap.forEach((key,value) -> {
                            VocabularyArray entryArray = VocabularyLoader.getInstance().searchArray(domain, key);
                            if (null == entryArray) {
                                parameterIssueList.add(new Issue(key, "invalid property."));
                            } else {
                                Issue issue = validateDescription(entryArray, key, value.getDescription());
                                if (issue != null) {
                                    parameterIssueList.add(issue);
                                }
                            }
                        });

                    } else {
                        parameterIssueList.add(createIssueObject(requestBodySchemaName, "request body schema is missing", "request body schema is null or empty"));
                    }
                }
            } else {
                parameterIssueList.addAll(getIssues(parameter.getName(), parameter.getDescription(), domain));
            }
        }
        return parameterIssueList;
    }

    /**
     *
     * @param definitions
     * @param finalMap
     * @param requestBodySchemaName
     */
    private void getBodySchemeParameters(Map<String, Model> definitions, Map<String, Property> finalMap, String requestBodySchemaName) {
        Map<String, Property> properties = definitions.get(requestBodySchemaName).getProperties();
        for (Map.Entry<String, Property> prop : properties.entrySet()) {
            String description = null;
            if (prop.getValue() instanceof ArrayProperty) {
                getPropertyListFromArrayProperty(prop, finalMap);
            }
            if (prop.getValue() instanceof ObjectProperty) {
                getPropertyListFromObjectProperty(prop, finalMap);
            } else {
                finalMap.put(prop.getKey(),prop.getValue());
            }
        }
    }

    /**
     *
     * @param prop
     * @param finalList
     * @return
     */
    private Map<String,Property> getPropertyListFromObjectProperty(Map.Entry<String, Property> prop, Map<String,Property> finalList) {
        ObjectProperty objProp = (ObjectProperty) prop.getValue();
        Map<String, Property> subPropertyMap = objProp.getProperties();

        for (Map.Entry<String, Property> property : subPropertyMap.entrySet()) {
            if (property.getValue() instanceof ArrayProperty) {
                getPropertyListFromArrayProperty(property, finalList);
            } else if (property.getValue() instanceof ObjectProperty) {
                getPropertyListFromObjectProperty(property, finalList);
            } else {
                finalList.put(property.getKey(),property.getValue());
            }
        }
        return finalList;
    }

    /**
     *
     * @param prop
     * @param finalList
     * @return
     */
    private Map<String,Property> getPropertyListFromArrayProperty(Map.Entry<String, Property> prop, Map<String,Property> finalList) {
        ArrayProperty ap = (ArrayProperty) prop.getValue();
        Map<String, Property> subPropertyMap = ((ObjectProperty) ap.getItems()).getProperties();

        for (Map.Entry<String, Property> property : subPropertyMap.entrySet()) {
            if (property.getValue() instanceof ArrayProperty) {
                getPropertyListFromArrayProperty(property, finalList);
            } else if (property.getValue() instanceof ObjectProperty) {
                getPropertyListFromObjectProperty(property, finalList);
            } else {
                finalList.put(property.getKey(), property.getValue());
            }
        }
        return finalList;
    }

    /**
     * 
     * @param bodyParameter
     * @return
     */
    private String getRequestBodySchemaName(BodyParameter bodyParameter) {
        String requestBodySchemaName = bodyParameter.getSchema().getReference();
        requestBodySchemaName = requestBodySchemaName.substring(requestBodySchemaName.lastIndexOf("/") + 1) + "Body";
        return requestBodySchemaName;
    }


}