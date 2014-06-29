package net.rolandfg.bin

import groovy.json.JsonBuilder
import groovy.json.JsonException
import groovy.json.JsonSlurper
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
            x 'enable debug mode (for troubleshooting)'
        }

        cli
    }

    /**
     * Process JSON input and print to stdout
     *
     * @param content the content to be processed
     * @param options a set of options (cfr. {@see JsonTX.parseCmdLine} or run with -h switch)
     */
    static void transform(Reader content, def options) {

        def jsonContent = new JsonSlurper().parse(content)

        def startNode = options.root
        def debugExpr = options.x ? 'println _;' : ''
        def filterExpr = options.f ? "root.findAll{ _ -> ${debugExpr}${options.f} }" : ""
        def mapExpr = options.m ? "root.collect{ _ -> ${debugExpr}${options.m} }" : ""
        def sortExpr = options.s ? "root.sort{ _ -> ${debugExpr}${options.s}}${options.'sort-desc' ? '.reverse()' : '' }" : ""

        if (options.x) {
            println "  Root: ${startNode} "
            println "Filter: ${filterExpr}"
            println "   Map: ${mapExpr}"
        }

        def root = startNode ? jsonContent."$startNode" : jsonContent
        if (!root) {
            throw new UnsupportedOperationException("Invalid root node: \"$startNode\"")
        }

        def nodes = null

        Binding binding = new Binding(root: root)
        GroovyShell gs = new GroovyShell(binding)

        if (filterExpr) {
            try {
                nodes = gs.evaluate(filterExpr)
                binding.setVariable('root', nodes)
            } catch (Exception ex) {
                throw new IllegalArgumentException("Invalid filter expression: ${options.f}", ex)
            }
            if (!nodes) throw new RuntimeException("Empty result.")
        }

        if (sortExpr) {
            try {
                nodes = gs.evaluate(sortExpr)
                binding.setVariable('root', nodes)
            } catch (Exception ex) {
                throw new IllegalArgumentException("Invalid sort expression: ${options.s}", ex)
            }
        }

        if (mapExpr) {
            try {
                nodes = gs.evaluate(mapExpr)
            } catch (Exception ex){
                throw new IllegalArgumentException("Invalid map expression: ${options.m}", ex)
            }
        }

        nodes ?: root
    }

    /**
     *
     * @param args
     */
    static void main(String[] args) {

        def cli = cliBuilder()
        def options = cli.parse(args)

        if (!options) exit(1)

        boolean debug = options.x
        def err = System.err.&println

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
            def nodes = transform(jsonContentReader, options)

            def builder = new JsonBuilder(options.flat ? nodes.flatten() : nodes)
            println options.p ? builder.toPrettyString() : builder.toString()

        } catch (JsonException jex) {
            err "Invalid JSON in input. ${jex.message}"
            if (debug) jex.printStackTrace()

        } catch (Exception ex) {
            err "Error. ${ex.message} ${ex?.cause?.message ?: ""}"

            if (debug) {
                ex.printStackTrace()
//                ex?.cause?.printStackTrace()
            }
        }

        exit(0)
    }

}