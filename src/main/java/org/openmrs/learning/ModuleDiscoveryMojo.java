package org.openmrs.learning;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Module Detection and Package Discovery Mojo
 * 
 * This plugin demonstrates how to:
 * 1. Auto-detect which OpenMRS module it's running in
 * 2. Discover all packages available in the module and its dependencies
 * 3. Run automatically during Maven build when added to POM
 */
@Mojo(name = "discover", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class ModuleDiscoveryMojo extends AbstractMojo {
    
    /**
     * The Maven project context - automatically injected
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;
    
    /**
     * Override module package detection if needed
     */
    @Parameter(property = "modulePackage")
    private String modulePackage;
    
    /**
     * Specific packages to scan for resources (overrides auto-detection)
     */
    @Parameter(property = "scanPackages")
    private List<String> scanPackages;
    
    /**
     * Enable verbose package discovery
     */
    @Parameter(property = "verbose", defaultValue = "false")
    private boolean verbose;
    
    /**
     * Show all packages or just module-related ones
     */
    @Parameter(property = "showAllPackages", defaultValue = "false")
    private boolean showAllPackages;
    
    /**
     * Generate OpenAPI specification using forked JVM approach
     */
    @Parameter(property = "generateOpenApi", defaultValue = "false")
    private boolean generateOpenApi;
    
    /**
     * Output file for OpenAPI specification
     */
    @Parameter(property = "openApiOutputFile", defaultValue = "${project.build.directory}/openapi-spec.json")
    private String openApiOutputFile;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("=== OpenMRS Module Discovery ===");
        
        // 1. Detect module information
        detectModuleInfo();
        
        // 2. Discover packages in classpath
        discoverPackages();
        
        // 3. Show build context
        showBuildContext();
        
        // 4. Generate OpenAPI spec if requested
        if (generateOpenApi) {
            generateOpenApiSpec();
        }
        
        getLog().info("=== Module Discovery Complete ===");
    }
    
    /**
     * Detect and display module information
     */
    private void detectModuleInfo() {
        getLog().info("--- Module Detection ---");
        getLog().info("Project ArtifactId: " + project.getArtifactId());
        getLog().info("Project GroupId: " + project.getGroupId());
        getLog().info("Project Version: " + project.getVersion());
        getLog().info("Project Base Directory: " + project.getBasedir().getAbsolutePath());
        
        String derivedPackage = deriveModulePackage();
        getLog().info("Derived Module Package: " + derivedPackage);
        
        if (modulePackage != null && !modulePackage.equals(derivedPackage)) {
            getLog().info("Override Module Package: " + modulePackage);
        }
        
        // Show scan package strategy
        List<String> effectiveScanPackages = getEffectiveScanPackages();
        getLog().info("Effective Scan Packages Strategy:");
        if (scanPackages != null && !scanPackages.isEmpty()) {
            getLog().info("  Using USER-CONFIGURED packages (" + scanPackages.size() + "):");
            for (String pkg : scanPackages) {
                getLog().info("    " + pkg);
            }
        } else {
            getLog().info("  Using AUTO-DETECTED packages (" + effectiveScanPackages.size() + "):");
            for (String pkg : effectiveScanPackages) {
                getLog().info("    " + pkg);
            }
        }
        
        // Check if this looks like an OpenMRS module
        boolean isOpenMRSModule = derivedPackage.startsWith("org.openmrs.module.");
        getLog().info("Is OpenMRS Module: " + isOpenMRSModule);
        
        // Check for config.xml (module descriptor)
        File configXml = new File(project.getBasedir(), "src/main/resources/config.xml");
        getLog().info("Has config.xml: " + configXml.exists());
    }
    
    /**
     * Discover packages available in the classpath
     */
    private void discoverPackages() {
        getLog().info("--- Package Discovery ---");
        
        try {
            Set<String> allPackages = new TreeSet<>();
            Set<String> modulePackages = new TreeSet<>();
            String targetModulePackage = getEffectiveModulePackage();
            
            // Scan compiled classes directory
            File classesDir = new File(project.getBuild().getOutputDirectory());
            if (classesDir.exists()) {
                scanDirectory(classesDir, "", allPackages, modulePackages, targetModulePackage);
            }
            
            // Scan dependency JARs
            for (org.apache.maven.model.Dependency dep : project.getDependencies()) {
                if (verbose) {
                    getLog().info("Scanning dependency: " + dep.getGroupId() + ":" + dep.getArtifactId());
                }
                // Note: In a real implementation, you'd resolve the JAR files
                // For now, we'll just show what dependencies are available
            }
            
            // Display results
            getLog().info("Total packages discovered: " + allPackages.size());
            getLog().info("Module-related packages: " + modulePackages.size());
            
            if (showAllPackages && verbose) {
                getLog().info("All packages:");
                for (String pkg : allPackages) {
                    getLog().info("  " + pkg);
                }
            }
            
            getLog().info("Module packages in " + targetModulePackage + ":");
            for (String pkg : modulePackages) {
                getLog().info("  " + pkg);
            }
            
        } catch (Exception e) {
            getLog().warn("Error during package discovery: " + e.getMessage());
        }
    }
    
    /**
     * Scan a directory for packages
     */
    private void scanDirectory(File dir, String packagePath, Set<String> allPackages, 
                              Set<String> modulePackages, String targetModulePackage) {
        if (!dir.exists() || !dir.isDirectory()) return;
        
        File[] files = dir.listFiles();
        if (files == null) return;
        
        boolean hasClasses = false;
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".class")) {
                hasClasses = true;
                break;
            }
        }
        
        if (hasClasses && !packagePath.isEmpty()) {
            allPackages.add(packagePath);
            if (packagePath.startsWith(targetModulePackage)) {
                modulePackages.add(packagePath);
            }
        }
        
        // Recurse into subdirectories
        for (File file : files) {
            if (file.isDirectory()) {
                String subPackage = packagePath.isEmpty() ? file.getName() : packagePath + "." + file.getName();
                scanDirectory(file, subPackage, allPackages, modulePackages, targetModulePackage);
            }
        }
    }
    
    /**
     * Show build context information
     */
    private void showBuildContext() {
        if (!verbose) return;
        
        getLog().info("--- Build Context ---");
        getLog().info("Build Directory: " + project.getBuild().getDirectory());
        getLog().info("Output Directory: " + project.getBuild().getOutputDirectory());
        getLog().info("Source Directory: " + project.getBuild().getSourceDirectory());
        
        getLog().info("Dependencies (" + project.getDependencies().size() + "):");
        for (org.apache.maven.model.Dependency dep : project.getDependencies()) {
            String scope = dep.getScope() != null ? dep.getScope() : "compile";
            getLog().info("  " + dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion() + " (" + scope + ")");
        }
    }
    
    /**
     * Get the effective module package (override or derived)
     */
    private String getEffectiveModulePackage() {
        return modulePackage != null && !modulePackage.trim().isEmpty() 
            ? modulePackage.trim() 
            : deriveModulePackage();
    }
    
    /**
     * Derive module package from Maven project context
     * This uses the same logic from HelloWorldMojo
     */
    private String deriveModulePackage() {
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
    
    /**
     * Get effective scan packages (user-configured or auto-detected)
     */
    private List<String> getEffectiveScanPackages() {
        if (scanPackages != null && !scanPackages.isEmpty()) {
            // User specified explicit packages
            return scanPackages;
        } else {
            // Auto-derive from module detection
            String modulePackage = getEffectiveModulePackage();
            return java.util.Arrays.asList(
                modulePackage + ".web.resources",
                modulePackage + ".api",
                modulePackage + ".web"
            );
        }
    }
    
    /**
     * Generate OpenAPI specification using forked JVM approach
     */
    private void generateOpenApiSpec() throws MojoExecutionException {
        getLog().info("=== Generating OpenAPI Specification ===");
        
        try {
            // Find the plugin JAR containing SimpleOpenApiRunner
            String pluginJarPath = findPluginJar();
            getLog().info("Using plugin JAR: " + pluginJarPath);
            
            // Build classpath for the target module
            List<String> classpath = buildClasspath();
            
            // Execute SimpleOpenApiRunner in forked JVM
            executeOpenApiRunner(pluginJarPath, classpath);
            
            getLog().info("OpenAPI specification generated successfully");
            
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate OpenAPI specification", e);
        }
    }
    
    /**
     * Find the plugin JAR containing SimpleOpenApiRunner
     */
    private String findPluginJar() throws MojoExecutionException {
        // Look for the plugin in Maven's plugin artifacts
        @SuppressWarnings("unchecked")
        Map<String, Artifact> pluginArtifacts = (Map<String, Artifact>) getPluginContext().get("plugin.artifactMap");
        
        getLog().info("DEBUG: Plugin context keys: " + getPluginContext().keySet());
        
        if (pluginArtifacts != null) {
            getLog().info("DEBUG: Found plugin.artifactMap with " + pluginArtifacts.size() + " artifacts");
            for (Map.Entry<String, Artifact> entry : pluginArtifacts.entrySet()) {
                getLog().info("DEBUG: Plugin artifact: " + entry.getKey() + " -> " + entry.getValue());
                if (entry.getValue() != null && entry.getValue().getFile() != null) {
                    getLog().info("DEBUG: Artifact file: " + entry.getValue().getFile().getAbsolutePath());
                }
            }
            
            for (Artifact artifact : pluginArtifacts.values()) {
                if (artifact.getGroupId().equals("org.openmrs.learning") && 
                    artifact.getArtifactId().equals("maven-plugin-parameter-test")) {
                    String jarPath = artifact.getFile().getAbsolutePath();
                    getLog().info("Found plugin JAR via artifactMap: " + jarPath);
                    return jarPath;
                }
            }
        } else {
            getLog().warn("DEBUG: plugin.artifactMap is null");
        }
        
        // Fallback: try to locate from Maven local repository
        String userHome = System.getProperty("user.home");
        String jarPath = userHome + "/.m2/repository/org/openmrs/learning/maven-plugin-parameter-test/1.0.0-SNAPSHOT/maven-plugin-parameter-test-1.0.0-SNAPSHOT.jar";
        File jarFile = new File(jarPath);
        
        if (jarFile.exists()) {
            getLog().info("Found plugin JAR via fallback: " + jarPath);
            return jarPath;
        }
        
        throw new MojoExecutionException("Could not locate plugin JAR containing SimpleOpenApiRunner");
    }
    
    /**
     * Build classpath for the target module
     */
    private List<String> buildClasspath() {
        List<String> classpath = new ArrayList<>();
        
        // Add compiled classes
        classpath.add(project.getBuild().getOutputDirectory());
        
        // Add dependencies
        @SuppressWarnings("unchecked")
        Set<Artifact> artifacts = project.getArtifacts();
        for (Artifact artifact : artifacts) {
            if (artifact.getFile() != null && artifact.getFile().exists()) {
                classpath.add(artifact.getFile().getAbsolutePath());
            }
        }
        
        getLog().info("Built classpath with " + classpath.size() + " entries");
        return classpath;
    }
    
    /**
     * Execute SimpleOpenApiRunner in forked JVM
     */
    private void executeOpenApiRunner(String pluginJarPath, List<String> classpath) throws Exception {
        List<String> command = new ArrayList<>();
        
        // Java executable
        String javaHome = System.getProperty("java.home");
        String javaExecutable = javaHome + File.separator + "bin" + File.separator + "java";
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            javaExecutable += ".exe";
        }
        command.add(javaExecutable);
        
        // Classpath: plugin JAR first, then module classpath
        StringBuilder cp = new StringBuilder();
        cp.append(pluginJarPath);
        for (String path : classpath) {
            cp.append(File.pathSeparator).append(path);
        }
        command.add("-cp");
        command.add(cp.toString());
        
        // System properties for SimpleOpenApiRunner
        String modulePackage = getEffectiveModulePackage();
        command.add("-DmodulePackage=" + modulePackage);
        command.add("-DoutputFile=" + openApiOutputFile);
        command.add("-DprojectName=" + project.getName());
        
        // Main class
        command.add("org.openmrs.learning.runner.SimpleOpenApiRunner");
        
        getLog().info("Executing command: " + String.join(" ", command));
        
        // Execute the process
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(project.getBasedir());
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        // Read output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                getLog().info("[OpenAPI] " + line);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new MojoExecutionException("OpenAPI generation failed with exit code: " + exitCode);
        }
    }
}
