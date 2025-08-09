package org.openmrs.learning;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;

/**
 * Hello World Mojo to demonstrate Maven plugin parameter patterns
 * for OpenMRS module-aware OpenAPI generation.
 */
@Mojo(name = "hello", defaultPhase = LifecyclePhase.VALIDATE)
public class HelloWorldMojo extends AbstractMojo {
    
    /**
     * The Maven project context - automatically injected
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;
    
    /**
     * Simple string parameter with default value
     */
    @Parameter(property = "greeting", defaultValue = "World")
    private String greeting;
    
    /**
     * Module package override - if not provided, will be derived from project
     */
    @Parameter(property = "modulePackage")
    private String modulePackage;
    
    /**
     * Enable verbose logging
     */
    @Parameter(property = "verbose", defaultValue = "false")
    private boolean verbose;
    
    /**
     * Output directory for generated files
     */
    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}")
    private File outputDirectory;
    
    /**
     * List of packages to scan for resources
     */
    @Parameter(property = "scanPackages")
    private List<String> scanPackages;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("=== Hello World Mojo Execution ===");
        
        // Display project context
        getLog().info("Project ArtifactId: " + project.getArtifactId());
        getLog().info("Project GroupId: " + project.getGroupId());
        getLog().info("Project Version: " + project.getVersion());
        getLog().info("Project Base Directory: " + project.getBasedir().getAbsolutePath());
        getLog().info("Build Output Directory: " + project.getBuild().getOutputDirectory());
        
        // Display parameters
        getLog().info("Greeting Parameter: " + greeting);
        getLog().info("Module Package: " + (modulePackage != null ? modulePackage : "NOT CONFIGURED"));
        getLog().info("Verbose Mode: " + verbose);
        getLog().info("Output Directory: " + outputDirectory.getAbsolutePath());
        
        if (scanPackages != null && !scanPackages.isEmpty()) {
            getLog().info("Scan Packages (" + scanPackages.size() + "):");
            for (String pkg : scanPackages) {
                getLog().info("  - " + pkg);
            }
        } else {
            getLog().info("Scan Packages: NOT CONFIGURED");
        }
        
        // Demonstrate module package derivation
        String derivedPackage = deriveModulePackage();
        getLog().info("Derived Module Package: " + derivedPackage);
        
        if (verbose) {
            getLog().info("=== VERBOSE MODE ENABLED ===");
            getLog().info("All project dependencies:");
            project.getDependencies().forEach(dep -> 
                getLog().info("  - " + dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion())
            );
        }
        
        // The actual greeting
        getLog().info("Hello " + greeting + " from Maven Plugin!");
        getLog().info("=== Hello World Mojo Complete ===");
    }
    
    /**
     * Derive module package from Maven project context
     * This is the logic you'll use in your OpenAPI plugin
     */
    private String deriveModulePackage() {
        // If explicitly configured, use that
        if (modulePackage != null && !modulePackage.trim().isEmpty()) {
            return modulePackage.trim();
        }
        
        // Otherwise derive from project structure
        String artifactId = project.getArtifactId();
        String groupId = project.getGroupId();
        
        // Handle common OpenMRS module patterns
        if (artifactId.startsWith("openmrs-module-")) {
            // openmrs-module-queue -> org.openmrs.module.queue
            String moduleName = artifactId.substring("openmrs-module-".length());
            return "org.openmrs.module." + moduleName;
        } else if (artifactId.endsWith("-omod") && groupId.equals("org.openmrs.module")) {
            // queue-omod (with groupId org.openmrs.module) -> org.openmrs.module.queue
            String moduleName = artifactId.substring(0, artifactId.length() - "-omod".length());
            return "org.openmrs.module." + moduleName;
        } else if (artifactId.endsWith("-api") && groupId.equals("org.openmrs.module")) {
            // queue-api (with groupId org.openmrs.module) -> org.openmrs.module.queue
            String moduleName = artifactId.substring(0, artifactId.length() - "-api".length());
            return "org.openmrs.module." + moduleName;
        } else if (groupId.equals("org.openmrs.module") && !artifactId.contains("-")) {
            // queue (with groupId org.openmrs.module) -> org.openmrs.module.queue
            return "org.openmrs.module." + artifactId;
        } else if (groupId.startsWith("org.openmrs.module.")) {
            // org.openmrs.module.queue -> org.openmrs.module.queue
            return groupId;
        }
        
        // Fallback: use groupId as-is
        return groupId;
    }
}
