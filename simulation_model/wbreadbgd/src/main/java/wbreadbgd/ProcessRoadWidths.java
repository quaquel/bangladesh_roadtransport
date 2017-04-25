package wbreadbgd;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;

import org.apache.xerces.stax.events.StartDocumentImpl;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;

/**
 * Process downloaded road width data (Flash file input) into a more readable format.
 * <p>
 * Copyright (c) 2013-2016 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="http://opentrafficsim.org/docs/current/license.html">OpenTrafficSim License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Jan 29, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class ProcessRoadWidths
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
        File[] files = new File("E:/RMMS").listFiles();
        for (File file : files)
        {
            if (!file.isDirectory() && file.toString().endsWith(".widths.txt"))
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
        // get the file with the content
        String outputfilename = file.getAbsolutePath().replace("widths", "widths.processed");
        String line = "";
        try (BufferedReader buf = new BufferedReader(new FileReader(file)))
        {
            line = buf.readLine();
            if (line == null)
            {
                System.err.println("Could not find file " + file.getAbsolutePath());
                return;
            }
        }

        System.out.println("Writing file " + file.getAbsolutePath());

        // [0] TotalRecord=3&
        // [1] RoadID=1311&
        // [2] RoadNo=Z3009&
        // [3] StartChainage=0:0.9:3.28&
        // [4] EndChainage=0.9:3.28:5.376&
        // [5] CWayWidth=5.45:5.46:3.82&
        // [6] NoOfLanes=1.5:1.5:1&
        // [7] RoadName=Sripur%2DBairagirchala+Road&
        // [8] RoadClass=Zilla+Road&
        // [9] RoadLength=5.376&
        // [10] StartLocation=Sripur&
        // [11] EndLocation=Bairagirchala&
        // [12] ROADW=1:2:2

        String road = file.getName().substring(0, file.getName().indexOf('.'));
        String[] parts = line.split("&");

        int totalRecords = 0;
        String roadId = "";
        String roadNo = "";
        String[] startChainages = new String[0];
        String[] endChainages = new String[0];
        String[] widths = new String[0];
        String[] nrOfLanes = new String[0];

        // if # of records empty, assume one lane over entire length
        if (parts[0].split("=").length == 1)
        {
            totalRecords = 1;
            roadId = parts[1].split("=")[1];
            roadNo = parts[2].split("=")[1];
            startChainages = new String[] { "0.0" };
            endChainages = new String[] { parts[9].split("=")[1] }; // take road length
            widths = new String[] { "3.5" }; // assumption
            nrOfLanes = new String[] { "1" };
        }
        else
        {
            totalRecords = Integer.parseInt(parts[0].split("=")[1]);
            roadId = parts[1].split("=")[1];
            roadNo = parts[2].split("=")[1];
            startChainages = parts[3].split("=")[1].split(":");
            endChainages = parts[4].split("=")[1].split(":");
            widths = parts[5].split("=")[1].split(":");
            nrOfLanes = parts[6].split("=")[1].split(":");
        }
        File datafile = new File(outputfilename);
        try (PrintWriter data = new PrintWriter(
                new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(datafile, false)), "UTF-8")))
        {
            data.println("roadNo\troadId\tstartChainage\tendChainage\twidth\tnrLanes");
            for (int i = 0; i < startChainages.length; i++)
            {
                data.println(roadNo + "\t" + roadId + "\t" + startChainages[i] + "\t" + endChainages[i] + "\t" + widths[i]
                        + "\t" + nrOfLanes[i]);
            }
        }
    }

}
