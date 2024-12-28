package dev.omyshko.contentmanagement;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TransitiveCallGraph {
    private static Map<String, List<String>> calleeGraph = new HashMap<>();
    private static Map<String, List<String>> callerGraph = new HashMap<>();

    public static void main(String[] args) throws IOException {
        // Path to the project
        File projectDir = new File("D:\\Development\\Projects\\ContentManagementApi");

        // Fully qualified name of the starting method
        String targetMethod = "dev.omyshko.contentmanagement.api.endpoint.InstructionProcessingEndpoint#processInstructions";

        // Build the initial call graph
        buildCallGraph(projectDir);

        // Perform recursive traversal for complete dependencies
        System.out.println("Recursive Call Graph for Target Method:");
        System.out.println("Target Method: " + targetMethod);

        // Transitive callees
        System.out.println("\nAll Transitive Callees:");
        Set<String> allCallees = getTransitiveDependencies(targetMethod, calleeGraph);
        allCallees.forEach(System.out::println);

        // Transitive callers
        System.out.println("\nAll Transitive Callers:");
        Set<String> allCallers = getTransitiveDependencies(targetMethod, callerGraph);
        allCallers.forEach(System.out::println);
    }

    private static void buildCallGraph(File projectDir) throws IOException {
        JavaParser parser = new JavaParser();

        // Analyze all Java files in the project
        for (File file : listJavaFiles(projectDir)) {
            CompilationUnit cu = parser.parse(file).getResult().orElseThrow();

            cu.findAll(MethodDeclaration.class).forEach(method -> {
                String methodSignature = getMethodSignature(method);

                // Collect forward relationships (callees)
                new CalleeCollector(methodSignature).visit(method, null);

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

    private static Set<String> getTransitiveDependencies(String method, Map<String, List<String>> graph) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(method);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (visited.contains(current)) continue;

            visited.add(current);
            List<String> dependencies = graph.getOrDefault(current, Collections.emptyList());
            queue.addAll(dependencies);
        }

        return visited;
    }

    // Visitor to collect callees (methods called by this method)
    private static class CalleeCollector extends VoidVisitorAdapter<Void> {
        private final String methodSignature;

        public CalleeCollector(String methodSignature) {
            this.methodSignature = methodSignature;
        }

        @Override
        public void visit(MethodCallExpr methodCall, Void arg) {
            super.visit(methodCall, arg);

            String callee = methodCall.getNameAsString();

            calleeGraph.computeIfAbsent(methodSignature, k -> new ArrayList<>()).add(callee);
        }
    }

    // Visitor to collect callers (methods calling this method)
    private static class CallerCollector extends VoidVisitorAdapter<Void> {
        private final String methodSignature;

        public CallerCollector(String methodSignature) {
            this.methodSignature = methodSignature;
        }

        @Override
        public void visit(MethodCallExpr methodCall, Void arg) {
            super.visit(methodCall, arg);

            String caller = methodCall.findAncestor(MethodDeclaration.class)
                    .map(TransitiveCallGraph::getMethodSignature).orElse("Unknown");
            String callee = methodCall.getNameAsString();

            if (callee.equals(methodSignature)) {
                callerGraph.computeIfAbsent(methodSignature, k -> new ArrayList<>()).add(caller);
            }
        }
    }
}
