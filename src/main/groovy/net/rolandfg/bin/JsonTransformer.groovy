package net.rolandfg.bin

import groovy.json.JsonBuilder
import groovy.json.JsonException
import groovy.json.JsonSlurper

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
        }

        cli
    }

    /**
     * Process JSON input and print to stdout
     *
     * @param content the content to be processed
     * @param options a set of options (cfr. {@see JsonTX.parseCmdLine} or run with -h switch)
     * @param debug   display debug informations
     */ 
    static void transform(Reader content, def options, Boolean debug) {

        def jsonContent = new JsonSlurper().parse(content)

        def prettyPrint = { nodes ->
            def builder = new JsonBuilder(options.flat ? nodes.flatten() : nodes)
            println options.p ? builder.toPrettyString() : builder.toString()
        }

        def startNode = options.root
        def filterExpr = options.f ? "root.findAll{ _ -> ${options.f} }" : ""
        def mapExpr = options.m ? "root.collect{ _ -> ${options.m} }" : ""
        def sortExpr = options.s ? "root.sort{ _ -> ${options.s}}${options.'sort-desc' ? '.reverse()' : '' }" : ""

        def root = startNode ? jsonContent."$startNode" : jsonContent

        if (debug) {
            System.out.println("  Root: ${startNode} ")
            System.out.println("Filter: ${filterExpr}")
            System.out.println("   Map: ${mapExpr}")
        }

        if (!root) {
            println "Unable to find \"$startNode\" as root node"
            System.exit(1)
        }

        if (debug) {
            System.out.println("")
            System.out.println("Processing...")
        }

        def nodes = null

        try {
            Binding binding = new Binding(root: root, debug: debug)
            GroovyShell gs = new GroovyShell(binding)

            if (filterExpr) {
                nodes = gs.evaluate(filterExpr)
                if (!nodes) throw new RuntimeException("Empty result.")
                binding.setVariable('root', nodes)
            }

            if (sortExpr) {
                nodes = gs.evaluate(sortExpr)
                binding.setVariable('root', nodes)
            }

            if (mapExpr) {
                nodes = gs.evaluate(mapExpr)
            }

            prettyPrint(nodes ?: root)

        } catch (Exception ex) {
            System.err.println("Expression eval FAILED. ${ex.message}")
            if (options.d) {
                ex.printStackTrace()
            }
        }

    }

    /**
     *
     * @param args
     */
    static void main(String[] args) {

        boolean debug = System.getenv("DEBUG")

        def cli = cliBuilder()
        def options = cli.parse(args)

        if (!options) {
            System.exit(1)
        }

        if (options.help) {
            cli.usage()
            System.exit(0)
        }

        if (options.'quickstart') {
            System.out.println("TODO print manual")
            System.exit(0)
        }

        boolean useStdin = options.arguments().size() == 0

        File inputFile = useStdin ? null : new File(options.arguments()[0])
        if (!useStdin && !inputFile.canRead()) {
            System.err.println("Unable to read: ${inputFile.absolutePath}")
            cli.usage()
            System.exit(1)
        }

        try {

            Reader jsonContentReader = useStdin ? new InputStreamReader(System.in) : new FileReader(inputFile)
            transform(jsonContentReader, options, debug)

        } catch (JsonException jex) {
            System.err.println("Invalid JSON in input. ${jex.message}")
            if (debug) jex.printStackTrace()

        } catch (Exception ex) {
            System.err.println("Error: ${ex.message}")
            if (debug) ex.printStackTrace()
        }

        System.exit(0)
    }

}