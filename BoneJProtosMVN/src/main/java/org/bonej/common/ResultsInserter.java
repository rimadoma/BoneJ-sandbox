package org.bonej.common;

import ij.measure.ResultsTable;

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
        if (resultsTable == null) {
            return;
        }

        this.resultsTable.setNaNEmptyCells(true);
        this.resultsTable = resultsTable;
    }

    public void setImageMeasurementInFirstFreeRow(String imageTitle, String measurementTitle, double measurementValue)
    {
        if (imageTitle == null || imageTitle.isEmpty()) {
            return;
        }

        if (measurementTitle == null || measurementTitle.isEmpty()) {
            return;
        }

        int rowNumber = rowOfImage(imageTitle);
        if (rowNumber < 0) {
            addNewRow(imageTitle, measurementTitle, measurementValue);
        }
    }

    private void addNewRow(String imageTitle, String measurementTitle, double measurementValue)
    {
        resultsTable.incrementCounter();
        resultsTable.addLabel(imageTitle);
        resultsTable.addValue(measurementTitle, measurementTitle);
    }

    private int rowOfImage(String imageTitle)
    {

        final int rows = resultsTable.getCounter();
        for (int row = 0; row < rows; row++) {
            String rowLabel = resultsTable.getLabel(row);
            if (imageTitle.equals(rowLabel)) {
                return row;
            }
        }

        return -1;
    }
}
