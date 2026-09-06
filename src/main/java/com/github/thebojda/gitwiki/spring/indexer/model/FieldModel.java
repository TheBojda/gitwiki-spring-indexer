package com.github.thebojda.gitwiki.spring.indexer.model;

import java.util.List;

public record FieldModel(
        String name,
        String type,
        String declaration,
        int beginLine,
        int endLine,
        List<String> annotations
) {
}