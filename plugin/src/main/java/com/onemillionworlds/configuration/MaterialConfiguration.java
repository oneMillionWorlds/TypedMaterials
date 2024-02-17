package com.onemillionworlds.configuration;

public class MaterialConfiguration {
    private final String name;
    private String jarFilterRegex;
    private String outputPackage;

    public MaterialConfiguration(String name) {
        this.name = name;
    }

    // Getters and setters for each property
    public String getName() {
        return name;
    }

    public String getJarFilterRegex() {
        return jarFilterRegex;
    }

    public void setJarFilterRegex(String jarFilterRegex) {
        this.jarFilterRegex = jarFilterRegex;
    }

    public String getOutputPackage() {
        return outputPackage;
    }

    public void setOutputPackage(String outputPackage) {
        this.outputPackage = outputPackage;
    }
}
