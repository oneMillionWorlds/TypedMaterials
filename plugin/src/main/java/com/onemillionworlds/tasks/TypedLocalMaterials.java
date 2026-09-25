package com.onemillionworlds.tasks;

import com.onemillionworlds.utilities.MaterialTyper;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@DisableCachingByDefault(because = "Generates source files directly into the project; quicker to regenerate than to fetch from a cache")
public abstract class TypedLocalMaterials extends DefaultTask{

    @Inject
    public TypedLocalMaterials(ProjectLayout layout){
        getResourcesDir().convention("resources");
        getBuiltFilesRecordFile().convention(layout.getBuildDirectory().file("typedMaterials/" + getName()));
    }

    @Input
    public abstract Property<String> getOutputPackage();

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getInputDirectory();

    @Internal
    public abstract DirectoryProperty getOutputSourcesRoot();

    /**
     * The name of the resources directory, everything after this in a material's path is its asset path.
     * E.g. "resources" or "assets"
     */
    @Input
    public abstract Property<String> getResourcesDir();

    /**
     * A file listing the fully qualified material classes generated, used by the material factory
     */
    @OutputFile
    public abstract RegularFileProperty getBuiltFilesRecordFile();

    @OutputDirectory
    public Provider<Directory> getOutputDirectory(){
        return getOutputSourcesRoot().dir(getOutputPackage().map(outputPackage -> outputPackage.replace(".", "/")));
    }

    @TaskAction
    public void createTypedMaterials() throws IOException{
        List<String> fullyQualifiedMaterialClasses = new ArrayList<>();

        File outputDirectory = getOutputDirectory().get().getAsFile();
        new File(outputDirectory, "wrapper").mkdirs();

        searchAndCreateClasses(getInputDirectory().get().getAsFile(), outputDirectory, fullyQualifiedMaterialClasses);

        try {
            Files.writeString(getBuiltFilesRecordFile().get().getAsFile().toPath(), String.join("\n", fullyQualifiedMaterialClasses));
        } catch (Exception e) {
            throw new RuntimeException("Error writing record of generation: " + getName() + ". " + e.getMessage(), e);
        }
    }

    private void searchAndCreateClasses(File file, File outputDirectory, List<String> fullyQualifiedMaterialClasses_out) throws IOException{
        if (file.isDirectory()){
            for( File fileToProcess : file.listFiles()){
                searchAndCreateClasses(fileToProcess, outputDirectory, fullyQualifiedMaterialClasses_out);
            }
        }else{
            if (file.getPath().endsWith(".j3md")){
                String outputPackage = getOutputPackage().get();
                String prePathRegex = ".*/" + getResourcesDir().get() + "/";

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
}
