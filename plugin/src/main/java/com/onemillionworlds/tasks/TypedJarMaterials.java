package com.onemillionworlds.tasks;

import com.onemillionworlds.utilities.MaterialTyper;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

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

public class TypedJarMaterials extends DefaultTask{

    String outputPackage;

    File outputSourcesRoot;

    String jarFilterRegex;

    @TaskAction
    public void createTypedMaterials(){

        Pattern pattern = Pattern.compile(jarFilterRegex);

        Configuration test = getProject().getConfigurations().getByName("runtimeClasspath");
        Set<File> resolve = test.resolve();

        List<String> fullyQualifiedMaterialClasses = new ArrayList<>();

        resolve.forEach(file -> {
            if (file.getName().endsWith(".jar") && pattern.matcher(file.getName()).matches()) {
                try (ZipInputStream zip = new ZipInputStream(new FileInputStream(file))) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        // Here you can filter for specific resources or types of files
                        if (entry.getName().endsWith(".j3md")) { // Example filter
                            // Use a StringBuilder to collect the file content
                            StringBuilder content = new StringBuilder();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(zip));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                content.append(line).append(System.lineSeparator());
                            }

                            File outputDirectory = getOutputDirectory();
                            String fullDefName = entry.getName().replace('\\', '/').replaceAll(".*/resources/", "");
                            String className = toUpperCamlCase(fullDefName.replace(".j3md", "").replaceAll("^.*/", "")) + "Material";
                            String originComment = fullDefName + " in library " + file.getName().replace(".jar", "");

                            String fileContentsMaterial = MaterialTyper.createMaterialClassFile(fullDefName, className, outputPackage, content.toString(), originComment, false);
                            String fileContentsWrapper = MaterialTyper.createMaterialClassFile(fullDefName, className, outputPackage, content.toString(), originComment, true);

                            File destination = new File(outputDirectory, className + ".java");
                            Files.writeString(destination.toPath(), fileContentsMaterial);

                            File destinationWrapper = new File(new File(outputDirectory, "wrapper"),className + "Wrapper.java");
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

        try {
            Files.writeString(getBuiltFilesRecordFile().toPath(), String.join("\n", fullyQualifiedMaterialClasses));
        } catch (Exception e) {
            throw new RuntimeException("Error writing record of generation: " + getName() + ". " + e.getMessage(), e);
        }
    }

    @Input
    public String getOutputPackage(){
        return outputPackage;
    }

    @OutputDirectory
    public File getOutputDirectory(){
        String packageFolder = outputPackage.replace(".", "/");
        return new File(outputSourcesRoot, packageFolder);
    }

    @OutputDirectory
    public File getOutputDirectoryWrapper(){
        return new File(getOutputDirectory(), "wrapper");
    }

    @OutputFile
    public File getBuiltFilesRecordFile(){
        return new File(getProject().getLayout().getBuildDirectory().dir("typedMaterials").get().getAsFile(), getName());
    }

    @Input
    public String getJarFilterRegex(){
        return jarFilterRegex;
    }

    public void setJarFilterRegex(String jarFilterRegex){
        this.jarFilterRegex = jarFilterRegex;
    }

    public void setOutputPackage(String outputPackage){
        this.outputPackage = outputPackage;
    }

    public void setOutputSourcesRoot(File outputSourcesRoot){
        this.outputSourcesRoot = outputSourcesRoot;
    }

    private static String toUpperCamlCase(String upperCamelCase){
        return upperCamelCase.substring(0, 1).toUpperCase() + upperCamelCase.substring(1);
    }

}
