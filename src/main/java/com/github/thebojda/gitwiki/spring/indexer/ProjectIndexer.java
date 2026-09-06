package com.github.thebojda.gitwiki.spring.indexer;

import com.github.thebojda.gitwiki.spring.indexer.model.JavaFileModel;
import com.github.thebojda.gitwiki.spring.indexer.parser.JavaSourceParser;
import com.github.thebojda.gitwiki.spring.indexer.wiki.MarkdownRenderer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ProjectIndexer {

    private final JavaSourceParser parser;
    private final MarkdownRenderer renderer;

    public ProjectIndexer() {
        this.parser = new JavaSourceParser();
        this.renderer = new MarkdownRenderer();
    }

    public void index(
            Path projectRoot,
            Path wikiRoot
    ) throws IOException {

        Path sourceRoot = projectRoot.resolve("src/main/java");

        if (!Files.isDirectory(sourceRoot)) {
            throw new IllegalArgumentException(
                    "Java source directory not found: " + sourceRoot
            );
        }

        Files.createDirectories(wikiRoot);

        List<Path> javaFiles;

        try (var stream = Files.walk(sourceRoot)) {
            javaFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }

        System.out.println(
                "Found " + javaFiles.size() + " Java source files."
        );

        for (Path javaFile : javaFiles) {
            indexFile(
                    projectRoot,
                    sourceRoot,
                    wikiRoot,
                    javaFile
            );
        }

        System.out.println(
                "Wiki generated at: " + wikiRoot
        );
    }

    private void indexFile(
            Path projectRoot,
            Path sourceRoot,
            Path wikiRoot,
            Path javaFile
    ) throws IOException {

        JavaFileModel model = parser.parse(
                projectRoot,
                javaFile
        );

        Path relativeJavaPath =
                sourceRoot.relativize(javaFile);

        String fileName =
                relativeJavaPath
                        .getFileName()
                        .toString();

        String markdownFileName =
                fileName.substring(
                        0,
                        fileName.length() - ".java".length()
                ) + ".md";

        Path parent =
                relativeJavaPath.getParent();

        Path wikiFile;

        if (parent == null) {
            wikiFile = wikiRoot.resolve(markdownFileName);
        } else {
            wikiFile = wikiRoot
                    .resolve(parent)
                    .resolve(markdownFileName);
        }

        Files.createDirectories(
                wikiFile.getParent()
        );

        String markdown = renderer.render(
                model,
                wikiFile,
                projectRoot
        );

        Files.writeString(
                wikiFile,
                markdown,
                StandardCharsets.UTF_8
        );

        System.out.println(
                relativeJavaPath
                        + " -> "
                        + projectRoot.relativize(wikiFile)
        );
    }
}