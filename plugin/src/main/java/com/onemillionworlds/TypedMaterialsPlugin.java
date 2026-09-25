package com.onemillionworlds;

import com.onemillionworlds.configuration.TypedMaterialsExtension;
import com.onemillionworlds.tasks.AssetConstants;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TypedMaterialsPlugin implements Plugin<Project> {
    public static final String DEFAULT_GENERATED_SOURCES_DIR = "src/main/generatedtypedmaterials/java";
    public static final String DEFAULT_GENERATED_RESOURCES_DIR = "src/main/generatedtypedmaterials/resources";

    public void apply(Project project) {

        TypedMaterialsExtension extension = project.getExtensions().create("typedMaterials", TypedMaterialsExtension.class, project);

        project.getPlugins().withType(JavaPlugin.class, javaPlugin -> {
            SourceSet mainSourceSet = project.getExtensions().getByType(SourceSetContainer.class).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            mainSourceSet.getJava().srcDir(extension.getGeneratedSources());
            mainSourceSet.getResources().srcDir(extension.getGeneratedResources());

            // The source set carries these dependencies anyway, but build scripts commonly replace the source dirs
            // (e.g. `srcDirs += ...`), losing them. So make sure the main consumers still depend on the generators
            project.getTasks().named(mainSourceSet.getCompileJavaTaskName()).configure(task -> task.dependsOn(extension.getGeneratedSources()));
            project.getTasks().named(mainSourceSet.getProcessResourcesTaskName()).configure(task -> task.dependsOn(extension.getGeneratedResources()));
        });

        project.getTasks().withType(AssetConstants.class).configureEach(task -> {
            task.getProjectName().convention(project.getName());
            task.getResourceDirectories().from(resourceDirectoriesToScan(project, extension));
        });

        project.getTasks().register("cleanTypedMaterials", Delete.class, task -> {
            task.setGroup("typedMaterials");
            task.delete(extension.getGeneratedSourcesDirectory());
        });

        project.getTasks().register("cleanTypedMaterialResources", Delete.class, task -> {
            task.setGroup("typedMaterials");
            task.delete(extension.getGeneratedResourcesDirectory());
        });

        project.getPlugins().withType(LifecycleBasePlugin.class, basePlugin ->
            project.getTasks().named(LifecycleBasePlugin.CLEAN_TASK_NAME).configure(cleanTask -> {
                cleanTask.dependsOn("cleanTypedMaterials");
                cleanTask.dependsOn("cleanTypedMaterialResources");
            })
        );
    }

    /**
     * The main resource directories of the project, excluding the directory the plugin itself generates resources into
     * (otherwise the assets file would be an input to its own generation).
     * <p>
     * Deliberately uses the plain directories (not the source directory file collection) so that the generated
     * resources directory's task dependencies don't create a dependency cycle.
     * </p>
     */
    private static Provider<Set<File>> resourceDirectoriesToScan(Project project, TypedMaterialsExtension extension){
        return project.provider(() -> {
            SourceSetContainer sourceSets = project.getExtensions().findByType(SourceSetContainer.class);
            Set<File> resourceDirs = sourceSets == null
                    ? Set.of(project.file("src/main/resources"))
                    : sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getResources().getSrcDirs();
            File generatedResourcesDir = extension.getGeneratedResourcesDirectory().get().getAsFile();
            return resourceDirs.stream()
                    .filter(dir -> !dir.equals(generatedResourcesDir))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        });
    }
}
