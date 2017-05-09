package wbprocessbgd;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVReader;

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
public class MakeBridgesFile3
{
    /** road info. */
    private static Map<String, List<RoadRecord>> roadMap = new HashMap<>();

    /**
     * Read the data from bridge files.
     * @param args arguments
     * @throws IOException on i/o error
     */
    public static void main(final String[] args) throws IOException
    {
        readRoadsCsv3("E:/RMMS/_roads3.csv");
        process("E:/BMMS/overview.tsv");
    }

    /**
     * Read the data from the bridge pages.
     * @param bmmsFile the tab-separated file
     * @throws IOException on i/o error
     */
    private static void process(final String bmmsFile) throws IOException
    {
        // loop through the data
        FileInputStream fis;
        if (new File(bmmsFile).canRead())
            fis = new FileInputStream(bmmsFile);
        else
            fis = new FileInputStream(MakeBridgesFile3.class.getResource(bmmsFile).getFile());
        try (PrintWriter overview = new PrintWriter(new OutputStreamWriter(
                new BufferedOutputStream(new FileOutputStream(new File("E:/BMMS/overview3.tsv"), false)), "UTF-8")))
        {
            overview.println("road\tkm\ttype\tLRPName\tname\tlength\tcondition\tstructureNr\troadName\t"
                    + "chainage\twidth\tconstructionYear\tspans\tzone\tcircle\tdivision\tsub-division\tlat\tlon\tEstimatedLoc");
            try (CSVReader reader = new CSVReader(new InputStreamReader(fis), '\t'))
            {
                String[] parts;
                parts = reader.readNext(); // skip first line
                while ((parts = reader.readNext()) != null)
                {
                    // road + "\t" + km + "\t" + type + "\t" + lrpName + "\t" + name + "\t" + length + "\t"
                    // + condition + "\t" + structureNr + "\t" + roadName + "\t" + chainage + "\t" + width + "\t"
                    // + constructionYear + "\t" + spans + "\t" + zone + "\t" + circle + "\t" + division + "\t"
                    // + subDivision + "\t" + lat + "\t" + lon + "\t" + estimatedLoc);

                    String road = parts[0];
                    String km = parts[1];
                    String type = parts[2];
                    String lrpName = parts[3];
                    String name = parts[4];
                    String length = parts[5];
                    String condition = parts[6];
                    String structureNr = parts[7];
                    String roadName = parts[8];
                    String chas = parts[9];
                    String width = parts[10];
                    String constructionYear = parts[11];
                    String spans = parts[12];
                    String zone = parts[13];
                    String circle = parts[14];
                    String division = parts[15];
                    String subDivision = parts[16];
                    String lats = parts[17];
                    String lons = parts[18];
                    String estimatedLoc = parts[19];

                    double lat = parseDouble(lats);
                    double lon = parseDouble(lons);
                    double chainage = parseDouble(chas);

                    if (roadName.length() > 0 && !Double.isNaN(chainage) && chainage > 0)
                    {
                        // look up chainage location in cleaned Road set
                        if (!roadMap.containsKey(road))
                        {
                            System.out.println(road + " not found in road dataset for bridge " + road + "." + lrpName + " at "
                                    + chainage + " km");
                        }
                        else
                        {
                            List<RoadRecord> roadList = roadMap.get(road);
                            if (roadList.get(roadList.size() - 1).chainage < chainage)
                            {
                                System.out.println(road + " with bridge " + road + "." + lrpName + " at " + chainage
                                        + " km is beyond the chainage of the road");
                            }
                            else
                            {
                                // interpolate
                                for (int i = 0; i < roadList.size() - 2; i++)
                                {
                                    if (roadList.get(i).chainage == chainage)
                                    {
                                        lat = roadList.get(i).lat;
                                        lon = roadList.get(i).lon;
                                        estimatedLoc = "exact";
                                        System.out.println(
                                                road + " with bridge " + road + "." + lrpName + " at " + chainage + " EXACT");
                                        break;
                                    }
                                    if (roadList.get(i).chainage < chainage && roadList.get(i + 1).chainage >= chainage)
                                    {
                                        double searchkm = chainage;
                                        double nextkm = roadList.get(i + 1).chainage;
                                        double prevkm = roadList.get(i).chainage;
                                        double ratio = (nextkm - searchkm) / (nextkm - prevkm);
                                        double latp = roadList.get(i).lat;
                                        double lonp = roadList.get(i).lon;
                                        double latn = roadList.get(i + 1).lat;
                                        double lonn = roadList.get(i + 1).lon;
                                        lat = latn - ratio * (latn - latp);
                                        lon = lonn - ratio * (lonn - lonp);
                                        estimatedLoc = "interpolate";
                                        System.out.println(road + " with bridge " + road + "." + lrpName + " at " + chainage
                                                + " INTERPOLATE");
                                        break;
                                    }
                                }

                                if (Double.isNaN(lat) || Double.isNaN(lon) || lat == 0.0 || lon == 0.0)
                                {
                                    System.out.println(road + " with bridge " + road + "." + lrpName + " at " + chainage
                                            + " COULD NOT ESTABLISH LATITUDE AND/OR LONGITUDE");
                                }
                                else
                                {
                                    overview.println(road + "\t" + km + "\t" + type + "\t" + lrpName + "\t" + name + "\t"
                                            + length + "\t" + condition + "\t" + structureNr + "\t" + roadName + "\t" + chainage
                                            + "\t" + width + "\t" + constructionYear + "\t" + spans + "\t" + zone + "\t"
                                            + circle + "\t" + division + "\t" + subDivision + "\t" + lat + "\t" + lon + "\t"
                                            + estimatedLoc);
                                    overview.flush();
                                }
                            }
                        }
                    }
                }
            }
        }
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

    /**
     * Read the roads with LRP coordinates
     * @param filename filename
     * @throws IOException on I/O error
     */
    private static void readRoadsCsv3(final String filename) throws IOException
    {
        FileInputStream fis;
        if (new File(filename).canRead())
            fis = new FileInputStream(filename);
        else
            fis = new FileInputStream(MakeBridgesFile3.class.getResource(filename).getFile());
        try (CSVReader reader = new CSVReader(new InputStreamReader(fis), ',', '"', 1))
        {
            String[] parts;
            String lastRoad = "";
            List<RoadRecord> roadList = new ArrayList<>();
            while ((parts = reader.readNext()) != null)
            {
                String roadName = parts[0];
                boolean newRoad = !roadName.equals(lastRoad);
                if (newRoad)
                {
                    roadList = new ArrayList<>();
                    roadMap.put(roadName, roadList);
                }
                String lrps = parts[2];
                String chas = parts[1];
                String lats = parts[3];
                String lons = parts[4];
                double lat = Double.parseDouble(lats);
                double lon = Double.parseDouble(lons);
                double chainage = Double.parseDouble(chas);
                String bf = parts[5];
                String type = parts[6];
                String name = parts[7];
                RoadRecord rr = new RoadRecord(lat, lon, lrps, chainage, type, name);
                rr.bf = bf;
                roadList.add(rr);
                lastRoad = roadName;
            }
        }
    }

}
