// A comment here for coloring to show in file w/ gedit.
import java.io.*
import java.util.zip.GZIPInputStream

// Based on an algorithm by maythesource.com:
// http://stackoverflow.com/a/15905916
def fastSplit(String s) {
	words = []
	boolean notInsideComma = true
	int start = 0
	int end = 0
	for (int i=0; i<s.length()-1; ++i) {
		if (s.charAt(i) == ',' && notInsideComma) {
			words += [s.substring(start, i)]
			start = i+1
		} else if (s.charAt(i) == '"') {
			notInsideComma = !notInsideComma;
		}
	}
	words += [s.substring(start)]
	return words 
}

runUUID = java.util.UUID.randomUUID()


graph = TitanFactory.open('./db.properties')
g = graph.traversal()

mgmt = graph.openManagement()

// Node properties
successful = mgmt.makePropertyKey("successful").dataType(Boolean.class).make()

run_uuid = mgmt.makePropertyKey("run_uuid").dataType(String.class).make()
uuid = mgmt.makePropertyKey("uuid").dataType(String.class).make()
generation = mgmt.makePropertyKey("generation").dataType(Integer.class).make()
location = mgmt.makePropertyKey("location").dataType(Integer.class).make()
genetic_operators = mgmt.makePropertyKey("genetic_operators").dataType(String.class).make()
//plush_genome = mgmt.makePropertyKey("plush_genome").dataType(String.class).make()
total_error = mgmt.makePropertyKey("total_error").dataType(Float.class).make()
is_random_replacement = mgmt.makePropertyKey("is_random_replacement").dataType(Boolean.class).make()
//error_vector = mgmt.makePropertyKey("error_vector").dataType(Float.class).cardinality(Cardinality.LIST).make()

num_children = mgmt.makePropertyKey("num_children").dataType(Integer.class).make()
num_selections = mgmt.makePropertyKey("num_selections").dataType(Integer.class).make()
num_ancestry_children = mgmt.makePropertyKey("num_ancestry_children").dataType(Integer.class).make()

// Vertex Labels
individual = mgmt.makeVertexLabel('individual').make()
run = mgmt.makeVertexLabel('run').make()

// Edge properties
parent_type = mgmt.makePropertyKey("parent_type").dataType(String.class).make()

// Indexing
successfulIndex = mgmt.buildIndex("successfulIndex", Vertex.class).addKey(successful).indexOnly(run).buildCompositeIndex()
uuidIndex = mgmt.buildIndex("uuidIndex",  Vertex.class).addKey(uuid).unique().buildCompositeIndex()
generationTotalError = mgmt.buildIndex('generationTotalError', Vertex.class).addKey(generation).addKey(total_error).buildMixedIndex("search")
mgmt.commit()
println("Done with setting keys.")

start = System.currentTimeMillis()

println("We're in the parse section!")
// Adding all verticies to graph
theCount = 0
runFileName = "data7.csv.gz"
nodeCSVzipped = "/Research/RSWN/recursive-variance-v3/" + runFileName

fileStream = new FileInputStream(nodeCSVzipped)
gzipStream = new GZIPInputStream(fileStream)
inputReader = new InputStreamReader(gzipStream)
reader = new BufferedReader(inputReader)
successful = false
maxGen = 0
while ((line = reader.readLine()) != null) {
	if (theCount % 1000 == 0) {
	    println("Count = ${theCount}")
	}
    if (theCount > 0) { 
		fields = fastSplit(line)
		// println fields
		// This only makes sense if we're using is-random-replacement
		// (and it's in location 9).
		
		if (fields[9] == "") {
			fields[9] = true
		}
		
		// errors = fields[10..-1].collect { it.toFloat() }

		if((theCount % 10000) == 0){
			println("Commiting at: "+theCount)
			graph.tx().commit()
		}
	// Remember to change this to fields[9] when working with non autoconstuctive runs!
		total_error = fields[8].toFloat()
		if (total_error == 0) {
			successful = true;
		}
		gen = fields[1].toInteger()
		if (gen > maxGen){
			maxGen = gen;
		}
		newVertex = graph.addVertex(label, "individual", "run_uuid", runUUID, "uuid", fields[0], "generation", fields[1].toInteger(), 
				"location", fields[2].toInteger(), "genetic_operators", fields[4], // "plush_genome", fields[7], 
				"total_error", total_error, "is_random_replacement", fields[9].toBoolean())
//				"total_error", total_error)
		// errors.each { newVertex.property("error_vector", it) }

		if (fields[3].length() > 5) {
			motherUuid = fields[3][4..39]
			// println "<" + motherUuid + "> ::: <" + fatherUuid + ">"
			mother = g.V().has("uuid", motherUuid).next()
			motherEdge = mother.addEdge('parent_of', newVertex)
			// We commented out the properties because they're not meaningful
			// for the non-autoconstructive runs.
			 motherEdge.property("parent_type", "mother")

			if (fields[3].length() > 48) {
				fatherUuid = fields[3][45..-5]
				father = g.V().has("uuid", fatherUuid).next()
				fatherEdge = father.addEdge('parent_of', newVertex)
				 fatherEdge.property("parent_type", "father")
			}

		}
	}
	++theCount
}
reader.close()
graph.tx().commit()

(0..maxGen).each { gen -> g.V().has('generation', gen).sideEffect { num_selections = it.get().edges(Direction.OUT).size(); it.get().property('num_selections', num_selections) }.iterate(); graph.tx().commit(); println gen }

(0..maxGen).each { gen -> g.V().has('generation', gen).sideEffect { edges = it.get().edges(Direction.OUT); num_children = edges.collect { it.inVertex() }.unique().size(); it.get().property('num_children', num_children) }.iterate(); graph.tx().commit(); println gen }

runVertex = graph.addVertex(label, "run", "run_uuid", runUUID, "data_file", runFileName, "successful", successful, "max_generation", maxGen)

graph.tx().commit()

println "Loading took (ms): " + (System.currentTimeMillis() - start)

