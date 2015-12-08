package protoOps;

import ij.ImagePlus;
import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;
import org.bonej.common.Centroid;
import org.bonej.common.Common;
import org.bonej.geometry.Vectors;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.analyzeSkeleton.*;
import sc.fiji.skeletonize3D.Skeletonize3D_;

import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michael Doube
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 * @todo is there a way to say which image types this Op can handle?
 */
@Plugin(type=Op.class, name = "triplePointAngles")
public class TriplePointAngles implements Op {
    public static final int DEFAULT_NTH_POINT = 0;
    public static final int VERTEX_TO_VERTEX = -1;

    private static final AnalyzeSkeleton_ skeletonAnalyzer = new AnalyzeSkeleton_();
    private static final Skeletonize3D_ skeletonizer = new Skeletonize3D_();

    //@todo change to Dataset, and then unwrap ImagePlus?
    @Parameter
    private ImagePlus inputImage = null;

    @Parameter(min = "0", required = false)
    private int nthPoint = DEFAULT_NTH_POINT;

    /**
     * A list of graphs containing list of triple points (vertices) containing a list angles (3) between each branch
     */
    @Parameter(type = ItemIO.OUTPUT)
    private double results[][][] = null;

    public double[][][] getResults() {
        return results;
    }

    public void setInputImage(ImagePlus image) {
        inputImage = image;
    }

    /**
     * Sets the distance of the angle measurement from the centroid of the triple points.
     *
     * @param   nthPoint    number pixels from the triple point centroid
     * @see     TriplePointAngles#nthPoint
     *
     */
    public void setNthPoint(int nthPoint) {
        this.nthPoint = nthPoint;
    }

    public void calculateTriplePointAngles() {
        checkImage(inputImage);
        checkNthPoint(nthPoint);

        skeletonizer.setup("", inputImage);
        skeletonizer.run(null);

        skeletonAnalyzer.setup("", inputImage);
        skeletonAnalyzer.run();
        Graph[] graphs = skeletonAnalyzer.getGraphs();

        if (graphs == null || graphs.length == 0) {
            // image could not be skeletonized, throw exception?
            results = null;
            return;
        }

        ArrayList<ArrayList<double[]>> graphsVertices = new ArrayList<>();

        for (int g = 0; g < graphs.length; g++) {
            ArrayList<Vertex> vertices = graphs[g].getVertices();
            ArrayList<double[]> vertexAngles = new ArrayList<>();

            for (int v = 0; v < vertices.size(); v++) {
                Vertex vertex = vertices.get(v);

                if (!isTriplePoint(vertex)) {
                    continue;
                }

                ArrayList<Edge> edges = vertex.getBranches();
                Edge edge0 = edges.get(0);
                Edge edge1 = edges.get(1);
                Edge edge2 = edges.get(2);

                double thetas[] = new double[3];
                if (nthPoint == VERTEX_TO_VERTEX) {
                    thetas[0] = vertexToVertexAngle(vertex, edge0, edge1);
                    thetas[1] = vertexToVertexAngle(vertex, edge0, edge2);
                    thetas[2] = vertexToVertexAngle(vertex, edge1, edge2);
                } else {
                    thetas[0] = vertexAngle(vertex, edge0, edge1);
                    thetas[1] = vertexAngle(vertex, edge0, edge2);
                    thetas[2] = vertexAngle(vertex, edge1, edge2);
                }

                vertexAngles.add(thetas);
            }
            graphsVertices.add(vertexAngles);
        }

        results = new double[graphsVertices.size()][][];
        final int treeSize = graphsVertices.size();
        for (int g = 0; g < treeSize; g++) {
            ArrayList<double []> vertexAngles = graphsVertices.get(g);
            final int graphSize = vertexAngles.size();
            results[g] = new double[graphSize][];
            for (int v = 0; v < graphSize; v++) {
                results[g][v] = vertexAngles.get(v);
            }
        }
    }

