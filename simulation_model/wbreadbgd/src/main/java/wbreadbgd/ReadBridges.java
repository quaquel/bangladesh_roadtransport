package wbreadbgd;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.logging.LogFactory;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;

/**
 * Read bridge data on basis of downloaded high-level pages for A, B, C, D category bridges (500/page).
 * <p>
 * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opensource.org/licenses/BSD-3-Clause">BSD 3-Clause License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Jan 29, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class ReadBridges
{
    /**
     * Read the data from bridge files.
     * @param args arguments
     * @throws IOException on i/o error
     * @throws MalformedURLException on URL error
     * @throws FailingHttpStatusCodeException on status code error from website
     */
    public static void main(final String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException
    {
        readData();
    }

    /**
     * Read the data from the bridge pages.
     * @throws IOException on i/o error
     * @throws MalformedURLException on URL error
     * @throws FailingHttpStatusCodeException on status code error from website
     */
    private static void readData() throws FailingHttpStatusCodeException, MalformedURLException, IOException
    {
        // loop through the data files in resources
        File[] files = new File("E:/java/opentrafficsim/workspace/wbreadbgd/src/main/resources/bridges").listFiles();
        for (File file : files)
        {
            if (!file.isDirectory() && file.toString().endsWith(".htm"))
            {
                process(file);
            }
        }
    }

    /**
     * process a htm-file with up to 500 records of bridges.
     * @param file the file to process
     * @throws IOException on i/o error
     * @throws MalformedURLException on URL error
     * @throws FailingHttpStatusCodeException on status code error from website
     */
    private static void process(final File file) throws FailingHttpStatusCodeException, MalformedURLException, IOException
    {
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);

        WebClient webClient = new WebClient(BrowserVersion.FIREFOX_45);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.getOptions().setTimeout(300000); // 5 minutes
        HtmlPage htmPage = webClient.getPage(file.toURI().toURL());

        new File("E:/BMMS").mkdirs();
        if (!new File("E:/BMMS/overview.tcv").exists())
        {
            try (PrintWriter overview = new PrintWriter(new OutputStreamWriter(
                    new BufferedOutputStream(new FileOutputStream(new File("E:/BMMS/overview.tcv"), true)), "UTF-8")))
            {
                overview.println("road\tkm\ttype\tLRPName\tname\tlength\tcondition\tstructureNr\troadName\t"
                        + "chainage\twidth\tconstructionYear\tspans\tzone\tcircle\tdivision\tsub-division\tlat\tlon\tEstimatedLoc");
            }
        }

        try (PrintWriter overview = new PrintWriter(new OutputStreamWriter(
                new BufferedOutputStream(new FileOutputStream(new File("E:/BMMS/overview.tcv"), true)), "UTF-8")))
        {
            // get the table with the content
            HtmlTable table = htmPage.getHtmlElementById("detailsTable");
            int rowNr = 0;
            for (final HtmlTableRow row : table.getRows())
            {
                if (rowNr++ > 0 && rowNr < table.getRows().size() - 1)
                {
                    List<HtmlTableCell> cells = row.getCells();
                    String road = cells.get(0).asText().trim();
                    String km = cells.get(1).asText().trim();
                    String type = cells.get(2).asText().trim();
                    String lrpName = cells.get(3).asText().trim();
                    String name = cells.get(4).asText().trim();
                    String length = cells.get(5).asText().trim();
                    String condition = cells.get(6).asText().trim();
                    String bcs2 = cells.get(7).asText().trim();
                    HtmlTableCell structureCell = cells.get(6);
                    String sts = structureCell.asXml().replaceAll("\"", "").replaceAll("&amp;", "&").trim();
                    int sts1 = sts.indexOf("ID=");
                    int sts2 = sts.indexOf("&vRoadID=", sts1);
                    String structureNr = sts.substring(sts1 + 3, sts2).trim();
                    String filename = "E:/BMMS/" + road + "/" + road + "." + lrpName + "." + structureNr;

                    System.out.println(condition + "  " + lrpName + ":  " + road + "-" + km + "  " + structureNr + "  " + name
                            + " (" + type + ")");

                    new File("E:/BMMS/" + road).mkdirs();

                    if (new File(filename + ".txt").exists())
                    {
                        // skip files we have already processed
                        continue;
                    }

                    // get the detailed BCS1 file
                    HtmlTableCell qualityCell = cells.get(6);
                    String qs = qualityCell.asXml();
                    int qs1 = qs.indexOf("<a href=");
                    int qs2 = qs.indexOf(" class=", qs1);
                    qs = qs.substring(qs1 + 8, qs2).replaceAll("\"", "").replaceAll("&amp;", "&").trim();
                    if (!qs.startsWith("http"))
                    {
                        qs = "http://www.rhd.gov.bd/BridgeDatabase/" + qs;
                    }
                    System.out.println(qs);
                    HtmlPage bcs1Page = webClient.getPage(qs);
                    if (bcs1Page != null)
                    {
                        File bcs1file = new File(filename + ".bcs1.htm");
                        try (PrintWriter bcs1data = new PrintWriter(new OutputStreamWriter(
                                new BufferedOutputStream(new FileOutputStream(bcs1file, false)), "UTF-8")))
                        {
                            bcs1data.print(clean(bcs1Page.asXml()));
                        }
                    }

                    String zone = "";
                    String circle = "";
                    String division = "";
                    String subDivision = "";
                    String roadName = "";
                    String lat = "";
                    String lon = "";
                    String constructionYear = "";
                    String spans = "";
                    String width = "";
                    String chainage = "";
                    String estimatedLoc = "bcs1";

                    if (!bcs1Page.asText().toLowerCase().contains("no record found"))
                    {
                        DomNodeList<HtmlElement> tables = bcs1Page.getBody().getElementsByTagName("table");

                        HtmlTable locationTable = (HtmlTable) tables.get(2);
                        zone = locationTable.getCellAt(0, 1).asText();
                        circle = locationTable.getCellAt(0, 3).asText();
                        division = locationTable.getCellAt(0, 5).asText();
                        subDivision = locationTable.getCellAt(0, 7).asText();
                        roadName = locationTable.getCellAt(1, 3).asText();
                        chainage = locationTable.getCellAt(2, 7).asText();
                        HtmlTable latLonTable = (HtmlTable) locationTable.getCellAt(1, 7).getElementsByTagName("table").get(0);
                        Double latDeg = parseDouble(latLonTable.getCellAt(1, 1).asText());
                        Double latMin = parseDouble(latLonTable.getCellAt(1, 2).asText());
                        Double latSec = parseDouble(latLonTable.getCellAt(1, 3).asText());
                        Double lonDeg = parseDouble(latLonTable.getCellAt(2, 1).asText());
                        Double lonMin = parseDouble(latLonTable.getCellAt(2, 2).asText());
                        Double lonSec = parseDouble(latLonTable.getCellAt(2, 3).asText());
                        if (!latDeg.isNaN() && !latMin.isNaN() && latSec.isNaN())
                        {
                            latSec = 0.0;
                            estimatedLoc = "bcs1_zerosec";
                        }
                        if (!lonDeg.isNaN() && !lonMin.isNaN() && lonSec.isNaN())
                        {
                            lonSec = 0.0;
                            estimatedLoc = "bcs1_zerosec";
                        }
                        if (!latDeg.isNaN() && !latMin.isNaN() && !latSec.isNaN() && !lonDeg.isNaN() && !lonMin.isNaN()
                                && !lonSec.isNaN())
                        {
                            lat = "" + (latDeg + latMin / 60.0 + latSec / 3600.0);
                            lon = "" + (lonDeg + lonMin / 60.0 + lonSec / 3600.0);
                        }

                        HtmlTable supStructTable = (HtmlTable) tables.get(7);
                        constructionYear = supStructTable.getCellAt(0, 3).asText();
                        spans = supStructTable.getCellAt(1, 1).asText();
                        width = supStructTable.getCellAt(2, 1).asText();
                    }

                    // get the detailed BCS2 file
                    HtmlTableCell bcs2Cell = cells.get(7);
                    String bcs2s = bcs2Cell.asXml();
                    int bcs2s1 = bcs2s.indexOf("<a href=");
                    int bcs2s2 = bcs2s.indexOf(" class=", bcs2s1);
                    bcs2s = bcs2s.substring(bcs2s1 + 8, bcs2s2).replaceAll("\"", "").replaceAll("&amp;", "&").trim();
                    if (!bcs2s.startsWith("http"))
                    {
                        bcs2s = "http://www.rhd.gov.bd/BridgeDatabase/" + bcs2s;
                    }
                    System.out.println(bcs2s);
                    HtmlPage bcs2Page = webClient.getPage(bcs2s);
                    if (bcs2Page != null)
                    {
                        File bcs2file = new File(filename + ".bcs2.htm");
                        try (PrintWriter bcs2data = new PrintWriter(new OutputStreamWriter(
                                new BufferedOutputStream(new FileOutputStream(bcs2file, false)), "UTF-8")))
                        {
                            bcs2data.print(clean(bcs2Page.asXml()));
                        }
                    }

                    // get the detailed BCS3 file
                    HtmlTableCell bcs3Cell = cells.get(8);
                    String bcs3s = bcs3Cell.asXml();
                    int bcs3s1 = bcs3s.indexOf("<a href=");
                    int bcs3s2 = bcs3s.indexOf(" class=", bcs3s1);
                    bcs3s = bcs3s.substring(bcs3s1 + 8, bcs3s2).replaceAll("\"", "").replaceAll("&amp;", "&").trim();
                    if (!bcs3s.startsWith("http"))
                    {
                        bcs3s = "http://www.rhd.gov.bd/BridgeDatabase/" + bcs3s;
                    }
                    System.out.println(bcs3s);
                    HtmlPage bcs3Page = webClient.getPage(bcs3s);
                    if (bcs3Page != null)
                    {
                        File bcs3file = new File(filename + ".bcs3.htm");
                        try (PrintWriter bcs3data = new PrintWriter(new OutputStreamWriter(
                                new BufferedOutputStream(new FileOutputStream(bcs3file, false)), "UTF-8")))
                        {
                            bcs3data.print(clean(bcs3Page.asXml()));
                        }
                    }

                    if (!bcs3Page.asText().toLowerCase().contains("no record found"))
                    {
                        DomNodeList<HtmlElement> tables = bcs3Page.getBody().getElementsByTagName("table");

                        HtmlTable locationTable = (HtmlTable) tables.get(1);
                        zone = locationTable.getCellAt(1, 1).asText();
                        circle = locationTable.getCellAt(1, 3).asText();
                        division = locationTable.getCellAt(1, 5).asText();
                        subDivision = locationTable.getCellAt(1, 7).asText();
                        roadName = locationTable.getCellAt(2, 3).asText(); // more detailed name than in BCS1
                        chainage = locationTable.getCellAt(2, 7).asText();
                    }

                    if (chainage.length() == 0)
                    {
                        chainage = km;
                    }

                    if (lat.length() == 0 || lon.length() == 0)
                    {
                        // look up approximate location in Road database
                        File roadfile = new File("E:/RMMS/" + road + ".lrps.htm");
                        if (roadfile.exists())
                        {
                            HtmlPage roadPage = webClient.getPage(roadfile.toURI().toURL());
                            DomNodeList<HtmlElement> lrpTables = roadPage.getBody().getElementsByTagName("table");
                            HtmlTable lrpTable = (HtmlTable) lrpTables.get(4);
                            String[] latlon = getLatLon(lrpName, lrpTable);
                            if (latlon != null)
                            {
                                lat = latlon[0];
                                lon = latlon[1];
                                estimatedLoc = "road_precise";
                            }
                            else
                            {
                                latlon = getLatLon(lrpTable, chainage);
                                if (latlon != null)
                                {
                                    lat = latlon[0];
                                    lon = latlon[1];
                                    estimatedLoc = latlon[2];
                                }
                                else
                                {
                                    estimatedLoc = "error";
                                }
                            }
                        }
                    }

                    overview.println(road + "\t" + km + "\t" + type + "\t" + lrpName + "\t" + name + "\t" + length + "\t"
                            + condition + "\t" + structureNr + "\t" + roadName + "\t" + chainage + "\t" + width + "\t"
                            + constructionYear + "\t" + spans + "\t" + zone + "\t" + circle + "\t" + division + "\t"
                            + subDivision + "\t" + lat + "\t" + lon + "\t" + estimatedLoc);
                    overview.flush();

                    File datafile = new File(filename + ".txt");
                    try (PrintWriter data = new PrintWriter(
                            new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(datafile, false)), "UTF-8")))
                    {
                        data.println("road" + "\t" + road);
                        data.println("km" + "\t" + km);
                        data.println("type" + "\t" + type);
                        data.println("lrpName" + "\t" + lrpName);
                        data.println("name" + "\t" + name);
                        data.println("length" + "\t" + length);
                        data.println("condition" + "\t" + condition);
                        data.println("structureNr" + "\t" + structureNr);
                        data.println("est cost" + "\t" + bcs2);

                        data.println("roadName" + "\t" + roadName);
                        data.println("chainage" + "\t" + chainage);
                        data.println("width" + "\t" + width);
                        data.println("constructionYear" + "\t" + constructionYear);
                        data.println("spans" + "\t" + spans);
                        data.println("zone" + "\t" + zone);
                        data.println("circle" + "\t" + circle);
                        data.println("division" + "\t" + division);
                        data.println("sub-division" + "\t" + subDivision);
                        data.println("lat" + "\t" + lat);
                        data.println("lon" + "\t" + lon);
                        data.println("estimated Location" + "\t" + estimatedLoc);

                        data.println("bcs1-file" + "\t" + qs);
                        data.println("bcs2-file" + "\t" + bcs2s);
                        data.println("bcs3-file" + "\t" + bcs3s);
                    }

                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        webClient.close();
    }

    /**
     * Clean the HTML string for non-printing characters above 127.
     * @param s input
     * @return cleaned string
     */
    private static String clean(final String s)
    {
        StringBuffer t = new StringBuffer();
        for (int b : s.getBytes())
        {
            if (b > 0 && b <= 127)
            {
                t.append((char) b);
            }
            else
            {
                t.append("&nbsp;");
            }
        }
        return t.toString();
    }

    /**
     * Find approximate lat/lon in Road table
     * @param lrpName search key
     * @param lrpTable html table
     * @return [lat, lon]
     */
    private static String[] getLatLon(final String lrpName, final HtmlTable lrpTable)
    {
        String[] latlon = null;
        for (final HtmlTableRow row : lrpTable.getRows())
        {
            List<HtmlTableCell> cells = row.getCells();
            if (cells.size() > 7)
            {
                String l = cells.get(1).asText();
                if (l.equalsIgnoreCase(lrpName))
                {
                    return new String[] { cells.get(5).asText(), cells.get(6).asText() };
                }
            }
        }
        return latlon;
    }

    /**
     * Find approximate lat/lon in Road table
     * @param lrpTable html table
     * @param chainage km-point to get closest to
     * @return [lat, lon]
     */
    private static String[] getLatLon(final HtmlTable lrpTable, final String chainage)
    {
        Double latp = 0.0;
        Double latn = 0.0;
        Double lonp = 0.0;
        Double lonn = 0.0;
        double searchkm = Double.parseDouble(chainage);
        double prevkm = 0.0;
        for (final HtmlTableRow row : lrpTable.getRows())
        {
            List<HtmlTableCell> cells = row.getCells();
            if (cells.size() > 7)
            {
                String skm = cells.get(2).asText().trim();
                if (skm.matches("[0-9]+[\\.]{0,1}[0-9]*"))
                {
                    Double km = parseDouble(skm);
                    latn = parseDouble(cells.get(5).asText());
                    lonn = parseDouble(cells.get(6).asText());
                    if (!latn.isNaN() && !lonn.isNaN() && !km.isNaN()) // skip empty rows
                    {
                        if (Math.abs(km - searchkm) < 0.01) // 10 meter precise
                        {
                            return new String[] { cells.get(5).asText(), cells.get(6).asText(), "road_chainage" };
                        }
                        if (km > searchkm && prevkm < searchkm)
                        {
                            double ratio = (km - searchkm) / (km - prevkm); // down from km
                            double lat = latn - ratio * (latn - latp);
                            double lon = lonn - ratio * (lonn - lonp);
                            return new String[] { "" + lat, "" + lon, "road_interpolate" };
                        }
                        prevkm = km;
                        latp = latn;
                        lonp = lonn;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Parse a number string ans return the double or NaN when the string could not be parsed.
     * @param s the string to parse
     * @return the double or NaN
     */
    private static Double parseDouble(final String s)
    {
        try
        {
            return Double.parseDouble(s);
        }
        catch (NumberFormatException e)
        {
            return Double.NaN;
        }
    }
}
