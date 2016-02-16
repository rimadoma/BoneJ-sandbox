package protoOps.triplePointAngles;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;

import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;

import org.bonej.common.Centroid;
import org.bonej.common.Common;
import org.bonej.common.ImageCheck;
import org.bonej.geometry.Vectors;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import sc.fiji.analyzeSkeleton.*;
import sc.fiji.skeletonize3D.Skeletonize3D_;
import ij.ImagePlus;

/**
 * Skeletonizes the input image, and then calculates the angles at each of its
 * triple points. A triple point is a point where three edges of the skeleton
 * meet at a single vertex. The plugin calculates angles between each of these
 * edges (branches), at each triple point (vertex) in each skeleton (graph) in
 * the image.
 *
 * @author Michael Doube
 * @author Richard Domander
 */
@Plugin(type = Op.class, name = "triplePointAngles")
public class TriplePointAngles implements Op {
	public static final int DEFAULT_NTH_POINT = 0;
	public static final int VERTEX_TO_VERTEX = -1;

	private final AnalyzeSkeleton_ skeletonAnalyzer = new AnalyzeSkeleton_();
	private final Skeletonize3D_ skeletonizer = new Skeletonize3D_();

	@Parameter(type = ItemIO.INPUT)
	private ImagePlus inputImage = null;

	@Parameter(min = "-1", required = false)
	private int nthPoint = DEFAULT_NTH_POINT;

	/**
	 * An Optional containing the results array.
     * The results are in an array of of skeletons
     * containing an array of triple points
     * containing an array of angles (3) between the branches of the triple point.
	 */
	@Parameter(type = ItemIO.OUTPUT)
	private double[][][] results;

	/**
     * Get the array of the angles of the triple points from the previous run
     *
     * @see     TriplePointAngles#results
	 * @return  An optional containing the array of results.
     *          The Optional is empty if calculateTriplePointAngles() hasn't been called yet,
     *          or calculateTriplePointAngles() failed
	 */
	public Optional<double[][][]> getResults() {
		return Optional.ofNullable(results);
	}

	/**
	 * Sets the input image for the Op
	 * 
	 * @throws NullPointerException if image == null
	 * @throws IllegalArgumentException if the image is not binary
	 */
	public void setInputImage(final ImagePlus image) throws NullPointerException, IllegalArgumentException {
		checkImage(image);

		inputImage = image;
	}

	/**
	 * Sets the distance of the angle measurement from the centroid of the
	 * triple points.
	 *
	 * @param nthPoint distance as voxels from the triple point centroid
	 * @throws IllegalArgumentException if nthPoint < 0 && nthPoint != TriplePointAngles#VERTEX_TO_VERTEX
	 */
	public void setNthPoint(int nthPoint) throws IllegalArgumentException {
		checkNthPoint(nthPoint);

		this.nthPoint = nthPoint;
	}

