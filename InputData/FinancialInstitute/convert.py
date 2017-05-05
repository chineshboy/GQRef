import xml.etree.ElementTree as ET
import sys
import string
import operator
import time
import networkx as nx


if len(sys.argv) < 4:
    print "convert XESFile GraphFile EdgeFile NodeFile"
    print "convert the XES Financial file into a GraphFile - Mapping File"
    exit(1)

xesFile = sys.argv[1]
graphFile = sys.argv[2]
edgeFile = sys.argv[3]
nodeFile = sys.argv[4]

start = time.time()
tree = ET.parse(xesFile)
end = time.time()
print 'Parsed ', xesFile,  ' in ', (end - start), ' seconds'

root = tree.getroot()

nodeLabels = {'start' : 0, 'end' : 1, 'no_res' : 2}
edgeLabels = {'end': 0}
lastEdgeLabel = 1
lastNodeLabel = 3
graphCount = 0
avgGraphSize = 0.0
maxGraphSize = 0



def check_graph(nodes, triples) :
	g = nx.Graph()
	for (label,node) in nodes : 
		g.add_node(node,l=label)
	for (n1,n2,e) in triples : 
		g.add_edge(n1, n2, l=e)
	return len(list(nx.connected_components(g))) == 1
 

# Format of an event (an edge)
# <event>
# 	<string key="org:resource" value="10913"/>
# 	<string key="lifecycle:transition" value="START"/>
# 	<string key="concept:name" value="W_Completeren aanvraag"/>
# 	<date key="time:timestamp" value="2011-10-08T10:49:43.434+02:00"/>
# </event>
start = time.time()
with open(graphFile, 'w') as f:
	for trace in root.iter('{http://www.xes-standard.org/}trace'):
		nodeALabel = 'start'
		nodeIndex = 0
		nodeA = nodeIndex
		graphNodes = {0 : 0}
		edges = set()
		triples = set()
		for event in trace.iter('{http://www.xes-standard.org/}event'): 
			if event[0].attrib['key'] == 'org:resource': 
				edgeLabel = event[0].attrib['value']
				label = string.replace(event[1].attrib['value'] + '-' + event[2].attrib['value'],' ', '') 
			else : 
				edgeLabel = 'no_res'
				label = string.replace(event[1].attrib['value'] + '-' + event[0].attrib['value'], ' ', '')
			if (edgeLabel not in edgeLabels): 
				edgeLabels[edgeLabel] = lastEdgeLabel
				lastEdgeLabel += 1
			if (label not in nodeLabels): 
				nodeLabels[label] = lastNodeLabel
				nodeBLabel = lastNodeLabel
				lastNodeLabel += 1
			else : 	
				nodeBLabel = nodeLabels[label]

			#Update the graph
			if nodeBLabel not in graphNodes : 
				nodeIndex += 1
				graphNodes[nodeBLabel] = nodeIndex
				nodeB = nodeIndex
			else : 
				nodeB = graphNodes[nodeBLabel]
			edge = edgeLabels[edgeLabel]
			# Delete self-loops, and multi-edges (cannot find DFScode)
			if nodeA != nodeB and (nodeA, nodeB) not in edges and (nodeB, nodeA) not in edges: 
				edges.add((nodeA,nodeB))
				triple = (nodeA, nodeB, edge)
				triples.add(triple)
				# print (triple)

			nodeALabel = nodeBLabel	
			nodeA = nodeB
		nodeIndex += 1
		triple = (nodeA, nodeIndex, 0)
		triples.add(triple)
		graphNodes[1] = nodeIndex 
		avgGraphSize += len(triples)
		if (len(triples) > maxGraphSize) : 
			maxGraphSize = len(triples)
		# print triple
		sorted_nodes = sorted(graphNodes.iteritems(), key=operator.itemgetter(1))
		if (check_graph(sorted_nodes,triples)) : 
			f.write('t # ' + str(graphCount) + "\n")
			for (k,v) in sorted_nodes: 
				f.write("v " + str(v) + " " + str(k) + "\n")
			for (n1,n2,e) in sorted(triples, key=lambda triple: triple[0]) : 
				f.write("e " + str(n1) + " " + str(n2) + " " + str(e)  + "\n")
			graphCount += 1
		else : 
			print "Graph %d is not connected" % (graphCount)

#Print some statistics of the dataset. 
avgGraphSize /= graphCount
end = time.time()
print 'Conversion performed in ', end - start, ' seconds'
print 'Number of graphs: ', graphCount
print 'Max Size graph: ', maxGraphSize
print 'Average Size graph: ', avgGraphSize
print 'Number of node labels: ', len(nodeLabels)
print 'Number of edge labels: ', len(edgeLabels)

with open(edgeFile, 'w') as f: 
	for k,v in edgeLabels.iteritems(): 
		f.write(str(v) + "\t" +  k + "\n")
with open(nodeFile, 'w') as f: 
	for k,v in nodeLabels.iteritems(): 
		f.write(str(v) + "\t" + k + "\n")



#print(count)
# print(len(traces))