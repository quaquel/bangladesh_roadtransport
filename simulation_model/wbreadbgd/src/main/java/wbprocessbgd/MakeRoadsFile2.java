package wbprocessbgd;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLDecoder;

/**
 * Read road data from downloaded road files.
 * <p>
 * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opensource.org/licenses/BSD-3-Clause">BSD 3-Clause License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Jan 29, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class MakeRoadsFile2
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
        if (!new File("E:/RMMS/_roads2.csv").exists())
        {
            try (PrintWriter overview = new PrintWriter(new OutputStreamWriter(
                    new BufferedOutputStream(new FileOutputStream(new File("E:/RMMS/_roads2.csv"), false)), "UTF-8")))
            {
                overview.println("\"road\",\"chainage\",\"lrp\",\"lat\",\"lon\",\"type\",\"name\"");
            }
        }

        try (PrintWriter overview = new PrintWriter(new OutputStreamWriter(
                new BufferedOutputStream(new FileOutputStream(new File("E:/RMMS/_roads2.csv"), true)), "UTF-8")))
        {
            String roadName = file.getName().split("\\.")[0];
            System.out.println(roadName);
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
                            String chas = parts[6].split("=")[1].replaceAll("\\%2E", ".");
                            String type = parts[3].split("=")[1].replaceAll("\\%2C", ",").replaceAll("\\+", "");
                            String name = parts[4].contains("=") && parts[4].split("=").length > 1
                                    ? URLDecoder.decode(parts[4].split("=")[1].replaceAll("\\+", " "), "UTF-8") : ".";
                            overview.println("\"" + roadName + "\",\"" + chas + "\",\"" + lrps + "\",\"" + lats + "\",\"" + lons
                                    + "\",\"" + type + "\",\"" + name + "\"");
                        }
                    }
                    line = buf.readLine();
                }
            }
        }
    }

}
