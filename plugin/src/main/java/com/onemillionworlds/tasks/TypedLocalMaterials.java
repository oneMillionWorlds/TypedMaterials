package com.onemillionworlds.tasks;

import com.onemillionworlds.utilities.MaterialTyper;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class TypedLocalMaterials extends DefaultTask{

    File inputDirectory;

    String outputPackage;

    File outputSourcesRoot;

    String resourcesDir = "resources";

    @TaskAction
    public void createTypedMaterials() throws IOException{
        List<String> fullyQualifiedMaterialClasses = new ArrayList<>();

        searchAndCreateClasses(inputDirectory, fullyQualifiedMaterialClasses);

        try {
            Files.writeString(getBuiltFilesRecordFile().toPath(), String.join("\n", fullyQualifiedMaterialClasses));
        } catch (Exception e) {
            throw new RuntimeException("Error writing record of generation: " + getName() + ". " + e.getMessage(), e);
        }
    }

    private void searchAndCreateClasses(File file, List<String> fullyQualifiedMaterialClasses_out) throws IOException{
        if (file.isDirectory()){
            for( File fileToProcess : file.listFiles()){
                searchAndCreateClasses(fileToProcess, fullyQualifiedMaterialClasses_out);
            }
        }else{
            if (file.getPath().endsWith(".j3md")){
                File outputDirectory = getOutputDirectory();

                String prePathRegex = ".*/" + resourcesDir + "/";

                String fullDefName = file.getPath().replace('\\', '/').replaceAll(prePathRegex, "");
                String className = file.toPath().getFileName().toString().replace(".j3md", "") + "Material";
                String originComment = fullDefName + " in local resources";

                String fileContentsMaterial = MaterialTyper.createMaterialClassFile(fullDefName, className, outputPackage, Files.readString(file.toPath()), originComment, false);
                String fileContentsWrapper = MaterialTyper.createMaterialClassFile(fullDefName, className, outputPackage, Files.readString(file.toPath()), originComment, true);

                File destinationMaterial = new File(outputDirectory, className + ".java");
                Files.writeString(destinationMaterial.toPath(), fileContentsMaterial);

                File destinationWrapper = new File(new File(outputDirectory, "wrapper"),className + "Wrapper.java");
                Files.writeString(destinationWrapper.toPath(), fileContentsWrapper);

                fullyQualifiedMaterialClasses_out.add(outputPackage + "." + className);
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

    @OutputFile
    public File getBuiltFilesRecordFile(){
        return new File(getProject().getLayout().getBuildDirectory().dir("typedMaterials").get().getAsFile(), getName());
    }

    @OutputDirectory
    public File getOutputDirectoryWrapper(){
        return new File(getOutputDirectory(), "wrapper");
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
