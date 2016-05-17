package net.coderodde.graph.sp.support;

import java.util.ArrayList;
import java.util.Arrays;
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
 * This class implements the BHPA algorithm discovered by Ira Pohl around 1971.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (May 15, 2016)
 */
public class BHPAPathFinder extends HeuristicPathFinder {

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

        if (source.equals(target)) {
            return new ArrayList<>(Arrays.asList(target));
        }

        final MinimumPriorityQueue<Integer> OPENA = new DaryHeap<>();
        final MinimumPriorityQueue<Integer> OPENB = new DaryHeap<>();

        final Set<Integer> CLOSEDA = new HashSet<>();
        final Set<Integer> CLOSEDB = new HashSet<>();

        final Map<Integer, Integer> PARENTSA = new HashMap<>();
        final Map<Integer, Integer> PARENTSB = new HashMap<>();

        final Map<Integer, Double> DISTANCEA = new HashMap<>();
        final Map<Integer, Double> DISTANCEB = new HashMap<>();

        OPENA.add(source, 0.0);
        OPENB.add(target, 0.0);

        PARENTSA.put(source, null);
        PARENTSB.put(target, null);

        DISTANCEA.put(source, 0.0);
        DISTANCEB.put(target, 0.0);

        Integer touchNode = null;
        double bestPathCost = Double.POSITIVE_INFINITY;

        while (!OPENA.isEmpty() && !OPENB.isEmpty()) {
            if (touchNode != null) {
                final Integer minA = OPENA.min();
                final Integer minB = OPENB.min();

                final double distanceA = DISTANCEA.get(minA) +
                                         heuristicFunction.estimate(minA, 
                                                                    target);

                final double distanceB = DISTANCEB.get(minB) +
                                         heuristicFunction.estimate(minB,
                                                                    source);

                if (bestPathCost <= Math.max(distanceA, distanceB)) {
                    return tracebackPath(touchNode, PARENTSA, PARENTSB);
                }
            }

            if (OPENA.size() + CLOSEDA.size() < OPENB.size() + CLOSEDB.size()) {
                final Integer current = OPENA.extractMinimum();
                CLOSEDA.add(current);

                for (final Integer child : digraph.getChildrenOf(current)) {
                    if (CLOSEDA.contains(child)) {
                        continue;
                    }

                    final double tentativeScore = DISTANCEA.get(current) +
                                                  digraph.getEdgeWeight(current, 
                                                                        child);

                    if (!DISTANCEA.containsKey(child)) {
                        DISTANCEA.put(child, tentativeScore);
                        PARENTSA.put(child, current);
                        OPENA.add(child,
                                  tentativeScore + 
                                  heuristicFunction.estimate(child, target));

                        if (CLOSEDB.contains(child)) {
                            final double pathCost = DISTANCEB.get(child) + 
                                                    tentativeScore;

                            if (bestPathCost > pathCost) {
                                bestPathCost = pathCost;
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

                        if (CLOSEDB.contains(child)) {
                            final double pathCost = DISTANCEB.get(child) + 
                                                    tentativeScore;

                            if (bestPathCost > pathCost) {
                                bestPathCost = pathCost;
                                touchNode = child;
                            }
                        }
                    }
                }
            } else {   
                final Integer current = OPENB.extractMinimum();
                CLOSEDB.add(current);

                for (final Integer parent : digraph.getParentsOf(current)) {
                    if (CLOSEDB.contains(parent)) {
                        continue;
                    }

                    final double tentativeScore = 
                            DISTANCEB.get(current) +
                            digraph.getEdgeWeight(parent, current);

                    if (!DISTANCEB.containsKey(parent)) {
                        DISTANCEB.put(parent, tentativeScore);
                        PARENTSB.put(parent, current);
                        OPENB.add(parent,
                                  tentativeScore + 
                                  heuristicFunction.estimate(parent, source));

                        if (CLOSEDA.contains(parent)) {
                            final double pathCost = DISTANCEA.get(parent) + 
                                                    tentativeScore;

                            if (bestPathCost > pathCost) {
                                bestPathCost = pathCost;
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

                        if (CLOSEDA.contains(parent)) {
                            final double pathCost = DISTANCEA.get(parent) + 
                                                    tentativeScore;

                            if (bestPathCost > pathCost) {
                                bestPathCost = pathCost;
                                touchNode = parent;
                            }
                        }
                    }
                }
            }
        }

        return new ArrayList<>();
    }
}
