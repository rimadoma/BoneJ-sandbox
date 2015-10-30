package bonej.common;

import Jama.Matrix;

/**
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 */
public class MatrixUtil {
    /**
     *  Returns the diagonal of the given matrix as a new column vector
     */
    public static Matrix diagonalAsColumn(Matrix m)
    {
        int rows = Math.min(m.getColumnDimension(), m.getRowDimension());
        Matrix column = new Matrix(rows, 1);

        for (int i=0; i<rows; i++) {
            column.set(i, 0, m.get(i, i));
        }

        return column;
    }

}
