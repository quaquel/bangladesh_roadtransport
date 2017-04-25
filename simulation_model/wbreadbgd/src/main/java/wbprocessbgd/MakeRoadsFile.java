package wbprocessbgd;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Read road data from downloaded road files.
 * <p>
 * Copyright (c) 2013-2016 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="http://opentrafficsim.org/docs/current/license.html">OpenTrafficSim License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Jan 29, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class MakeRoadsFile
{
    /**
     * Read the data from road files.
     * @param args arguments
     * @throws IOException on i/o error
     */
    public static void main(final String[] args) throws IOException
    {
        readData();
    }

    /**
     * Read the data from the bridge pages.
     * @throws IOException on i/o error
     */
    private static void readData() throws IOException
    {
        // loop through the data files in resources
        File[] files = new File("E:/RMMS").listFiles();
        for (File file : files)
        {
            if (!file.isDirectory() && file.toString().endsWith(".lrps.htm"))
            {
                process(file);
            }
        }
    }

    /**
     * process a htm-road-file.
     * @param file the file to process
     * @throws IOException on i/o error
     */
    private static void process(final File file) throws IOException
    {
        new File("E:/RMMS").mkdirs();
        if (!new File("E:/RMMS/_roads.tcv").exists())
        {
            try (PrintWriter overview = new PrintWriter(new OutputStreamWriter(
                    new BufferedOutputStream(new FileOutputStream(new File("E:/RMMS/_roads.tcv"), false)), "UTF-8")))
            {
                overview.println("road\tlrp1\tlat1\tlon1\tlrp2\tlat2\tlon2");
            }
        }

        try (PrintWriter overview = new PrintWriter(new OutputStreamWriter(
                new BufferedOutputStream(new FileOutputStream(new File("E:/RMMS/_roads.tcv"), true)), "UTF-8")))
        {
            String roadName = file.getName().split("\\.")[0];
            overview.print(roadName);
            System.out.println(roadName);;
            String line = "";
            try (BufferedReader buf = new BufferedReader(new FileReader(file)))
            {
                line = buf.readLine();
                while (line != null)
                {
                    if (line.contains("<a href=\"lrpmaps.asp?Latitude="))
                    {
                        String[] parts = line.split("\\&amp\\;");
                        if (parts.length > 2 && !line.contains("Latitude=&amp;"))
                        {
                            String lats = parts[0].substring(parts[0].indexOf("Latitude")).split("=")[1];
                            String lons = parts[1].substring(parts[1].indexOf("Longitu")).split("=")[1];
                            String lrps = parts[2].split("=")[1].replaceAll("\\+", "");
                            overview.print("\t" + lrps + "\t" + lats + "\t" + lons);
                        }
                    }
                    line = buf.readLine();
                }
            }
            overview.println();
        }
    }

}
