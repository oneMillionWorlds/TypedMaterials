package com.onemillionworlds;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests where generated files end up
 */
public class GeneratedLocationFunctionalTest extends FunctionalTestBase{

    private static final String MATERIAL = """
            MaterialDef [NAME] {
                MaterialParameters {
                    Float FillFraction
                }
            }
            """;

    @Test
    void nothingIsGeneratedIntoTheSourceTree() throws IOException{
        writeString(getSettingsFile(), "");
        writeString(getBuildFile(),
                """
                    plugins {
                      id('java')
                      id('com.onemillionworlds.typed-materials')
                    };
                    repositories {
                        mavenCentral()
                    }
                    dependencies {
                         implementation "org.jmonkeyengine:jme3-core:3.6.1-stable"
                    }
                    typedMaterials{
                      localMaterialsSearch('com.myproject.materials')
                      assetsConstant('com.myproject.assets.Assets')
                      assetsFile()
                    }
                    """);
        writeString(new File(localMaterialsRoot_resourcesStyle(), "PowerMeter.j3md"), MATERIAL.replace("[NAME]", "PowerMeter"));

        runBuild("assemble");

        assertTrue(getGeneratedJavaFile("localTypedMaterials", "com/myproject/materials/PowerMeterMaterial.java").exists());
        assertTrue(getGeneratedJavaFile("assetConstants", "com/myproject/assets/Assets.java").exists());
        assertTrue(getGeneratedJavaFile("materialFactory", "com/onemillionworlds/typedmaterials/materials/MaterialFactory.java").exists());
        assertTrue(getGeneratedResourcesFile("assetsFile", "com_onemillionworlds_typedmaterials_assets.txt").exists());
        assertTrue(new File(projectDir, "build/resources/main/com_onemillionworlds_typedmaterials_assets.txt").exists(), "Assets file should be packaged as a resource");

        assertFalse(new File(projectDir, "src/main/generatedtypedmaterials").exists());
    }

    @Test
    void removedMaterialsAreRemovedFromGeneratedSources() throws IOException{
        writeString(getSettingsFile(), "");
        writeString(getBuildFile(),
                """
                    plugins {
                      id('java')
                      id('com.onemillionworlds.typed-materials')
                    };
                    repositories {
                        mavenCentral()
                    }
                    dependencies {
                         implementation "org.jmonkeyengine:jme3-core:3.6.1-stable"
                    }
                    typedMaterials{
                      localMaterialsSearch('com.myproject.materials')
                    }
                    """);
        File keptMaterial = new File(localMaterialsRoot_resourcesStyle(), "Kept.j3md");
        File removedMaterial = new File(localMaterialsRoot_resourcesStyle(), "Removed.j3md");
        writeString(keptMaterial, MATERIAL.replace("[NAME]", "Kept"));
        writeString(removedMaterial, MATERIAL.replace("[NAME]", "Removed"));

        runBuild("assemble");
        assertTrue(getGeneratedJavaFile("localTypedMaterials", "com/myproject/materials/RemovedMaterial.java").exists());

        assertTrue(removedMaterial.delete());
        runBuild("assemble");

        assertTrue(getGeneratedJavaFile("localTypedMaterials", "com/myproject/materials/KeptMaterial.java").exists());
        assertFalse(getGeneratedJavaFile("localTypedMaterials", "com/myproject/materials/RemovedMaterial.java").exists());
        assertFalse(getGeneratedJavaFile("localTypedMaterials", "com/myproject/materials/wrapper/RemovedMaterialWrapper.java").exists());
    }

    @Test
    void customGeneratedDirectory() throws IOException{
        writeString(getSettingsFile(), "");
        writeString(getBuildFile(),
                """
                    plugins {
                      id('java')
                      id('com.onemillionworlds.typed-materials')
                    };
                    repositories {
                        mavenCentral()
                    }
                    dependencies {
                         implementation "org.jmonkeyengine:jme3-core:3.6.1-stable"
                    }
                    typedMaterials{
                      generatedSourcesDirectory = layout.buildDirectory.dir('somewhereElse')
                      localMaterialsSearch('com.myproject.materials')
                    }
                    """);
        writeString(new File(localMaterialsRoot_resourcesStyle(), "PowerMeter.j3md"), MATERIAL.replace("[NAME]", "PowerMeter"));

        runBuild("assemble");

        assertTrue(new File(projectDir, "build/somewhereElse/localTypedMaterials/com/myproject/materials/PowerMeterMaterial.java").exists());
    }

    private BuildResult runBuild(String task){
        return GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(task, "--stacktrace")
                .withProjectDir(projectDir)
                .build();
    }
}
