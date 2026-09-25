package com.onemillionworlds;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigurationCacheFunctionalTest extends FunctionalTestBase{

    /**
     * Runs every kind of task the plugin can create twice; the second run should reuse the configuration cache entry
     * and find everything up-to-date
     */
    @Test
    void configurationCacheIsReused() throws IOException{
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
                      localMaterialsSearch('com.myproject.materials')
                      assetsConstant('com.myproject.assets.Assets')
                      jarAssetsFile(".*jme3-core.*", ".*/Textures/.*")
                    }
                    """);
        writeString(new File(localMaterialsRoot_resourcesStyle(), "Empty.j3md"), "MaterialDef Empty {\n    MaterialParameters {\n    }\n}\n");

        BuildResult first = runAssemble();
        assertTrue(first.getOutput().contains("Configuration cache entry stored"), first.getOutput());

        BuildResult second = runAssemble();
        assertTrue(second.getOutput().contains("Configuration cache entry reused"), second.getOutput());
        for(String task : new String[]{":jmeCoreMaterials", ":localTypedMaterials", ":materialFactory", ":assetConstants", ":assetsFile", ":compileJava"}){
            assertEquals(TaskOutcome.UP_TO_DATE, second.task(task).getOutcome(), task);
        }
    }

    private BuildResult runAssemble(){
        return GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments("assemble", "--stacktrace")
                .withProjectDir(projectDir)
                .build();
    }
}
