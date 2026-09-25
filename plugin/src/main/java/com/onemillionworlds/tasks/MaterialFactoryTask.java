package com.onemillionworlds.tasks;

import com.onemillionworlds.utilities.FactoryBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * This is a task that looks at all the other material generating tasks and records all materials in a single factory
 * <p>
 * (Mostly this task is used internally by the plugin)
 * </p>
 */
@DisableCachingByDefault(because = "Generation is cheap; quicker to regenerate than to fetch from a cache")
public abstract class MaterialFactoryTask extends DefaultTask{

    /**
     * The directory this task generates the factory into. It should not be shared with any
     * other task, so that Gradle can remove stale outputs (e.g. from deleted materials)
     */
    @OutputDirectory
    public abstract DirectoryProperty getOutputSourcesRoot();

    @Input
    public abstract Property<String> getFullyQualifiedOutputClass();

    /**
     * The record files produced by the material generating tasks, each listing the fully qualified material classes
     * that task produced
     */
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getMaterialRecordFiles();

    @TaskAction
    public void createMaterialsFactory(){
        //determine all the fullyQualified material classes

        List<String> fullyQualifiedMaterialClasses = new ArrayList<>();
        for(File file : getMaterialRecordFiles()){
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
            Path destination = getOutputFile().get().getAsFile().toPath();
            Files.createDirectories(destination.getParent());
            Files.writeString(destination, content);
        } catch(IOException e){
            throw new RuntimeException(e);
        }

    }

    @Internal
    public Provider<RegularFile> getOutputFile(){
        return getOutputSourcesRoot().file(getFullyQualifiedOutputClass().map(fqcn -> fqcn.replace(".", "/") + ".java"));
    }

    private String getDestinationPackage(){
        return getFullyQualifiedOutputClass().get()
                .replaceAll("\\.[A-Za-z0-9_]+$", "");
    }

    private String getClassName(){
        return getFullyQualifiedOutputClass().get()
                .replaceAll(".*\\.", "");
    }
}
