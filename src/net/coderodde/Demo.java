package net.coderodde;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.coderodde.graph.Digraph;
import net.coderodde.graph.sp.HeuristicPathFinder;
import net.coderodde.graph.sp.support.AStarPathFinder;
import net.coderodde.graph.sp.support.BHPAPathFinder;
import net.coderodde.graph.sp.support.NewBidirectionalAStarPathFinder;
import net.coderodde.graph.sp.support.ParallelNewBidirectionalAStarPathFinder;
import net.coderodde.graph.util.HeuristicFunction;
import net.coderodde.graph.util.support.PointHeuristicFunction;

public class Demo {

    private static final int GRAPH_SIZE = 300_000;
    private static final int GRAPH_ARCS = 3_000_000;
    private static final double SPACE_WIDTH = 1000.0;
    private static final double SPACE_HEIGHT = 1000.0;
    private static final double LENGTH_FACTOR = 1.2;
    
    public static void main(final String... args) {
        final long seed = System.nanoTime();
        final Random random = new Random(seed);
        final GraphData data = createRandomGraph(GRAPH_SIZE,
                                                 GRAPH_ARCS,
                                                 SPACE_WIDTH,
                                                 SPACE_HEIGHT,
                                                 LENGTH_FACTOR,
                                                 random);
        System.out.println("Seed = " + seed);
        
        final Integer source = random.nextInt(data.digraph.size());
        final Integer target = random.nextInt(data.digraph.size());
        
        System.out.println("Source: " + source);
        System.out.println("Target: " + target);
        
        List<Integer> path1 = profile(data, 
                                      new AStarPathFinder(), 
                                      source, 
                                      target);
        
        List<Integer> path2 = profile(data, 
                                      new BHPAPathFinder(),
                                      source,
                                      target);
        
        List<Integer> path3 = profile(data, 
                                      new NewBidirectionalAStarPathFinder(),
                                      source, 
                                      target);
        
        List<Integer> path4 = 
                profile(data, 
                        new ParallelNewBidirectionalAStarPathFinder(),
                        source, 
                        target);
        
        final boolean agree = path1.equals(path2) && path2.equals(path3);
        System.out.println("Algorithms agree: " + agree);
    }
    
    private static List<Integer> profile(final GraphData data, 
                                         final HeuristicPathFinder finder,
                                         final Integer source,
                                         final Integer target) {
        final long startTime = System.nanoTime();
        List<Integer> path = finder.search(data.digraph, 
                                           data.heuristicFunction, 
                                           source, 
                                           target);
        final long endTime = System.nanoTime();
        
        System.out.printf("%s in %.0f milliseconds.\n", 
                          finder.getClass().getSimpleName(),
                          (endTime - startTime) / 1e6f);
        
        return path;
    }
    
    private static final class GraphData {
        Digraph digraph;
        HeuristicFunction heuristicFunction;
    }
    
    private static GraphData createRandomGraph(final int nodes,
                                               final int arcs,
                                               final double width,
                                               final double height,
                                               final double lengthFactor,
                                               final Random random) {
        GraphData ret = new GraphData();
        final Digraph digraph = new Digraph();
        final PointHeuristicFunction heuristicFunction = 
                new PointHeuristicFunction();
        
        for (int i = 0; i < nodes; ++i) {
            final Point2D.Double point = getRandomPoint(width,
                                                        height, 
                                                        random);
            final Integer node = i;
            digraph.addNode(node);
            heuristicFunction.map(node, point);
        }
        
        final List<Integer> nodeList = new ArrayList<>(digraph.getAllNodes());
        
        for (int i = 0; i < arcs; ++i) {
            final Integer tail = choose(nodeList, random);
            final Integer head = choose(nodeList, random);
            final double distance = heuristicFunction.estimate(tail, head);
            digraph.addEdge(tail, head, distance * lengthFactor);
        }
        
        ret.digraph = digraph;
        ret.heuristicFunction = heuristicFunction;
        return ret;
    }
    
    private static <E> E choose(final List<E> list, final Random random) {
        return list.get(random.nextInt(list.size()));
    }
    
    private static Point2D.Double getRandomPoint(final double width,
                                                 final double height,
                                                 final Random random) {
        return new Point2D.Double(random.nextFloat() * width, 
                                  random.nextFloat() * height);
    }
}
