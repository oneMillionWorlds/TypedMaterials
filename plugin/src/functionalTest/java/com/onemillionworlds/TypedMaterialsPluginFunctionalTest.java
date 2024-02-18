/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.onemillionworlds;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.io.FileWriter;
import java.nio.file.Files;

import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class TypedMaterialsPluginFunctionalTest {

    private static final String localMaterial = """
            MaterialDef PowerMeter {
                        
                MaterialParameters {
                    Texture2D NoPowerTexture
                    Texture2D FullPowerTexture
                    // Current usage fraction, from 0 to 1
                    Float FillFraction
                    // 0 or 1, if indeterminate will have a stiped pattern
                    Int Indeterminate
                }
            }
            Technique {
                other stuff
            }
            """;

    @TempDir
    File projectDir;

    private File getBuildFile() {
        return new File(projectDir, "build.gradle");
    }

    private File getSettingsFile() {
        return new File(projectDir, "settings.gradle");
    }

    private File getGeneratedJavaFilesRoot() {
        return new File(projectDir,  "src/main/generated/java");
    }


    private File getGeneratedJavaFile(String pathRelativeToRoot) {
        return new File(getGeneratedJavaFilesRoot(),  pathRelativeToRoot);
    }

    private File localMaterialsRoot_resourcesStyle() {
        File directory = new File(projectDir,  "src/main/resources/MatDefs");
        directory.mkdirs();
        return directory;
    }

    private File localMaterialsRoot_assetsStyle() {
        File directory = new File(projectDir,  "Assets/MatDefs");
        directory.mkdirs();
        return directory;
    }

    @Test
    void correctConfiguredTasks() throws IOException {
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
                         implementation 'org.jmonkeyengine:jme3-core:3.6.1-stable'
                    }
                    typedMaterials{
                        librarySearch("alternateTypedMaterials", ".*core.*", "com.onemillionworlds.core.materials")
                    }
                    
                    """);

        // Run the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("assemble");
        runner.withProjectDir(projectDir);
        BuildResult result = runner.build();

        BuildTask alternateTask = result.task(":alternateTypedMaterials");
        assertNotNull(alternateTask);

    }

    @Test
    void outputsProcessedFilesFromJMEMaterials() throws IOException {
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
                         implementation 'org.jmonkeyengine:jme3-core:3.6.1-stable'
                    }
                    typedMaterials{
                      jmeMaterials()
                    }
                    """);

        // Run the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("assemble");
        runner.withProjectDir(projectDir);
        runner.build();

        File lightingMaterial = getGeneratedJavaFile("org/jme3/core/materials/LightingMaterial.java");
        assertTrue(lightingMaterial.exists());
        String content = Files.readString(lightingMaterial.toPath());

        assertTrue(content.contains("package org.jme3.core.materials;"));

        assertTrue(content.contains("""
                    public LightingMaterial(AssetManager contentMan) {
                        super(contentMan, "Common/MatDefs/Light/Lighting.j3md");
                    }
                """));

        assertTrue(content.contains("""
                    /**
                     *  For Morph animation
                     */
                    public void setMorphWeights(float[] morphWeights){
                        setParam("MorphWeights", VarType.FloatArray,  morphWeights);
                    }
                """));

    }

    @Test
    void outputsProcessedFilesFromLocalMaterials_resourcesStyle() throws IOException {
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
                         implementation 'org.jmonkeyengine:jme3-core:3.6.1-stable'
                    }
                    typedMaterials{
                      localMaterialsSearch('com.myproject.materials')
                    }
                    """);
        writeString(new File(localMaterialsRoot_resourcesStyle(), "PowerMeter.j3md"), localMaterial);

        // Run the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("assemble");
        runner.withProjectDir(projectDir);
        BuildResult result = runner.build();

        BuildTask localMaterialsTask = result.task(":localTypedMaterials");
        assertNotNull(localMaterialsTask);
        assertEquals(localMaterialsTask.getOutcome(), TaskOutcome.SUCCESS);

        File powerMeterMaterial = getGeneratedJavaFile("com/myproject/materials/PowerMeterMaterial.java");
        assertTrue(powerMeterMaterial.exists());
        String content = Files.readString(powerMeterMaterial.toPath());

        assertTrue(content.contains("package com.myproject.materials;"));

        assertTrue(content.contains("""
                    public PowerMeterMaterial(AssetManager contentMan) {
                        super(contentMan, "MatDefs/PowerMeter.j3md");
                    }
                """));

        assertTrue(content.contains("""
                    /**
                     *  Current usage fraction, from 0 to 1
                     */
                    public void setFillFraction(float fillFraction){
                        setFloat("FillFraction", fillFraction);
                    }
                """));

    }

    @Test
    void outputsProcessedFilesFromLocalMaterials_assetsStyle() throws IOException {
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
                         implementation 'org.jmonkeyengine:jme3-core:3.6.1-stable'
                    }
                    typedMaterials{
                      localMaterialsSearch('com.myproject.materials', "assets", "assets")
                    }
                    """);
        writeString(new File(localMaterialsRoot_assetsStyle(), "PowerMeter.j3md"), localMaterial);

        // Run the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("assemble");
        runner.withProjectDir(projectDir);
        BuildResult result = runner.build();

        BuildTask localMaterialsTask = result.task(":localTypedMaterials");
        assertNotNull(localMaterialsTask);
        assertEquals(localMaterialsTask.getOutcome(), TaskOutcome.SUCCESS);

        File powerMeterMaterial = getGeneratedJavaFile("com/myproject/materials/PowerMeterMaterial.java");
        assertTrue(powerMeterMaterial.exists());
        String content = Files.readString(powerMeterMaterial.toPath());

        assertTrue(content.contains("package com.myproject.materials;"));

        assertTrue(content.contains("""
                    public PowerMeterMaterial(AssetManager contentMan) {
                        super(contentMan, "MatDefs/PowerMeter.j3md");
                    }
                """));

        assertTrue(content.contains("""
                    /**
                     *  Current usage fraction, from 0 to 1
                     */
                    public void setFillFraction(float fillFraction){
                        setFloat("FillFraction", fillFraction);
                    }
                """));

    }


    private void writeString(File file, String string) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            writer.write(string);
        }
    }
}
