package org.bonej.common;

import ij.measure.ResultsTable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:rdomander@rvc.ac.uk">Richard Domander</a>
 * @date 17/11/15
 */
public class ResultsInserterTest
{
    private static ResultsInserter resultsInserter;
    private static final String emptyString = "";

    @BeforeClass
    public static void oneTimeSetup()
    {
        resultsInserter = new ResultsInserter();
    }

    @Test (expected = NullPointerException.class)
    public void testSetResultsTableThrowsNullPointerExceptionWhenResultsTableIsNull() throws Exception
    {
        resultsInserter.setResultsTable(null);
    }

    @Test
    public void testSetImageMeasurementInFirstFreeRowDoesNothingIfImageTitleIsNullOrEmpty() throws Exception
    {
        ResultsTable resultsTable = resultsInserter.getResultsTable();
        int beforeCount;
        int afterCount;

        // null title
        beforeCount = resultsTable.getCounter();
        resultsInserter.setImageMeasurementInFirstFreeRow(null, "measurementTitle", 1.0);
        afterCount = resultsTable.getCounter();
        assertEquals("ResultInserter must not add a row if imageTitle is null", beforeCount, afterCount);

        // empty title
        resultsTable = resultsInserter.getResultsTable();
        beforeCount = resultsTable.getCounter();
        resultsInserter.setImageMeasurementInFirstFreeRow(emptyString, "measurementTitle", 1.0);
        afterCount = resultsTable.getCounter();
        assertEquals("ResultInserter must not add a row if imageTitle is empty", beforeCount, afterCount);
    }

    @Test
    public void testSetImageMeasurementInFirstFreeRowDoesNothingIfMeasurementTitleIsNullOrEmpty() throws Exception
    {
        ResultsTable resultsTable = resultsInserter.getResultsTable();
        int beforeCount;
        int afterCount;

        // null title
        beforeCount = resultsTable.getCounter();
        resultsInserter.setImageMeasurementInFirstFreeRow("ImageTitle", null, 1.0);
        afterCount = resultsTable.getCounter();
        assertEquals("ResultInserter must not add a row if measurementTitle is null", beforeCount, afterCount);

        // empty title
        resultsTable = resultsInserter.getResultsTable();
        beforeCount = resultsTable.getCounter();
        resultsInserter.setImageMeasurementInFirstFreeRow("ImageTitle", emptyString, 1.0);
        afterCount = resultsTable.getCounter();
        assertEquals("ResultInserter must not add a row if measurementTitle is empty", beforeCount, afterCount);
    }
}