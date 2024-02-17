/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.onemillionworlds;

import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.api.Project;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TypedMaterialsPluginTest {
    @Test
    void pluginRegisterDefaultTasks() {
        // Create a test project and apply the plugin
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("java");
        project.getPlugins().apply("com.onemillionworlds.typedmaterials");

        // Verify the default tasks are registered
        assertNotNull(project.getTasks().findByName("core"));
        assertNotNull(project.getTasks().findByName("effects"));
    }
}
