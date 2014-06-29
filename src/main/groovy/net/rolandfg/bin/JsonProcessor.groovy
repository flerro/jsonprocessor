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

import groovy.json.JsonBuilder
import groovy.json.JsonException
import groovy.json.JsonSlurper

import static System.exit

class JsonProcessor {

    /**
     * Build an object to handle command line arguments
     *
     * @return a cli builder object
     */
    static CliBuilder cliBuilder() {
        CliBuilder cli = new CliBuilder(usage: 'java -jar jpr.jar [options] input.json',
                                        header: '\nJSON filtering and transformation leveraging Groovy expressivity.\nOptions:',
                                        footer: '\neg. List names of people over 18, descending by age\n' +
                                                '.\n' +
                                                '.   Input: [{"name":"Andrea","age":19}, {"name":"Bianca", "age": 21}, {"name":"Carlo", "age":16}]\n' +
                                                '.\n' +
                                                '.     Cmd: java -jar jpr.jar -f "_.age > 18" -m _.name -s _.age --sort-dec input.json\n' +
                                                '.\n' +
                                                '.  Output: ["Bianca","Andrea"]\n' +
                                                '.\n' +
                                                '.   The undescore (_) references current node in expression.',
                                        stopAtNonOption: false,
                                        width: 105)
        cli.with {
            _ longOpt: 'root', args: 1, argName: 'base_node', 'use a different root node for transformations'
            f longOpt: 'filter', args: 1, argName: 'expr', 'a boolean Groovy expression to filter input nodes'
            e longOpt: 'reduce', args: 1, argName: 'expr', 'a Groovy expression applied on each input node'
            t longOpt: 'flat', 'flatten the output, [[a], [b,c]] -> [a,b,c]'
            h longOpt: 'help', 'print this message'
            p longOpt: 'pretty', 'prettyprint the output'
            s longOpt: 'sort', args: 1, argName: 'sort_expr', 'a Groovy expression used to sort output nodes'
            _ longOpt: 'sort-desc', 'reverse sort'
            _ longOpt: 'quickstart', 'print a quick-start manual'
            x longOpt: 'debug','enable debug mode (for troubleshooting)'
        }

        cli
    }

    /**
     * Build a Groovy snippet from input options
     *
     * @param options options from command line
     * @return a Groovy script as string
     */
    static String buildExpr(Object options) {
        def debugExpr = { w -> options.x ? 'println \'' + w.padLeft(10) + ': \' + _; ' : '' }
//        def debugExpr =  { w -> '' }

        StringBuilder source = new StringBuilder()
        if (options.x) source << 'nodes = '
        source << 'root'
        if (options.f) source << ".findAll{ _ -> ${debugExpr('Filter')}${options.f} }"
        if (options.s) source << ".sort{ _ -> ${debugExpr('Sort')}${options.s} }"
        if (options.'sort-desc') source << '.reverse()'
        if (options.e) source << ".collect{ _ -> ${debugExpr('Reduce')}${options.e} }"
        if (options.x) source << ";println '    Output: ' + nodes;nodes"

        source.toString()
    }

    /**
     * Process JSON input and print to stdout
     *
     * @param content the content to be processed
     * @param options a set of options (cfr. {@see JsonTX.parseCmdLine} or run with -h switch)
     */
    static def transform(Object rootNode, String source) {
        try {
            Binding binding = new Binding(root: rootNode)
            GroovyShell gs = new GroovyShell(binding)
            gs.evaluate(source)
        } catch (Exception ex){
            throw new IllegalArgumentException("Exception executing: ${source}\n" +
                    "Check for error in the groovy expression " +
                    "(eg. mispelled property name, invalid root node, syntax error).\n" +
                    "Original error: ${ex.message}", ex)
        }
    }

    // ----------------------------------------------------------------------------------------------

    static void main(String[] args) {
        def err = System.err.&println

        def cli = cliBuilder()
        def options = cli.parse(args)
        if (!options) exit(1)

        boolean debug = options.x

        if (options.help) {
            cli.usage()
            exit(1)
        }

        if (options.'quickstart') {
            println "More info at http://www.rolandfg.net/2014/06/29/json-commandline-processor/#usage"
            exit(0)
        }

        boolean useStdin = options.arguments().size() == 0
        boolean isXMLSource = options.xml

        File inputFile = useStdin ? null : new File(options.arguments()[0])
        if (!useStdin && !inputFile.canRead()) {
            err "Unable to read: ${inputFile.absolutePath}"
            cli.usage()
            exit(1)
        }

        try {
            Reader jsonContentReader = useStdin ? new InputStreamReader(System.in) : new FileReader(inputFile)

            def jsonContent = new JsonSlurper().parse(jsonContentReader)
            String expr = buildExpr(options)
            if (debug) println "Expression: ${expr}"

            def nodes = transform(options.root ? (jsonContent."${options.root}") : jsonContent, expr)
            def builder = new JsonBuilder(options.flat ? nodes.flatten() : nodes)

            println options.p ? builder.toPrettyString() : builder.toString()

        } catch (JsonException jex) {
            err "Invalid input. ${jex.message}"
            if (debug) jex.printStackTrace()

        } catch (Exception ex) {
            err ex.message
            if (debug) ex.printStackTrace()
        }

        exit(0)
    }

}