package net.rolandfg.bin

import groovy.json.JsonBuilder
import groovy.json.JsonException
import groovy.json.JsonSlurper
import org.apache.commons.cli.Options

import static System.exit

class JsonTransformer {

    /**
     *
     * @return a cli builder obkect
     */
    static CliBuilder cliBuilder() {
        CliBuilder cli = new CliBuilder(usage: 'jsontx [options] input.json',
                                        header: '\nJSON filtering and transformation leveraging Groovy expressivity.\nOptions:',
                                        footer: '\neg. List names of people over 18, descending by age\n' +
                                                '.\n' +
                                                '.   Input: [{"name":"Andrea","age":19} {"name":"Bianca", "age": 21}, {"name":"Carlo", "age":16}]\n' +
                                                '.\n' +
                                                '.     Cmd: jsontx -f "_.age > 18" -m _.name -s _.age --sort-dec input.json\n' +
                                                '.\n' +
                                                '.  Output: ["Bianca","Andrea"]\n' +
                                                '.\n' +
                                                '.   The undescore (_) references current node in expression.',
                                        stopAtNonOption: false,
                                        width: 105)
        cli.with {
            _ longOpt: 'root', args: 1, argName: 'base_node', 'use a different root node for transformations'
            f longOpt: 'filter', args: 1, argName: 'filter_expr', 'a boolean Groovy expression to filter input nodes'
            m longOpt: 'map', args: 1, argName: 'map_expr', 'a Groovy expression applied on each input node'
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
        if (options.m) source << ".collect{ _ -> ${debugExpr('Map')}${options.m} }"
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
            gs.evaluate(source.toString())
        } catch (Exception ex){
            throw new IllegalArgumentException("Invalid expression: ${source}", ex)
        }
    }


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
            println "TODO print manual"
            exit(0)
        }

        boolean useStdin = options.arguments().size() == 0

        File inputFile = useStdin ? null : new File(options.arguments()[0])
        if (!useStdin && !inputFile.canRead()) {
            err "Unable to read: ${inputFile.absolutePath}"
            cli.usage()
            exit(1)
        }

        try {
            Reader jsonContentReader = useStdin ? new InputStreamReader(System.in) : new FileReader(inputFile)

            def jsonContent = new JsonSlurper().parse(jsonContentReader)
            def startNode = options.root
            def rootNode = startNode ? jsonContent."$startNode" : jsonContent
            if (!rootNode) {
               err "Invalid root node: \"$startNode\""
               exit(1)
            }

            String expr = buildExpr(options)
            if (debug) println "Expression: ${expr}"

            def nodes = transform(rootNode, expr)
            def builder = new JsonBuilder(options.flat ? nodes.flatten() : nodes)

            println options.p ? builder.toPrettyString() : builder.toString()

        } catch (JsonException jex) {
            err "Invalid input. ${jex.message}"
            if (debug) jex.printStackTrace()

        } catch (Exception ex) {
            err "${ex.message}\n  ${ex?.cause?.message ?: ""}"
            if (debug) ex.printStackTrace()
        }

        exit(0)
    }

}