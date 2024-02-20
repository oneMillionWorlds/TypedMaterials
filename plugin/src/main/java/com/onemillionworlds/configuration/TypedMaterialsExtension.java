package com.onemillionworlds.configuration;

import com.onemillionworlds.TypedMaterialsPlugin;
import com.onemillionworlds.tasks.LocalAssetConstants;
import com.onemillionworlds.tasks.MaterialFactoryTask;
import com.onemillionworlds.tasks.TypedJarMaterials;
import com.onemillionworlds.tasks.TypedLocalMaterials;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

@SuppressWarnings("unused")
public class TypedMaterialsExtension{

    private final Project project;

    /**
     * The directory where the generated sources will be placed relative to the module root.
     * Default is "src/main/generated/java"
     */
    private final Property<String> generatedSourcesDir;

    private final Property<String> materialFactoryClass;

    public TypedMaterialsExtension(Project project) {
        this.project = project;
        ObjectFactory objectFactory = project.getObjects();

        generatedSourcesDir = objectFactory.property(String.class);
        generatedSourcesDir.set(TypedMaterialsPlugin.DEFAULT_GENERATED_SOURCES_DIR);

        materialFactoryClass = objectFactory.property(String.class);
        materialFactoryClass.set("com.onemillionworlds.typedmaterials.materials.MaterialFactory");
    }

    public void jmeMaterials(){
        librarySearch("jmeCoreMaterials", ".*jme3-core.*", "org.jme3.core.materials");
        librarySearch("jmeEffectsMaterials", ".*jme3-effects.*", "org.jme3.effects.materials");
    }

    public void assetConstants(String fullyQualifiedAssetsClass){
        assetConstants(fullyQualifiedAssetsClass, "src/main/resources", "resources");
    }

    public void assetConstants(String fullyQualifiedAssetsClass, String assetsDirectory, String resourcesDirName){
        project.getTasks().create("assetConstants", LocalAssetConstants.class, task -> {
            task.setGroup("typedMaterials");
            task.setFullyQualifiedAssetsClass(fullyQualifiedAssetsClass);
            task.setAssetsFolder(project.file(assetsDirectory));
            task.setResourcesDirName(resourcesDirName);
            task.setOutputSourcesRoot(project.file(generatedSourcesDir));
        });
        project.getTasks().named("compileJava").configure(compileJava -> compileJava.dependsOn("assetConstants"));
    }

    /**
     * Registers a task to search for materials in the jars of the project's runtime classpath
     * @param taskName just a name for the task, needs to be unique
     * @param jarFilterRegex a regex to filter the jars to search for materials. E.g. ".*jme3-core.*" will search
     *                       the core jme library
     * @param outputPackage the package where the generated materials will be placed. E.g. "org.jme3.core.materials"
     */
    public void librarySearch(String taskName, String jarFilterRegex, String outputPackage){
        project.getTasks().register(taskName, TypedJarMaterials.class, task -> {
            task.setGroup("typedMaterials");
            task.setOutputPackage(outputPackage);
            task.setOutputSourcesRoot(project.file(generatedSourcesDir));
            task.setJarFilterRegex(jarFilterRegex);
        });
        setUpMaterialFactoryTaskIfNotPresent();
        project.getTasks().named("materialFactory").configure(compileJava -> compileJava.dependsOn(taskName));
    }

    /**
     * Registers a task to search for materials in the standard location of your local project.
     * This function assumes a typical gradle structure, where the materials are in the "src/main/resources/MatDefs" directory.
     *
     * @param outputPackage the package where the generated materials will be placed. E.g. "com.mygame.materials"
     */
    public void localMaterialsSearch(String outputPackage){
        localMaterialsSearch(outputPackage, "src/main/resources/MatDefs", "resources");
    }

    /**
     * Registers a task to search for materials in the given directory of your local project.
     *
     * @param outputPackage the package where the generated materials will be placed. E.g. "com.mygame.materials"
     * @param materialsDirectory the path relative to the module root. E.g. "src/main/resources/MatDefs" or "assets"
     * @param resourcesDirName the name of the resources directory. E.g. "resources" or "assets"
     */
    public void localMaterialsSearch(String outputPackage, String materialsDirectory, String resourcesDirName){
        project.getTasks().register("localTypedMaterials", TypedLocalMaterials.class, task -> {
            task.setGroup("typedMaterials");
            task.setInputDirectory(project.file(materialsDirectory));
            task.setOutputPackage(outputPackage);
            task.setOutputSourcesRoot(project.file(generatedSourcesDir));
            task.setResourcesDir(resourcesDirName);
        });
        setUpMaterialFactoryTaskIfNotPresent();
        project.getTasks().named("materialFactory").configure(compileJava -> compileJava.dependsOn("localTypedMaterials"));
    }

    /**
     * The directory where the generated sources will be placed relative to the module root.
     * Default is "src/main/generated/java"
     */
    public Property<String> getGeneratedSourcesDir(){
        return generatedSourcesDir;
    }

    public Property<String> getMaterialFactoryClass(){
        return materialFactoryClass;
    }

    private void setUpMaterialFactoryTaskIfNotPresent(){
        if(project.getTasks().findByName("materialFactory") == null){
            project.getTasks().create("materialFactory", MaterialFactoryTask.class, task -> {
                task.setGroup("typedMaterials");
                task.setOutputSourcesRoot(project.file(generatedSourcesDir));
                task.setFullyQualifiedOutputClass(materialFactoryClass.get());
            });
            project.getTasks().named("compileJava").configure(compileJava -> compileJava.dependsOn("materialFactory"));
        }
    }
}
