package nl.tudelft.pa.wbtransport.test;

/**
 * <p>
 * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opensource.org/licenses/BSD-3-Clause">BSD 3-Clause License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version May 16, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class Test
{

    /**
     * 
     */
    public Test()
    {
    }

    private static double distance(double lat1, double lon1, double lat2, double lon2)
    {
        double p = 0.017453292519943295; // Math.PI / 180
        double a = 0.5 - Math.cos((lat2 - lat1) * p) / 2.0
                + Math.cos(lat1 * p) * Math.cos(lat2 * p) * (1.0 - Math.cos((lon2 - lon1) * p)) / 2.0;
        return 12742000.0 * Math.asin(Math.sqrt(a)); // 2 * R; R = 6371 km, distance in m.
    }
    /**
     * @param args args
     */
    public static void main(String[] args)
    {
        System.out.println(distance(52.005221, 4.359277, 51.985353, 4.391034)); // should be 3100 m
    }

}
