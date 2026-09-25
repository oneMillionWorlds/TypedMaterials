package com.onemillionworlds.tasks;

import com.onemillionworlds.utilities.MaterialTyper;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@DisableCachingByDefault(because = "Generates source files directly into the project; quicker to regenerate than to fetch from a cache")
public abstract class TypedJarMaterials extends DefaultTask{

    @Inject
    public TypedJarMaterials(ProjectLayout layout){
        getBuiltFilesRecordFile().convention(layout.getBuildDirectory().file("typedMaterials/" + getName()));
    }

    @Input
    public abstract Property<String> getOutputPackage();

    @Internal
    public abstract DirectoryProperty getOutputSourcesRoot();

    @Input
    public abstract Property<String> getJarFilterRegex();

    /**
     * The classpath whose jars will be searched for materials.
     * Jar names are significant (they are used for filtering), so this is NAME_ONLY rather than a @Classpath
     */
    @InputFiles
    @PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract ConfigurableFileCollection getRuntimeClasspath();

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
    public void createTypedMaterials(){

        Pattern pattern = Pattern.compile(getJarFilterRegex().get());
        String outputPackage = getOutputPackage().get();
        File outputDirectory = getOutputDirectory().get().getAsFile();
        File outputDirectoryWrapper = new File(outputDirectory, "wrapper");
        outputDirectoryWrapper.mkdirs();

        Set<File> resolve = getRuntimeClasspath().getFiles();

        List<String> fullyQualifiedMaterialClasses = new ArrayList<>();

        resolve.forEach(file -> {
            if (file.getName().endsWith(".jar") && pattern.matcher(file.getName()).matches()) {
                try (ZipInputStream zip = new ZipInputStream(new FileInputStream(file))) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        if (entry.getName().endsWith(".j3md")) {
                            StringBuilder content = new StringBuilder();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(zip));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                content.append(line).append(System.lineSeparator());
                            }

                            String fullDefName = entry.getName().replace('\\', '/').replaceAll(".*/resources/", "");
                            String className = toUpperCamlCase(fullDefName.replace(".j3md", "").replaceAll("^.*/", "")) + "Material";
                            String originComment = fullDefName + " in library " + file.getName().replace(".jar", "");

                            String fileContentsMaterial = MaterialTyper.createMaterialClassFile(fullDefName, className, outputPackage, content.toString(), originComment, false);
                            String fileContentsWrapper = MaterialTyper.createMaterialClassFile(fullDefName, className, outputPackage, content.toString(), originComment, true);

                            File destination = new File(outputDirectory, className + ".java");
                            Files.writeString(destination.toPath(), fileContentsMaterial);

                            File destinationWrapper = new File(outputDirectoryWrapper, className + "Wrapper.java");
                            Files.writeString(destinationWrapper.toPath(), fileContentsWrapper);

                            fullyQualifiedMaterialClasses.add(outputPackage + "." + className);
                        }
                        zip.closeEntry();
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error processing JAR: " + file + e.getMessage(), e);
                }
            }
        });

        if (fullyQualifiedMaterialClasses.isEmpty()){
            getLogger().warn("No materials found in JARs matching filter: " + getJarFilterRegex().get() + ", all jars: " + resolve);
        }

        try {
            Files.writeString(getBuiltFilesRecordFile().get().getAsFile().toPath(), String.join("\n", fullyQualifiedMaterialClasses));
        } catch (Exception e) {
            throw new RuntimeException("Error writing record of generation: " + getName() + ". " + e.getMessage(), e);
        }
    }

    private static String toUpperCamlCase(String upperCamelCase){
        return upperCamelCase.substring(0, 1).toUpperCase() + upperCamelCase.substring(1);
    }

}
