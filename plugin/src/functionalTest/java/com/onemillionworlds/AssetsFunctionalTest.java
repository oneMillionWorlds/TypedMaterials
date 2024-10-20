package com.onemillionworlds;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AssetsFunctionalTest extends FunctionalTestBase{

    @Test
    void localAssetConstants() throws IOException{
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
                      assetConstants('com.myproject.assets.Assets')
                    }
                    """);


        String fileContents = "This is just a test";
        File resourcesRoot = localResourcesRoot_resourcesStyle();
        File materialsRoot = new File(resourcesRoot, "MatDefs");
        File texturesRoot = new File(resourcesRoot, "Textures");
        File texturesSubFolder = new File(texturesRoot, "texturesSubFolder");

        materialsRoot.mkdirs();
        texturesSubFolder.mkdirs();

        File textureFile1 = new File(texturesRoot, "texture1.txt");
        File textureFile1_clashing = new File(texturesRoot, "texture1.json");
        File textureFile3 = new File(texturesRoot, "texture3.txt");

        File textureFile4_inSubFolder = new File(texturesSubFolder, "snakeCaseTest.txt");

        writeString(textureFile1, fileContents);
        writeString(textureFile1_clashing, fileContents);
        writeString(textureFile3, fileContents);
        writeString(textureFile4_inSubFolder, fileContents);

        writeString(new File(materialsRoot, "Material1.txt"), fileContents);

        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("assemble", "--stacktrace");
        runner.withProjectDir(projectDir);
        BuildResult result = runner.build();

        String content = Files.readString(getGeneratedJavaFile("com/myproject/assets/Assets.java").toPath());

        String expectedMaterialsSection = """
                    public static class MatDefs{
                        public static final String FOLDER_PATH = "MatDefs";
                        public static final String MATERIAL1 = "MatDefs/Material1.txt";
                        
                        public static String child(String child){
                            return FOLDER_PATH + "/" + child;
                        }
                    }
                """;

        String expectedTexturesSection = """
                    public static class Textures{
                        public static final String FOLDER_PATH = "Textures";
                        public static final String TEXTURE1 = "Textures/texture1.json";
                        public static final String TEXTURE1_TXT = "Textures/texture1.txt";
                        public static final String TEXTURE3 = "Textures/texture3.txt";
                        
                        public static String child(String child){
                            return FOLDER_PATH + "/" + child;
                        }
                        public static class TexturesSubFolder{
                            public static final String FOLDER_PATH = "Textures/texturesSubFolder";
                            public static final String SNAKE_CASE_TEST = "Textures/texturesSubFolder/snakeCaseTest.txt";
                            
                            public static String child(String child){
                                return FOLDER_PATH + "/" + child;
                            }
                        }
                    }
                """;

        assertTrue(content.contains(expectedMaterialsSection), content);
        assertTrue(content.contains(expectedTexturesSection), content);
    }

    /**
     * In java a class Foo can't contain a child class also called Foo. This test
     * ensures those aren't generated
     */
    @Test
    void localAssetConstantsCopesWithNestedFoldersWithSameName() throws IOException{
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
                      assetConstants('com.myproject.assets.Assets')
                    }
                    """);


        String fileContents = "This is just a test";
        File resourcesRoot = localResourcesRoot_resourcesStyle();
        File texturesRoot = new File(resourcesRoot, "Textures");
        File texturesSubFolder = new File(texturesRoot, "Textures");
        File texturesSubSubFolder = new File(texturesSubFolder, "Textures");

        texturesSubSubFolder.mkdirs();

        File textureFile1 = new File(texturesSubFolder, "texture1.txt");
        File textureFile1_clashing = new File(texturesSubFolder, "texture1.json");
        File textureFile3 = new File(texturesSubFolder, "texture3.txt");

        File textureFile4 = new File(texturesSubSubFolder, "texture4.txt");

        writeString(textureFile1, fileContents);
        writeString(textureFile1_clashing, fileContents);
        writeString(textureFile3, fileContents);
        writeString(textureFile4, fileContents);

        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("assemble");
        runner.withProjectDir(projectDir);
        BuildResult result = runner.build();

        String content = Files.readString(getGeneratedJavaFile("com/myproject/assets/Assets.java").toPath());


        String expectedTexturesSection = """
                    public static class Textures{
                        public static final String FOLDER_PATH = "Textures";
                        
                        public static String child(String child){
                            return FOLDER_PATH + "/" + child;
                        }
                        public static class Textures_2{
                            public static final String FOLDER_PATH = "Textures/Textures";
                            public static final String TEXTURE1 = "Textures/Textures/texture1.json";
                            public static final String TEXTURE1_TXT = "Textures/Textures/texture1.txt";
                            public static final String TEXTURE3 = "Textures/Textures/texture3.txt";
                                              
                            public static String child(String child){
                                return FOLDER_PATH + "/" + child;
                            }
                            public static class Textures_3{
                                public static final String FOLDER_PATH = "Textures/Textures/Textures";
                                public static final String TEXTURE4 = "Textures/Textures/Textures/texture4.txt";
                                
                                public static String child(String child){
                                    return FOLDER_PATH + "/" + child;
                                }
                            }
                        }
                    }
                """;

        assertTrue(content.contains(expectedTexturesSection), content);
    }

    /**
     * In java a class Foo can't contain a child class also called Foo. This test
     * ensures those aren't generated
     */
    @Test
    void localAssetConstantsFlatFileListing() throws IOException{
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
                    """);


        String fileContents = "This is just a test";
        File resourcesRoot = localResourcesRoot_resourcesStyle();
        File texturesRoot = new File(resourcesRoot, "Textures");
        File texturesSubFolder = new File(texturesRoot, "Textures");
        File texturesSubSubFolder = new File(texturesSubFolder, "Textures");

        texturesSubSubFolder.mkdirs();

        File textureFile1 = new File(texturesSubFolder, "texture1.txt");
        File textureFile1_clashing = new File(texturesSubFolder, "texture1.json");
        File textureFile3 = new File(texturesSubFolder, "texture3.txt");

        File textureFile4 = new File(texturesSubSubFolder, "texture4.txt");

        writeString(textureFile1, fileContents);
        writeString(textureFile1_clashing, fileContents);
        writeString(textureFile3, fileContents);
        writeString(textureFile4, fileContents);

        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("assemble");
        runner.withProjectDir(projectDir);
        BuildResult result = runner.build();

        String content = Files.readString(getGeneratedResourcesFile("com.onemillionworlds.typedmaterials.Assets.txt").toPath());


        String expectedTexturesSection = """
                Textures/Textures/texture1.json
                Textures/Textures/texture1.txt
                Textures/Textures/texture3.txt
                Textures/Textures/Textures/texture4.txt
                """;

        assertTrue(content.contains(expectedTexturesSection), content);
    }

    /**
     * In java a class Foo can't contain a child class also called Foo. This test
     * ensures those aren't generated
     */
    @Test
    void jarAssetConstantsFlatFileListing() throws IOException{
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
                      jarAssetsFile(".*jme3-core.*")
                    }
                    """);

        // Run the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("assemble","--stacktrace");
        runner.withProjectDir(projectDir);
        runner.build();

        String content = Files.readString(getGeneratedResourcesFile("com.onemillionworlds.typedmaterials.Assets.txt").toPath());

        String expectedAssetsSection = """
                ::jme3-core-3.6.1-stable
                joystick-mapping.properties
                META-INF/MANIFEST.MF
                Interface/Fonts/Default.fnt
                Interface/Fonts/Console.png
                Interface/Fonts/Console.fnt
                Interface/Fonts/Default.png
                """;

        assertTrue(content.contains(expectedAssetsSection), content);

    }


    /**
     * In java a class Foo can't contain a child class also called Foo. This test
     * ensures those aren't generated
     */
    @Test
    void jarAssetConstantsFlatFileListingWithFilter() throws IOException{
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
                      jarAssetsFile(".*jme3-core.*", ".*/Textures/.*")
                    }
                    """);

        // Run the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("assemble","--stacktrace");
        runner.withProjectDir(projectDir);
        runner.build();

        String content = Files.readString(getGeneratedResourcesFile("com.onemillionworlds.typedmaterials.Assets.txt").toPath());

        String expectedAssetsSection = """
                ::jme3-core-3.6.1-stable
                Common/Textures/MissingTexture.png
                Common/Textures/integrateBRDF.ktx
                Common/Textures/MissingMaterial.png
                Common/Textures/dot.png
                Common/Textures/MissingModel.png
                """;

        assertTrue(content.contains(expectedAssetsSection), content);

    }
}
