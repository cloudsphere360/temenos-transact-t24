/**
 * ﻿    Copyright © Temenos Headquarters SA 2018.  All rights reserved.
 */
package com.bct.training.fusionapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.temenos.irf.logging.Logger;
import com.temenos.irf.logging.LoggerFactory;
import com.temenos.irf.vocab.Issue;
import com.temenos.irf.vocab.ResourceValidator;
import com.temenos.vocab.VocabularyArray;
import com.temenos.vocab.VocabularyEntry;
import com.temenos.vocab.VocabularyLoader;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.json.JSONObject;

import java.util.*;

/**
 * Parentclass for the common validations.
 */
public class Validator {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public static final List<String> SKIP_LIST = Arrays.asList("validate_only", "page_size", "page_start", "page_token", "payload", "disablePagination");
    public static final List<String> RESOURCE_LIST = Arrays.asList("party","holdings","order","reference","product","meta","system","orgination" );
    public static final String SWAGGER = "swagger";
    public static final String OPENAPI = "openapi";

    /**
     * validate the swagger file content.
     *
     * @param fileContent content of the file
     * @param fileName    name of the file
     * @return ObjectNode
     */
    public ObjectNode validate(String fileContent, String fileName) {

        DocumentContext jsonContext = JsonPath.parse(((Object) fileContent).toString());
        JSONObject json = new JSONObject(jsonContext.jsonString());
        Map<String, List<Issue>> fieldIssues = null;
        if (json.has(SWAGGER) && json.getString(SWAGGER).equals("2.0")) {

            Swagger result = new SwaggerParser().parse(json.toString());
            fieldIssues = new SwaggerValidator().validate(result);
        } else if (json.has(OPENAPI) && json.getString(OPENAPI).startsWith("3.")) {

            OpenAPI openApi = new OpenAPIV3Parser().readContents(json.toString(), null, null).getOpenAPI();
            fieldIssues = new OpenApiValidator().validate(openApi);
        }
        ObjectNode response = createResponse(fieldIssues, fileName);
        if(response.get("body") != null){
            logger.error(response.toString());
        }
        return response;
    }

    /**
     * Method to create final response.
     * @param fieldIssues
     * @param fileName
     * @return
     */
    private ObjectNode createResponse(Map<String, List<Issue>> fieldIssues, String fileName) {
        final ObjectMapper mapper = new ObjectMapper();
        final ArrayNode bodyNode = mapper.createArrayNode();
        final ObjectNode header = mapper.createObjectNode();
        final ObjectNode response = mapper.createObjectNode();

        if (fieldIssues != null && !fieldIssues.isEmpty()) {
            for (Map.Entry<String, List<Issue>> issue : fieldIssues.entrySet()) {

                final ObjectNode operationNode = mapper.createObjectNode();
                operationNode.put("operation", issue.getKey());
                final ArrayNode issuesNode = mapper.createArrayNode();
                for (Issue issueList : issue.getValue()) {
                    ObjectNode item = mapper.createObjectNode();
                    item.put("key", issueList.getKey());
                    item.put("message", issueList.getMessage());
                    item.put("cause", issueList.getCause());
                    issuesNode.add(item);
                }
                operationNode.putPOJO("errors", issuesNode);
                bodyNode.add(operationNode);
            }
            header.put("fileName", fileName);
            response.putPOJO("header", header);
            response.putPOJO("body", bodyNode);
        }
        return response;
    }

    /**
     * utility method.
     * @param fieldIssues
     * @param urlIssuesList
     * @param url
     */
    protected void addToAllIssues(Map<String, List<Issue>> fieldIssues, List<Issue> urlIssuesList, String url) {
        if (!urlIssuesList.isEmpty()) {
            List<Issue> existing = fieldIssues.getOrDefault(url, new ArrayList<>());
            existing.addAll(urlIssuesList);
            fieldIssues.put(url, existing);
        }
    }

