package com.onemillionworlds.tasks.assets;

import com.onemillionworlds.tasks.AssetConstants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AssetsFolder{
    String assetFolderPath;

    Map<String, AssetsFolder> subfolders = new LinkedHashMap<>();

    List<AssetItem> assetsOnThisLevel = new ArrayList<>();

    public AssetsFolder(String assetFolderPath){
        this.assetFolderPath = assetFolderPath;
    }

    public String getJavaClassContent(int indentLevel, List<String> parentFolders){
        StringBuilder content = new StringBuilder();

        Set<String> usedSimpleNames = new HashSet<>();

        if(indentLevel>1){
            content
                    .append(" ".repeat(indentLevel * 4))
                    .append("public static final String FOLDER_PATH")
                    .append(" = \"").append(assetFolderPath).append("\";\n");
            usedSimpleNames.add("FOLDER_PATH");
        }
        for(AssetItem asset : assetsOnThisLevel){
            String complexName = toUpperSnakeCase(makeValidJavaIdentifier(asset.name));
            String simpleName = toUpperSnakeCase(makeValidJavaIdentifier(asset.name.replaceAll("\\..*", "")));
            String nameToUse;
            if (!usedSimpleNames.contains(simpleName)){
                nameToUse = simpleName;
                usedSimpleNames.add(simpleName);
            }else{
                nameToUse = complexName;
            }

            if(asset.context!=null){
                content
                        .append(" ".repeat(indentLevel*4))
                        .append("/* ").append("\n");
                content
                        .append(" ".repeat(indentLevel*4))
                        .append(" * ").append(asset.context).append("\n");
                content
                        .append(" ".repeat(indentLevel*4))
                        .append("*/ ").append("\n");
            }

            content
                    .append(" ".repeat(indentLevel*4))
                    .append("public static final String ").append(nameToUse)
                    .append(" = \"").append(assetFolderPath + "/"+asset.name).append("\";\n");
        }

        if(indentLevel>1){
            content.append("\n")
                    .append(" ".repeat(indentLevel * 4)).append("public static String child(String child){\n")
                    .append(" ".repeat(indentLevel * 4)).append("    return FOLDER_PATH + \"/\" + child;\n")
                    .append(" ".repeat(indentLevel * 4)).append("}\n");
        }

        for(Map.Entry<String, AssetsFolder> subfolder : subfolders.entrySet()){
            List<String> parentFoldersForChild = new ArrayList<>(parentFolders);
            parentFoldersForChild.add(subfolder.getKey());
            content
                    .append(" ".repeat(indentLevel*4))
                    .append("public static class ").append(makeValidJavaClass(subfolder.getKey(), parentFolders))
                    .append("{\n")
                    .append(subfolder.getValue().getJavaClassContent(indentLevel+1, parentFoldersForChild))
                    .append(" ".repeat(indentLevel*4))
                    .append("}\n");
        }

        return content.toString();
    }

    /**
     * This is just a list of all the files, which is put into the resources folder to be pickd up in other modules
     * (if desired) to create a single super assets class
     */
    public StringBuilder getFileListingContent(){
        ContextProvider contextProvider = new ContextProvider();
        return getFileListingContent(List.of(), contextProvider);
    }

    /**
     * This is just a list of all the files, which is put into the resources folder to be pickd up in other modules
     * (if desired) to create a single super assets class
     */
    public StringBuilder getFileListingContent(List<String> parentFolders, ContextProvider contextProvider){
        String context = contextProvider.getContext();
        StringBuilder content = new StringBuilder();
        for(AssetItem assetOnThisLevel : assetsOnThisLevel){
            if(!Objects.equals(assetOnThisLevel.context, context)){
                context = assetOnThisLevel.context;
                contextProvider.setContext(context); //this allows other bits of the build to keep the context
                content.append(AssetConstants.FLAT_FILE_CONTEXT_CHANGE).append(context).append("\n");
            }
            for(String parentFolder : parentFolders){
                content.append(parentFolder).append("/");
            }
            if(!assetFolderPath.isEmpty()){
                content.append(assetFolderPath).append("/");
            }
            content.append(assetOnThisLevel.name).append("\n");
        }

        List<String> parentFoldersForChildren = new ArrayList<>(parentFolders);
        if(!assetFolderPath.isEmpty()){
            parentFoldersForChildren.add(assetFolderPath);
        }

        for(AssetsFolder subfolder : subfolders.values()){
            content.append(subfolder.getFileListingContent(parentFoldersForChildren, contextProvider));
        }
        return content;
    }

    public void addAsset(AssetItem asset){
        assetsOnThisLevel.add(asset);
    }

    public void addAssetFromFullRelativePath(String fullRelativePath, String context){

        if(fullRelativePath.startsWith("/")){
            //this happens when the asset is in the root of the assets folder
            fullRelativePath = fullRelativePath.substring(1);
        }

        if(fullRelativePath.contains("/")){
            String subfolder = fullRelativePath.substring(0, fullRelativePath.indexOf("/"));
            String assetName = fullRelativePath.substring(fullRelativePath.indexOf("/")+1);

            subfolders.computeIfAbsent(subfolder, AssetsFolder::new).addAssetFromFullRelativePath(assetName, context);
        }else {
            addAsset(new AssetItem(fullRelativePath, context));
        }
    }

    public void addSubfolder(String subfolder, AssetsFolder assetsFolder){
        subfolders.put(subfolder, assetsFolder);
    }

    private String makeValidJavaClass(String name, List<String> parentFolders){
        String validIdentifier = makeValidJavaIdentifier(name);
        int numberOfParentsWithSameName = (int) parentFolders.stream()
                .map(this::makeValidJavaIdentifier)
                .filter(folder -> folder.equals(validIdentifier)).count();
        String numericSuffix = numberOfParentsWithSameName == 0 ? "" : "_"+(numberOfParentsWithSameName+1);

        String validIdentifierWithSuffix = makeValidJavaIdentifier(name) + numericSuffix;
        return validIdentifierWithSuffix.substring(0, 1).toUpperCase() + validIdentifierWithSuffix.substring(1);
    }

    private String makeValidJavaIdentifier(String name){
        String corrected = name.replaceAll("[^a-zA-Z0-9]", "_");

        if (Character.isDigit(corrected.charAt(0))){
            corrected = "_" + corrected;
        }
        return corrected;
    }

    private String toUpperSnakeCase(String name){
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
    }

    public void addAll(AssetsFolder assetsFolder){
        assetsOnThisLevel.addAll(assetsFolder.assetsOnThisLevel);
        for(Map.Entry<String, AssetsFolder> subfolder : assetsFolder.subfolders.entrySet()){
            if (subfolders.containsKey(subfolder.getKey())){
                subfolders.get(subfolder.getKey()).addAll(subfolder.getValue());
            }else{
                subfolders.put(subfolder.getKey(), subfolder.getValue());
            }
        }
    }

    public static class AssetItem{
        /**
         * This is which library it came from, used only in a comment. Can be null, in which case it is not included
         */
        String context;
        String name;

        public AssetItem(String name, String context){
            this.context = context;
            this.name = name;
        }
    }

    public static class ContextProvider{
        String context;

        public String getContext(){
            return context;
        }

        public void setContext(String context){
            this.context = context;
        }
    }

}
