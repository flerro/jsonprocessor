/*
    The MIT License (MIT)

    Copyright (c) 2014 Francesco Lerro

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
*/

package net.rolandfg.bin

import groovy.json.JsonSlurper

import static JsonProcessor.*

class JsonProcessorTest extends GroovyTestCase {

    // -------------------------------------------------------------------------------------------------- cli parser

    void testBuildExprSimpleEntries() {
        String expectedExpr = "root.findAll{ _ -> _.age > 18 }.sort{ _ -> _.age }.reverse().collectEntries{ _ -> _.name }"
        String expectedExprWithDebug =  "nodes = root.findAll{ _ -> println '    Filter: ' + _; _.age > 18 }.sort{ _ -> println '      Sort: ' + _; _.age }.reverse().collectEntries{ _ -> println '   Entries: ' + _; _.name };println '    Output: ' + nodes;nodes"
        Map options = [ f: "_.age > 18", e: "_.name", s: "_.age", "sort-desc": true]

        assert buildExpr(options) == expectedExpr

        options.x = true
        assert buildExpr(options) == expectedExprWithDebug
    }

    void testBuildExprSimple() {
        String expectedExpr = "root.findAll{ _ -> _.age > 18 }.sort{ _ -> _.age }.reverse().collect{ _ -> _.name }"
        String expectedExprWithDebug =  "nodes = root.findAll{ _ -> println '    Filter: ' + _; _.age > 18 }.sort{ _ -> println '      Sort: ' + _; _.age }.reverse().collect{ _ -> println '   Collect: ' + _; _.name };println '    Output: ' + nodes;nodes"
        Map options = [ f: "_.age > 18", c: "_.name", s: "_.age", "sort-desc": true]

        assert buildExpr(options) == expectedExpr

        options.x = true
        assert buildExpr(options) == expectedExprWithDebug

    }

    void testCliBuilderOptionsHavingParams() {
        String rootNode = "root"
        String filterExpr = "filter"
        String collectExpr = "collect"
        String entriesExpr = "collectEntries"
        String sortExpr = "sort"
        String fileName = "input.json"
        String[] args = " -f ${filterExpr} -c ${collectExpr} -e ${entriesExpr} -s ${sortExpr} --root ${rootNode} ${fileName}".split()
        String[] longArgs = " --filter ${filterExpr} --collect ${collectExpr} --entries ${entriesExpr}  --sort ${sortExpr} --root ${rootNode} ${fileName}".split()

        CliBuilder cli = cliBuilder()

        def check = { options ->
            assert options, "Invalid args"
            assert options.arguments() && options.arguments()[0] == fileName, "Input file NOT parsed"
            assert options.f && options.f == filterExpr && options.filter == filterExpr, "Filter expression NOT parsed: ${options.f}"
            assert options.c && options.c == collectExpr && options.collect == collectExpr, "Collect expression NOT parsed"
            assert options.e && options.e == entriesExpr && options.'entries' == entriesExpr, "Entries expression NOT parsed"
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

    void testBuildComplexExpression() {
        String expectedExpr = "root.findAll{ _ -> _.name.endsWith('e') }.collect{ _ -> [(_.name) : (_.val)] }"
        Map options = [ flat: true, f : "_.name.endsWith('e')", c: "[(_.name) : (_.val)]", e: "[(_.name) : (_.val)]"]

        // -e option should be ignored

        assert buildExpr(options) == expectedExpr
    }

    // -------------------------------------------------------------------------------------------------- transformation

    void testFilter() {
        List items = [[age:19, name:"Andrea"], [age:21, name:"Beatrice"], [age:16, name:"Carlo"]]
        List expectedItems = [[age:21, name:"Beatrice"]]
        String expr = "root.findAll { _ -> _.age > 19 }"

        assert transform(items, expr) == expectedItems
    }

    void testCollect() {
        List items = [[age:19, name:"Andrea"], [age:21, name:"Beatrice"], [age:16, name:"Carlo"]]
        List expectedItems = items.collect { it.name }
        String expr = "root.collect { _ -> _.name }"

        assert transform(items, expr) == expectedItems
    }

    void testCollectEntries() {
        List items = [[age:19, name:"Andrea"], [age:21, name:"Beatrice"], [age:16, name:"Carlo"]]
        Map expectedItems = items.collectEntries { [(it.name) : (it.age)] }
        String expr = "root.collectEntries { _ -> [(_.name) : (_.age)] }"

        assert transform(items, expr) == expectedItems
    }

    void testSort() {
        List items = [[age:19, name:"Andrea"], [age:21, name:"Beatrice"], [age:16, name:"Carlo"]]
        List expectedItems = items.sort { it.age }
        String expr = "root.sort { _ -> _.age > 19 }"

        assert transform(items, expr) == expectedItems
    }

    // -------------------------------------------------------------------------------------------------- integration tests

    def checkMarkupTransformation = { String content, String[] args, expectedOutput ->
        def options = cliBuilder().parse(args)
        assert options, "Invalid CLI"

        def markup = new JsonSlurper().parseText(content)
        String source = buildExpr(options)
        assert source

        assert transform(options.root ? (markup."${options.root}") : markup, source) == expectedOutput
    }

    void testIntegrationCollect() {
        String content = '[{"name":"Andrea","age":19},{"name":"Beatrice", "age": 21},{"name":"Carlo", "age":16}]'
        String[] cli = ["-x", "-f", "_.age > 18", "-c", "_.name", "-s", "_.age", "--sort-desc"]
        List expectedOutput = ["Beatrice","Andrea"]

        checkMarkupTransformation(content, cli, expectedOutput)
    }

    void testIntegrationCollectEntries() {
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

        String[] cli = [ "--root", "metadata", "-f","_.name.endsWith('e')", "-e", "[(_.name) : (_.val)]" ]
        Map expectedOutput = [media_type:"application/json", website:"json.org"]

        checkMarkupTransformation(content, cli, expectedOutput)
    }
}
