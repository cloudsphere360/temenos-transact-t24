/*******************************************************************************
 * Copyright © Temenos Headquarters SA 2021.  All rights reserved.
 *******************************************************************************/
package com.bct.training.fusionapi;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.temenos.irf.config.microservice.ConfigApiHelper;
import com.temenos.irf.logging.Logger;
import com.temenos.irf.logging.LoggerFactory;

/**
 * EndpointDetailsExtractor is used to generate the json file under classes
 * folder with all the available list of services in the war file with endpoint
 * details. Structure of the details stored in json file will be similar to 
 * the meta/apis response.
 * Ex: {
        "serviceDetails": [{
                "services": [{
                    "endpoints": [{
                            "method": "GET",
                            "operationId": "getDetails",
                            "uri": "/test"
                     }],
                    "service": "system.service.v1.0.0"
                 }],
                "key": "system-service-v1.0.0-swagger"
            }]
        }
 *
 */
public class EndpointDetailsExtractor {
    
    private final static Logger logger = LoggerFactory.getLogger(EndpointDetailsExtractor.class);
    public static void main(String[] args) {
        ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
        try {
            //Get the list of service xml resources from classpath using resource pattern
            Resource[] mappingLocations = patternResolver.getResources("classpath*:/services/**/*-service-v*.xml");
            JSONArray serviceDetails = new JSONArray();
            //Loop through the list of services and get the endpoint details from each service
            for (Resource resource : mappingLocations) {
                String serviceXmlContent = ConfigApiHelper.convertResourceToString(resource);
                if(!StringUtils.isBlank(serviceXmlContent)) {
                    try {
                        //Extract the service name and operationIds from each service. And add the endpoint details to jsonarray
                        ConfigApiHelper.extractEndpointDetails(serviceXmlContent, serviceDetails);
                        
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                        //If there is any error in parsing the xml, proceed to the next service without terminating the process
                        continue;
                    }
                }
            }
            writeEndpointDetailsToJson(serviceDetails);
        } catch (IOException e) {
            logger.error(e.getMessage());  
        }
    }
    
    private static void writeEndpointDetailsToJson(JSONArray serviceDetails) {
        // Data to write to the JSON file
        JSONObject servicesJson = new JSONObject();
        servicesJson.put("serviceDetails", serviceDetails);

        try {
            // Write the JSON data to a json file under the class folder in the war
            File fileDirectory = new File(EndpointDetailsExtractor.class.getProtectionDomain().getCodeSource().getLocation().getPath()+"irf-config");
            if(!fileDirectory.exists())
                fileDirectory.mkdirs();
                
            //Create apiCatalog json file under irf-config folder
            File file = new File(fileDirectory.getPath() + File.separator + "apiCatalog.json");
            if(!file.exists())
                file.createNewFile();
            //Write the extracted endpoint details to the json file 
            FileUtils.writeStringToFile(file, servicesJson.toString(), "UTF-8");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}
