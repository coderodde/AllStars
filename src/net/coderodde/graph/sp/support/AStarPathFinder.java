package net.coderodde.graph.sp.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.coderodde.graph.Digraph;
import net.coderodde.graph.sp.HeuristicPathFinder;
import net.coderodde.graph.util.HeuristicFunction;
import net.coderodde.util.MinimumPriorityQueue;
import net.coderodde.util.support.DaryHeap;

/**
 * This class implements 
 * <a href="https://en.wikipedia.org/wiki/A*_search_algorithm">A*</a> search 
 * algorithm.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (May 15, 2016)
 */
public class AStarPathFinder extends HeuristicPathFinder {
    
    /**
     * {@inheritDoc }
     */
    @Override
    public List<Integer> search(final Digraph digraph, 
                                final HeuristicFunction heuristicFunction, 
                                final Integer source,
                                final Integer target) {
        Objects.requireNonNull(digraph, "The input digraph is null.");
        Objects.requireNonNull(heuristicFunction, 
                               "The input heuristic function is null.");
        Objects.requireNonNull(source, "The source node is null.");
        Objects.requireNonNull(target, "The target node is null.");
        
        final MinimumPriorityQueue<Integer> OPEN = new DaryHeap<>();
        final Set<Integer> CLOSED = new HashSet<>();
        final Map<Integer, Integer> PARENTS = new HashMap<>();
        final Map<Integer, Double> DISTANCE = new HashMap<>();
        
        OPEN.add(source, heuristicFunction.estimate(source, target));
        PARENTS.put(source, null);
        DISTANCE.put(source, 0.0);
        
        while (!OPEN.isEmpty()) {
            final Integer current = OPEN.extractMinimum();
            
            if (current.equals(target)) {
                return tracebackPath(target, PARENTS);
            }
            
            CLOSED.add(current);
            
            for (final Integer child : digraph.getChildrenOf(current)) {
                if (CLOSED.contains(child)) {
                    continue;
                }
                
                final double tentativeCost = 
                        DISTANCE.get(current) +
                        digraph.getEdgeWeight(current, child);
                
                if (!DISTANCE.containsKey(child)) {
                    DISTANCE.put(child, tentativeCost);
                    PARENTS.put(child, current);
                    OPEN.add(child, 
                             tentativeCost +
                             heuristicFunction.estimate(child, target));
                } else if (DISTANCE.get(child) > tentativeCost) {
                    DISTANCE.put(child, tentativeCost);
                    PARENTS.put(child, current);
                    OPEN.decreasePriority(child,
                                          tentativeCost + 
                                          heuristicFunction.estimate(child, 
                                                                     target));
                }
            }
        }
        
        return new ArrayList<>();
    }   
}
