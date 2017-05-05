import sys
import csv
import string
import operator
import time

if len(sys.argv) < 3:
    print "convert SessionFile GraphFile"
    print "convert the session file into a GraphFile - Mapping File"
    exit(1)

sessionFile = sys.argv[1]
graphFile = sys.argv[2]


#Format of an entry
#29/Dec/1998:16:26:39 	208.134.230.125	0	465L	640L	418L	640L	426L	105L	447L	620L	250L	27L	465L	659$

#Start and end node in the flow. 
START = 0
END = 10
#Set the minimum number of nodes allowed to have significant graphs
MIN_SIZE_GRAPH = 6
QUIT = 1000

edgeLabels = {'L' : 1, 'M' : 2, 'N' : 3, 'O' : 4, 'P' : 5, 'Q' : 6, 'R' : 7, 'S' : 8, 'T' : 9}
nodeLabels = set()
lastEdgeLabel = 1
lastNodeLabel = 3
graphCount = 0
avgGraphSize = 0.0
maxGraphSize = 0


start = time.time()
with open(sessionFile, 'rb') as f, open(graphFile, 'w') as graph :
	reader = csv.reader(f, delimiter='\t', quoting=csv.QUOTE_NONE)
	for session in reader:
		if len(session) >= (MIN_SIZE_GRAPH + 2): 
			edges = set() 
			triples = set()
			graphNodes = {0 : 0}
			nodeALabel = START
			prevEdge = START
			nodeA = 0
			nodeIndex = 0
			#print session
			for i in range(3, len(session) - 1) :
				action = session[i]
				nodeBLabel = int(action[0:len(action) - 1])
				nodeLabels.add(nodeBLabel)
				edgeLabel = edgeLabels[action[len(action) - 1]]	
				if nodeBLabel not in graphNodes: 
					nodeIndex += 1
					nodeB = nodeIndex
					graphNodes[nodeBLabel] = nodeIndex
				else :
					nodeB = graphNodes[nodeBLabel]
				if (nodeA, nodeB) not in edges and (nodeB, nodeA) not in edges and nodeA != nodeB : 
					edges.add((nodeA,nodeB))
					triple = (nodeA, nodeB, prevEdge)
					triples.add(triple)
				prevEdge = edgeLabel
				nodeALabel = nodeBLabel	
				nodeA = nodeB
			#End of session
			if session[len(session) - 1] == '-1' : 
				nodeBLabel = QUIT
				nodeB = nodeIndex + 1
				graphNodes[nodeBLabel] = nodeIndex + 1
			else : 
				nodeBLabel = int(session[len(session) - 1])
				if nodeBLabel not in graphNodes : 
					nodeIndex += 1
					nodeB = nodeIndex
					graphNodes[nodeBLabel] = nodeIndex
				else : 
					nodeB = graphNodes[nodeBLabel]
			if (nodeA, nodeB) not in edges and (nodeB, nodeA) not in edges and nodeA != nodeB : 
				triple = (nodeA, nodeB, prevEdge)
				triples.add(triple)
			avgGraphSize += len(triples)
			if len(triples) > maxGraphSize : 
				maxGraphSize = len(triples)
			graph.write('t # ' + str(graphCount) + "\n")
			graphCount += 1
			orderedNodes = sorted(graphNodes.iteritems(), key=operator.itemgetter(1))
			for (k,v) in orderedNodes: 
				graph.write("v " + str(v) + " " + str(k) + "\n")
			for (n1,n2,e) in sorted(triples, key=lambda triple: triple[0]) : 
				graph.write("e " + str(n1) + " " + str(n2) + " " + str(e)  + "\n")

#print statistics on the conversion
end = time.time()
avgGraphSize /= graphCount
print 'Conversion performed in ', end - start, ' seconds'
print 'Number of graphs: ', graphCount
print 'Max Size graph: ', maxGraphSize
print 'Average Size graph: ', avgGraphSize
print 'Number of node labels: ', len(nodeLabels)
print 'Number of edge labels: ', len(edgeLabels)
	 		#DEBUG
	   		#s += "(" + str(nodeBLabel) + "," + str(edgeLabel) + ")"
     			