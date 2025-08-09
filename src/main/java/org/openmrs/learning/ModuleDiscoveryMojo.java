package org.openmrs.learning;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
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
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("=== OpenMRS Module Discovery ===");
        
        // 1. Detect module information
        detectModuleInfo();
        
        // 2. Discover packages in classpath
        discoverPackages();
        
        // 3. Show build context
        showBuildContext();
        
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
}
