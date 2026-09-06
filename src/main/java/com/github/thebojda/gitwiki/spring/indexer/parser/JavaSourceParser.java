package com.github.thebojda.gitwiki.spring.indexer.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.thebojda.gitwiki.spring.indexer.analyzer.SpringRoleAnalyzer;
import com.github.thebojda.gitwiki.spring.indexer.model.ConstructorModel;
import com.github.thebojda.gitwiki.spring.indexer.model.JavaFileModel;
import com.github.thebojda.gitwiki.spring.indexer.model.MethodModel;
import com.github.thebojda.gitwiki.spring.indexer.model.TypeModel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class JavaSourceParser {

    private final JavaParser javaParser;
    private final SpringRoleAnalyzer springRoleAnalyzer;

    public JavaSourceParser() {
        ParserConfiguration configuration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);

        this.javaParser = new JavaParser(configuration);
        this.springRoleAnalyzer = new SpringRoleAnalyzer();
    }

    public JavaFileModel parse(Path projectRoot, Path sourceFile) throws IOException {
        ParseResult<CompilationUnit> result = javaParser.parse(sourceFile);

        CompilationUnit compilationUnit = result.getResult()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Could not parse Java source file: "
                                + sourceFile
                                + System.lineSeparator()
                                + result.getProblems()
                ));

        String packageName = compilationUnit
                .getPackageDeclaration()
                .map(packageDeclaration -> packageDeclaration.getNameAsString())
                .orElse("");

        List<TypeModel> types = compilationUnit
                .getTypes()
                .stream()
                .map(this::parseType)
                .sorted(Comparator.comparing(TypeModel::name))
                .toList();

        Path relativeSourcePath = projectRoot.relativize(sourceFile);

        return new JavaFileModel(
                relativeSourcePath,
                packageName,
                types
        );
    }

    private TypeModel parseType(TypeDeclaration<?> type) {
        List<String> annotations = type
                .getAnnotations()
                .stream()
                .map(Object::toString)
                .sorted()
                .toList();

        List<String> extendedTypes = List.of();
        List<String> implementedTypes = List.of();

        if (type instanceof ClassOrInterfaceDeclaration declaration) {
            extendedTypes = declaration
                    .getExtendedTypes()
                    .stream()
                    .map(Object::toString)
                    .sorted()
                    .toList();

            implementedTypes = declaration
                    .getImplementedTypes()
                    .stream()
                    .map(Object::toString)
                    .sorted()
                    .toList();
        }

        List<ConstructorModel> constructors = parseConstructors(type);
        List<String> dependencies = parseDependencies(type);
        List<MethodModel> methods = parseMethods(type);

        String springRole = springRoleAnalyzer.detectRole(type);

        return new TypeModel(
                type.getNameAsString(),
                getTypeKind(type),
                getBeginLine(type),
                getEndLine(type),
                annotations,
                extendedTypes,
                implementedTypes,
                springRole,
                dependencies,
                constructors,
                methods
        );
    }

    private List<String> parseDependencies(TypeDeclaration<?> type) {
        List<String> dependencies = new ArrayList<>();

        for (ConstructorDeclaration constructor : type.getConstructors()) {
            if (!isPublicOrProtected(constructor)) {
                continue;
            }

            constructor.getParameters().forEach(parameter ->
                    dependencies.add(parameter.getTypeAsString())
            );
        }

        return dependencies
                .stream()
                .distinct()
                .sorted()
                .toList();
    }

    private List<ConstructorModel> parseConstructors(TypeDeclaration<?> type) {
        return type
                .getConstructors()
                .stream()
                .filter(this::isPublicOrProtected)
                .map(constructor -> new ConstructorModel(
                        constructor.getDeclarationAsString(
                                true,
                                true,
                                true
                        ),
                        getBeginLine(constructor),
                        getEndLine(constructor),
                        constructor.getAnnotations()
                                .stream()
                                .map(Object::toString)
                                .sorted()
                                .toList()
                ))
                .sorted(Comparator.comparing(ConstructorModel::signature))
                .toList();
    }

    private List<MethodModel> parseMethods(TypeDeclaration<?> type) {
        return type
                .getMethods()
                .stream()
                .filter(this::isPublicOrProtected)
                .map(method -> new MethodModel(
                        method.getNameAsString(),
                        method.getDeclarationAsString(
                                true,
                                true,
                                true
                        ),
                        method.getTypeAsString(),
                        getBeginLine(method),
                        getEndLine(method),
                        method.getAnnotations()
                                .stream()
                                .map(Object::toString)
                                .sorted()
                                .toList()
                ))
                .sorted(
                        Comparator
                                .comparing(MethodModel::name)
                                .thenComparing(MethodModel::signature)
                )
                .toList();
    }

    private boolean isPublicOrProtected(MethodDeclaration method) {
        return method.hasModifier(Modifier.Keyword.PUBLIC)
                || method.hasModifier(Modifier.Keyword.PROTECTED);
    }

    private boolean isPublicOrProtected(ConstructorDeclaration constructor) {
        return constructor.hasModifier(Modifier.Keyword.PUBLIC)
                || constructor.hasModifier(Modifier.Keyword.PROTECTED);
    }

    private String getTypeKind(TypeDeclaration<?> type) {
        if (type instanceof ClassOrInterfaceDeclaration declaration) {
            return declaration.isInterface()
                    ? "interface"
                    : "class";
        }

        if (type instanceof EnumDeclaration) {
            return "enum";
        }

        if (type instanceof RecordDeclaration) {
            return "record";
        }

        if (type instanceof AnnotationDeclaration) {
            return "annotation";
        }

        return "type";
    }

    private int getBeginLine(Node node) {
        return node
                .getBegin()
                .map(position -> position.line)
                .orElse(-1);
    }

    private int getEndLine(Node node) {
        return node
                .getEnd()
                .map(position -> position.line)
                .orElse(-1);
    }
}