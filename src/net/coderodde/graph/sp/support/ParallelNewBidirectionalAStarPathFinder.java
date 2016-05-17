package net.coderodde.graph.sp.support;

import java.util.List;
import net.coderodde.graph.Digraph;
import net.coderodde.graph.sp.HeuristicPathFinder;
import net.coderodde.graph.util.HeuristicFunction;

/**
 * This class implements a bidirectional heuristic graph search algorithm that
 * runs the two search trees in parallel using hardware concurrency. It was 
 * published in the paper ""A Parallel Bidirectional Heuristic Search Algorithm"
 * by Luis Henrique Oliveira Rios, and Luiz Chaimowicz.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (May 15, 2016)
 */
public class ParallelNewBidirectionalAStarPathFinder
extends HeuristicPathFinder {

    /**
     * {@inheritDoc } 
     */
    @Override
    public List<Integer> search(final Digraph digraph, 
                                final HeuristicFunction heuristicFunction, 
                                final Integer source, 
                                final Integer target) {
        return null;
    }
}
