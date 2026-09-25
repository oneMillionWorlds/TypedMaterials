package com.onemillionworlds;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class FunctionalTestBase{

    @TempDir
    File projectDir;

    /**
     * All functional tests run with the configuration cache on (and failing on any problems) to ensure the plugin
     * remains compatible with it
     */
    @BeforeEach
    void enableConfigurationCache() throws IOException{
        writeString(new File(projectDir, "gradle.properties"), """
                org.gradle.configuration-cache=true
                org.gradle.configuration-cache.problems=fail
                """);
    }

    protected File getBuildFile() {
        return new File(projectDir, "build.gradle");
    }

    protected File getBuildFile(String moduleName) {
        return new File(projectDir, moduleName + "/build.gradle");
    }

    protected File getSettingsFile() {
        return new File(projectDir, "settings.gradle");
    }

    protected File getGeneratedJavaFilesRoot() {
        return new File(projectDir,  "build/generated/sources/typedMaterials");
    }

    protected File getGeneratedResourcesFilesRoot() {
        return new File(projectDir,  "build/generated/resources/typedMaterials");
    }

    /**
     * @param taskName the task that generated the file (each task generates into its own directory)
     */
    protected File getGeneratedJavaFile(String taskName, String pathRelativeToRoot) {
        return new File(getGeneratedJavaFilesRoot(),  taskName + "/" + pathRelativeToRoot);
    }

    /**
     * @param taskName the task that generated the file (each task generates into its own directory)
     */
    protected File getGeneratedResourcesFile(String taskName, String pathRelativeToRoot) {
        return new File(getGeneratedResourcesFilesRoot(),  taskName + "/" + pathRelativeToRoot);
    }

    protected File localResourcesRoot_resourcesStyle(){
        File directory = new File(projectDir,  "src/main/resources");
        directory.mkdirs();
        return directory;
    }

    protected File localResourcesRoot_resourcesStyle(String moduleName){
        File directory = new File(projectDir,  moduleName + "/src/main/resources");
        directory.mkdirs();
        return directory;
    }

    protected File localMaterialsRoot_resourcesStyle() {
        File directory = new File(localResourcesRoot_resourcesStyle(),  "/MatDefs");
        directory.mkdirs();
        return directory;
    }

    protected File localMaterialsRoot_assetsStyle() {
        File directory = new File(projectDir,  "Assets/MatDefs");
        directory.mkdirs();
        return directory;
    }

    protected void writeString(File file, String string) throws IOException{
        file.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(file)) {
            writer.write(string);
        }
    }
}
