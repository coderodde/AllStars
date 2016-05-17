package net.coderodde.graph.sp.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import net.coderodde.graph.Digraph;
import net.coderodde.graph.sp.HeuristicPathFinder;
import net.coderodde.graph.util.HeuristicFunction;
import net.coderodde.util.MinimumPriorityQueue;
import net.coderodde.util.support.DaryHeap;

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
        Objects.requireNonNull(digraph, "The input digraph is null.");
        Objects.requireNonNull(heuristicFunction, 
                               "The input heuristic function is null.");
        Objects.requireNonNull(source, "The source node is null.");
        Objects.requireNonNull(target, "The target node is null.");

        if (source.equals(target)) {
            return new ArrayList<>(Arrays.asList(target));
        }

        final PathLengthHolder pathLengthHolder = new PathLengthHolder();
        final Set<Integer> CLOSED = 
                Collections.<Integer>newSetFromMap(new ConcurrentHashMap<>());

        final SearchThread forwardThread = 
                new ForwardSearchThread(CLOSED,
                                        digraph,
                                        heuristicFunction, 
                                        source, 
                                        target, 
                                        pathLengthHolder);

        final SearchThread backwardThread = 
                new BackwardSearchThread(CLOSED,
                                         digraph, 
                                         heuristicFunction, 
                                         source, 
                                         target, 
                                         pathLengthHolder);

        forwardThread.setBrotherThread(backwardThread);
        backwardThread.setBrotherThread(forwardThread);

        forwardThread.start();
        backwardThread.start();

        try {
            forwardThread.join();
            backwardThread.join();
        } catch (InterruptedException ex) {
            throw new RuntimeException("PNBA* was interrupted!");
        }

        final Integer touchNode = pathLengthHolder.getTouchNode();

        if (touchNode == null) {
            return new ArrayList<>();
        }

        return tracebackPath(touchNode,
                             forwardThread.getParentMap(),
                             backwardThread.getParentMap());
    }

    private static final class PathLengthHolder {

        private final Semaphore mutex = new Semaphore(1);
        private volatile double length = Double.POSITIVE_INFINITY;
        private volatile Integer touchNode;

        double read() {
            return length;
        }

        void tryUpdate(final double length, final Integer touchNode) {
            mutex.acquireUninterruptibly();

            if (this.length > length) {
                this.length = length;
                this.touchNode = touchNode;
            }

            mutex.release();
        }

        Integer getTouchNode() {
            return touchNode;
        }
    }

    private static class SearchThread
    extends Thread {

        protected volatile boolean finished;
        protected volatile double F;
        protected SearchThread brotherThread;
        protected final MinimumPriorityQueue<Integer> OPEN = new DaryHeap<>();
        protected final Set<Integer> CLOSED;
        protected final Map<Integer, Integer> PARENTS = new HashMap<>();
        protected final Map<Integer, Double> DISTANCE = new HashMap<>();
        protected final Digraph digraph;
        protected final HeuristicFunction heuristicFunction;
        protected final PathLengthHolder pathLengthHolder;
        protected final Integer source;
        protected final Integer target;

        SearchThread(final Set<Integer> CLOSED,
                     final Digraph digraph,
                     final HeuristicFunction heuristicFunction,
                     final Integer source,
                     final Integer target,
                     final PathLengthHolder pathLengthHolder) {
            this.CLOSED = CLOSED;
            this.digraph = digraph;
            this.heuristicFunction = heuristicFunction;
            this.pathLengthHolder = pathLengthHolder;
            this.source = source;
            this.target = target;
        }

        SearchThread getBrotherThread() {
            return this.brotherThread;
        }

        void setBrotherThread(SearchThread brotherThread) {
            this.brotherThread = brotherThread;
        }

        void finish() {
            finished = true;
            brotherThread.finished = true;
        }

        double getF() {
            return F;
        }

        Map<Integer, Integer> getParentMap() {
            return PARENTS;
        }

        Map<Integer, Double> getDistanceMap() {
            return DISTANCE;
        }
    }

    private static final class ForwardSearchThread extends SearchThread {

        ForwardSearchThread(final Set<Integer> CLOSED,
                            final Digraph digraph,
                            final HeuristicFunction heuristicFunction,
                            final Integer source,
                            final Integer target,
                            final PathLengthHolder pathLengthHolder) {

            super(CLOSED,
                  digraph,
                  heuristicFunction, 
                  source, 
                  target,
                  pathLengthHolder);
        }

        @Override
        public void run() {
            F = heuristicFunction.estimate(source, target);
            PARENTS.put(source, null);
            DISTANCE.put(source, 0.0);
            OPEN.add(source, F);

            while (!finished) {
                if (OPEN.isEmpty()) {
                    finish();
                    return;
                }

                final Integer current = OPEN.extractMinimum();

                if (CLOSED.contains(current)) {
                    continue;
                }

                final double f = DISTANCE.get(current) + 
                           heuristicFunction.estimate(current, target);
                final double L = pathLengthHolder.read();
                double tmp = DISTANCE.get(current) + 
                             brotherThread.getF() - 
                             heuristicFunction.estimate(current, source);

                if (f < L && tmp < L) {
                    for (final Integer child : digraph.getChildrenOf(current)) {
                        if (CLOSED.contains(child)) {
                            continue;
                        }

                        double tentativeScore = DISTANCE.get(current) + 
                                                digraph.getEdgeWeight(current, 
                                                                      child);

                        if (!DISTANCE.containsKey(child)) {
                            DISTANCE.put(child, tentativeScore);
                            PARENTS.put(child, current);
                            OPEN.add(child, 
                                     tentativeScore + 
                                     heuristicFunction.estimate(child, target));

                            Map<Integer, Double> OTHER_DISTANCE = 
                                    getBrotherThread().getDistanceMap();

                            Double g2 = OTHER_DISTANCE.get(child);

                            if (g2 != null) {
                                double tmpDist = g2 + DISTANCE.get(child);

                                if (pathLengthHolder.read() > tmpDist) {
                                    pathLengthHolder.tryUpdate(tmpDist, child);
                                }
                            }
                        } else if (DISTANCE.get(child) > tentativeScore) {
                            DISTANCE.put(child, tentativeScore);
                            PARENTS.put(child, current);
                            OPEN.decreasePriority(
                                     child,
                                     tentativeScore +
                                     heuristicFunction.estimate(child, target));

                            Map<Integer, Double> OTHER_DISTANCE = getBrotherThread()
                                                           .getDistanceMap();

                            Double g2 = OTHER_DISTANCE.get(child);

                            if (g2 != null) {
                                double tmpDist = g2 + DISTANCE.get(child);

                                if (pathLengthHolder.read() > tmpDist) {
                                    pathLengthHolder.tryUpdate(tmpDist, child);
                                }
                            }
                        }
                    }

                    CLOSED.add(current);
                }

                if (OPEN.isEmpty()) {
                    finish();
                    return;
                }

                this.F = DISTANCE.get(OPEN.min()) + 
                         heuristicFunction.estimate(OPEN.min(), target);
            }
        }
    }

    private static final class BackwardSearchThread extends SearchThread {

        BackwardSearchThread(final Set<Integer> CLOSED,
                             final Digraph digraph,
                             final HeuristicFunction heuristicFunction,
                             final Integer source,
                             final Integer target,
                             final PathLengthHolder pathLengthHolder) {
            super(CLOSED,
                  digraph, 
                  heuristicFunction,
                  source,
                  target,
                  pathLengthHolder);
        }

        @Override
        public void run() {
            F = heuristicFunction.estimate(source, target);
            PARENTS.put(target, null);
            DISTANCE.put(target, 0.0);
            OPEN.add(target, F);

            while (!finished) {
                if (OPEN.isEmpty()) {
                    finish();
                    return;
                }

                final Integer current = OPEN.extractMinimum();

                if (CLOSED.contains(current)) {
                    continue;
                }

                final double f = DISTANCE.get(current) + 
                           heuristicFunction.estimate(current, source);
                final double L = pathLengthHolder.read();
                double tmp = DISTANCE.get(current) + 
                             brotherThread.getF() - 
                             heuristicFunction.estimate(current, target);

                if (f < L && tmp < L) {
                    for (final Integer parent : digraph.getParentsOf(current)) {
                        if (CLOSED.contains(parent)) {
                            continue;
                        }

                        double tentativeScore = DISTANCE.get(current) + 
                                                digraph.getEdgeWeight(parent, 
                                                                      current);
                        if (!DISTANCE.containsKey(parent)) {
                            DISTANCE.put(parent, tentativeScore);
                            PARENTS.put(parent, current);
                            OPEN.add(parent, 
                                     tentativeScore + 
                                     heuristicFunction.estimate(parent, 
                                                                source));

                            Map<Integer, Double> OTHER_DISTANCE =
                                    getBrotherThread().getDistanceMap();

                            Double g2 = OTHER_DISTANCE.get(parent);

                            if (g2 != null) {
                                double tmpDist = g2 + DISTANCE.get(parent);

                                if (pathLengthHolder.read() > tmpDist) {
                                    pathLengthHolder.tryUpdate(tmpDist, parent);
                                }
                            }
                        } else if (DISTANCE.get(parent) > tentativeScore) {
                            DISTANCE.put(parent, tentativeScore);
                            PARENTS.put(parent, current);
                            OPEN.decreasePriority(
                                     parent,
                                     tentativeScore +
                                     heuristicFunction.estimate(parent, 
                                                                source));

                            Map<Integer, Double> OTHER_DISTANCE = 
                                    getBrotherThread().getDistanceMap();

                            Double g2 = OTHER_DISTANCE.get(parent);

                            if (g2 != null) {
                                double tmpDist = g2 + DISTANCE.get(parent);

                                if (pathLengthHolder.read() > tmpDist) {
                                    pathLengthHolder.tryUpdate(tmpDist, parent);
                                }
                            }
                        }
                    }

                    CLOSED.add(current);
                }

                if (OPEN.isEmpty()) {
                    finish();
                    return;
                }

                this.F = DISTANCE.get(OPEN.min()) + 
                         heuristicFunction.estimate(OPEN.min(), target);
            }
        }
    }
}
