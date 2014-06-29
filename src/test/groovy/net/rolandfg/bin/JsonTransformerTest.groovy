package net.rolandfg.bin

import groovy.json.JsonSlurper

import static net.rolandfg.bin.JsonTransformer.*

class JsonTransformerTest extends GroovyTestCase {

    // -------------------------------------------------------------------------------------------------- cli parser

    void testBuildExprSimple() {
        String expectedExpr = "root.findAll{ _ -> _.age > 18 }.sort{ _ -> _.age }.reverse().collect{ _ -> _.name }"
        String expectedExprWithDebug =  "nodes = root.findAll{ _ -> println '    Filter: ' + _; _.age > 18 }.sort{ _ -> println '      Sort: ' + _; _.age }.reverse().collect{ _ -> println '    Reduce: ' + _; _.name };println '    Output: ' + nodes;nodes"
        Map options = [ f: "_.age > 18", e: "_.name", s: "_.age", "sort-desc": true]

        assert buildExpr(options) == expectedExpr

        options.x = true
        assert buildExpr(options) == expectedExprWithDebug
    }

    void testCliBuilderOptionsHavingParams() {
        String rootNode = "root"
        String filterExpr = "filter"
        String mapExpr = "map"
        String sortExpr = "sort"
        String fileName = "input.json"
        String[] args = " -f ${filterExpr} -e ${mapExpr} -s ${sortExpr} --root ${rootNode} ${fileName}".split()
        String[] longArgs = " --filter ${filterExpr} --reduce ${mapExpr} --sort ${sortExpr} --root ${rootNode} ${fileName}".split()

        CliBuilder cli = cliBuilder()

        def check = { options ->
            assert options, "Invalid args"
            assert options.arguments() && options.arguments()[0] == fileName, "Input file NOT parsed"
            assert options.f && options.f == filterExpr && options.filter == filterExpr, "Filter expression NOT parsed: ${options.f}"
            assert options.e && options.e == mapExpr && options.reduce == mapExpr, "Reduce expression NOT parsed"
            assert options.s && options.s == sortExpr && options.sort == sortExpr, "Sort expression NOT parsed"
            assert options.root && options.root == rootNode, "Root node expression NOT parsed"
        }

        check(cli.parse(args))
        check(cli.parse(longArgs))
    }

    // -------------------------------------------------------------------------------------------------- expressions builder

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

    void testBuildExprComplex() {
        String expectedExpr = "root.findAll{ _ -> _.name.endsWith('e') }.collect{ _ -> [(_.name) : (_.val)] }"
        Map options = [ flat: true, f : "_.name.endsWith('e')", e: "[(_.name) : (_.val)]"]
//        root.findAll{ _ -> _.name.endsWith('e') }.collect{ _ -> [(_.name) : (_.val)] }
//        root.findAll{ _ -> _.name.endsWith('e') }.collect{  _ -> [(_.name) : (_.val)] }
        assert buildExpr(options) == expectedExpr
    }

    // -------------------------------------------------------------------------------------------------- transformation

    void testFilter() {
        List items = [[age:19, name:"Andrea"], [age:21, name:"Beatrice"], [age:16, name:"Carlo"]]
        List expectedItems = [[age:21, name:"Beatrice"]]
        String expr = "root.findAll { _ -> _.age > 19 }"

        assert transform(items, expr) == expectedItems
    }

    void testMap() {
        List items = [[age:19, name:"Andrea"], [age:21, name:"Beatrice"], [age:16, name:"Carlo"]]
        List expectedItems = items.collect { it.name }
        String expr = "root.collect { _ -> _.name }"

        assert transform(items, expr) == expectedItems
    }

    void testSort() {
        List items = [[age:19, name:"Andrea"], [age:21, name:"Beatrice"], [age:16, name:"Carlo"]]
        List expectedItems = items.sort { it.age }
        String expr = "root.sort { _ -> _.age > 19 }"

        assert transform(items, expr) == expectedItems
    }

    // -------------------------------------------------------------------------------------------------- integration tests

    def checkJSONTransformation = { String content, String[] args, expectedOutput ->
        def options = cliBuilder().parse(args)
        assert options, "Invalid CLI"

        def jsonContent = new JsonSlurper().parseText(content)
        String source = buildExpr(options)
        assert source

        assert transform(options.root ? (jsonContent."${options.root}") : jsonContent, source) == expectedOutput
    }

    void testIntegrationSimple() {
        String content = '[{"name":"Andrea","age":19},{"name":"Beatrice", "age": 21},{"name":"Carlo", "age":16}]'
        String[] cli = ["-x", "-f", "_.age > 18", "-e", "_.name", "-s", "_.age", "--sort-desc"]
        List expectedOutput = ["Beatrice","Andrea"]

        checkJSONTransformation(content, cli, expectedOutput)
    }

    void testIntegrationComplex() {
        String content = '''{
                                "type": "sample",
                                "link": "http://file-sample.com/json",
                                "name": "JavaScript Object Notation",
                                "metadata": [
                                    {
                                        "name": "extension",
                                        "val": ".json"
                                    },
                                    {
                                        "name": "media_type",
                                        "val": "application/json"
                                    },
                                    {
                                        "name": "website",
                                        "val": "json.org"
                                    }
                                ]
                            }'''

        String[] cli = [ "--root", "metadata", "--flat", "-f","_.name.endsWith('e')", "-e", "[(_.name) : (_.val)]" ]
        List expectedOutput = [[media_type:"application/json"], [website:"json.org"]]

        checkJSONTransformation(content, cli, expectedOutput)
    }
}
