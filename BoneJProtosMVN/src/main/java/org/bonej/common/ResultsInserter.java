package org.bonej.common;

import ij.measure.ResultsTable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A wrapper class for ResultsTable used to insert measurements according to the following policy:
 * 1)
 * 2)
 * 3)
 * 4)
 *
 * By default the class uses the instance returned by ResultsTable.getResultsTable()
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 * @author Michael Doube
 */
public class ResultsInserter
{
    private static final String DEFAULT_RESULTS_TABLE_TITLE = "Results";
    private ResultsTable resultsTable;

    public ResultsInserter()
    {
        setResultsTable(ResultsTable.getResultsTable());
    }

    public ResultsTable getResultsTable()
    {
        return resultsTable;
    }

    /**
     * Sets the ResultsTable the ResultsInserter uses
     *
     * @param   resultsTable    The table where the values are inserted
     * @post    this.resultsTable != null
     */
    public void setResultsTable(ResultsTable resultsTable)
    {
        checkNotNull(resultsTable, "The ResultsTable in ResultsInserter must not be set null");

        this.resultsTable = resultsTable;
        this.resultsTable.setNaNEmptyCells(true);
        this.resultsTable = resultsTable;
    }

    public void setImageMeasurementInFirstFreeRow(String imageTitle, String measurementHeading, double measurementValue)
    {
        if (imageTitle == null || imageTitle.isEmpty()) {
            return;
        }

        if (measurementHeading == null || measurementHeading.isEmpty()) {
            return;
        }

        int rowNumber = rowOfLabel(imageTitle);
        if (rowNumber < 0) {
            addNewRow(imageTitle, measurementHeading, measurementValue);
            return;
        }

        int columnNumber = columnOfHeading(measurementHeading);
        if (columnNumber < 0) {
            //add measurement to row #rowNumber
        } else {
            addNewRow(imageTitle, measurementHeading, measurementValue);
        }
    }

    private int columnOfHeading(String heading)
    {
        return -1;
    }

    private void addNewRow(String imageTitle, String measurementTitle, double measurementValue)
    {
        resultsTable.incrementCounter();
        resultsTable.addLabel(imageTitle);
        resultsTable.addValue(measurementTitle, measurementValue);
    }

    private int rowOfLabel(String label)
    {

        final int rows = resultsTable.getCounter();
        for (int row = 0; row < rows; row++) {
            String rowLabel = resultsTable.getLabel(row);
            if (label.equals(rowLabel)) {
                return row;
            }
        }

        return -1;
    }
}
