package com.onemillionworlds.tasks;

import com.onemillionworlds.utilities.FactoryBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * This is a task that looks at all the other material generating tasks and records all materials in a single factory
 * <p>
 * (Mostly this task is used internally by the plugin)
 * </p>
 */
public class MaterialFactoryTask extends DefaultTask{

    File outputSourcesRoot;

    private String fullyQuailfiedOutputClass;

    public void setOutputSourcesRoot(File outputSourcesRoot){
        this.outputSourcesRoot = outputSourcesRoot;
    }

    @Input
    public String getFullyQuailfiedOutputClass(){
        return fullyQuailfiedOutputClass;
    }

    public void setFullyQualifiedOutputClass(String fullyQuailfiedOutputClass){
        this.fullyQuailfiedOutputClass = fullyQuailfiedOutputClass;
    }

    @TaskAction
    public void createMaterialsFactory(){
        //determine all the fullyQualified material classes

        List<String> fullyQualifiedMaterialClasses = new ArrayList<>();
        for(File file : getMaterialResortDirectory().listFiles()){
            //read the file and add the fully qualified class name to the list
            try (Stream<String> stream = Files.lines(file.toPath())) {
                stream.forEach(fullyQualifiedMaterialClasses::add);
            } catch(IOException e){
                throw new RuntimeException(e);
            }
        }

        String content = FactoryBuilder.createFactoryClassFile(
                getDestinationPackage(),
                getClassName(),
                fullyQualifiedMaterialClasses
        );

        try{
            Files.writeString(getOutputFile().toPath(), content);
        } catch(IOException e){
            throw new RuntimeException(e);
        }

    }

    @InputDirectory
    public File getMaterialResortDirectory(){
        return getProject().getLayout().getBuildDirectory().dir("typedMaterials").get().getAsFile();
    }

    @OutputFile
    public File getOutputFile(){
        String packageFolder = getDestinationPackage()
                .replace(".", "/");

        return new File(new File(outputSourcesRoot, packageFolder), getClassName()+ ".java");
    }

    private String getDestinationPackage(){
        return fullyQuailfiedOutputClass
                .replaceAll("\\.[A-Za-z0-9_]+$", "");
    }

    private String getClassName(){
        return fullyQuailfiedOutputClass
                .replaceAll(".*\\.", "");
    }
}
