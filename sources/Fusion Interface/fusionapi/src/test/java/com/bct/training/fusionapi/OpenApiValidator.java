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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

/**
 * Class that validates the OpenApi (Swagger 3.0)
 */
public class OpenApiValidator extends Validator {

    /**
     * validate the open Api document
     * @param openAPI Object of OpenApi
     * @return
     */
    public Map<String, List<Issue>> validate(OpenAPI openAPI) {

        Map<String, List<Issue>> fieldIssues = new HashMap<>();
        Map<String, PathItem> paths = openAPI.getPaths();

        for (Map.Entry<String, PathItem> path : paths.entrySet()) {
            List<io.swagger.v3.oas.models.Operation> operationsList = path.getValue().readOperations();
            String domain = getDomain();
            operationsList.forEach(op -> {
                List<Issue> issues = new ArrayList<>();
                String operationId = op.getOperationId();
                ResourceValidator.validateOperation(operationId, domain, issues);
                if (!issues.isEmpty()) {
                    fieldIssues.put(operationId, issues);
                }
                List<Issue> parameterIssueList = getOpenApiParameterIssues(op, domain);
                addToAllIssues(fieldIssues, parameterIssueList, operationId);
                List<Issue> urlIssuesList = getUrlIssues(path.getKey(), domain);
                addToAllIssues(fieldIssues, urlIssuesList, operationId);
                List<Issue> requestBodyIssuesList = getRequestBodyIssues(openAPI, op, domain);
                addToAllIssues(fieldIssues, requestBodyIssuesList, operationId);
            });
        }
        return fieldIssues;
    }

    /**
     * Method to get the description from the body parameter
     * @param schema
     * @return
     */
    private String getDescriptionFromRequestBodyPropertiesForOpenApi(Map.Entry<String, Schema> schema) {
        String desc = "";
        if (schema.getValue() instanceof StringSchema) {
            StringSchema obj = (StringSchema) schema.getValue();
            desc = obj.getDescription() != null ? obj.getDescription() : "";
        } else if (schema.getValue() instanceof NumberSchema) {
            NumberSchema obj = (NumberSchema) schema.getValue();
            desc = obj.getDescription() != null ? obj.getDescription() : "";
        } else if (schema.getValue() instanceof DateSchema) {
            DateSchema obj = (DateSchema) schema.getValue();
            desc = obj.getDescription() != null ? obj.getDescription() : "";
        }
        return desc;
    }

    /**
     * Method to get the Parameter Issues.
     * @param operation
     * @param domain
     * @return
     */
    private List<Issue> getOpenApiParameterIssues(io.swagger.v3.oas.models.Operation operation, String domain) {
        List<Issue> parameterIssueList = new ArrayList<>();
        operation.getParameters()
                .stream()
                .map(parameter -> getIssues(parameter.getName(), parameter.getDescription(), domain))
                .forEach(parameterIssueList::addAll);
        return parameterIssueList;
    }

    /**
     * Method to find the request body issues.
     * @param openAPI
     * @param op
     * @param domain
     * @return
     */
    private List<Issue> getRequestBodyIssues(OpenAPI openAPI, io.swagger.v3.oas.models.Operation op, String domain) {
        List<Issue> requestBodyIssues = new ArrayList<>();
        if (op.getRequestBody() != null && op.getRequestBody().getDescription().equalsIgnoreCase("body payload")) {
            String requestBodySchemaName = getRequestBodySchemeName(op);

            Map<String, io.swagger.v3.oas.models.media.Schema> schemaProperties = openAPI.getComponents().getSchemas().get(requestBodySchemaName).getProperties();
            schemaProperties.entrySet()
                    .forEach(schema -> {
                        String description = getDescriptionFromRequestBodyPropertiesForOpenApi(schema);
                        VocabularyArray entryArray = VocabularyLoader.getInstance().searchArray(domain, schema.getKey());
                        if (null == entryArray) {
                            requestBodyIssues.add(new Issue(schema.getKey(), "invalid property."));
                        } else {
                            Issue issue = validateDescription(entryArray, schema.getKey(), description);
                             if (issue != null) {
                                 requestBodyIssues.add(issue);
                             }
                        }
                     });
        }
        return requestBodyIssues;
    }

    /**
     * method to return the RequestBody schema name.
     * @param op
     * @return
     */
    private String getRequestBodySchemeName(io.swagger.v3.oas.models.Operation op) {
        String requestBodySchemaName = op.getRequestBody().getContent().get("application/json").getSchema().get$ref();
        requestBodySchemaName = requestBodySchemaName.substring(requestBodySchemaName.lastIndexOf("/") + 1) + "Body";
        return requestBodySchemaName;
    }

}
