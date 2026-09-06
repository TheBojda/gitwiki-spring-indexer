package com.github.thebojda.gitwiki.spring.indexer.model;

import java.util.List;

public record ConstructorModel(
        String signature,
        int beginLine,
        int endLine,
        List<String> annotations
) {
}
