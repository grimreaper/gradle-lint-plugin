package com.netflix.nebula.lint.rule

import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codenarc.rule.AbstractAstVisitorRule

class UnnecessaryDependencyTupleExpressionRule extends AbstractAstVisitorRule {
    String name = 'UnnecessaryDependencyTupleExpression'
    int priority = 3
    Class astVisitorClass = UnnecessaryDependencyTupleExpressionAstVisitor
}

class UnnecessaryDependencyTupleExpressionAstVisitor extends AbstractLintRule {
    @Override
    void visitMethodCallExpression(MethodCallExpression call) {
        if(call.methodAsString == 'dependencies') {
            def args = call.arguments.expressions as List
            if(!args.empty && args[-1] instanceof ClosureExpression) {
                def code = (args[-1] as ClosureExpression).code as BlockStatement
                code.statements
                    .findAll { it instanceof ExpressionStatement }
                    .collect { it.expression }
                    .findAll {
                        it instanceof MethodCallExpression &&
                        it.arguments instanceof TupleExpression &&
                        it.arguments.expressions.any { it instanceof MapExpression } &&
                        !hasConfExpression(it as MethodCallExpression)
                    }
                    .each { MethodCallExpression m ->
                        def callSource = getSourceCode().line(m.lineNumber - 1)
                        addViolation(m, "Use the shortcut form of the dependency $callSource")
                        correctIfPossible(m)
                    }
            }
        }
        super.visitMethodCallExpression(call)
    }

    /**
     * There is no way to express a conf in the generally preferred shortened 'group:artifact:version' style syntax
     * @param m
     * @return <code>true</code> if a conf is present on this dependency
     */
    static boolean hasConfExpression(MethodCallExpression m) {
        def tuple = m.arguments as TupleExpression
        return tuple.expressions
                .collect { it.mapEntryExpressions }
                .flatten()
                .any {
                    it instanceof MapEntryExpression &&
                            it.keyExpression instanceof ConstantExpression &&
                            it.keyExpression.value == 'conf'
                }
    }

    void correctIfPossible(MethodCallExpression m) {
        if(!isCorrectable()) return

        def args = (m.arguments.expressions.find { it instanceof MapExpression } as MapExpression)
                .mapEntryExpressions
        def group = '', artifact = '', version = ''
        args.each {
            def val = it.valueExpression.text
            switch(it.keyExpression.text) {
            case 'group':
                group = val; break
            case 'name':
                artifact = val; break
            case 'version':
                version = val; break
            }
        }
        correctableSourceCode.inlineReplace(m, "${m.method.text} '$group:$artifact:$version'")
    }
}