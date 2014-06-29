package net.rolandfg.bin

import static net.rolandfg.bin.JsonTransformer.*

class JsonTransformerTest extends GroovyTestCase {

    void testCliBuilderOptionsHavingParams() {
        String rootNode = "root"
        String filterExpr = "filter"
        String mapExpr = "map"
        String sortExpr = "sort"
        String fileName = "input.json"
        String[] args = " -f ${filterExpr} -m ${mapExpr} -s ${sortExpr} --root ${rootNode} ${fileName}".split()
        String[] longArgs = " --filter ${filterExpr} --map ${mapExpr} --sort ${sortExpr} --root ${rootNode} ${fileName}".split()

        CliBuilder cli = cliBuilder()

        def check = { options ->
            assert options, "Invalid args"
            assert options.arguments() && options.arguments()[0] == fileName, "Input file NOT parsed"
            assert options.f && options.f == filterExpr && options.filter == filterExpr, "Filter expression NOT parsed: ${options.f}"
            assert options.m && options.m == mapExpr && options.map == mapExpr, "Map expression NOT parsed"
            assert options.s && options.s == sortExpr && options.sort == sortExpr, "Sort expression NOT parsed"
            assert options.root && options.root == rootNode, "Root node expression NOT parsed"
        }

        check(cli.parse(args))
        check(cli.parse(longArgs))
    }

    void testCliBuilderOptionsWithNoParams() {
        String[] args = " -x -h -t -p --quickstart --sort-desc".split()
        String[] longArgs = " --debug --help --flat --pretty --quickstart --sort-desc".split()

        def check = { options ->
            assert options, "Invalid args"
            assert options.x
            assert options.h
            assert options.t
            assert options.p
            assert options.quickstart
            assert options.'sort-desc'
        }

        def cli = cliBuilder()
        check(cli.parse(args))
        check(cli.parse(longArgs))
    }

    void testBuildExpr() {
        String expectedExpr = "root.findAll{ _ -> _.age > 18 }.sort{ _ -> _.age }.reverse().collect{ _ -> _.name }"
        String expectedExprWithDebug = "nodes = root.findAll{ _ -> println '    Filter: ' + _; _.age > 18 }.sort{ _ -> println '      Sort: ' + _; _.age }.reverse().collect{ _ -> println '       Map: ' + _; _.name };println '    Output: ' + nodes;nodes"
        Map options = [ f: "_.age > 18", m: "_.name", s: "_.age", "sort-desc": true]

        assert buildExpr(options) == expectedExpr

        options.x = true
        assert buildExpr(options) == expectedExprWithDebug
    }
}