    @Override
    public void run() {
        calculateTriplePointAngles();
    }

    @Override
    public OpEnvironment ops() {
        return null;
    }

    @Override
    public void setEnvironment(OpEnvironment opEnvironment) {

    }

    /**
     * @param   image
     * @throws  NullPointerException if image == null
     * @throws  IllegalArgumentException if image.getBitDepth() != 8
     */
    //region -- Utility methods --
    public static void checkImage(ImagePlus image) {
        checkNotNull(image, "Input image cannot be set null");
        checkArgument(image.getBitDepth() == 8, "The bit depth of the input image must be 8");
    }

    /**
     * @param   nthPoint
     * @throws  IllegalArgumentException if parameter nthPoint < 0 && nthPoint != TriplePointAngles#VERTEX_TO_VERTEX
     */
    public static void checkNthPoint(int nthPoint) {
        checkArgument(nthPoint >= 0 || nthPoint == VERTEX_TO_VERTEX, "Invalid nth point value");
    }
    //endregion

    //region -- Helper methods --
    private static boolean isVoxel26Connected(Point point, Point voxel) {
        int xDistance = Math.abs(point.x - voxel.x);
        int yDistance = Math.abs(point.y - voxel.y);
        int zDistance = Math.abs(point.z - voxel.z);

        return xDistance <= 1 && yDistance <= 1 && zDistance <= 1;
    }

    private static boolean isTriplePoint(Vertex vertex) {
        return vertex.getBranches().size() == 3;
    }

    private static double vertexToVertexAngle(Vertex vertex, Edge edge0, Edge edge1) {
        Vertex oppositeVertex0 = edge0.getOppositeVertex(vertex);
        Vertex oppositeVertex1 = edge1.getOppositeVertex(vertex);

        ArrayList<Point> vertexPoints = vertex.getPoints();
        ArrayList<Point> oppositeVertex0Points = oppositeVertex0.getPoints();
        ArrayList<Point> oppositeVertex1Points = oppositeVertex1.getPoints();

        double[] vertexCentroid = Centroid.getCentroid(vertexPoints);
        double[] oppositeVertex0Centroid = Centroid.getCentroid(oppositeVertex0Points);
        double[] oppositeVertex1Centroid = Centroid.getCentroid(oppositeVertex1Points);

        return Vectors.joinedVectorAngle(
                oppositeVertex0Centroid[0], oppositeVertex0Centroid[1], oppositeVertex0Centroid[2],
                oppositeVertex1Centroid[0], oppositeVertex1Centroid[1], oppositeVertex1Centroid[2],
                vertexCentroid[0], vertexCentroid[1], vertexCentroid[2]);
    }

    private double vertexAngle(Vertex vertex, Edge edge0, Edge edge1) {
        Point p0 = getNthPointOfEdge(vertex, edge0);
        Point p1 = getNthPointOfEdge(vertex, edge1);

        double cv[] = Centroid.getCentroid(vertex.getPoints());
        return Vectors.joinedVectorAngle(p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, cv[0], cv[1],
                cv[2]);
    }

    private Point getNthPointOfEdge(Vertex vertex, Edge edge) {
        ArrayList<Point> vertexPoints = vertex.getPoints();
        ArrayList<Point> edgePoints = edge.getSlabs();
        boolean startAtZero = false;
        Point edgeStart = edgePoints.get(0);

        for (Point vertexPoint : vertexPoints) {
            if (isVoxel26Connected(edgeStart, vertexPoint)) {
                startAtZero = true;
                break;
            }
        }

        nthPoint = Common.clamp(nthPoint, 0, edgePoints.size() - 1);

        if (startAtZero) {
            // Vertex is the start vertex of the edge so start counting "up"
            return edgePoints.get(nthPoint);
        }

        // Vertex is the end vertex of the edge so start counting "down"
        return edgePoints.get(edgePoints.size() - nthPoint - 1);
    }
    //endregion
}
