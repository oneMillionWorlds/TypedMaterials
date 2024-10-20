package com.onemillionworlds.tasks;

import com.onemillionworlds.tasks.assets.AssetsFolder;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AssetConstants extends DefaultTask{

    public static final String FLAT_FILE_CONTEXT_CHANGE = "::";
    private static final String ASSETS_FILE_NAME = "com.onemillionworlds.typedmaterials.assets.txt";

    private static final String classTemplate = """
                                     package [PACKAGE];
                                     
                                     /**
                                      * AUTOGENERATED CLASS, do not modify
                                      * <p>
                                      * Generated by the Typed Materials plugin. For more information:
                                      * </p>
                                      * @see <a href="https://github.com/oneMillionWorlds/TypedMaterials/wiki">MaterialTyper Plugin Wiki</a>
                                      */
                                     @SuppressWarnings("all")
                                     public class [CLASS] {
                                     
                                     [CONTENT]
                                     
                                     }""";

    /**
     * If present, use this as the class name for the Assets constants class
     */
    String fullyQualifiedAssetsClass;

    /**
     * If present use this as a regex to defermine if a jar should be fully searched for assets.
     * (This means going through every file in the jar, not using the flat file that helpful libraries may
     * leave in the jar)
     */
    String jarFilterRegex;

    /**
     * If present, use this regex to filter the files within the jars, only those that match will be included
     */
    String withinJarFileRegex;

    /**
     * If present, use this sources root to output all the assets as a java class
     */
    File outputSourcesRoot;

    /**
     * If present, use this resources root to output all the assets as a flat file
     */
    File outputResourcesRoot;

    @TaskAction
    public void createTypedMaterials(){
        AssetsFolder assetsFolder = new AssetsFolder("");

        for(File folder : getFileCollectionFromSourceDirs()){
            assetsFolder.addAll(searchForAllFiles(folder, "", null));
        }

        AssetsFolder resourcesFolderFast = searchAllFromResourceFlatFiles();
        assetsFolder.addAll(resourcesFolderFast);

        if(jarFilterRegex!=null && !jarFilterRegex.isBlank()){
            AssetsFolder jarAssets = searchAllFromJars(jarFilterRegex);
            assetsFolder.addAll(jarAssets);
        }

        try {
            if(getDestinationFile() != null){
                String classContent = assetsFolder.getJavaClassContent(1, List.of(getClassName()));

                String fullClass = classTemplate
                        .replace("[PACKAGE]", getDestinationPackage())
                        .replace("[CLASS]", getClassName())
                        .replace("[CONTENT]", classContent);
                Files.writeString(getDestinationFile().toPath(), fullClass);
            }
            if(getDestinationFlatFile()!=null){
                String flatFileStringBuilder = FLAT_FILE_CONTEXT_CHANGE+getProject().getName() + "\n" +
                        assetsFolder.getFileListingContent();
                Files.writeString(getDestinationFlatFile().toPath(), flatFileStringBuilder);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error writing record of generation: " + getName() + ". " + e.getMessage(), e);
        }
    }

    private AssetsFolder searchAllFromJars(String jarFilterRegex){
        Pattern pattern = Pattern.compile(jarFilterRegex);

        Pattern withinJarPattern = Pattern.compile(withinJarFileRegex == null ? ".*" : withinJarFileRegex);

        Configuration test = getProject().getConfigurations().getByName("runtimeClasspath");
        Set<File> resolve = test.resolve();

        AssetsFolder assetsFolder = new AssetsFolder("");
        resolve.forEach(file -> {
            if (file.getName().endsWith(".jar") && pattern.matcher(file.getName()).matches()) {
                String context = file.getName().replace(".jar", "");

                try (ZipInputStream zip = new ZipInputStream(new FileInputStream(file))) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        if(entry.isDirectory()){
                            continue;
                        }
                        if(entry.getName().endsWith(".class")){
                            continue;
                        }
                        if(!withinJarPattern.matcher(entry.getName()).matches()){
                            continue;
                        }

                        assetsFolder.addAssetFromFullRelativePath(entry.getName(), context);
                        zip.closeEntry();
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error processing JAR: " + file + e.getMessage(), e);
                }
            }
        });
        return assetsFolder;
    }

    private AssetsFolder searchForAllFiles(File file, String currentPath, String context){

        AssetsFolder assetsFolder = new AssetsFolder(currentPath);

        File[] files = file.listFiles();

        if(files==null){
            return assetsFolder;
        }

        for( File fileToProcess : Arrays.stream(files).sorted().toList()){
            if (fileToProcess.isFile()){
                assetsFolder.addAsset(new AssetsFolder.AssetItem(fileToProcess.getName(), context));
            }else{
                AssetsFolder subFolder = searchForAllFiles(fileToProcess, currentPath + (currentPath.isBlank()?"":"/") + fileToProcess.getName(), context);
                assetsFolder.addSubfolder(fileToProcess.getName(), subFolder);
            }
        }

        return assetsFolder;
    }

    private AssetsFolder searchAllFromResourceFlatFiles(){
        AssetsFolder assetsFolder = new AssetsFolder("");

        List<String> allLibraryAssetFiles = loadAllLibraryAssetFiles();
        String context = null;
        for(String asset : allLibraryAssetFiles){
            if(asset.startsWith(FLAT_FILE_CONTEXT_CHANGE)){
                context = asset.substring(FLAT_FILE_CONTEXT_CHANGE.length());
                continue;
            }
            assetsFolder.addAssetFromFullRelativePath(asset, context);
        }

        return assetsFolder;
    }

    @Optional
    @Input
    public String getFullyQualifiedAssetsClass(){
        return fullyQualifiedAssetsClass;
    }

    @Optional
    @Input
    public String getJarFilterRegex(){
        return jarFilterRegex;
    }

    @Optional
    @Input
    public String getWithinJarFileRegex(){
        return withinJarFileRegex;
    }

    public void setWithinJarFileRegex(String withinJarFileRegex){
        this.withinJarFileRegex = withinJarFileRegex;
    }

    /*
    @InputFiles
    public Collection<File> getFoldersToProcess(){
        SourceSetContainer sourceSets = (SourceSetContainer) getProject().getProperties().get("sourceSets");
        SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        return mainSourceSet.getResources().getSrcDirs();
    }*/

    @InputFiles
    public FileCollection getFileCollectionFromSourceDirs() {
        Project project = getProject();
        SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
        SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        return project.files(mainSourceSet.getResources().getSrcDirs());
    }

    @Optional
    @OutputFile
    public File getDestinationFile(){
        if(outputSourcesRoot == null || fullyQualifiedAssetsClass == null){
            return null;
        }
        return new File(outputSourcesRoot, fullyQualifiedAssetsClass.replace(".", "/") + ".java");
    }

    @Optional
    @OutputFile
    public File getDestinationFlatFile(){
        if(outputResourcesRoot == null){
            return null;
        }
        return new File(outputResourcesRoot, ASSETS_FILE_NAME);
    }

    public void setOutputSourcesRoot(File outputSourcesRoot){
        this.outputSourcesRoot = outputSourcesRoot;
    }

    public void setOutputResourcesRoot(File outputResourcesRoot){
        this.outputResourcesRoot = outputResourcesRoot;
    }

    public void setFullyQualifiedAssetsClass(String fullyQualifiedAssetsClass){
        this.fullyQualifiedAssetsClass = fullyQualifiedAssetsClass;
    }

    private String getDestinationPackage(){
        return fullyQualifiedAssetsClass
                .replaceAll("\\.[A-Za-z0-9_]+$", "");
    }

    private String getClassName(){
        return fullyQualifiedAssetsClass
                .replaceAll(".*\\.", "");
    }

    private List<String> loadAllLibraryAssetFiles(){
        ClassLoader classLoader = AssetConstants.class.getClassLoader();

        List<String> content = new ArrayList<>();
        try {
            Enumeration<URL> resources = classLoader.getResources(ASSETS_FILE_NAME);
            while (resources.hasMoreElements()) {
                URL resourceUrl = resources.nextElement();
                System.out.println("Found resource at: " + resourceUrl);

                try (InputStream inputStream = resourceUrl.openStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.add(line);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading assets file: " + e.getMessage(), e);
        }
        return content;
    }

    public void setJarFilterRegex(String jarFilterRegex){
        this.jarFilterRegex = jarFilterRegex;
    }


}
