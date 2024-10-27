package com.onemillionworlds;

import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class FunctionalTestBase{

    @TempDir
    File projectDir;
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
        return new File(projectDir,  "src/main/generatedtypedmaterials/java");
    }

    protected File getGeneratedResourcesFilesRoot() {
        return new File(projectDir,  "src/main/generatedtypedmaterials/resources");
    }

    protected File getGeneratedJavaFile(String pathRelativeToRoot) {
        return new File(getGeneratedJavaFilesRoot(),  pathRelativeToRoot);
    }

    protected File getGeneratedResourcesFile(String pathRelativeToRoot) {
        return new File(getGeneratedResourcesFilesRoot(),  pathRelativeToRoot);
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
