package com.github.thebojda.gitwiki.spring.indexer.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.thebojda.gitwiki.spring.indexer.model.FieldModel;
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

    public JavaSourceParser() {
        ParserConfiguration configuration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);

        this.javaParser = new JavaParser(configuration);
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

        List<FieldModel> fields = parseFields(type);
        List<MethodModel> methods = parseMethods(type);

        return new TypeModel(
                type.getNameAsString(),
                getTypeKind(type),
                getBeginLine(type),
                getEndLine(type),
                annotations,
                fields,
                methods
        );
    }

    private List<FieldModel> parseFields(TypeDeclaration<?> type) {
        List<FieldModel> result = new ArrayList<>();

        for (FieldDeclaration field : type.getFields()) {

            List<String> annotations = field
                    .getAnnotations()
                    .stream()
                    .map(Object::toString)
                    .sorted()
                    .toList();

            for (VariableDeclarator variable : field.getVariables()) {

                String modifiers = field
                        .getModifiers()
                        .stream()
                        .map(modifier -> modifier.getKeyword().asString())
                        .reduce(
                                "",
                                (left, right) -> left + right + " "
                        );

                String declaration =
                        modifiers
                                + variable.getTypeAsString()
                                + " "
                                + variable.getNameAsString()
                                + variable.getInitializer()
                                .map(initializer -> " = " + initializer)
                                .orElse("")
                                + ";";

                result.add(
                        new FieldModel(
                                variable.getNameAsString(),
                                variable.getTypeAsString(),
                                declaration.trim(),
                                getBeginLine(field),
                                getEndLine(field),
                                annotations
                        )
                );
            }
        }

        return result
                .stream()
                .sorted(Comparator.comparing(FieldModel::name))
                .toList();
    }

    private List<MethodModel> parseMethods(TypeDeclaration<?> type) {
        return type
                .getMethods()
                .stream()
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