package net.coderodde.graph.util;

/**
 * This interface defines the API shared by all heuristic functions.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (May 15, 2016)
 */
public interface HeuristicFunction {

    /**
     * Returns an optimistic estimate for distance between {@code from} and
     * {@code to}.
     * 
     * @param from a node.
     * @param to   another node.
     * @return an optimistic estimate for distance between the two nodes.
     */
    public double estimate(final Integer from, final Integer to);
}
