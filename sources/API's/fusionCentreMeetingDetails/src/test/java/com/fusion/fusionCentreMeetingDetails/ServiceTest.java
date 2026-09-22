/**
 * ﻿    Copyright © Temenos Headquarters SA 2018.  All rights reserved.
 */
package com.fusion.fusionCentreMeetingDetails;

import org.junit.Test;

import com.temenos.irf.logging.Logger;
import com.temenos.irf.logging.LoggerFactory;
import com.temenos.irf.oas.validator.OasSchemaValidator;

import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for simple service.
 */
public class ServiceTest

{
    @Test
    public void testSwaggerFiles() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource("api-docs");
        // Check if api-docs folder is present
        if (url != null) {
            String path = url.getPath();
            List<String> errorlist = new ArrayList<>();

            // Get list of all swagger files in api-docs folder
            File[] files = new File(path).listFiles();
            for (File f : files) {
                Swagger swagger = new SwaggerParser().read(f.getAbsolutePath());
                if (null == swagger) {
                    // once check if it is OpenAPI 3.0 swagger
                    OpenAPI openAPI = new OpenAPIV3Parser().read(f.getAbsolutePath());
                    if (null != openAPI) {
                        System.out.println("Validating OpenAPI 3.0 swagger :  " + f.getName());
                        OasSchemaValidator validator = new OasSchemaValidator(f.getAbsolutePath());
                        errorlist = validator.validateOpenAPi(openAPI);
                        if (errorlist != null && !errorlist.isEmpty())
                            System.out.println("Errors in swagger file " + f.getName() + ":" + errorlist);
                    } else {
                        System.out.println("Invalid swagger file: " + f.getName());
                    }
                } else {
                    System.out.println("Validating swagger 2.0 :  " + f.getName());
                    OasSchemaValidator validator = new OasSchemaValidator(f.getAbsolutePath());
                    errorlist = validator.validateSwagger(swagger);
                    if (errorlist != null && !errorlist.isEmpty())
                        System.out.println("Errors in swagger file " + f.getName() + ":" + errorlist);
                    // assertion check has been commented out till the
                    // vocabulary is build completely.
                    // assertTrue(errorlist.isEmpty());
                }
            }
        }
    }
}