package com.onemillionworlds;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test that a module gets assets from jme, a module has its own local assets and a third module creates a
 * single Asset constant from the assets found in the other two modules.
 */
public class ComplexMultimoduleAssetsFunctionalTest extends FunctionalTestBase{

    @Test
    void complexMultiModuleAssetsFunctionalTest() throws IOException{

        writeString(getSettingsFile(), """
                include 'assetsJme'
                include 'assetsLocal'
                """);

        writeString(getBuildFile("assetsJme"), """
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


        writeString(getBuildFile("assetsLocal"),
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

        writeString(getBuildFile(),
                """
                    import com.onemillionworlds.tasks.AssetConstants
                    
                    plugins {
                      id('java')
                      id('com.onemillionworlds.typed-materials')
                    };
                    repositories {
                        mavenCentral()
                    }
                    typedMaterials{
                      //assetsConstantFromDependencies('com.myproject.assets.Assets',['assetsJme','assetsLocal'])
                    }
                    dependencies {
                        implementation project(':assetsJme')
                        implementation project(':assetsLocal')
                    }
                    
                    tasks.register('copyTypedLocalMaterialsFile', Copy) {
                        def sourceModule = project(':assetsLocal')
                        def sourceFilePath = 'build/resources/main/com_onemillionworlds_typedmaterials_assets.txt'
                    
                        dependsOn sourceModule.tasks.named('processResources')
                        from sourceModule.file(sourceFilePath) // Reference the file within the source module
                        into "${buildDir}/typedMaterials/localMaterials" // Destination folder in build directory
                    }
                    
                    tasks.named('processResources') {
                        dependsOn 'copyTypedLocalMaterialsFile'
                    }
                    
                    tasks.register('copyTypedJarMaterialsFile', Copy) {
                        def sourceModule = project(':assetsJme')
                        def sourceFilePath = 'build/resources/main/com_onemillionworlds_typedmaterials_assets.txt'
                    
                        dependsOn sourceModule.tasks.named('processResources')
                        from sourceModule.file(sourceFilePath) // Reference the file within the source module
                        into "${buildDir}/typedMaterials/jmeMaterials" // Destination folder in build directory
                    }
                    
                    tasks.register('generateAssetsConstant', AssetConstants) {
                        dependsOn 'copyTypedJarMaterialsFile'
                        dependsOn 'copyTypedLocalMaterialsFile'
                        assetFlatFiles = files(
                        "${buildDir}/typedMaterials/jmeMaterials/com_onemillionworlds_typedmaterials_assets.txt",
                        "${buildDir}/typedMaterials/localMaterials/com_onemillionworlds_typedmaterials_assets.txt")
                        fullyQualifiedAssetsClass = 'com.myproject.assets.Assets'
                        outputSourcesRoot = file("${projectDir}/src/main/generated/java")
                    }
                    
                    tasks.named('compileJava') {
                        dependsOn 'generateAssetsConstant'
                    }
                    
                    sourceSets.main.java.srcDirs += 'src/main/generated/java'
                    
                    """);


        String fileContents = "This is just a test";

        File localAssetsRoot = localResourcesRoot_resourcesStyle("assetsLocal");
        File texturesRoot = new File(localAssetsRoot, "Textures");
        texturesRoot.mkdirs();

        writeString(new File(localAssetsRoot, "topTest.txt"), fileContents);
        writeString(new File(texturesRoot, "nestText.txt"), fileContents);

        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("assemble", "--stacktrace");
        runner.withProjectDir(projectDir);
        BuildResult result = runner.build();

        String content = Files.readString(getGeneratedJavaFile("com/myproject/assets/Assets.java").toPath());

        String expected = """
                package com.myproject.assets;
                
                /**
                 * AUTOGENERATED CLASS, do not modify
                 * <p>
                 * Generated by the Typed Materials plugin. For more information:
                 * </p>
                 * @see <a href="https://github.com/oneMillionWorlds/TypedMaterials/wiki">MaterialTyper Plugin Wiki</a>
                 */
                @SuppressWarnings("all")
                public class Assets {
                
                    /*
                     * assetsLocal
                     */
                    public static final String TOP_TEST = "/topTest.txt";
                    public static class Common{
                        public static final String FOLDER_PATH = "Common";
                
                        public static String child(String child){
                            return FOLDER_PATH + "/" + child;
                        }
                        public static class Textures{
                            public static final String FOLDER_PATH = "Common/Textures";
                            /*
                             * jme3-core-3.6.1-stable
                             */
                            public static final String MISSING_TEXTURE = "Common/Textures/MissingTexture.png";
                            /*
                             * jme3-core-3.6.1-stable
                             */
                            public static final String INTEGRATE_BRDF = "Common/Textures/integrateBRDF.ktx";
                            /*
                             * jme3-core-3.6.1-stable
                             */
                            public static final String MISSING_MATERIAL = "Common/Textures/MissingMaterial.png";
                            /*
                             * jme3-core-3.6.1-stable
                             */
                            public static final String DOT = "Common/Textures/dot.png";
                            /*
                             * jme3-core-3.6.1-stable
                             */
                            public static final String MISSING_MODEL = "Common/Textures/MissingModel.png";
                
                            public static String child(String child){
                                return FOLDER_PATH + "/" + child;
                            }
                        }
                    }
                    public static class Textures{
                        public static final String FOLDER_PATH = "Textures";
                        /*
                         * assetsLocal
                         */
                        public static final String NEST_TEXT = "Textures/nestText.txt";
                
                        public static String child(String child){
                            return FOLDER_PATH + "/" + child;
                        }
                    }
                
                
                }
                
                """;

        assertEquals(expected.trim(), content.trim());
    }


}
