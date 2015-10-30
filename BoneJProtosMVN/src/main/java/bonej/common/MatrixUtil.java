package bonej.common;

import Jama.Matrix;

/**
 * Created by Richard Domander on 26/10/15.
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
