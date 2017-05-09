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
 * Read road data on basis of downloaded high-level pages for N, R, and Z category roads (25/page).
 * <p>
 * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opensource.org/licenses/BSD-3-Clause">BSD 3-Clause License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Jan 29, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class ReadRoadWidths
{
    /**
     * Read the data from road files.
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
     * Read the data from the downloaded road files.
     * @throws IOException on i/o error
     * @throws MalformedURLException on URL error
     * @throws FailingHttpStatusCodeException on status code error from website
     */
    private static void readData() throws FailingHttpStatusCodeException, MalformedURLException, IOException
    {
        // loop through the data files in resources
        File[] files = new File("E:/java/opentrafficsim/workspace/wbreadbgd/src/main/resources/roads").listFiles();
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

        new File("E:/RMMS").mkdirs();
        // get the table with the content
        DomNodeList<HtmlElement> htmTables = htmPage.getBody().getElementsByTagName("table");
        HtmlTable table = (HtmlTable) htmTables.get(4);
        int rowNr = 0;
        for (final HtmlTableRow row : table.getRows())
        {
            if (rowNr++ > 1)
            {
                List<HtmlTableCell> cells = row.getCells();
                String road = cells.get(0).asText().trim();
                String name = cells.get(1).asText().trim();
                String length = cells.get(2).asText().trim();
                String from = cells.get(3).asText().trim();
                String to = cells.get(4).asText().trim();

                System.out.println(road + "  " + name + "  " + length + " km.  " + from + " - " + to);

                // get the road width file
                HtmlTableCell roadCell = cells.get(0);
                String rs = roadCell.asXml();
                int qs1 = rs.indexOf("<a href=");
                int qs2 = rs.indexOf(">", qs1);
                rs = rs.substring(qs1 + 8, qs2).replaceAll("\"", "").replaceAll("&amp;", "&").trim();
                if (!rs.startsWith("http"))
                {
                    rs = "http://www.rhd.gov.bd/RoadDatabase/" + rs;
                }
                rs = rs.replace("roaddetail", "loadData");
                System.out.println(rs);
                HtmlPage roadPage = webClient.getPage(rs);
                if (roadPage != null)
                {
                    File roadfile = new File("E:/RMMS/" + road + ".widths.txt");
                    try (PrintWriter roaddata = new PrintWriter(
                            new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(roadfile, false)), "UTF-8")))
                    {
                        roaddata.print(roadPage.asText());
                    }
                }
            }
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

}
