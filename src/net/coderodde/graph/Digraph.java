package net.coderodde.graph;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class implements a directed graph.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (Jan 11, 2016)
 */
public class Digraph {

    private final Map<Integer, 
                      Map<Integer, 
                          Double>> parentMap = new LinkedHashMap<>();

    private final Map<Integer, 
                      Map<Integer, 
                          Double>> childMap = new LinkedHashMap<>();

    private int edges;
    
    public int size() {
        return parentMap.size();
    }

    public int getNumberOfEdges() {
        return edges;
    }

    public boolean addNode(int nodeId) {
        if (parentMap.containsKey(nodeId)) {
            return false;
        }

        parentMap.put(nodeId, new LinkedHashMap<>());
        childMap .put(nodeId, new LinkedHashMap<>());
        return true;
    }

    public boolean hasNode(int nodeId) {
        return parentMap.containsKey(nodeId);
    }

    public boolean clearNode(int nodeId) {
        if (!hasNode(nodeId)) {
            return false;
        }

        Map<Integer, Double> parents = parentMap.get(nodeId);
        Map<Integer, Double> children = childMap.get(nodeId);

        if (parents.isEmpty() && children.isEmpty()) {
            return false;
        }

        for (Integer childId : children.keySet()) {
            parentMap.get(childId).remove(nodeId);
        }

        for (Integer parentId : parents.keySet()) {
            childMap.get(parentId).remove(nodeId);
        }

        edges -= parents.size() + children.size();
        parents.clear();
        children.clear();
        return true;
    }

    public boolean removeNode(int nodeId) {
        if (!hasNode(nodeId)) {
            return false;
        }

        clearNode(nodeId);
        parentMap.remove(nodeId);
        childMap.remove(nodeId);
        return true;
    }

    public boolean addEdge(int tailNodeId, int headNodeId, double weight) {
        addNode(tailNodeId);
        addNode(headNodeId);

        if (childMap.get(tailNodeId).containsKey(headNodeId)) {
            double oldWeight = childMap.get(tailNodeId).get(headNodeId);
            childMap.get(tailNodeId).put(headNodeId, weight);
            parentMap.get(headNodeId).put(tailNodeId, weight);
            
            if (oldWeight != weight) {
                return true;
            }
            
            return false;
        } else {
            childMap.get(tailNodeId).put(headNodeId, weight);
            parentMap.get(headNodeId).put(tailNodeId, weight);
            edges++;
            return true;
        }
    }

    public boolean hasEdge(int tailNodeId, int headNodeId) {
        if (!childMap.containsKey(tailNodeId)) {
            return false;
        }

        return childMap.get(tailNodeId).containsKey(headNodeId);
    }

    public double getEdgeWeight(int tailNodeId, int headNodeId) {
        if (!hasEdge(tailNodeId, headNodeId)) {
            return Double.NaN;
        }

        return childMap.get(tailNodeId).get(headNodeId);
    }

    public boolean removeEdge(int tailNodeId, int headNodeId) {
        if (!childMap.containsKey(tailNodeId)) {
            return false;
        }

        if (!childMap.get(tailNodeId).containsKey(headNodeId)) {
            return false;
        }

        childMap .get(tailNodeId).remove(headNodeId);
        parentMap.get(headNodeId).remove(tailNodeId);
        edges--;
        return true;
    }

    public Set<Integer> getChildrenOf(int nodeId) {
        if (!childMap.containsKey(nodeId)) {
            return Collections.<Integer>emptySet();
        }

        return Collections.
                <Integer>unmodifiableSet(childMap.get(nodeId).keySet());
    }

    public Set<Integer> getParentsOf(int nodeId) {
        if (!parentMap.containsKey(nodeId)) {
            return Collections.<Integer>emptySet();
        }

        return Collections.
                <Integer>unmodifiableSet(parentMap.get(nodeId).keySet());
    }

    public Set<Integer> getAllNodes() {
        return Collections.<Integer>unmodifiableSet(childMap.keySet());
    }

    public void clear() {
        childMap.clear();
        parentMap.clear();
        edges = 0;
    }
}
