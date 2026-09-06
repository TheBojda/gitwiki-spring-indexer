package com.github.thebojda.gitwiki.spring.indexer.model;

import java.util.List;

public record TypeModel(
        String name,
        String kind,
        int beginLine,
        int endLine,
        List<String> annotations,
        List<String> extendedTypes,
        List<String> implementedTypes,
        String springRole,
        List<String> dependencies,
        List<ConstructorModel> constructors,
        List<MethodModel> methods
) {
}