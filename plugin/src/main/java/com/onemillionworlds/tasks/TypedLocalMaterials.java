package com.onemillionworlds.tasks;

import com.onemillionworlds.utilities.MaterialTyper;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TypedLocalMaterials extends DefaultTask{

    File inputDirectory;

    String outputPackage;

    File outputSourcesRoot;

    String resourcesDir = "resources";

    @TaskAction
    public void createTypedMaterials() throws IOException{
        searchAndCreateClasses(inputDirectory);
    }

    private void searchAndCreateClasses(File file) throws IOException{
        if (file.isDirectory()){
            for( File fileToProcess : file.listFiles()){
                searchAndCreateClasses(fileToProcess);
            }
        }else{
            if (file.getPath().endsWith(".j3md")){
                File outputDirectory = getOutputDirectory();


                String prePathRegex = ".*/" + resourcesDir + "/";

                String fullDefName = file.getPath().replace('\\', '/').replaceAll(prePathRegex, "");
                String className = file.toPath().getFileName().toString().replace(".j3md", "") + "Material";
                String originComment = fullDefName + " in local resources";

                String fileContents = MaterialTyper.createMaterialClassFile(fullDefName, className, outputPackage, Files.readString(file.toPath()), originComment);
                File destination = new File(outputDirectory, className + ".java");
                Files.writeString(destination.toPath(), fileContents);
            }
        }
    }

    @Input
    public String getOutputPackage(){
        return outputPackage;
    }

    @InputDirectory
    public File getInputDirectory(){
        return inputDirectory;
    }

    @OutputDirectory
    public File getOutputDirectory(){
        String packageFolder = outputPackage.replace(".", "/");
        return new File(outputSourcesRoot, packageFolder);
    }

    @Input
    public String getResourcesDir(){
        return resourcesDir;
    }

    public void setResourcesDir(String resourcesDir){
        this.resourcesDir = resourcesDir;
    }

    public void setOutputSourcesRoot(File outputSourcesRoot){
        this.outputSourcesRoot = outputSourcesRoot;
    }

    public void setOutputPackage(String outputPackage){
        this.outputPackage = outputPackage;
    }

    public void setInputDirectory(File inputDirectory){
        this.inputDirectory = inputDirectory;
    }


}
