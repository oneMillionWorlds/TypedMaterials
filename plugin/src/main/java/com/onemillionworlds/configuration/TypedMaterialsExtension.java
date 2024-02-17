package com.onemillionworlds.configuration;

import com.onemillionworlds.TypedMaterialsPlugin;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;

public class TypedMaterialsExtension{

    private boolean enableLibraryMaterialsSearch = true;

    private final NamedDomainObjectContainer<MaterialConfiguration> librariesToScan;

    private String localProjectMaterialsLocation = null;

    private String localProjectMaterialPackage = "com.onemillionworlds.materials";

    private String generatedSourcesDir = TypedMaterialsPlugin.DEFAULT_GENERATED_SOURCES_DIR;

    public TypedMaterialsExtension(Project project) {
        ObjectFactory objectFactory = project.getObjects();
        this.librariesToScan = objectFactory.domainObjectContainer(MaterialConfiguration.class);

        MaterialConfiguration core = librariesToScan.create("core");
        core.setJarFilterRegex(".*jme3-core.*");
        core.setOutputPackage("org.jme3.core.materials");

        MaterialConfiguration effects = librariesToScan.create("effects");
        effects.setJarFilterRegex(".*jme3-effects.*");
        effects.setOutputPackage("org.jme3.effects.materials");
    }

    public NamedDomainObjectContainer<MaterialConfiguration> getLibrariesToScan() {
        return librariesToScan;
    }

    public String getLocalProjectMaterialsLocation(){
        return localProjectMaterialsLocation;
    }

    public String getLocalProjectMaterialPackage(){
        return localProjectMaterialPackage;
    }

    public void setLocalProjectMaterialsLocation(String localProjectMaterialsLocation){
        this.localProjectMaterialsLocation = localProjectMaterialsLocation;
    }

    public void setLocalProjectMaterialPackage(String localProjectMaterialPackage){
        this.localProjectMaterialPackage = localProjectMaterialPackage;
    }

    public String getGeneratedSourcesDir(){
        return generatedSourcesDir;
    }

    public void setGeneratedSourcesDir(String generatedSourcesDir){
        this.generatedSourcesDir = generatedSourcesDir;
    }

    public boolean isEnableLibraryMaterialsSearch() {
        return enableLibraryMaterialsSearch;
    }

    public void setEnableLibraryMaterialsSearch(boolean enableLibraryMaterialsSearch){
        this.enableLibraryMaterialsSearch = enableLibraryMaterialsSearch;
    }
}
