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
 * This class implements a bidirectional heuristic graph search algorithm called
 * <i>New bidirectional A* - NBA*</i>. It was published in the paper 
 * "Yet another bidirectional algorithm for shortest paths" by Wim Pijls and
 * Henk Post.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (May 15, 2016)
 */
public class NewBidirectionalAStarPathFinder extends HeuristicPathFinder {

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
        
        final MinimumPriorityQueue<Integer> OPENA = new DaryHeap<>();
        final MinimumPriorityQueue<Integer> OPENB = new DaryHeap<>();
        
        final Set<Integer> CLOSED = new HashSet<>();
        
        final Map<Integer, Integer> PARENTSA = new HashMap<>();
        final Map<Integer, Integer> PARENTSB = new HashMap<>();
        
        final Map<Integer, Double> DISTANCEA = new HashMap<>();
        final Map<Integer, Double> DISTANCEB = new HashMap<>();
        
        double bestPathCost = Double.POSITIVE_INFINITY;
        double fA = heuristicFunction.estimate(source, target);
        double fB = heuristicFunction.estimate(target, source);
        Integer touchNode = null;
        
        OPENA.add(source, fA);
        OPENB.add(target, fB);
        
        PARENTSA.put(source, null);
        PARENTSB.put(target, null);
        
        DISTANCEA.put(source, 0.0);
        DISTANCEB.put(target, 0.0);
        
        while (!OPENA.isEmpty() && !OPENB.isEmpty()) {
            if (OPENA.size() < OPENB.size()) {
                final Integer current = OPENA.extractMinimum();
                CLOSED.add(current);
                
                final double currentDistance = DISTANCEA.get(current);
                
                if (currentDistance + 
                        heuristicFunction.estimate(current, target) 
                        >= bestPathCost 
                        || 
                        currentDistance + fB -
                        heuristicFunction.estimate(current, source)
                        >= bestPathCost) {
                    // Reject 'current'.
                    continue;
                }
                
                for (final Integer child : digraph.getChildrenOf(current)) {
                    if (CLOSED.contains(child)) {
                        continue;
                    }
                    
                    final double tentativeScore = 
                            DISTANCEA.get(current) + 
                            digraph.getEdgeWeight(current, child);
                    
                    if (!DISTANCEA.containsKey(child)) {
                        DISTANCEA.put(child, tentativeScore);
                        PARENTSA.put(child, current);
                        OPENA.add(child, 
                                  tentativeScore +
                                  heuristicFunction.estimate(child, target));

                        if (DISTANCEB.containsKey(child)) {
                            double pathLength = tentativeScore +
                                                DISTANCEB.get(child);

                            if (bestPathCost > pathLength) {
                                bestPathCost = pathLength;
                                touchNode = child;
                            }
                        }
                    } else if (DISTANCEA.get(child) > tentativeScore) {
                        DISTANCEA.put(child, tentativeScore);
                        PARENTSA.put(child, current);
                        OPENA.decreasePriority(
                                child,
                                tentativeScore +
                                heuristicFunction.estimate(child, target));

                        if (DISTANCEB.containsKey(child)) {
                            double pathLength = tentativeScore +
                                                DISTANCEB.get(child);

                            if (bestPathCost > pathLength) {
                                bestPathCost = pathLength;
                                touchNode = child;
                            }
                        }
                    }
                }
                
                if (!OPENA.isEmpty()) {
                    final Integer min = OPENA.min();
                    fA = DISTANCEA.get(min) + 
                         heuristicFunction.estimate(current, target);
                }
            } else {
                final Integer current = OPENB.extractMinimum();
                CLOSED.add(current);
                
                final double currentDistance = DISTANCEB.get(current);
                
                if (currentDistance + 
                        heuristicFunction.estimate(current, source)
                        >= bestPathCost
                        ||
                        currentDistance + fA 
                        - heuristicFunction.estimate(current, target)
                        >= bestPathCost) {
                    // Reject 'current'.
                    continue;
                } 
                
                for (final Integer parent : digraph.getParentsOf(current)) {
                    if (CLOSED.contains(parent)) {
                        continue;
                    }

                    double tentativeScore = DISTANCEB.get(current) +
                                            digraph.getEdgeWeight(parent, 
                                                                  current);
                    if (!DISTANCEB.containsKey(parent)) {
                        DISTANCEB.put(parent, tentativeScore);
                        PARENTSB.put(parent, current);
                        OPENB.add(parent, 
                                  tentativeScore +
                                  heuristicFunction.estimate(parent, source));

                        if (DISTANCEA.containsKey(parent)) {
                            double pathLength = tentativeScore +
                                                DISTANCEA.get(parent);

                            if (bestPathCost > pathLength) {
                                bestPathCost = pathLength;
                                touchNode = parent;
                            }
                        }
                    } else if (DISTANCEB.get(parent) > tentativeScore) {
                        DISTANCEB.put(parent, tentativeScore);
                        PARENTSB.put(parent, current);
                        OPENB.decreasePriority(
                                parent,
                                tentativeScore +
                                heuristicFunction.estimate(parent, source));

                        if (DISTANCEA.containsKey(parent)) {
                            double pathLength = tentativeScore +
                                                DISTANCEA.get(parent);

                            if (bestPathCost > pathLength) {
                                bestPathCost = pathLength;
                                touchNode = parent;
                            }
                        }
                    }
                }
                
                if (!OPENB.isEmpty()) {
                    final Integer min = OPENB.min();
                    fB = DISTANCEB.get(min) + 
                         heuristicFunction.estimate(current, source);
                }
            }
        }
        
        if (touchNode == null) {
            return new ArrayList<>();
        }
        
        return tracebackPath(touchNode, PARENTSA, PARENTSB);
    }
}
