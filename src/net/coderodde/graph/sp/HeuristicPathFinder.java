package net.coderodde.graph.sp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.coderodde.graph.Digraph;
import net.coderodde.graph.util.HeuristicFunction;

/**
 * This abstract class defines the API shared by all the actual shortest path 
 * algorithms.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 
 */
public abstract class HeuristicPathFinder {
   
    /**
     * Searches a shortest path in {@code digraph} from {@code source} to
     * {@code target}.
     * 
     * @param digraph           the directed graph in which to search.
     * @param heuristicFunction the heuristic function used in search.
     * @param source            the source node.
     * @param target            the target node.
     * @return a list of integers representing a path from {@code source} to
     *         {@code target} in {@code digraph} or an empty list if the target
     *         node is not reachable from the source node.
     */
    public abstract List<Integer> search(final Digraph digraph,
                                         final HeuristicFunction heuristicFunction,
                                         final Integer source, 
                                         final Integer target);
    
    protected List<Integer> 
        tracebackPath(final Integer target,
                      final Map<Integer, Integer> parentMap) {
        final List<Integer> path = new ArrayList<>();
        Integer current = target;
        
        while (current != null) {
            path.add(current);
            current = parentMap.get(current);
        }
        
        Collections.<Integer>reverse(path);
        return path;
    }
        
    protected List<Integer> 
        tracebackPath(final Integer touch, 
                      final Map<Integer, Integer> parentMapA,
                      final Map<Integer, Integer> parentMapB) {
        final List<Integer> path = tracebackPath(touch, parentMapA);
        Integer current = parentMapB.get(touch);
        
        while (current != null) {
            path.add(current);
            current = parentMapB.get(current);
        }
        
        return path;
    }
}
