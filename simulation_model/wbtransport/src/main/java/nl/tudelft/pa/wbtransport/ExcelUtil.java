/**
 * 
 */
package nl.tudelft.pa.wbtransport;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

/**
 * Copyright (c) 2011-2013 TU Delft, Faculty of TBM, Systems and Simulation <br>
 * This software is licensed without restrictions to Nederlandse Organisatie voor Toegepast Natuurwetenschappelijk
 * Onderzoek TNO (TNO), Erasmus University Rotterdam, Delft University of Technology, Panteia B.V., Stichting Projecten
 * Binnenvaart, Ab Ovo Nederland B.V., Modality Software Solutions B.V., and Rijkswaterstaat - Dienst Water, Verkeer en
 * Leefomgeving, including the right to sub-license sources and derived products to third parties. <br>
 * 
 * @version Nov 30, 2012 <br>
 * @author <a href="http://tudelft.nl/averbraeck">Alexander Verbraeck </a>
 */
public class ExcelUtil
{
    /**
     * Value from Excel cell
     * 
     * @param row as HSSF row
     * @param column as String (A, B, .., AA, AB, ..)
     * @return string value
     */
    public static String cellValue(final Row row, final String column)
    {
        int colnr = column.charAt(column.length() - 1) - 65;
        if (column.length() > 1)
            colnr += 26 * (column.charAt(column.length() - 2) - 64);
        return ExcelUtil.cellValue(row, colnr);
    }

    /**
     * Value from Excel cell
     * 
     * @param row as HSSF row
     * @param colnr column
     * @return string value
     */
    public static String cellValue(final Row row, final int colnr)
    {
        if (row == null)
            return "";
        Cell cell = row.getCell(colnr);
        if (cell == null)
            return "";
        switch (cell.getCellType())
        {
        case Cell.CELL_TYPE_STRING:
            return cell.getRichStringCellValue().getString();
        case Cell.CELL_TYPE_NUMERIC:
            if (DateUtil.isCellDateFormatted(cell))
            {
                return cell.getDateCellValue().toString();
            } else
            {
                return "" + cell.getNumericCellValue();
            }
        case Cell.CELL_TYPE_BOOLEAN:
            return cell.getBooleanCellValue() ? "1" : "0";
        case Cell.CELL_TYPE_FORMULA:
            switch (cell.getCachedFormulaResultType())
            {
            case Cell.CELL_TYPE_STRING:
                return cell.getRichStringCellValue().getString();
            case Cell.CELL_TYPE_NUMERIC:
                if (DateUtil.isCellDateFormatted(cell))
                {
                    return cell.getDateCellValue().toString();
                } else
                {
                    return "" + cell.getNumericCellValue();
                }
            case Cell.CELL_TYPE_BOOLEAN:
                return cell.getBooleanCellValue() ? "1" : "0";
            default:
                return "";
            }
        default:
            return "";
        }
    }

    /**
     * Value from Excel cell
     * 
     * @param row as HSSF row
     * @param column as String (A, B, .., AA, AB, ..)
     * @return string value
     * @throws Exception on read error
     */
    public static double cellValueDouble(final Row row, final String column) throws Exception
    {
        String s = ExcelUtil.cellValue(row, column);
        try
        {
            return Double.parseDouble(s);
        } catch (Exception cause)
        {
            throw new Exception("Error parsing Excel double value " + s + " in cell " + column + row.getRowNum(), cause);
        }
    }

    /**
     * Value from Excel cell
     * 
     * @param row as HSSF row
     * @param column as String (A, B, .., AA, AB, ..)
     * @return string value
     */
    public static double cellValueDoubleNull(final Row row, final String column)
    {
        String s = ExcelUtil.cellValue(row, column);
        try
        {
            return Double.parseDouble(s);
        } catch (Exception cause)
        {
            return 0.0d;
        }
    }
    
    /**
     * Value from Excel cell
     * 
     * @param row as HSSF row
     * @param column as String (A, B, .., AA, AB, ..)
     * @return string value
     */
    public static double cellValueDoubleMinOne(final Row row, final String column)
    {
        String s = ExcelUtil.cellValue(row, column);
        try
        {
            return Double.parseDouble(s);
        } catch (Exception cause)
        {
            return -1.0d;
        }
    }

    /**
     * Value from Excel cell
     * 
     * @param row as HSSF row
     * @param colnr column
     * @return string value
     */
    public static double cellValueDoubleNull(final Row row, final int colnr)
    {
        String s = ExcelUtil.cellValue(row, colnr);
        try
        {
            return Double.parseDouble(s);
        } catch (Exception cause)
        {
            return 0.0d;
        }
    }
    
    /**
     * Value from Excel cell
     * 
     * @param row as HSSF row
     * @param colnr column
     * @return string value
     */
    public static double cellValueDoubleMinOne(final Row row, final int colnr)
    {
        String s = ExcelUtil.cellValue(row, colnr);
        try
        {
            return Double.parseDouble(s);
        } catch (Exception cause)
        {
            return -1.0d;
        }
    }

    /**
     * Value from Excel cell
     * 
     * @param row as HSSF row
     * @param column as String (A, B, .., AA, AB, ..)
     * @return string value
     * @throws Exception;
     */
    public static int cellValueInt(final Row row, final String column) throws Exception
    {
        String s = ExcelUtil.cellValue(row, column);
        try
        {
            double d = Double.parseDouble(s);
            return (int) Math.rint(d);
        } catch (Exception cause)
        {
            throw new Exception("Error parsing Excel integer value " + s + " in cell " + column + row.getRowNum(),
                    cause);
        }
    }

    /**
     * Value from Excel cell
     * 
     * @param row as HSSF row
     * @param column as String (A, B, .., AA, AB, ..)
     * @return string value
     */
    public static int cellValueIntNull(final Row row, final String column)
    {
        String s = ExcelUtil.cellValue(row, column);
        try
        {
            double d = Double.parseDouble(s);
            return (int) Math.rint(d);
        } catch (Exception cause)
        {
            return 0;
        }
    }
}
