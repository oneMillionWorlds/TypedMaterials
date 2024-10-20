package com.onemillionworlds.tasks.assets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AssetsFolderTest{

    @Test
    void addAssetFromFullRelativePath(){
        AssetsFolder assetsFolder = new AssetsFolder("");
        assetsFolder.addAssetFromFullRelativePath("Models/box.txt",null);
        assetsFolder.addAssetFromFullRelativePath("Models/trees/tree1.png",null);
        assetsFolder.addAssetFromFullRelativePath("Models/trees/tree2.png",null);

        String items = assetsFolder.getFileListingContent().toString();
        String expected = """
                Models/box.txt
                Models/trees/tree1.png
                Models/trees/tree2.png
                """;

        assertEquals(expected, items);
    }

    @Test
    void addAssetFromFullRelativePathWithContext(){
        AssetsFolder assetsFolder = new AssetsFolder("");
        assetsFolder.addAssetFromFullRelativePath("Models/box.txt","Apple");
        assetsFolder.addAssetFromFullRelativePath("Models/trees/tree1.png","Apple");
        assetsFolder.addAssetFromFullRelativePath("Models/trees/tree2.png","Pear");
        assetsFolder.addAssetFromFullRelativePath("Models/trees/tree3.png","Pear");
        assetsFolder.addAssetFromFullRelativePath("Models/trees/tree4.png","Apple");

        String items = assetsFolder.getFileListingContent().toString();
        String expected = """
                ::Apple
                Models/box.txt
                Models/trees/tree1.png
                ::Pear
                Models/trees/tree2.png
                Models/trees/tree3.png
                ::Apple
                Models/trees/tree4.png
                """;

        assertEquals(expected, items);
    }

    @Test
    void addAssetTopLevelFile(){
        AssetsFolder assetsFolder = new AssetsFolder("");
        assetsFolder.addAssetFromFullRelativePath("/joystick-mapping.properties",null);

        String items = assetsFolder.getFileListingContent().toString();
        String expected = """
                joystick-mapping.properties
                """;

        assertEquals(expected, items);
    }
}