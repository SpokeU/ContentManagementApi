package dev.omyshko.contentmanagement.toolsmain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import lombok.Data;
import lombok.ToString;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileAnalyzer {

    public static void main(String[] args) throws Exception {
        String filePath = "D:\\Development\\Projects\\UserManagementSystem\\src\\main\\java\\dev\\omyshko\\usermanagementsystem\\api\\model\\UserController.java";

        FileAnalyzer analyzer = new FileAnalyzer();
        List<CodeBlock> blocks = analyzer.analyzeFile(filePath);
        System.out.println(new ObjectMapper().writeValueAsString(blocks));
    }


    public List<CodeBlock> analyzeFile(String filePath) throws Exception {
        List<CodeBlock> blocks = new ArrayList<>();
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        CompilationUnit compilationUnit = new JavaParser().parse(content).getResult().orElseThrow();

        compilationUnit.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            CodeBlock classBlock = new CodeBlock();
            classBlock.setFilename(Paths.get(filePath).getFileName().toString());
            classBlock.setPath(filePath);
            classBlock.setBlock(classDecl.getNameAsString());
            classBlock.setCategory("Class");
            classBlock.setTriggerType("Internal Logic"); // Classes are not directly triggered
            classBlock.setDetails("Contains methods and dependencies");

            List<CodeDependency> dependencies = new ArrayList<>();
            classDecl.findAll(MethodDeclaration.class).forEach(method -> {
                dependencies.addAll(findMethodDependencies(method, classDecl.getNameAsString()));
            });

            classBlock.setDependencies(dependencies);
            blocks.add(classBlock);

        });

        return blocks;
    }

    private List<CodeDependency> findMethodDependencies(MethodDeclaration method, String className) {
        List<CodeDependency> dependencies = new ArrayList<>();
        List<MethodCallExpr> callees = method.findAll(MethodCallExpr.class);
        callees.forEach(call -> {
            String fullyQualifiedCallee = "";

            // Check if it's a static method call
            if (call.getScope().isPresent()) {
                // Instance method call (non-static)
                Expression scope = call.getScope().get();
                if (scope instanceof NameExpr) {
                    // Handle case where scope is a class instance variable, like `this.variable.method()`
                    String scopeClassName = resolveClassName(scope);
                    fullyQualifiedCallee = scopeClassName + "#" + call.getNameAsString();
                } else {
                    // Handle other scope cases (e.g., static method call, method call on `super`, etc.)
                    fullyQualifiedCallee = resolveClassName(scope) + "#" + call.getNameAsString();
                }
            } else {
                // Static method call
                fullyQualifiedCallee = resolveClassName(call) + "#" + call.getNameAsString();
            }

            // Add the resolved method dependency
            dependencies.add(new CodeDependency(className, fullyQualifiedCallee));
        });
        return dependencies;
    }

    // Helper function to resolve class name
    private String resolveClassName(Expression expr) {
        // Depending on the structure of your code, you can use this function to resolve the class name
        // For example, if expr is a NameExpr, resolve it as className, etc.
        if (expr instanceof NameExpr) {
            NameExpr nameExpr = (NameExpr) expr;
            return nameExpr.getNameAsString();
        } else if (expr instanceof FieldAccessExpr) {
            // Handle field access expressions
            FieldAccessExpr fieldAccess = (FieldAccessExpr) expr;
            return fieldAccess.getScope().toString();
        }
        // Add more cases as needed (e.g., handling method call on `super`, etc.)
        return "";
    }


    @ToString
    @Data
    public class CodeBlock {
        private String filename;
        private String path;
        private String block;
        private String category;
        private String triggerType;
        private String details;
        private String content;
        private List<CodeDependency> dependencies;

        // Constructors, Getters, Setters, and toString() method omitted for brevity
    }

    @ToString
    @Data
    // Represents a dependency between blocks.
    public class CodeDependency {
        private String block;
        private String type;

        public CodeDependency(String block, String type) {
            this.block = block;
            this.type = type;
        }

        // Getters, Setters, and toString() method omitted for brevity
    }
}
