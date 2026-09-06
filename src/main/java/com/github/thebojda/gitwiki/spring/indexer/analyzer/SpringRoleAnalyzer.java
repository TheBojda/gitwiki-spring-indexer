package com.github.thebojda.gitwiki.spring.indexer.analyzer;

import com.github.javaparser.ast.body.TypeDeclaration;

import java.util.Set;

public class SpringRoleAnalyzer {

    private static final Set<String> CONTROLLER_ANNOTATIONS = Set.of(
            "Controller",
            "RestController"
    );

    private static final Set<String> SERVICE_ANNOTATIONS = Set.of(
            "Service"
    );

    private static final Set<String> REPOSITORY_ANNOTATIONS = Set.of(
            "Repository"
    );

    private static final Set<String> COMPONENT_ANNOTATIONS = Set.of(
            "Component"
    );

    private static final Set<String> CONFIGURATION_ANNOTATIONS = Set.of(
            "Configuration"
    );

    public String detectRole(TypeDeclaration<?> type) {
        for (var annotation : type.getAnnotations()) {
            String name = annotation.getName().getIdentifier();

            if ("RestController".equals(name)) {
                return "rest-controller";
            }

            if (CONTROLLER_ANNOTATIONS.contains(name)) {
                return "controller";
            }

            if (SERVICE_ANNOTATIONS.contains(name)) {
                return "service";
            }

            if (REPOSITORY_ANNOTATIONS.contains(name)) {
                return "repository";
            }

            if (CONFIGURATION_ANNOTATIONS.contains(name)) {
                return "configuration";
            }

            if (COMPONENT_ANNOTATIONS.contains(name)) {
                return "component";
            }

            if ("Entity".equals(name)) {
                return "entity";
            }
        }

        return null;
    }
}
