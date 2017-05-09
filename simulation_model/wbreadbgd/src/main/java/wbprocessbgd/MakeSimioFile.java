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
public class MakeSimioFile
{
    /** road info. */
    private static Map<String, List<RoadRecordB>> roadMap = new HashMap<>();

    /**
     * Read the data from bridge files.
     * @param args arguments
     * @throws IOException on i/o error
     */
    public static void main(final String[] args) throws IOException
    {
        readRoadsCsv3("E:/RMMS/_roads3.csv");
        readBridgesTsv("E:/BMMS/overview3.tsv");
        writeExcel("E:/WBSIM_Lab2b/simio_bgd");
    }

    /**
     * Read the data from the bridge pages.
     * @param bmmsFile the tab-separated file
     * @throws IOException on i/o error
     */
    private static void readBridgesTsv(final String bmmsFile) throws IOException
    {
        // loop through the data
        FileInputStream fis;
        if (new File(bmmsFile).canRead())
            fis = new FileInputStream(bmmsFile);
        else
            fis = new FileInputStream(MakeSimioFile.class.getResource(bmmsFile).getFile());
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
                String lrpName = parts[3];
                String name = parts[4];
                String lens = parts[5];
                String condition = parts[6];
                String chas = parts[9];
                String wids = parts[10];
                String lats = parts[17];
                String lons = parts[18];

                double lat = parseDouble(lats);
                double lon = parseDouble(lons);
                double chainage = parseDouble(chas);
                double width = parseDouble(wids);
                double length = parseDouble(lens);

                BridgeRecord br = new BridgeRecord(road, lrpName, length, condition, chainage, width, lat, lon);

                // look up chainage location in cleaned Road set
                List<RoadRecordB> roadList = roadMap.get(road);
                for (int i = 0; i < roadList.size() - 2; i++)
                {
                    if (roadList.get(i).chainage <= chainage && roadList.get(i + 1).chainage >= chainage)
                    {
                        RoadRecordB rr = new RoadRecordB(road, lat, lon, lrpName, chainage, "Bridge." + condition, name, br);
                        roadList.add(i, rr);
                        break;
                    }
                }
            }
        }
    }

    /**
     * @param fn base part of the filename. Three extensions will be created
     * @throws IOException on i/o error
     */
    private static void writeExcel(final String fn) throws IOException
    {
        try (PrintWriter objects = new PrintWriter(new OutputStreamWriter(
                new BufferedOutputStream(new FileOutputStream(new File(fn + "_objects.tsv"), false)), "UTF-8")))
        {
            objects.println("Object Class\tObject Name\tX\tY\tZ\tLength\tWidth\tHeight\tEntityType\tInitialNumberInSystem\t"
                    + "RideOnTransporter\tTransporterName\tProcessingAddOnProcess\tPropertyB\tPropertyC");
            try (PrintWriter links = new PrintWriter(new OutputStreamWriter(
                    new BufferedOutputStream(new FileOutputStream(new File(fn + "_links.tsv"), false)), "UTF-8")))
            {
                links.println("Link Class\tLink Name\tFrom Node\tTo Node\tNetwork\tWidth\tHeight\tType\t"
                        + "Property A\tProperty B\tProperty C");
                try (PrintWriter vertices = new PrintWriter(new OutputStreamWriter(
                        new BufferedOutputStream(new FileOutputStream(new File(fn + "_vertices.tsv"), false)), "UTF-8")))
                {
                    vertices.println("Link Name\tVertex X\tVertex Y\tVertex Z");

                    // loop through all the roads
                    for (List<RoadRecordB> rrList : roadMap.values())
                    {
                        RoadRecordB lastRR = null;
                        for (RoadRecordB rr : rrList)
                        {
                            String name = rr.road + "_" + rr.lrp;
                            if (rr.bridgeRecord == null)
                            {
                                objects.println("BasicNode\t" + name + "\t" + tlon(rr.lon) + "\t0\t" + tlat(rr.lat));
                            }
                            else
                            {
                                objects.println("TransferNode\t" + name + "\t" + tlon(rr.lon) + "\t0\t" + tlat(rr.lat));
                            }
                            if (lastRR != null)
                            {
                                String linkName = rr.road + "_" + lastRR.lrp + "_" + rr.lrp;
                                String fromNode = lastRR.road + "_" + lastRR.lrp;
                                links.println("Path\t" + linkName + "\t" + fromNode + "\t" + name + "\t\t\t\tBidirectional");
                            }
                            lastRR = rr;
                        }
                    }
                }
            }
        }
    }

    private static double tlat(final double lat)
    {
        return (lat - 23) * 100;
    }

    private static double tlon(final double lon)
    {
        return (lon - 90) * 100;
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
            fis = new FileInputStream(MakeSimioFile.class.getResource(filename).getFile());
        try (CSVReader reader = new CSVReader(new InputStreamReader(fis), ',', '"', 1))
        {
            String[] parts;
            String lastRoad = "";
            List<RoadRecordB> roadList = new ArrayList<>();
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
                RoadRecordB rr = new RoadRecordB(roadName, lat, lon, lrps, chainage, type, name, null);
                rr.bf = bf;
                roadList.add(rr);
                lastRoad = roadName;
            }
        }
    }

    /** */
    protected static class RoadRecordB extends RoadRecord
    {
        /** */
        String road;

        /** */
        BridgeRecord bridgeRecord;

        /**
         * @param road road code
         * @param lat lat
         * @param lon lon
         * @param lrp lrp
         * @param chainage km
         * @param type type
         * @param name name
         * @param bridgeRecord bridge or null
         */
        public RoadRecordB(String road, double lat, double lon, String lrp, double chainage, String type, String name,
                BridgeRecord bridgeRecord)
        {
            super(lat, lon, lrp, chainage, type, name);
            this.road = road;
            this.bridgeRecord = bridgeRecord;
        }

    }

    /** */
    protected static class BridgeRecord
    {
        /** */
        String road;

        /** */
        String lrpName;

        /** */
        double length;

        /** */
        String condition;

        /** */
        double chainage;

        /** */
        double width;

        /** */
        double lat;

        /** */
        double lon;

        /**
         * @param road road
         * @param lrpName lrp
         * @param length len
         * @param condition cond A/B/C/D
         * @param chainage km
         * @param width in m
         * @param lat lat
         * @param lon lon
         */
        public BridgeRecord(String road, String lrpName, double length, String condition, double chainage, double width,
                double lat, double lon)
        {
            super();
            this.road = road;
            this.lrpName = lrpName;
            this.length = length;
            this.condition = condition;
            this.chainage = chainage;
            this.width = width;
            this.lat = lat;
            this.lon = lon;
        }

    }
}