    /**
     * method to find URL issues
     *
     * @param key
     * @param domain
     * @return
     */
    protected List<Issue> getUrlIssues(String key, String domain) {
        String uri = key;
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }
        List<Issue> urlIssueList = new ArrayList<>();
        for (String inputUri : uri.split("/")) {

            if (!RESOURCE_LIST.contains(inputUri)) {
                Issue issue = null;
                if (inputUri.contains("{")) {
                    inputUri = inputUri.replace("{", "").replace("}", "");
                    issue = ResourceValidator.validateType(inputUri, domain, "property");
                    if (issue != null) {
                        urlIssueList.add(createIssueObject(issue.getKey(), "not a valid property", "url"));
                    }
                } else {
                    issue = ResourceValidator.validateType(inputUri, domain, "resource");
                    if (issue != null) {
                        urlIssueList.add(createIssueObject(issue.getKey(), "not a valid resource", "url"));
                    }
                }
            }

        }
        return urlIssueList;
    }

    /**
     * method to get the issues.
     * @param name
     * @param description
     * @param domain
     * @return
     */
    protected List<Issue> getIssues(String name, String description, String domain) {
        List<Issue> parameterIssueList = new ArrayList<>();
        VocabularyArray entryArray = VocabularyLoader.getInstance().searchArray(domain, name);
        if (null == entryArray && !SKIP_LIST.contains(name)) {
            parameterIssueList.add(new Issue(name, "invalid property."));
        } else {
            Issue nameIssue = getParameterIssues(entryArray, name);
            if (nameIssue != null) {
                parameterIssueList.add(nameIssue);
            }
            Issue descIssue = getDescriptionIssues(entryArray, name, description);
            if (descIssue != null) {
                parameterIssueList.add(descIssue);
            }
        }
        return parameterIssueList;
    }

    /**
     * returns an Issue object from DescriptionIssues
     * @param entryArray
     * @param name
     * @param description
     * @return
     */
    private Issue getDescriptionIssues(VocabularyArray entryArray, String name, String description) {
        if (!SKIP_LIST.contains(name)) {
            if(description == null) //If the description is null, add the error for that key
                return createIssueObject(name, "invalid description", "invalid description");
            else
                return validateDescription(entryArray, name, description);
        }
        return null;
    }

    /**
     * returns an Issue object from ParameterIssues
     * @param entryArray
     * @param name
     * @return
     */
    private Issue getParameterIssues(VocabularyArray entryArray, String name) {
        if (!SKIP_LIST.contains(name)) {
            return validateProperty(entryArray, name);
        }
        return null;
    }
    
    /**
     * Method to validate the property
     * @param entryArray
     * @param name
     * @return
     */
    protected Issue validateProperty(VocabularyArray entryArray, String name) {
        Iterator<VocabularyEntry> var3 = entryArray.getVocabArray().iterator();
        VocabularyEntry entry;
        do {
            if (!var3.hasNext()) {
                return null;
            }
            entry = var3.next();
        } while (entry.getKey().equals(name) || entry.getPlural().equals(name));
        return createIssueObject(name, "invalid property Use", "invalid property defined");
    }

    /**
     * method to validate the description.
     * @param entryArray
     * @param name
     * @param description
     * @return
     */
    protected Issue validateDescription(VocabularyArray entryArray, String name, String description) {
        int size = entryArray.getVocabArray().size();
        for (VocabularyEntry entry : entryArray.getVocabArray()) {
            int index = entryArray.getVocabArray().indexOf(entry);
            if ((entry.getKey().equals(name) || entry.getPlural().equals(name)) && (!(entry.getDescription().equals(description) || entry.getDescriptions().equals(description)))) {
                    if (null != entry.getDomainSpecificDescriptions() && ! entry.getDomainSpecificDescriptions().isEmpty()) {
                        for (Map.Entry<String,String> domainSpecific : entry.getDomainSpecificDescriptions().entrySet()) {
                            if (null != domainSpecific && domainSpecific.getValue() != null && !domainSpecific.getValue().contains(description)) {
                                if(index>=size-1)//If the description does not match, check for the other entry in vocab array if available, otherwise adds the error
                                    return createIssueObject(name, "missing or invalid description", "description miss match");
                            } else //If the description matches, returns null without checking other entries in the array
                                return null;
                        }
                    } else if(index>=size-1) //If the entry is the last one in the array and description does not match, adds the error
                            return createIssueObject(name, "missing or invalid description", "description miss match");
            } else
                return null;
        }
        return null;
    }
    
    /**
     * crate an issue object
     * @param name
     * @param message
     * @param cause
     * @return
     */
    protected Issue createIssueObject(String name, String message, String cause) {
        Issue iss = new Issue(name, message);
        iss.setCause(cause);
        return iss;
    }
    
    /**
     * return an dummy doamin.
     * @return
     */
    protected String getDomain() {
        return "retail";
    }
}