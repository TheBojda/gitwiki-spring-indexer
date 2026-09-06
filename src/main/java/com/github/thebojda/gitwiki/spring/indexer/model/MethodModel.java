package com.github.thebojda.gitwiki.spring.indexer.model;

import java.util.List;

public record MethodModel(
        String name,
        String signature,
        String returnType,
        int beginLine,
        int endLine,
        List<String> annotations
) {
}