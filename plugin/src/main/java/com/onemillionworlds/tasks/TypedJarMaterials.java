package com.onemillionworlds.tasks;

import com.onemillionworlds.utilities.MaterialTyper;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
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

                            String fileContents = MaterialTyper.createMaterialClassFile(fullDefName, className, outputPackage, content.toString(), originComment);

                            File destination = new File(outputDirectory, className + ".java");
                            Files.writeString(destination.toPath(), fileContents);
                        }
                        zip.closeEntry();
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error processing JAR: " + file + e.getMessage(), e);
                }
            }
        });
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
