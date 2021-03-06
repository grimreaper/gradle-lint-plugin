/*
 * Copyright 2015-2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.nebula.lint.plugin

import com.netflix.nebula.lint.GradleViolation
import com.netflix.nebula.lint.rule.GradleAstUtil
import com.netflix.nebula.lint.rule.GradleLintRule
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.control.SourceUnit
import org.codenarc.analyzer.AbstractSourceAnalyzer
import org.codenarc.results.DirectoryResults
import org.codenarc.results.FileResults
import org.codenarc.results.Results
import org.codenarc.rule.Rule
import org.codenarc.ruleset.CompositeRuleSet
import org.codenarc.ruleset.ListRuleSet
import org.codenarc.ruleset.RuleSet
import org.codenarc.source.SourceString
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException

class LintService {
    def registry = new LintRuleRegistry()

    /**
     * An analyzer that can be used over and over again against multiple subprojects, compiling the results, and recording
     * the affected files according to which files the violation fixes touch
     */
    class ReportableAnalyzer extends AbstractSourceAnalyzer {
        DirectoryResults results

        ReportableAnalyzer(Project project) {
            results = new DirectoryResults(project.projectDir.absolutePath)
        }

        Results analyze(String source, RuleSet ruleSet) {
            def violations = (collectViolations(new SourceString(source), ruleSet) as List<GradleViolation>)

            violations.groupBy { it.file }
                .each { file, fileViolations ->
                    results.addChild(new FileResults(file.absolutePath, fileViolations))
                    results.numberOfFilesInThisDirectory++
                }

            results
        }

        @Override
        Results analyze(RuleSet ruleSet) {
            throw new UnsupportedOperationException('use the two argument form instead')
        }

        List getSourceDirectories() {
            []
        }
    }

    private RuleSet ruleSetForProject(Project p) {
        if (p.buildFile.exists()) {
            GradleLintExtension extension
            try {
                extension = p.extensions.getByType(GradleLintExtension)
            } catch (UnknownDomainObjectException ignored) {
                // if the subproject has not applied lint, use the extension configuration from the root project
                extension = p.rootProject.extensions.getByType(GradleLintExtension)
            }

            def rules = (p.hasProperty('gradleLint.rules') ? p.property('gradleLint.rules') : null)?.toString()?.split(',')?.toList() ?:
                    extension.rules + extension.criticalRules

            def includedRules = rules.unique()
                    .collect { registry.buildRules(it, p, extension.criticalRules.contains(it)) }
                    .flatten() as List<Rule>

            def excludedRules = (p.hasProperty('gradleLint.excludedRules') ?
                    p.property('gradleLint.excludedRules').toString().split(',').toList() : []) + extension.excludedRules
            if(!excludedRules.isEmpty())
                includedRules.retainAll { !excludedRules.contains(it.name) }

            return RuleSetFactory.configureRuleSet(includedRules)
        }
        return new ListRuleSet([])
    }

    RuleSet ruleSet(Project project) {
        def ruleSet = new CompositeRuleSet()
        ([project] + project.subprojects).each { p -> ruleSet.addRuleSet(ruleSetForProject(p)) }
        return ruleSet
    }

    Results lint(Project project) {
        def analyzer = new ReportableAnalyzer(project)

        ([project] + project.subprojects).each { p ->
            def ruleSet = ruleSetForProject(p)
            if(!ruleSet.rules.isEmpty()) {
                filesToLint(p).each { f ->
                    // establish which file we are linting for each rule
                    ruleSet.rules.each { rule ->
                        if(rule instanceof GradleLintRule)
                            rule.buildFile = f
                    }

                    analyzer.analyze(f.text, ruleSet)
                }
            }
        }

        return analyzer.results
    }

    List<File> filesToLint(Project p) {
        def files = [p.buildFile]

        def buildAst = new AstBuilder().buildFromString(p.buildFile.text)[0]
        if(buildAst instanceof BlockStatement) {
            new ClassCodeVisitorSupport() {
                @Override
                void visitMethodCallExpression(MethodCallExpression call) {
                    if (call.methodAsString == 'apply') {
                        def args = GradleAstUtil.collectEntryExpressions(call)
                        if(args['from']) {
                            def applyFrom = new File(p.projectDir, args['from'])
                            if(applyFrom.exists()) {
                                files += applyFrom
                            }
                        }
                    }
                    super.visitMethodCallExpression(call)
                }

                @Override
                protected SourceUnit getSourceUnit() {
                    return null // irrelevant
                }
            }.visitBlockStatement(buildAst as BlockStatement)
        }

        files
    }
}
