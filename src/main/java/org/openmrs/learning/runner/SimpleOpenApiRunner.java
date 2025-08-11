package org.openmrs.learning.runner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Simple OpenAPI spec generator runner (no OpenMRS dependencies yet)
 * This will prove the forked JVM + main JAR approach works
 */
public class SimpleOpenApiRunner {
    
    public static void main(String[] args) {
        try {
            new SimpleOpenApiRunner().execute();
            System.exit(0);
        } catch (Exception e) {
            System.err.println("OpenAPI generation failed: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
    
    public void execute() throws IOException {
        System.out.println("[Runner] Starting simple OpenAPI generation...");
        
        // Get parameters from system properties
        String modulePackage = System.getProperty("modulePackage");
        String outputFile = System.getProperty("outputFile");
        String projectName = System.getProperty("projectName", "Unknown Project");
        
        System.out.println("[Runner] Module package: " + modulePackage);
        System.out.println("[Runner] Output file: " + outputFile);
        System.out.println("[Runner] Project name: " + projectName);
        
        // Generate simple OpenAPI spec
        String openApiSpec = generateSimpleSpec(modulePackage, projectName);
        
        // Write to file
        File file = new File(outputFile != null ? outputFile : "simple-openapi-spec.json");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(openApiSpec);
        }
        
        System.out.println("[Runner] Simple OpenAPI spec written to: " + file.getAbsolutePath());
        System.out.println("[Runner] Generation completed successfully!");
    }
    
    private String generateSimpleSpec(String modulePackage, String projectName) {
        // Generate a simple but valid OpenAPI spec using string concatenation
        return "{\n" +
               "  \"openapi\": \"3.0.0\",\n" +
               "  \"info\": {\n" +
               "    \"title\": \"" + projectName + " API\",\n" +
               "    \"version\": \"1.0.0\",\n" +
               "    \"description\": \"Generated OpenAPI spec for module package: " + modulePackage + "\"\n" +
               "  },\n" +
               "  \"paths\": {\n" +
               "    \"/example\": {\n" +
               "      \"get\": {\n" +
               "        \"summary\": \"Example endpoint for " + modulePackage + "\",\n" +
               "        \"responses\": {\n" +
               "          \"200\": {\n" +
               "            \"description\": \"Success\",\n" +
               "            \"content\": {\n" +
               "              \"application/json\": {\n" +
               "                \"schema\": {\n" +
               "                  \"type\": \"object\",\n" +
               "                  \"properties\": {\n" +
               "                    \"message\": {\n" +
               "                      \"type\": \"string\",\n" +
               "                      \"example\": \"Hello from " + modulePackage + "!\"\n" +
               "                    }\n" +
               "                  }\n" +
               "                }\n" +
               "              }\n" +
               "            }\n" +
               "          }\n" +
               "        }\n" +
               "      }\n" +
               "    }\n" +
               "  }\n" +
               "}";
    }
}
