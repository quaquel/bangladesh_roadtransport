package wbprocessbgd;

/**
 * <p>
 * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opensource.org/licenses/BSD-3-Clause">BSD 3-Clause License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Feb 26, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class RoadRecord
{
    /** lat. */
    double lat;

    /** lon. */
    double lon;

    /** lrp. */
    String lrp = "";

    /** chainage in km. */
    double chainage;

    /** type. */
    String type = "";

    /** name. */
    String name = "";

    /** bridge or ferry. */
    String bf = "";

    /**
     * @param lat lat
     * @param lon lon
     * @param lrp lrp
     * @param chainage chanage in km
     * @param type type
     * @param name name
     */
    public RoadRecord(double lat, double lon, String lrp, double chainage, String type, String name)
    {
        super();
        this.lat = lat;
        this.lon = lon;
        this.lrp = lrp;
        this.chainage = chainage;
        this.type = type;
        this.name = name;
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return "RoadRecord [lat=" + this.lat + ", lon=" + this.lon + ", lrp=" + this.lrp + ", chainage=" + this.chainage
                + ", bf=" + this.bf + ", type=" + this.type + ", name=" + this.name + "]";
    }

}
