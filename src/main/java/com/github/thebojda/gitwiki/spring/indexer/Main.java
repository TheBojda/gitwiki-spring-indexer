package com.github.thebojda.gitwiki.spring.indexer;

import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "gitwiki-spring-indexer",
        mixinStandardHelpOptions = true,
        description = "Generates a deterministic Markdown wiki from a Spring Java project."
)
public class Main implements Callable<Integer> {

    @CommandLine.Parameters(
            index = "0",
            description = "Path to the Spring project.",
            defaultValue = "."
    )
    private Path projectPath;

    @CommandLine.Option(
            names = {"-o", "--output"},
            description = "Wiki output directory relative to the project.",
            defaultValue = "wiki"
    )
    private Path outputPath;

    @Override
    public Integer call() throws Exception {
        Path projectRoot = projectPath.toAbsolutePath().normalize();
        Path wikiRoot = projectRoot.resolve(outputPath).normalize();

        ProjectIndexer indexer = new ProjectIndexer();

        indexer.index(projectRoot, wikiRoot);

        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}