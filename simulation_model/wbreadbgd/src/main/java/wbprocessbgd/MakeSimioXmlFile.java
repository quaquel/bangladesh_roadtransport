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
 * Copyright (c) 2013-2016 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="http://opentrafficsim.org/docs/current/license.html">OpenTrafficSim License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Jan 29, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class MakeSimioXmlFile
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
        writeXml("E:/WBSIM_Lab2b/simio_bgd_roads.xml");
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
            fis = new FileInputStream(MakeSimioXmlFile.class.getResource(bmmsFile).getFile());
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
    private static void writeXml(final String fn) throws IOException
    {
        try (PrintWriter xml = new PrintWriter(
                new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(new File(fn), false)), "UTF-8")))
        {
            String s10 = "          ";
            // loop through all the roads, and write the nodes
            for (List<RoadRecordB> rrList : roadMap.values())
            {
                for (RoadRecordB rr : rrList)
                {
                    String name = rr.road + "_" + rr.lrp;
                    xml.print(s10 + "<Node Name=\"" + name + "\"");
                    if (rr.bridgeRecord == null)
                    {
                        xml.print(" Type=\"BasicNode\"");
                    }
                    else
                    {
                        xml.print(" Type=\"TransferNode\"");
                    }
                    xml.println(s10 + " Location=\"" + tlon(rr.lon) + " 0 " + tlat(rr.lat) + "\" Scope=\"Public\">");
                    xml.println(s10 + "  <Properties>");
                    xml.println(s10 + "    <Property Name=\"InitialCost\" Units=\"USD\">0.0</Property>");
                    xml.println(s10 + "    <Property Name=\"InitialCostRate\" Units=\"USD per Hour\">0.0</Property>");
                    xml.println(s10 + "    <Property Name=\"ResourceIdleCostRate\" Units=\"USD per Hour\">0.0</Property>");
                    xml.println(s10 + "    <Property Name=\"ResourceCostPerUse\" Units=\"USD\">0.0</Property>");
                    xml.println(s10 + "    <Property Name=\"ResourceUsageCostRate\" Units=\"USD per Hour\">0.0</Property>");
                    xml.println(s10 + "    <Property Name=\"DynamicSelectionRule\">");
                    xml.println(s10 + "     <Value>None</Value>");
                    xml.println(s10 + "    </Property>");
                    xml.println(s10 + "  </Properties>");
                    xml.println(s10 + "  <Graphics ExternallyVisible=\"True\" />");
                    xml.println(s10 + "</Node>");
                }
            }

            // loop through all the roads, and write the paths
            for (List<RoadRecordB> rrList : roadMap.values())
            {
                RoadRecordB lastRR = null;
                for (RoadRecordB rr : rrList)
                {
                    String name = rr.road + "_" + rr.lrp;
                    if (lastRR != null)
                    {
                        String linkName = rr.road + "_" + lastRR.lrp + "_" + rr.lrp;
                        String fromNode = lastRR.road + "_" + lastRR.lrp;
                        double d = Math.sqrt((tlon(rr.lon) - tlon(lastRR.lon)) * (tlon(rr.lon) - tlon(lastRR.lon))
                                + (tlat(rr.lat) - tlat(lastRR.lat)) * (tlat(rr.lat) - tlat(lastRR.lat)));
                        xml.println(s10 + "<Link Name=\"" + linkName + "\" Type=\"Path\" Size=\"" + d
                                + " 0 0\" Scope=\"Public\" Start=\"" + fromNode + "\" End=\"" + name + "\">");
                        xml.println(s10 + "  <Properties>");
                        xml.println(s10 + "    <Property Name=\"InitialCost\" Units=\"USD\">0.0</Property>");
                        xml.println(s10 + "    <Property Name=\"InitialCostRate\" Units=\"USD per Hour\">0.0</Property>");
                        xml.println(s10 + "    <Property Name=\"ResourceIdleCostRate\" Units=\"USD per Hour\">0.0</Property>");
                        xml.println(s10 + "    <Property Name=\"ResourceCostPerUse\" Units=\"USD\">0.0</Property>");
                        xml.println(s10 + "    <Property Name=\"ResourceUsageCostRate\" Units=\"USD per Hour\">0.0</Property>");
                        xml.println(s10 + "    <Property Name=\"DynamicSelectionRule\">");
                        xml.println(s10 + "      <Value>None</Value>");
                        xml.println(s10 + "    </Property>");
                        xml.println(s10 + "    <Property Name=\"Type\">Bidirectional</Property>");
                        xml.println(s10 + "  </Properties>");
                        xml.println(s10 + "  <Graphics ExternallyVisible=\"True\" />");
                        xml.println(s10 + "</Link>");
                    }
                    lastRR = rr;
                }
            }
        }
    }

    private static double tlat(final double lat)
    {
        return -(lat - 23) * 1000;
    }

    private static double tlon(final double lon)
    {
        return (lon - 90) * 1000;
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
            fis = new FileInputStream(MakeSimioXmlFile.class.getResource(filename).getFile());
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
