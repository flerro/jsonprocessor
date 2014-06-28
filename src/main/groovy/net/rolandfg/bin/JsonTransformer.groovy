package net.rolandfg.bin

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

class JsonTransformer {

    /**
     *
     * @return a cli builder obkect
     */
    static CliBuilder cliBuilder() {
        CliBuilder cli = new CliBuilder(usage: 'jtx [options] input.json', header: 'A JSON filter/map processor.\nOptions:')
        cli.with {
            root longOpt: 'root-node', args: 1, argName: 'base_node', 'the node to be manipulated, ' +
                    'if empty will use the actual root node of the given JSON object'
            f longOpt: 'filter', args: 1, argName: 'filter_expr', 'A groovy expression to filter JSON nodes, ' +
                    'if empty all input nodes will be used'
            m longOpt: 'map', args: 1, argName: 'map_expr', 'A groovy expression to manipulate JSON nodes structure, ' +
                    'if empty the input structure is reflected into the output'
            flat 'flatten the JSON output, es. [[a], [b,c]] -> [a,b,c]'
            h longOpt: 'help', 'print this message'
            man 'print a quick start manual'
            d longOpt: 'debug', 'print some informations while running'
        }

        cli
    }

    /**
     * Process JSON input and print to stdout
     *
     * @param content the content to be processed
     * @param options a set of options (cfr. {@see JsonTX.parseCmdLine} or run with -h switch)
     */ 
    static void filterAndMap(Reader content, def options) {

        def jsonContent = new JsonSlurper().parse(content)

        String internalCurrentNodeName = "currentNode."
        def rebase = { exp -> exp.replaceAll(/^\./, internalCurrentNodeName)
                                            .replaceAll(/([\[\{\( ])\./, '$1' + internalCurrentNodeName) }

        def fixColonPrecedence = { exp -> exp.replaceAll(/(.*):(.*)/, '($1):($2)')}

        def startNode = options.root
        def filterExpr = options.f ? rebase(options.f) : ""
        def mapExpr = options.m ? fixColonPrecedence(rebase(options.m)) : ""

        def root = startNode ? jsonContent."$startNode" : jsonContent

        if (options.d) {
            System.out.println("  Root: ${startNode}")
            System.out.println("Filter: ${filterExpr}")
            System.out.println("   Map: ${mapExpr}")
        }

        if (!root) {
            println "Unable to find \"$startNode\" as root node"
            System.exit(1)
        }

        if (options.d) {
            System.out.println("Processing...")
        }

        
        def nodes = null

        try {

            nodes = !filterExpr ? root : root.findAll { n ->
                                    if (options.d) {
                                        System.out.println(" ... ${n.toString()}")
                                    }
                                    filterExpr ? Eval.me('currentNode', n, filterExpr) : true
                                }.collect { n ->
                                    mapExpr ? Eval.me('currentNode', n, mapExpr) : n
                                }

        } catch (Exception ex) {
            System.err.println("Invalid expression")
            ex.printStackTrace()
        }

        println new JsonBuilder(options.flat ? nodes.flatten() : nodes).toPrettyString()
    }

    /**
     *
     * @param args
     */
    static void main(String[] args) {

        def cli = cliBuilder()
        def options = cli.parse(args)

        if (options.h) {
            cli.usage()
            System.exit(0)
        }

        if (options.man) {
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

        Reader jsonContentReader = useStdin ? new InputStreamReader(System.in) : new FileReader(inputFile)
        filterAndMap(jsonContentReader, options)

        System.exit(0)
    }

}