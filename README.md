# JSON Processor

A [Groovy](http://groovy.codehaus.org/) powered command line tool to manipulate JSON content. 

**Superseded** by a (WIP) [re-implementation in node.js](https://github.com/flerro/jop).

## Build

This is a [Gradle](http://www.gradle.org) project. Gradle wrapper is included in the distribution.

Run tests:
	
	./gradlew test

Create a distribution ZIP:

    ./gradlew shadowJar distZip

## Install 

Download the distibution zip file:
<p><div class="button"><a href="https://dl.dropboxusercontent.com/u/282511/jop.zip">jop.zip</a></div></p>

and move the jar file into a location you can easily reference, for example:

```language-bash
  wget https://dl.dropboxusercontent.com/u/282511/jop-0.6.zip
  unzip -d /tmp jop-0.6.zip
  mv /tmp/jop-0.6/jop.jar ~/bin/
  
  # optionally move the scripts (has some limitations, read the known issues)
  mv /tmp/jop-0.6/jop ~/bin/jop
  chmod +x ~/bin/jop
```

The tool needs a [Java Virtual Machine](http://java.com/en/download/index.jsp) installed. Distribution package contains also a sample shell script for easy tool startup: ```jop``` (but it needs improvements, cfr. [known issues](#issues)).

## Usage sample 

	usage: java -jar jop.jar [options] [input.json]

	JSON filtering and transformation leveraging Groovy expressivity.
	Options:
	 -c,--collect <expr>     expr is applied on each node, results are collected as List
	 -e,--entries <expr>     expr is applied on each node, results are collected as Map
	 -f,--filter <expr>      expr is a predicate to filter input nodes
	 -h,--help               print this message
	 -p,--pretty             prettyprint the output
	    --quickstart         print a quick-start manual
	    --root <base_node>   use base_node as the root node for content processing
	 -s,--sort <sort_expr>   a Groovy expression used to sort output nodes
	    --sort-desc          reverse sort
	 -t,--flat               flatten the output (ignored with -e)
	 -x,--debug              enable debug mode (for troubleshooting)

	Example: From input.json, List names and sort descending by age
	|
	| $ cat input.json
	|
	| [{"name":"Andrea","age":19}, {"name":"Bianca","age": 21}, {"name":"Carlo","age":16}]
	|
	| $ java -jar jop.jar -s _.age --sort-dec -c _.name input.json
	|
	| ["Bianca","Andrea","Carlo"]
	|
	|  # The undescore (_) references current node in expression.