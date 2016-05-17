package net.coderodde.graph.util.support;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import net.coderodde.graph.util.HeuristicFunction;

/**
 * This class implements a heuristic function. It stores an associative array 
 * mapping each graph node to a point in a plane, and uses them for computing 
 * Euclidean distance between two given graph nodes.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (May 15, 2016)
 */
public class PointHeuristicFunction implements HeuristicFunction {

    private final Map<Integer, Point2D.Double> map = new HashMap<>();
    
    public void map(final Integer node, final Point2D.Double point) {
        this.map.put(node, point);
    }
    
    @Override
    public double estimate(final Integer from, final Integer to) {
        return map.get(from).distance(map.get(to));
    }
}
