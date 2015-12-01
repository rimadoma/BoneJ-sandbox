package protoOps;

import ij.ImagePlus;
import net.imagej.ops.Op;
import org.bonej.common.Centroid;
import org.bonej.common.Common;
import org.bonej.geometry.Vectors;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.analyzeSkeleton.*;

import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michael Doube
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 * @todo test with old tests
 * @todo is there a way to say which image types this Op can handle?
 */
@Plugin(type=Op.class, name = "triplePointAngles")
public class TriplePointAngles implements Op {
    public static final int DEFAULT_NTH_POINT = 0;
    public static final int VERTEX_TO_VERTEX = -1;

    private static final AnalyzeSkeleton_ skeletonAnalyzer = new AnalyzeSkeleton_();

    //@todo change to Dataset, and then unwrap ImagePlus?
    @Parameter(required = true)
    private ImagePlus inputImage = null;

    @Parameter
    private int nthPixel = DEFAULT_NTH_POINT;

    @Parameter(type = ItemIO.OUTPUT)
    private double results[][][] = null;

    @Override
    public void run() {
        calculateTriplePointAngles();
    }

    public double[][][] getResults() {
        return results;
    }

    public void setInputImage(ImagePlus image) {
        checkNotNull(image, "Input image cannot be set null");
        checkArgument(image.getBitDepth() == 8, "The bit depth of the input image must be 8");

        inputImage = image;
    }

    public void calculateTriplePointAngles() {
        checkNotNull(inputImage, "Input image cannot be null");
        checkArgument(inputImage.getBitDepth() == 8, "The bit depth of the input image must be 8");

        skeletonAnalyzer.setup("", inputImage);
        skeletonAnalyzer.run();
        Graph[] graphs = skeletonAnalyzer.getGraphs();
        results = new double[graphs.length][][];
        int g = 0;

        for (Graph graph : graphs) {
            ArrayList<Vertex> vertices = graph.getVertices();
            double[][] vertexAngles = new double[vertices.size()][3];
            int v = 0;
            for (Vertex vertex : vertices) {
                if (!isTriplePoint(vertex)) {
                    vertexAngles[v] = null;
                    v++;
                    continue;
                }

                ArrayList<Edge> edges = vertex.getBranches();

                Edge edge0 = edges.get(0);
                Edge edge1 = edges.get(1);
                Edge edge2 = edges.get(2);

                double theta0 = vertexAngle(vertex, edge0, edge1, nthPixel);
                double theta1 = vertexAngle(vertex, edge0, edge2, nthPixel);
                double theta2 = vertexAngle(vertex, edge1, edge2, nthPixel);

                double[] thetas = { theta0, theta1, theta2 };

                vertexAngles[v] = thetas;
                v++;
            }
            results[g] = vertexAngles;
            g++;
        }
    }

    private static boolean isTriplePoint(Vertex vertex) {
        return vertex.getBranches().size() == 3;
    }

    public static boolean isInNeighborhood(Point p0, Point v) {
        for (int x = v.x - 1; x <= v.x + 1; x++) {
            for (int y = v.y - 1; y <= v.y + 1; y++) {
                for (int z = v.z - 1; z <= v.z + 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    if (x == p0.x && y == p0.y && z == p0.z) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private Point getNthPoint(Vertex vertex, Edge edge, int nthPixel) {
        ArrayList<Point> vertexPoints = vertex.getPoints();
        ArrayList<Point> edgePoints = edge.getSlabs();
        boolean startAtZero = false;
        Point edgeStart = edgePoints.get(0);

        for (Point vertexPoint : vertexPoints) {
            if (isInNeighborhood(edgeStart, vertexPoint)) {
                startAtZero = true;
                break;
            }
        }

        nthPixel = Common.clamp(nthPixel, 0, edgePoints.size() - 1);

        if (startAtZero) {
            // Vertex is the start vertex of the edge so start counting "up"
            return edgePoints.get(nthPixel);
        }

        // Vertex is the end vertex of the edge so start counting "down"
        return edgePoints.get(edgePoints.size() - nthPixel - 1);
    }

    private double vertexAngle(Vertex vertex, Edge edge0, Edge edge1, int nthPixel) {
        if (nthPixel == VERTEX_TO_VERTEX) {
            return vertexAngle(vertex, edge0, edge1);
        }

        Point p0 = getNthPoint(vertex, edge0, nthPixel);
        Point p1 = getNthPoint(vertex, edge1, nthPixel);

        double cv[] = Centroid.getCentroid(vertex.getPoints());
        return Vectors.joinedVectorAngle(p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, cv[0], cv[1],
                cv[2]);
    }

    private static double vertexAngle(Vertex vertex, Edge edge0, Edge edge1) {
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
}
