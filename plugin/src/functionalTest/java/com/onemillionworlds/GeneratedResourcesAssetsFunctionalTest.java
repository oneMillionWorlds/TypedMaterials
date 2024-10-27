package com.onemillionworlds;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeneratedResourcesAssetsFunctionalTest extends FunctionalTestBase{

    /**
     * The assetsFile function creates a generated resource. Ensure it doesn't get
     * into a cycle of reevaluating itself
     */
    @Test
    void generatedResourcesWorksWithAssetsFile() throws IOException{

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
                    typedMaterials{
                        assetsFile()
                    }
                    
                    task copyResource(type: Copy) {
                        from 'src/main/resources/copySource.txt'
                        into 'src/main/generated/resources'
                        rename { String fileName ->
                                    fileName == 'copySource.txt' ? 'copyResult.txt' : fileName
                                }
                    }
                    
                    sourceSets.main.resources.srcDirs += 'src/main/generated/resources'
                    
                    assetsFile.dependsOn copyResource
                    """);

        File localAssetsRoot = localResourcesRoot_resourcesStyle();
        writeString(new File(localAssetsRoot, "copySource.txt"), "File contents");

        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("assemble", "--stacktrace");
        runner.withProjectDir(projectDir);
        BuildResult result = runner.build();

        String content = Files.readString(getGeneratedResourcesFile("com_onemillionworlds_typedmaterials_assets.txt").toPath());

        String expectedValue = """
                copySource.txt
                copyResult.txt
                """;

       assertTrue(content.contains(expectedValue), content);
    }


}
