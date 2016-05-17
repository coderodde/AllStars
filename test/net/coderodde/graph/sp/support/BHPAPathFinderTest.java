package net.coderodde.graph.sp.support;

import java.awt.geom.Point2D;
import net.coderodde.graph.Digraph;
import net.coderodde.graph.util.HeuristicFunction;
import net.coderodde.graph.util.support.PointHeuristicFunction;
import org.junit.Test;
import static org.junit.Assert.*;

public class BHPAPathFinderTest {
    

    @Test
    public void testSearch() {
        Digraph digraph = new Digraph();
        digraph.addEdge(0, 1, 1.0);
        digraph.addEdge(1, 2, 1.0);
        digraph.addEdge(2, 0, 1.0);
        
        PointHeuristicFunction hf = new PointHeuristicFunction();
        Point2D.Double p = new Point2D.Double();
        hf.map(0, new Point2D.Double(0.0, 0.0));
        hf.map(1, new Point2D.Double(0.5, 0.5));
        hf.map(2, new Point2D.Double(0.5, 0.0));
        
        System.out.println(new BHPAPathFinder().search(digraph, hf, 0, 0));
    }
    
}
