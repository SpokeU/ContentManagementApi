package dev.omyshko.contentmanagement;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BiDirectionalCallGraph {
    private static Map<String, List<String>> calleeGraph = new HashMap<>();
    private static Map<String, List<String>> callerGraph = new HashMap<>();

    public static void main(String[] args) throws IOException {
        // Path to the project
        File projectDir = new File("D:\\Development\\Projects\\ContentManagementApi");

        // Fully qualified name of the starting method
        String targetMethod = "dev.omyshko.contentmanagement.api.endpoint.InstructionProcessingEndpoint#processInstructions";

        // Build the call graph
        buildCallGraph(projectDir, targetMethod);

        // Print forward dependencies (callees)
        System.out.println("Callees (Methods Called):");
        printGraph(calleeGraph);

        // Print backward dependencies (callers)
        System.out.println("\nCallers (Methods Calling):");
        printGraph(callerGraph);
    }

    private static void buildCallGraph(File projectDir, String targetMethod) throws IOException {
        JavaParser parser = new JavaParser();

        // Analyze all Java files in the project
        for (File file : listJavaFiles(projectDir)) {
            CompilationUnit cu = parser.parse(file).getResult().orElseThrow();

            cu.findAll(MethodDeclaration.class).forEach(method -> {
                String methodSignature = getMethodSignature(method);

                // Collect forward relationships (callees)
                if (methodSignature.equals(targetMethod)) {
                    new CalleeCollector().visit(method, null);
                }

                // Collect backward relationships (callers)
                new CallerCollector(methodSignature).visit(method, null);
            });
        }
    }

    private static List<File> listJavaFiles(File dir) {
        List<File> files = new ArrayList<>();
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                files.addAll(listJavaFiles(file));
            }
        } else if (dir.getName().endsWith(".java")) {
            files.add(dir);
        }
        return files;
    }

    private static String getMethodSignature(MethodDeclaration method) {
        return method.findAncestor(CompilationUnit.class).flatMap(CompilationUnit::getPackageDeclaration)
                .map(pkg -> pkg.getNameAsString() + ".").orElse("")
                + method.findAncestor(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)
                .map(cls -> cls.getNameAsString() + "#").orElse("")
                + method.getNameAsString();
    }

    private static void printGraph(Map<String, List<String>> graph) {
        graph.forEach((key, value) -> {
            System.out.println(key + " -> " + value);
        });
    }

    // Visitor to collect callees (methods called by this method)
    private static class CalleeCollector extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(MethodCallExpr methodCall, Void arg) {
            super.visit(methodCall, arg);

            String caller = methodCall.findAncestor(MethodDeclaration.class)
                    .map(BiDirectionalCallGraph::getMethodSignature).orElse("Unknown");
            String callee = methodCall.getNameAsString();

            calleeGraph.computeIfAbsent(caller, k -> new ArrayList<>()).add(callee);
        }
    }

    // Visitor to collect callers (methods calling this method)
    private static class CallerCollector extends VoidVisitorAdapter<Void> {
        private final String targetMethod;

        public CallerCollector(String targetMethod) {
            this.targetMethod = targetMethod;
        }

        @Override
        public void visit(MethodCallExpr methodCall, Void arg) {
            super.visit(methodCall, arg);

            String caller = methodCall.findAncestor(MethodDeclaration.class)
                    .map(BiDirectionalCallGraph::getMethodSignature).orElse("Unknown");
            String callee = methodCall.getNameAsString();

            if (callee.equals(targetMethod)) {
                callerGraph.computeIfAbsent(targetMethod, k -> new ArrayList<>()).add(caller);
            }
        }
    }
}
