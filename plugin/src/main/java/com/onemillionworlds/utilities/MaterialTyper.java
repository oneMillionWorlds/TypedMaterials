package com.onemillionworlds.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MaterialTyper{

    private static final String classTemplate = """
                                     package [PACKAGE];
                                     
                                     import com.jme3.material.Material;
                                     import com.jme3.asset.AssetManager;
                                     import com.jme3.texture.Texture;
                                     import com.jme3.texture.Texture;
                                     import com.jme3.shader.VarType;
                                     import com.jme3.math.ColorRGBA;
                                     import com.jme3.math.Vector2f;
                                     import com.jme3.math.Vector3f;
                                     import com.jme3.math.Vector4f;
                                     import com.jme3.math.Matrix3f;
                                     import com.jme3.math.Matrix4f;
                                     import com.jme3.texture.Texture3D;
                                     import com.jme3.texture.TextureArray;
                                     import com.jme3.texture.TextureCubeMap;
                                     import com.jme3.shader.BufferObject;

                                     /**
                                      * AUTOGENERATED CLASS, do not modify
                                      * <p>
                                      * Generated from [GENERATED_FROM] by the MaterialTyper plugin. For more information:
                                      * </p>
                                      * @see <a href="https://github.com/oneMillionWorlds/TypedMaterials/wiki">MaterialTyper Plugin Wiki</a>
                                      */
                                     @SuppressWarnings("all")
                                     public class [MATERIAL_NAME] extends Material {
                                     
                                         /**
                                         * Do not use this constructor. Serialization purposes only.
                                         */
                                         public [MATERIAL_NAME]() {
                                             super();
                                         }
                                     
                                         public [MATERIAL_NAME](AssetManager contentMan) {
                                             super(contentMan, "[DEF_NAME]");
                                         }
                                     
                                     [CONTENT]
                                     
                                     }""";

    private static final String wrapperTemplate = """
                                     package [PACKAGE].wrapper;
                                     
                                     import com.jme3.material.Material;
                                     import com.jme3.asset.AssetManager;
                                     import com.jme3.texture.Texture;
                                     import com.jme3.texture.Texture;
                                     import com.jme3.shader.VarType;
                                     import com.jme3.math.ColorRGBA;
                                     import com.jme3.math.Vector2f;
                                     import com.jme3.math.Vector3f;
                                     import com.jme3.math.Vector4f;
                                     import com.jme3.math.Matrix3f;
                                     import com.jme3.math.Matrix4f;
                                     import com.jme3.texture.Texture3D;
                                     import com.jme3.texture.TextureArray;
                                     import com.jme3.texture.TextureCubeMap;
                                     import com.jme3.shader.BufferObject;

                                     /**
                                      * AUTOGENERATED CLASS, do not modify
                                      * <p>
                                      * Generated from [GENERATED_FROM] by the MaterialTyper plugin. For more information:
                                      * </p>
                                      * @see <a href="https://github.com/oneMillionWorlds/TypedMaterials/wiki">MaterialTyper Plugin Wiki</a>
                                      * <p>
                                      * This class is a wrapper around the Material class, it provides a more user-friendly API to
                                      * set and get parameters but cannot be used in the same way as the Material class.
                                      * use the getMaterial() method to get the underlying Material object.
                                      * </p>
                                      */
                                     @SuppressWarnings("all")
                                     public class [MATERIAL_NAME]Wrapper{
                                     
                                         private final Material material;
                                     
                                         public [MATERIAL_NAME]Wrapper(Material material) {
                                             if(material == null){
                                                 throw new IllegalArgumentException("Material cannot be null");
                                             }
                                             if(!material.getMaterialDef().getAssetName().equals("[DEF_NAME]")){
                                                 throw new IllegalArgumentException("Material is not of type [DEF_NAME] but is " + material.getMaterialDef().getAssetName());
                                             }
                                             this.material = material;
                                         }
                                         
                                         /**
                                          * @return the underlying Material object
                                          */
                                         public Material getMaterial(){
                                             return material;
                                         }
                                     
                                     [CONTENT]
                                     
                                     }""";

    private static final String setMethodTemplate = """
                                       public void set[PARAMETER_NAME]([TYPE] [PARAMETER_NAME_LOWER_CAMEL_CASE]){
                                           [CONTENT];
                                       }""";

    private static final String getMethodTemplate = """
                                        public [TYPE] get[PARAMETER_NAME](){
                                             [CONTENT];
                                        }""";

    private static final Pattern extractMaterialParameters = Pattern.compile("MaterialParameters *\\{*([^}]*)}");

    private static final Pattern commentLine = Pattern.compile("^ *//(.*)");

    private static final Pattern paramLine = Pattern.compile("^ *([A-Z][^ :;]*)[ :]+([A-Z][^ :;]*)");

    private static final Map<String, MaterialParameterMetadata> templates = new HashMap<>();

    static{
        templates.put("Int", new MaterialParameterMetadata(
                "int",
                "setInt(\"[PARAMETER_NAME]\", [PARAMETER_NAME_LOWER_CAMEL_CASE])",
                "return getParamValue(\"[PARAMETER_NAME]\")"
                ));
        templates.put("Float", new MaterialParameterMetadata(
                "float",
                "setFloat(\"[PARAMETER_NAME]\", [PARAMETER_NAME_LOWER_CAMEL_CASE])",
                "return getParamValue(\"[PARAMETER_NAME]\")"
        ));
        templates.put("Texture2D", new MaterialParameterMetadata(
                "Texture",
                "setTexture(\"[PARAMETER_NAME]\", [PARAMETER_NAME_LOWER_CAMEL_CASE])",
                "return getParamValue(\"[PARAMETER_NAME]\")"
        ));
        templates.put("Color", new MaterialParameterMetadata(
                "ColorRGBA",
                "setColor(\"[PARAMETER_NAME]\", [PARAMETER_NAME_LOWER_CAMEL_CASE])",
                "return getParamValue(\"[PARAMETER_NAME]\")"
        ));

        BiConsumer<String, String> simpleAddTemplate = (materialType, javaType) -> templates.put(materialType, new MaterialParameterMetadata(
                javaType,
                "setParam(\"[PARAMETER_NAME]\", VarType."+materialType+",  [PARAMETER_NAME_LOWER_CAMEL_CASE])",
                "return getParamValue(\"[PARAMETER_NAME]\")"
        ));

        simpleAddTemplate.accept("Vector2", "Vector2f");
        simpleAddTemplate.accept("Vector3", "Vector3f");
        simpleAddTemplate.accept("Vector4", "Vector4f");

        simpleAddTemplate.accept("IntArray", "int[]");
        simpleAddTemplate.accept("FloatArray", "float[]");
        simpleAddTemplate.accept("Vector2Array", "Vector2f[]");
        simpleAddTemplate.accept("Vector3Array", "Vector3f[]");
        simpleAddTemplate.accept("Vector4Array", "Vector4f[]");

        simpleAddTemplate.accept("Boolean", "boolean");

        simpleAddTemplate.accept("Matrix3", "Matrix3f");
        simpleAddTemplate.accept("Matrix4", "Matrix4f");

        simpleAddTemplate.accept("Matrix3Array", "Matrix3f[]");
        simpleAddTemplate.accept("Matrix4Array", "Matrix4f[]");

        simpleAddTemplate.accept("Texture3D", "Texture3D");
        simpleAddTemplate.accept("TextureArray", "TextureArray");
        simpleAddTemplate.accept("TextureCubeMap", "TextureCubeMap");
        simpleAddTemplate.accept("BufferObject", "BufferObject");
    }



    public static String createMaterialClassFile(String fullDefName, String className, String packageName, String j3mdContents, String generatedFromComment, boolean wrapper) throws IOException{
        Matcher matcher = extractMaterialParameters.matcher(j3mdContents);
        String materialParams = "";
        if (matcher.find()){
            materialParams = matcher.group(1);
        }

        StringBuilder content = new StringBuilder();
        StringBuilder commentUnderConstruction = new StringBuilder();
        boolean anyComment = false;

        BufferedReader bufReader = new BufferedReader(new StringReader(materialParams));

        String line=null;
        while( (line=bufReader.readLine()) != null )
        {

            Matcher commentMatcher = commentLine.matcher(line);
            Matcher paramMatcher = paramLine.matcher(line);
            if (commentMatcher.find()){
                if(anyComment){
                    commentUnderConstruction.append(" <br>\n");
                }

                commentUnderConstruction.append(" * " +commentMatcher.group(1));

                anyComment = true;
            }else if(paramMatcher.find()){
                String type = paramMatcher.group(1);
                String nameUpperCamelCase = paramMatcher.group(2);

                String comment = commentUnderConstruction.toString();


                MaterialParameterMetadata materialParameterMetadata = templates.get(type);

                if (materialParameterMetadata == null){
                    throw new RuntimeException("Unknown material parameter type: " + type);
                }

                String setTemplate = materialParameterMetadata.setTemplate;
                String getTemplate = materialParameterMetadata.getTemplate;

                if(wrapper){
                    setTemplate = "material." + setTemplate;
                    getTemplate = getTemplate.replace("return ", "return material.");
                }

                String setMethod = setMethodTemplate
                        .replace("[TYPE]", materialParameterMetadata.javaType)
                        .replace("[CONTENT]", setTemplate)
                        .replace("[PARAMETER_NAME]", nameUpperCamelCase)
                        .replace("[PARAMETER_NAME_LOWER_CAMEL_CASE]", toLowerCamlCase(nameUpperCamelCase));

                String getMethod = getMethodTemplate
                        .replace("[CONTENT]", getTemplate)
                        .replace("[TYPE]", materialParameterMetadata.javaType)
                        .replace("[PARAMETER_NAME]", nameUpperCamelCase)
                        .replace("[PARAMETER_NAME_LOWER_CAMEL_CASE]", toLowerCamlCase(nameUpperCamelCase));

                if(!comment.isBlank()){
                    content.append("/**\n" + comment + "\n */\n");
                }
                content.append(setMethod).append("\n\n");
                if(!comment.isBlank()){
                    content.append("\n/**\n" + comment + "\n */\n");
                }
                content.append(getMethod).append("\n\n");
                anyComment = false;
                commentUnderConstruction = new StringBuilder();
            }

        }


        String fullClass = (wrapper ? wrapperTemplate : classTemplate)
                .replace("[DEF_NAME]", fullDefName)
                .replace("[MATERIAL_NAME]", className)
                .replace("[PACKAGE]", packageName)
                .replace("[GENERATED_FROM]", generatedFromComment)
                .replace("[CONTENT]", indent(content.toString(), 4));

        return fullClass;
    }

    private static String toLowerCamlCase(String upperCamelCase){
        return upperCamelCase.substring(0, 1).toLowerCase() + upperCamelCase.substring(1);
    }

    /**
     * Indents a multiline string with the requested number of spaces
     */
    private static String indent(String input, int spaces){
        String[] lines = input.split("\n");
        StringBuilder output = new StringBuilder();
        for (String line : lines){
            output.append(" ".repeat(spaces)).append(line).append("\n");
        }
        return output.toString();

    }

    private static String getResourceFileAsString(String fileName) {
        try (InputStream is = MaterialTyper.class.getResourceAsStream(fileName)) {
            if (is == null) return null;
            try (InputStreamReader isr = new InputStreamReader(is);
                BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        } catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    private record MaterialParameterMetadata(String javaType, String setTemplate, String getTemplate){

    }

}
