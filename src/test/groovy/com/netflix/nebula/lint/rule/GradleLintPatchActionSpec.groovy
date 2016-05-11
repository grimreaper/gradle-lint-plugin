import java.nio.file.Files

    GradleViolation violation

    @Rule
    TemporaryFolder temp
        violation = new GradleViolation(GradleViolation.Level.Warning,
                temp.root, // does not matter
                null, // does not matter
                1, // does not matter
                'doesnotmatter',
                'doesnotmatter')
        '''.stripIndent()
        def fix = new GradleLintReplaceWith(violation, f, 2..2, 1, 2, '*')
            diff --git a/my.txt b/my.txt
    def 'delete file patch'() {
        setup:
        def f = temp.newFile('my.txt')

        f.text = '''\
        a
        '''.stripIndent()

        when:
        def fix = new GradleLintDeleteFile(violation, f)
        def patch = new GradleLintPatchAction(project).patch([fix])

        then:
        patch == '''
            diff --git a/my.txt b/my.txt
            deleted file mode 100644
            --- a/my.txt
            +++ /dev/null
            @@ -1,1 +0,0 @@
            -a
             '''.substring(1).stripIndent()
    }

    def 'create regular file patch'() {
        setup:
        def f = new File(project.rootDir, 'my.txt')

        when:
        def fix = new GradleLintCreateFile(violation, f, 'hello')
        def patch = new GradleLintPatchAction(project).patch([fix])

        then:
        patch == '''
            diff --git a/my.txt b/my.txt
            new file mode 100644
            --- /dev/null
            +++ b/my.txt
            @@ -0,0 +1,1 @@
            +hello
            \\ No newline at end of file
             '''.substring(1).stripIndent()
    }

    def 'replaceAll patch'() {
        setup:
        def f = temp.newFile('my.txt')

        f.text = '''\
        a
        b
        c
        '''.stripIndent()

        def changes = '''\
        hello
        multiline
        '''.stripIndent()

        when:
        def lines = f.readLines()
        def fix = new GradleLintReplaceWith(violation, f, 1..lines.size(), 1, lines[-1].length() + 1, changes)
        def patch = new GradleLintPatchAction(project).patch([fix])

        then:
        patch == '''
            diff --git a/my.txt b/my.txt
            --- a/my.txt
            +++ b/my.txt
            @@ -1,3 +1,2 @@
            -a
            -b
            -c
            +hello
            +multiline
             '''.substring(1).stripIndent()
    }

    def 'create executable file patch'() {
        setup:
        def f = new File(project.rootDir, 'exec.sh')
        f.text = 'execute me'

        when:
        def fix = new GradleLintCreateFile(violation, f, 'hello', FileMode.Executable)
        def patch = new GradleLintPatchAction(project).patch([fix])

        then:
        patch == '''
            diff --git a/exec.sh b/exec.sh
            new file mode 100755
            --- /dev/null
            +++ b/exec.sh
            @@ -0,0 +1,1 @@
            +hello
            \\ No newline at end of file
             '''.substring(1).stripIndent()
    }

    def 'delete symlink and replace with executable'() {
        setup:
        def f = temp.newFile('real.txt')
        f.text = 'hello world'
        def symlink = new File(project.rootDir, 'gradle')
        Files.createSymbolicLink(symlink.toPath(), f.toPath())

        when:
        def delete = new GradleLintDeleteFile(violation, symlink)
        def create = new GradleLintCreateFile(violation, new File(project.rootDir, 'gradle/some/dir.txt'), 'new file', FileMode.Executable)
        def patch = new GradleLintPatchAction(project).patch([delete, create])

        then:
        patch == """\
            diff --git a/gradle b/gradle
            deleted file mode 120000
            --- a/gradle
            +++ /dev/null
            @@ -1,1 +0,0 @@
            -${f.absolutePath}
            \\ No newline at end of file
            diff --git a/gradle/some/dir.txt b/gradle/some/dir.txt
            new file mode 100755
            --- /dev/null
            +++ b/gradle/some/dir.txt
            @@ -0,0 +1,1 @@
            +new file
            \\ No newline at end of file
            """.stripIndent()
    }


    def 'delete symlink and create file patch'() {
        setup:
        def f = temp.newFile('real.txt')
        f.text = 'hello world'
        def symlink = new File(project.rootDir, 'gradle')
        Files.createSymbolicLink(symlink.toPath(), f.toPath())

        when:
        def delete = new GradleLintDeleteFile(violation, symlink)
        def create = new GradleLintCreateFile(violation, new File(project.rootDir, 'gradle/some/dir.txt'), 'new file')
        def patch = new GradleLintPatchAction(project).patch([delete, create])

        then:
        patch == """\
            diff --git a/gradle b/gradle
            deleted file mode 120000
            --- a/gradle
            +++ /dev/null
            @@ -1,1 +0,0 @@
            -${f.absolutePath}
            \\ No newline at end of file
            diff --git a/gradle/some/dir.txt b/gradle/some/dir.txt
            new file mode 100644
            --- /dev/null
            +++ b/gradle/some/dir.txt
            @@ -0,0 +1,1 @@
            +new file
            \\ No newline at end of file
            """.stripIndent()
    }

    def 'delete and create patches'() {
        setup:
        def f = temp.newFile('my.txt')

        f.text = '''\
        a
        b
        c
        '''.stripIndent()

        when:
        def delFix = new GradleLintDeleteFile(violation, f)
        def createFix = new GradleLintCreateFile(violation, f, 'hello')
        def patch = new GradleLintPatchAction(project).patch([delFix, createFix])

        then:
        patch == '''
            diff --git a/my.txt b/my.txt
            deleted file mode 100644
            --- a/my.txt
            +++ /dev/null
            @@ -1,3 +0,0 @@
            -a
            -b
            -c
            diff --git a/my.txt b/my.txt
            new file mode 100644
            --- /dev/null
            +++ b/my.txt
            @@ -0,0 +1,1 @@
            +hello
            \\ No newline at end of file
             '''.substring(1).stripIndent()
    }

        def fix = new GradleLintReplaceWith(violation, f, 1..1, 2, 3, '*')
            diff --git a/my.txt b/my.txt
        def fix = new GradleLintReplaceWith(violation, f, 1..2, 2, 3, '*')
            diff --git a/my.txt b/my.txt
            diff --git a/my.txt b/my.txt
        generator.patch([new GradleLintReplaceWith(violation, f, 1..1, 1, 2, '')]) == expect
        generator.patch([new GradleLintReplaceWith(violation, f, 1..1, 1, -1, '')]) == expect
        generator.patch([new GradleLintDeleteLines(violation, f, 1..1)]) == expect
            diff --git a/my.txt b/my.txt
        generator.patch([new GradleLintDeleteLines(violation, f, 1..1)]) == expect
        generator.patch([new GradleLintInsertAfter(violation, f, 1, 'b')]) == '''
            diff --git a/my.txt b/my.txt
        generator.patch([new GradleLintInsertBefore(violation, f, 1, 'b')]) == '''
            diff --git a/my.txt b/my.txt
        '''.stripIndent()
        def fix1 = new GradleLintReplaceWith(violation, f, 1..1, 1, 2, '*')
        def fix2 = new GradleLintReplaceWith(violation, f, 9..9, 1, 2, '*')
            diff --git a/my.txt b/my.txt
            @@ -1,9 +1,9 @@
             e
    def 'patches whose starts overlap'() {
        '''.stripIndent()
        def fix1 = new GradleLintReplaceWith(violation, f, 2..3, 1, 2, 'd')
        def fix2 = new GradleLintInsertBefore(violation, f, 2, '*')
            diff --git a/my.txt b/my.txt
            --- a/my.txt
            +++ b/my.txt
            @@ -1,3 +1,3 @@
             a
            +*
            -b
            -c
            +d
            '''.substring(1).stripIndent()
    }

    def 'patches that overlap by one line'() {
        setup:
        def f = temp.newFile('my.txt')

        f.text = '''\
        a
        b
        c
        d
        e
        f
        g
        '''.stripIndent()

        when:
        def fix1 = new GradleLintReplaceWith(violation, f, 1..1, 1, 2, '*')
        def fix2 = new GradleLintReplaceWith(violation, f, 7..7, 1, 2, '*')
        def patch = new GradleLintPatchAction(project).patch([fix1, fix2])

        then:
        patch == '''
            diff --git a/my.txt b/my.txt
            --- a/my.txt
            +++ b/my.txt
            @@ -1,7 +1,7 @@
            -a
            +*
             b
             c
             d
             e
             f
            -g
            +*
            '''.substring(1).stripIndent()
    }

    def 'overlapping patch context'() {
        setup:
        def f = temp.newFile('my.txt')

        f.text = '''\
        a
        b
        c
        '''.stripIndent()

        when:
        def fix1 = new GradleLintReplaceWith(violation, f, 1..1, 1, 2, '*')
        def fix2 = new GradleLintReplaceWith(violation, f, 3..3, 1, 2, '*')
        def patch = new GradleLintPatchAction(project).patch([fix1, fix2])

        then:
        patch == '''
            diff --git a/my.txt b/my.txt
        '''.stripIndent()
        def fix = new GradleLintReplaceWith(violation, f, 1..1, 1, 2, '*')
            diff --git a/my.txt b/my.txt
        '''.stripIndent()
        def fix = new GradleLintReplaceWith(violation, f, 3..3, 1, 2, '*')
            diff --git a/my.txt b/my.txt
        def fix1 = new GradleLintInsertAfter(violation, f, 1, 'c')
        def fix2 = new GradleLintDeleteLines(violation, f, 3..3)
            diff --git a/my.txt b/my.txt
        b'''.stripIndent()
        def fix = new GradleLintReplaceWith(violation, f, 1..1, 1, 2, '*')
            diff --git a/my.txt b/my.txt
        def fix = new GradleLintReplaceWith(violation, f, 1..1, 1, 2, '*')
            diff --git a/my.txt b/my.txt
        def fix = new GradleLintInsertAfter(violation, f, 1, 'b\n')
            diff --git a/my.txt b/my.txt
            diff --git a/my.txt b/my.txt
        def fix = new GradleLintInsertBefore(violation, f, 1, 'a\n')
        fix = new GradleLintInsertAfter(violation, f, 0, 'a\n')

    def 'overlapping patches (patches that occupy the same line)'() {
        setup:
        def f = temp.newFile('my.txt')

        f.text = '''\
        ab
        c
        '''.stripIndent()

        when:
        def fix1 = new GradleLintReplaceWith(violation, f, 1..2, 1, 2, 'e')
        def fix2 = new GradleLintReplaceWith(violation, f, 1..1, 2, 3, 'f')
        def patch = new GradleLintPatchAction(project).patch([fix1, fix2])

        then: 'the second fix is ignored, and would be best applied on a second pass'
        patch == '''
            diff --git a/my.txt b/my.txt
            --- a/my.txt
            +++ b/my.txt
            @@ -1,2 +1,1 @@
            -ab
            -c
            +e
            '''.substring(1).stripIndent()
    }