import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;

/** Standalone editor tool; uses the JDT libraries installed by Zed's Java extension. */
public final class SortMembers {
    public static void main(String[] args) throws Exception {
        String source = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> options = new HashMap<>();
        JavaCore.setComplianceOptions(JavaCore.latestSupportedJavaVersion(), options);
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setCompilerOptions(options);
        parser.setSource(source.toCharArray());
        CompilationUnit unit = (CompilationUnit) parser.createAST(null);
        for (IProblem problem : unit.getProblems()) {
            if (problem.isError()) {
                throw new IllegalArgumentException("Line " + problem.getSourceLineNumber() + ": " + problem.getMessage());
            }
        }
        ASTRewrite rewrite = ASTRewrite.create(unit.getAST());
        unit.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration type) {
                order(type, type.bodyDeclarations(), rewrite.getListRewrite(type, TypeDeclaration.BODY_DECLARATIONS_PROPERTY));
                return true;
            }

            @Override
            public boolean visit(EnumDeclaration type) {
                order(type, type.bodyDeclarations(), rewrite.getListRewrite(type, EnumDeclaration.BODY_DECLARATIONS_PROPERTY));
                return true;
            }

            @Override
            public boolean visit(RecordDeclaration type) {
                order(type, type.bodyDeclarations(), rewrite.getListRewrite(type, RecordDeclaration.BODY_DECLARATIONS_PROPERTY));
                return true;
            }

            @Override
            public boolean visit(AnonymousClassDeclaration type) {
                order(type, type.bodyDeclarations(), rewrite.getListRewrite(type, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY));
                return true;
            }
        });
        Document document = new Document(source);
        rewrite.rewriteAST(document, options).apply(document);
        System.out.write(document.get().getBytes(StandardCharsets.UTF_8));
    }

    private static void order(ASTNode owner, List<?> declarations, ListRewrite rewrite) {
        List<MethodDeclaration> methods = declarations.stream()
            .filter(MethodDeclaration.class::isInstance)
            .map(MethodDeclaration.class::cast)
            .filter(method -> !method.isConstructor())
            .toList();
        List<MethodDeclaration> ordered = new ArrayList<>();
        for (int visibility : new int[] { Modifier.PUBLIC, Modifier.PROTECTED, 0 }) {
            for (MethodDeclaration method : methods) {
                int flags = method.getModifiers() & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE);
                if (flags == 0 && owner instanceof TypeDeclaration type && type.isInterface()) {
                    flags = Modifier.PUBLIC;
                }
                if (flags == visibility) {
                    ordered.add(method);
                }
            }
        }
        Set<MethodDeclaration> visited = new HashSet<>();
        List<MethodDeclaration> privateMethods = new ArrayList<>();
        for (MethodDeclaration root : ordered) {
            dependencies(root, owner, methods, visited, privateMethods);
        }
        for (MethodDeclaration method : methods) {
            if (Modifier.isPrivate(method.getModifiers()) && visited.add(method)) {
                privateMethods.add(method);
            }
        }
        ordered.addAll(privateMethods);
        for (int i = 0; i < methods.size(); i++) {
            if (methods.get(i) != ordered.get(i)) {
                rewrite.replace(methods.get(i), rewrite.getASTRewrite().createMoveTarget(ordered.get(i)), null);
            }
        }
    }

    private static void dependencies(MethodDeclaration method, ASTNode owner, List<MethodDeclaration> methods,
        Set<MethodDeclaration> visited, List<MethodDeclaration> ordered) {
        method.accept(new ASTVisitor() {
            private void call(String name, int arity) {
                List<MethodDeclaration> candidates = methods.stream()
                    .filter(candidate -> candidate.getName().getIdentifier().equals(name))
                    .filter(candidate -> accepts(candidate, arity))
                    .toList();
                if (candidates.stream().noneMatch(candidate -> Modifier.isPrivate(candidate.getModifiers()))) {
                    return;
                }
                if (candidates.size() != 1) {
                    throw new IllegalArgumentException("Ambiguous overload " + name + "; member sorting was not applied.");
                }
                MethodDeclaration target = candidates.getFirst();
                if (visited.add(target)) {
                    ordered.add(target);
                    dependencies(target, owner, methods, visited, ordered);
                }
            }

            private boolean local(String receiver) {
                String ownerName = owner instanceof org.eclipse.jdt.core.dom.AbstractTypeDeclaration type
                    ? type.getName().getIdentifier() : "";
                return receiver.equals("this") || (!ownerName.isEmpty() && receiver.equals(ownerName));
            }

            @Override
            public boolean visit(MethodInvocation call) {
                if (call.getExpression() == null || local(call.getExpression().toString())) {
                    call(call.getName().getIdentifier(), call.arguments().size());
                }
                return true;
            }

            @Override
            public boolean visit(ExpressionMethodReference reference) {
                if (local(reference.getExpression().toString())) {
                    call(reference.getName().getIdentifier(), -1);
                }
                return true;
            }

            @Override
            public boolean visit(TypeMethodReference reference) {
                if (local(reference.getType().toString())) {
                    call(reference.getName().getIdentifier(), -1);
                }
                return true;
            }

            @Override public boolean visit(TypeDeclaration type) { return false; }
            @Override public boolean visit(EnumDeclaration type) { return false; }
            @Override public boolean visit(RecordDeclaration type) { return false; }
            @Override public boolean visit(AnonymousClassDeclaration type) { return false; }
        });
    }

    private static boolean accepts(MethodDeclaration method, int arity) {
        if (arity < 0 || method.parameters().size() == arity) {
            return true;
        }
        return !method.parameters().isEmpty()
            && ((SingleVariableDeclaration) method.parameters().getLast()).isVarargs()
            && arity >= method.parameters().size() - 1;
    }
}
