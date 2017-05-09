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
import java.util.ArrayList;
import java.util.List;

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
public class MakeRoadsFile4
{
    /** Padma bridge or ferry? */
    private static boolean padmaBridge = false;

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
        if (new File("E:/RMMS/_roads4.csv").exists())
        {
            new File("E:/RMMS/_roads4.csv").delete();
        }
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
        if (!new File("E:/RMMS/_roads4.csv").exists())
        {
            try (PrintWriter overview = new PrintWriter(new OutputStreamWriter(
                    new BufferedOutputStream(new FileOutputStream(new File("E:/RMMS/_roads4.csv"), false)), "UTF-8")))
            {
                overview.println("\"road\",\"chainage\",\"lrp\",\"lat\",\"lon\",\"gap\",\"type\",\"name\"");
            }
        }

        try (PrintWriter overview = new PrintWriter(new OutputStreamWriter(
                new BufferedOutputStream(new FileOutputStream(new File("E:/RMMS/_roads4.csv"), true)), "UTF-8")))
        {
            String roadName = file.getName().split("\\.")[0];
            System.out.println(roadName);
            String line = "";
            List<RoadRecord> rrList = new ArrayList<>();
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
                            double lat = Double.parseDouble(lats);
                            String lons = parts[1].substring(parts[1].indexOf("Longitu")).split("=")[1];
                            double lon = Double.parseDouble(lons);
                            String lrps = parts[2].split("=")[1].replaceAll("\\+", "");
                            String chas = parts[6].split("=")[1].replaceAll("\\%2E", ".");
                            double chainage = Double.parseDouble(chas);
                            String type = parts[3].split("=")[1].replaceAll("\\%2C", ",").replaceAll("\\+", "");
                            type = type.replaceAll("[\\t\\r\\n\\s]", " ");
                            String name = parts[4].contains("=") && parts[4].split("=").length > 1
                                    ? URLDecoder.decode(parts[4].split("=")[1].replaceAll("\\+", " "), "UTF-8") : ".";
                            name = name.replaceAll("[\\t\\r\\n\\s]", " ");
                            RoadRecord rr = new RoadRecord(lat, lon, lrps, chainage, type, name);
                            rrList.add(rr);
                        }
                    }
                    line = buf.readLine();
                }
            }

            // reverse outside-BGD problems
            for (RoadRecord rr : rrList)
            {
                if (20.757 < rr.lon && rr.lon < 26.635 && 88.0 < rr.lat && rr.lat < 92.615)
                {
                    // reverse
                    System.out.println(roadName + " Reverse " + rr);
                    double lat = rr.lat;
                    rr.lat = rr.lon;
                    rr.lon = lat;
                    continue;
                }
            }

            // clean chainage monotony problems
            int ii = 0;
            while (ii < rrList.size() - 1)
            {
                RoadRecord rr1 = rrList.get(ii);
                RoadRecord rr2 = rrList.get(ii + 1);
                if (rr2.chainage < rr1.chainage)
                {
                    System.out.println(roadName + " Monotony " + rr1 + " to " + rr2);
                    rrList.remove(ii + 1);
                }
                else
                {
                    ii++;
                }
            }

            // clean within river
            ii = 0;
            while (ii < rrList.size() - 1)
            {
                RoadRecord rr = rrList.get(ii);
                if (rr.lat == 0 || rr.lon == 0 || (rr.name.toLowerCase().contains("within river")))
                {
                    System.out.println(roadName + " In river - removed " + rr);
                    rrList.remove(ii);
                }
                else
                {
                    ii++;
                }
            }

            // take out same chainages / LRPs / coordinates
            ii = 0;
            while (ii < rrList.size())
            {
                if (ii + 1 < rrList.size())
                {
                    RoadRecord rr1 = rrList.get(ii);
                    RoadRecord rr2 = rrList.get(ii + 1);
                    if (rr1.lrp.equals(rr2.lrp) || (rr1.lat == rr2.lat && rr1.lon == rr2.lon) || rr1.chainage == rr2.chainage)
                    {
                        rrList.remove(ii + 1);
                        rr1.name += " / " + rr2.name;
                        rr1.type += " / " + rr2.type;
                        System.out.println(roadName + " Removed duplicate " + rr1 + " == " + rr2);
                    }
                    else
                    {
                        ii++;
                    }
                }
                else
                {
                    ii++;
                }
            }

            // start point repairs...
            if (roadName.equals("Z5019") || roadName.equals("Z1611"))
            {
                rrList.get(0).lat += 1;
            }
            if (roadName.equals("Z3711") || roadName.equals("Z7717"))
            {
                rrList.get(0).lat -= 1;
            }
            if (roadName.equals("Z7717"))
            {
                rrList.get(rrList.size() - 3).lat = 22.486663;
                rrList.get(rrList.size() - 2).lat = 22.484163;
            }
            if (roadName.equals("N602") || roadName.equals("Z8604"))
            {
                rrList.get(0).lon += 1;
            }
            if (roadName.equals("Z4606") || roadName.equals("Z1129"))
            {
                rrList.get(0).lon -= 1;
            }

            // clean +1 / -1 degree errors.
            ii = 0;
            while (ii < rrList.size() - 1)
            {
                RoadRecord rr1 = rrList.get(ii);
                RoadRecord rr2 = rrList.get(ii + 1);
                double d12 = distanceKm(rr1.lat, rr1.lon, rr2.lat, rr2.lon);
                double c12 = rr2.chainage - rr1.chainage;
                if (d12 - c12 > 0.95 * Math.min(deltaLat, deltaLonMin) && d12 - c12 < 1.05 * Math.max(deltaLat, deltaLonMax))
                {
                    if (distanceKm(rr1.lat, rr1.lon, rr2.lat - 1.0, rr2.lon) - c12 < 2.0)
                    {
                        rr2.lat -= 1.0;
                        System.out.println(roadName + " Distance lat-1 repaired " + rr1 + " to " + rr2);
                    }
                    else if (distanceKm(rr1.lat, rr1.lon, rr2.lat + 1.0, rr2.lon) - c12 < 2.0)
                    {
                        rr2.lat += 1.0;
                        System.out.println(roadName + " Distance lat+1 repaired " + rr1 + " to " + rr2);
                    }
                    else if (distanceKm(rr1.lat, rr1.lon, rr2.lat, rr2.lon - 1.0) - c12 < 2.0)
                    {
                        rr2.lon -= 1.0;
                        System.out.println(roadName + " Distance lon-1 repaired " + rr1 + " to " + rr2);
                    }
                    else if (distanceKm(rr1.lat, rr1.lon, rr2.lat, rr2.lon + 1.0) - c12 < 2.0)
                    {
                        rr2.lon += 1.0;
                        System.out.println(roadName + " Distance lon+1 repaired " + rr1 + " to " + rr2);
                    }
                }
                ii++;
            }

            // clean simple peaks in chainage - distance differences.
            ii = 0;
            while (ii < rrList.size() - 2)
            {
                RoadRecord rr1 = rrList.get(ii);
                RoadRecord rr2 = rrList.get(ii + 1);
                RoadRecord rr3 = rrList.get(ii + 2);
                double d12 = distanceKm(rr1.lat, rr1.lon, rr2.lat, rr2.lon);
                double c12 = rr2.chainage - rr1.chainage;
                double d23 = distanceKm(rr2.lat, rr2.lon, rr3.lat, rr3.lon);
                double c23 = rr3.chainage - rr2.chainage;
                double d13 = distanceKm(rr1.lat, rr1.lon, rr3.lat, rr3.lon);
                double c13 = rr3.chainage - rr1.chainage;
                if (d12 / c12 > 1.2 && d23 / c23 > 1.2 && d13 / c13 < 2.0)
                {
                    rr2.lat = rr1.lat + (rr3.lat - rr1.lat) * (c12 / c13);
                    rr2.lon = rr1.lon + (rr3.lon - rr1.lon) * (c12 / c13);
                    System.out.println(roadName + " Distance repaired " + rr1 + " to " + rr2);
                }
                ii++;
            }

            // clean start outliers in chainage - distance differences.
            if (rrList.size() > 2)
            {
                RoadRecord rr1 = rrList.get(0);
                RoadRecord rr2 = rrList.get(1);
                double d12 = distanceKm(rr1.lat, rr1.lon, rr2.lat, rr2.lon);
                double c12 = rr2.chainage - rr1.chainage;
                if (d12 - c12 > 2.0 && d12 > 2.0)
                {
                    RoadRecord rr3 = rrList.get(2);
                    double c23 = rr3.chainage - rr2.chainage;
                    rr1.lat = rr2.lat - (rr3.lat - rr2.lat) * c12 / c23;
                    rr1.lon = rr2.lon - (rr3.lon - rr2.lon) * c12 / c23;
                    System.out.println(roadName + " Start repaired " + rr1 + " to " + rr2);
                }
            }

            // clean end outliers in chainage - distance differences.
            if (rrList.size() > 2)
            {
                int e = rrList.size() - 1;
                RoadRecord rr1 = rrList.get(e);
                RoadRecord rr2 = rrList.get(e - 1);
                double d12 = distanceKm(rr1.lat, rr1.lon, rr2.lat, rr2.lon);
                double c12 = rr1.chainage - rr2.chainage;
                if (d12 - c12 > 2.0 && d12 > 2.0)
                {
                    RoadRecord rr3 = rrList.get(e - 2);
                    double c23 = rr2.chainage - rr3.chainage;
                    rr1.lat = rr2.lat - (rr2.lat - rr3.lat) * c12 / c23;
                    rr1.lon = rr2.lon - (rr2.lon - rr3.lon) * c12 / c23;
                    System.out.println(roadName + " End repaired " + rr2 + " to " + rr1);
                }
            }

            // delete multiple far away outliers...
            ii = 0;
            while (ii < rrList.size() - 2)
            {
                RoadRecord rr1 = rrList.get(ii);
                RoadRecord rr2 = rrList.get(ii + 1);
                double d12 = distanceKm(rr1.lat, rr1.lon, rr2.lat, rr2.lon);
                double c12 = rr2.chainage - rr1.chainage;
                if (d12 / c12 > 2.0 && d12 > 5.0)
                {
                    rrList.remove(ii + 1);
                    System.out.println(roadName + " Distance repaired - removed LRP; c12=" + c12 + ", d12=" + d12 + "  " + rr2);
                }
                else
                {
                    ii++;
                }
            }

            // get the bridges and the ferries
            int[] iiarr = { 0 };
            while (iiarr[0] < rrList.size() - 1)
            {
                /************************* FERRIES ***************************/

                EvaluateLRPs checkFerries = new EvaluateLRPs()
                {
                    @Override
                    boolean eval(RoadRecord rr1, RoadRecord rr2)
                    {
                        if ((rr1.name.toLowerCase().contains("ferry") && rr2.name.toLowerCase().contains("ferry"))
                                || (rr1.name.toLowerCase().contains("ghat") && rr2.name.toLowerCase().contains("ghat")))
                        {
                            if ((rr1.type.toLowerCase().contains("other") && rr2.type.toLowerCase().contains("other"))
                                    || (rr1.type.toLowerCase().contains("ferry") && rr2.type.toLowerCase().contains("ferry")))
                            {
                                return true;
                            }
                            return false;
                        }
                        return false;
                    }
                };
                if (checkFerries.checkConditions(rrList, iiarr, "FS", "FE"))
                {
                    continue;
                }

                EvaluateLRPs checkFerries2 = new EvaluateLRPs()
                {
                    @Override
                    boolean eval(RoadRecord rr1, RoadRecord rr2)
                    {
                        return (rr1.type.toLowerCase().contains("ferry") && rr2.type.toLowerCase().contains("ferry"));
                    }
                };
                if (checkFerries2.checkConditions(rrList, iiarr, "FS", "FE"))
                {
                    continue;
                }

                EvaluateLRPs checkGhat = new EvaluateLRPs()
                {
                    @Override
                    boolean eval(RoadRecord rr1, RoadRecord rr2)
                    {
                        return (rr1.type.toLowerCase().contains("ghat") && rr2.type.toLowerCase().contains("ghat"));
                    }
                };
                if (checkGhat.checkConditions(rrList, iiarr, "FS", "FE"))
                {
                    continue;
                }

                /**************************** BRIDGES ******************************/

                EvaluateLRPs checkBridges1 = new EvaluateLRPs()
                {
                    @Override
                    boolean eval(RoadRecord rr1, RoadRecord rr2)
                    {
                        return (rr1.name.toLowerCase().contains("bridge") && rr1.name.toLowerCase().contains("start")
                                && rr2.name.toLowerCase().contains("bridge") && rr2.name.toLowerCase().contains("end"));
                    }
                };
                if (checkBridges1.checkConditions(rrList, iiarr, "BS", "BE"))
                {
                    continue;
                }

                EvaluateLRPs checkBridges2 = new EvaluateLRPs()
                {
                    @Override
                    boolean eval(RoadRecord rr1, RoadRecord rr2)
                    {
                        return (rr1.name.toLowerCase().contains("brije") && rr1.name.toLowerCase().contains("start")
                                && rr2.name.toLowerCase().contains("brije") && rr2.name.toLowerCase().contains("end"));
                    }
                };
                if (checkBridges2.checkConditions(rrList, iiarr, "BS", "BE"))
                {
                    continue;
                }

                EvaluateLRPs checkBridges3 = new EvaluateLRPs()
                {
                    @Override
                    boolean eval(RoadRecord rr1, RoadRecord rr2)
                    {
                        return (rr1.type.toLowerCase().contains("bridge") && rr2.type.toLowerCase().contains("bridge"));
                    }
                };
                if (checkBridges3.checkConditions(rrList, iiarr, "BS", "BE"))
                {
                    continue;
                }

                /**************************** The new PADMA bridge or ferry *****************************/

                if (roadName.equals("N8") && rrList.get(iiarr[0]).lrp.contains("034a")
                        && rrList.get(iiarr[0] + 1).lrp.contains("044a"))
                {
                    if (padmaBridge)
                    {
                        rrList.get(iiarr[0]).bf = "BS";
                        rrList.get(iiarr[0] + 1).bf = "BE";
                    }
                    else
                    {
                        rrList.get(iiarr[0]).bf = "FS";
                        rrList.get(iiarr[0] + 1).bf = "FE";
                    }
                    iiarr[0] += 2;
                    continue;
                }

                /*********************************** GAPS AS LAST ***************************************/

                EvaluateLRPs checkRiverGap = new EvaluateLRPs()
                {
                    @Override
                    boolean eval(RoadRecord rr1, RoadRecord rr2)
                    {
                        return (rr1.name.toLowerCase().contains("river start") && rr2.name.toLowerCase().contains("river end"))
                                || (rr2.name.toLowerCase().contains("river start")
                                        && rr1.name.toLowerCase().contains("river end"));
                    }
                };
                if (checkRiverGap.checkConditions(rrList, iiarr, "GS", "GE"))
                {
                    continue;
                }

                EvaluateLRPs checkRoadGap = new EvaluateLRPs()
                {
                    @Override
                    boolean eval(RoadRecord rr1, RoadRecord rr2)
                    {
                        return (rr1.name.toLowerCase().contains("road start") && rr2.name.toLowerCase().contains("road end"))
                                || (rr2.name.toLowerCase().contains("road start")
                                        && rr1.name.toLowerCase().contains("road end"));
                    }
                };
                if (checkRoadGap.checkConditions(rrList, iiarr, "GS", "GE"))
                {
                    continue;
                }

                EvaluateLRPs checkRoadStartStopGap = new EvaluateLRPs()
                {
                    @Override
                    boolean eval(RoadRecord rr1, RoadRecord rr2)
                    {
                        return (rr1.name.toLowerCase().contains("road stops") && (rr2.name.toLowerCase().contains("road starts")
                                || rr2.name.toLowerCase().contains("road restarts")))
                                || (rr2.name.toLowerCase().contains("road stops")
                                        && rr1.name.toLowerCase().contains("road starts")
                                        || rr1.name.toLowerCase().contains("road restarts"));
                    }
                };
                if (checkRoadStartStopGap.checkConditions(rrList, iiarr, "GS", "GE"))
                {
                    continue;
                }

                EvaluateLRPs checkRoadDiscontinuesGap = new EvaluateLRPs()
                {
                    @Override
                    boolean eval(RoadRecord rr1, RoadRecord rr2)
                    {
                        return (rr1.name.toLowerCase().contains("discontinu") && rr2.name.toLowerCase().contains("start"))
                                || (rr2.name.toLowerCase().contains("start") && rr1.name.toLowerCase().contains("discontinu"));
                    }
                };
                if (checkRoadDiscontinuesGap.checkConditions(rrList, iiarr, "GS", "GE"))
                {
                    continue;
                }

                if (rrList.get(iiarr[0]).name.toLowerCase().contains("road discontinued"))
                {
                    rrList.get(iiarr[0]).bf = "GS";
                    rrList.get(iiarr[0] + 1).bf = "GE";
                    iiarr[0] += 2;
                    continue;
                }

                iiarr[0]++;
            }

            // write if still proper road...
            if (rrList.size() < 4)
            {
                System.out.println(roadName + " NOT INCLUDED -- #POINTS < 4");
            }
            else
            {
                RoadRecord rrS = rrList.get(0);
                RoadRecord rrE = rrList.get(rrList.size() - 1);
                double dSE = distanceKm(rrS.lat, rrS.lon, rrE.lat, rrE.lon);
                double cSE = rrE.chainage - rrS.chainage;
                if (dSE / cSE > 1.2)
                {
                    System.out.println(roadName + " NOT INCLUDED -- dSE/cSE = " + dSE / cSE + " > 1.2");
                }
                else
                {
                    for (RoadRecord rr : rrList)
                    {
                        overview.println("\"" + roadName + "\",\"" + rr.chainage + "\",\"" + rr.lrp + "\",\"" + rr.lat + "\",\""
                                + rr.lon + "\",\"" + rr.bf + "\",\"" + rr.type + "\",\"" + rr.name + "\"");
                    }
                }
            }
        }
    }

    /** distance 20-21 on lon 88. */
    private static double deltaLat = distanceKm(22, 88, 23, 88);

    /** distance 89-90 on lat 20 vs 26. */
    private static double deltaLonMin = Math.min(distanceKm(20, 89, 20, 90), distanceKm(26, 89, 26, 90));

    /** distance 89-90 on lat 20 vs 26. */
    private static double deltaLonMax = Math.max(distanceKm(20, 89, 20, 90), distanceKm(26, 89, 26, 90));

    /**
     * Haversine formula.
     * @param lat1 lat 1
     * @param lon1 lon 1
     * @param lat2 lat 2
     * @param lon2 lon 2
     * @return distance in km.
     */
    private static double distanceKm(final double lat1, final double lon1, final double lat2, final double lon2)
    {
        double p = 0.017453292519943295; // Math.PI / 180
        double a = 0.5 - Math.cos((lat2 - lat1) * p) / 2.0
                + Math.cos(lat1 * p) * Math.cos(lat2 * p) * (1.0 - Math.cos((lon2 - lon1) * p)) / 2.0;
        return 12742.0 * Math.asin(Math.sqrt(a)); // 2 * R; R = 6371 km
    }

    /** Evaluate conditions on two LRPs. */
    abstract static class EvaluateLRPs
    {
        /**
         * Evaluate bridge / ferry / gap
         * @param rr1 road record 1
         * @param rr2 road record 2
         * @return true/false based on the evaluation function
         */
        abstract boolean eval(RoadRecord rr1, RoadRecord rr2);

        /**
         * Apply the evaluation on the road records and delete intermediate LRPs if necessary.
         * @param rrList list of road records
         * @param iiarr int[] containing the pointer to the first road record
         * @param startString GS/BS/FS to indicate gap/bridge/ferry
         * @param endString GE/BE/FE to indicate gap/bridge/ferry
         * @return true when gap/ferry/bridge found
         */
        boolean checkConditions(List<RoadRecord> rrList, int[] iiarr, String startString, String endString)
        {

            if (eval(rrList.get(iiarr[0]), rrList.get(iiarr[0] + 1)))
            {
                rrList.get(iiarr[0]).bf = startString;
                rrList.get(iiarr[0] + 1).bf = endString;
                iiarr[0] += 2;
                return true;
            }
            if (iiarr[0] < rrList.size() - 2 && eval(rrList.get(iiarr[0]), rrList.get(iiarr[0] + 2)))
            {
                rrList.get(iiarr[0]).bf = startString;
                rrList.get(iiarr[0] + 2).bf = endString;
                rrList.remove(iiarr[0] + 1);
                iiarr[0] += 2;
                return true;
            }
            if (iiarr[0] < rrList.size() - 3 && eval(rrList.get(iiarr[0]), rrList.get(iiarr[0] + 3)))
            {
                rrList.get(iiarr[0]).bf = startString;
                rrList.get(iiarr[0] + 3).bf = endString;
                rrList.remove(iiarr[0] + 1);
                rrList.remove(iiarr[0] + 1);
                iiarr[0] += 2;
                return true;
            }
            return false;
        }

    }
}
