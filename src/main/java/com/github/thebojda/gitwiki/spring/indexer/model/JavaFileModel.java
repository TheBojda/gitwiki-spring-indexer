package com.github.thebojda.gitwiki.spring.indexer.model;

import java.nio.file.Path;
import java.util.List;

public record JavaFileModel(
        Path sourcePath,
        String packageName,
        List<TypeModel> types
) {
}