	/**
	 * Calculates the angles of the triple points in the input image
     *
	 * @throws NullPointerException if this.inputImage == null
	 * @throws IllegalArgumentException if this.inputImage is not binary
	 * @throws IllegalArgumentException if this.inputImage could not be skeletonized
	 */
	public void calculateTriplePointAngles() throws NullPointerException, IllegalArgumentException {
		checkImage(inputImage);

		results = null;

		skeletonizer.setup("", inputImage);
		skeletonizer.run(null);

		skeletonAnalyzer.setup("", inputImage);
		skeletonAnalyzer.run();
		Graph[] graphs = skeletonAnalyzer.getGraphs();

		if (graphs == null || graphs.length == 0) {
			throw new IllegalArgumentException("Input image could not be skeletonized");
		}

		ArrayList<ArrayList<double[]>> graphsVertices = new ArrayList<>();

		for (Graph graph : graphs) {
			final Stream<Vertex> vertices = graph.getVertices().stream().filter(TriplePointAngles::isTriplePoint);
			final ArrayList<double[]> vertexAngles = new ArrayList<>();
            vertices.forEach(v -> vertexAngles.add(calculateAnglesForVertex(v)));
			graphsVertices.add(vertexAngles);
		}

        createResults(graphsVertices);
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

	// region -- Helper methods --
    private void createResults(final ArrayList<ArrayList<double[]>> graphs) {
        final int numGraphs = graphs.size();
        results = new double[numGraphs][][];

        for (int g = 0; g < numGraphs; g++) {
            final ArrayList<double[]> triplePoints = graphs.get(g);
            final int numTriplePoints = triplePoints.size();
            results[g] = new double[numTriplePoints][];
            results[g] = triplePoints.toArray(results[g]);
        }
    }

    private double[] calculateAnglesForVertex(final Vertex vertex) {
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

        return thetas;
    }

	/**
	 * Checks if the plugin can process the given image
	 *
	 * @throws NullPointerException
	 *             if image == null
	 * @throws IllegalArgumentException
	 *             if image is not binary
	 */
	private static void checkImage(final ImagePlus image) {
		checkNotNull(image, "Must have an input image");
		checkArgument(ImageCheck.isBinary(image), "Input image must be binary");
	}

	/**
	 * Checks if the the plugin can use the given nthPoint value
	 *
	 * @throws IllegalArgumentException
	 *             if parameter nthPoint < 0 && nthPoint !=
	 *             TriplePointAngles#VERTEX_TO_VERTEX
	 */
	private static void checkNthPoint(final int nthPoint) {
		checkArgument(nthPoint >= 0 || nthPoint == VERTEX_TO_VERTEX, "Invalid nth point value");
	}

	private static boolean isVoxel26Connected(final Point point, final Point voxel) {
		int xDistance = Math.abs(point.x - voxel.x);
		int yDistance = Math.abs(point.y - voxel.y);
		int zDistance = Math.abs(point.z - voxel.z);

		return xDistance <= 1 && yDistance <= 1 && zDistance <= 1;
	}

	private static boolean isTriplePoint(final Vertex vertex) {
		return vertex.getBranches().size() == 3;
	}

	private static double vertexToVertexAngle(final Vertex vertex, final Edge edge0, final Edge edge1) {
		Vertex oppositeVertex0 = edge0.getOppositeVertex(vertex);
		Vertex oppositeVertex1 = edge1.getOppositeVertex(vertex);

		ArrayList<Point> vertexPoints = vertex.getPoints();
		ArrayList<Point> oppositeVertex0Points = oppositeVertex0.getPoints();
		ArrayList<Point> oppositeVertex1Points = oppositeVertex1.getPoints();

		double[] vertexCentroid = Centroid.getCentroidCoordinates(vertexPoints).get();
		double[] oppositeVertex0Centroid = Centroid.getCentroidCoordinates(oppositeVertex0Points).get();
		double[] oppositeVertex1Centroid = Centroid.getCentroidCoordinates(oppositeVertex1Points).get();

		return Vectors.joinedVectorAngle(oppositeVertex0Centroid[0], oppositeVertex0Centroid[1],
				oppositeVertex0Centroid[2], oppositeVertex1Centroid[0], oppositeVertex1Centroid[1],
				oppositeVertex1Centroid[2], vertexCentroid[0], vertexCentroid[1], vertexCentroid[2]);
	}

	private double vertexAngle(final Vertex vertex, final Edge edge0, final Edge edge1) {
		Point p0 = getNthPointOfEdge(vertex, edge0);
		Point p1 = getNthPointOfEdge(vertex, edge1);

		double cv[] = Centroid.getCentroidCoordinates(vertex.getPoints()).get();
		return Vectors.joinedVectorAngle(p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, cv[0], cv[1], cv[2]);
	}

    private Point getNthPointOfEdge(final Vertex vertex, final Edge edge) {
        ArrayList<Point> vertexPoints = vertex.getPoints();
		ArrayList<Point> edgePoints = edge.getSlabs();

        if (edgePoints.isEmpty()) {
            // No slabs, edge has only an end-point and a junction point
            ArrayList<Point> oppositeVertexPoints = edge.getOppositeVertex(vertex).getPoints();
            return Centroid.getCentroidPoint(oppositeVertexPoints).get();
        }

		final Point edgeStart = edgePoints.get(0);
        final boolean startAtZero = vertexPoints.stream().anyMatch(p -> isVoxel26Connected(edgeStart, p));

        int nthEdgePoint = Common.clamp(nthPoint, 0, edgePoints.size() - 1);

		if (startAtZero) {
			// Vertex is the start vertex of the edge so start counting "up"
			return edgePoints.get(nthEdgePoint);
		}

		// Vertex is the end vertex of the edge so start counting "down"
		return edgePoints.get(edgePoints.size() - nthEdgePoint - 1);
	}
	// endregion
}
