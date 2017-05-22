package nl.tudelft.pa.wbtransport.road;

/**
 * <p>
 * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opensource.org/licenses/BSD-3-Clause">BSD 3-Clause License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Feb 25, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public enum GapPoint
{
    /** gap start. */
    GAP_START,

    /** gap end. */
    GAP_END,

    /** bridge start. */
    BRIDGE_START,

    /** bridge end. */
    BRIDGE_END,

    /** ferry start. */
    FERRY_START,

    /** ferry end. */
    FERRY_END,

    /** no gap. */
    ROAD;

    /**
     * @return true if gap start.
     */
    public boolean isGapStart()
    {
        return this.equals(GAP_START);
    }

    /**
     * @return true if gap end.
     */
    public boolean isGapEnd()
    {
        return this.equals(GAP_END);
    }

    /**
     * @return true if bridge start.
     */
    public boolean isBridgeStart()
    {
        return this.equals(BRIDGE_START);
    }

    /**
     * @return true if bridge end.
     */
    public boolean isBridgeEnd()
    {
        return this.equals(BRIDGE_END);
    }

    /**
     * @return true if ferry start.
     */
    public boolean isFerryStart()
    {
        return this.equals(FERRY_START);
    }

    /**
     * @return true if ferry end.
     */
    public boolean isFerryEnd()
    {
        return this.equals(FERRY_END);
    }

    /**
     * @return true if normal road.
     */
    public boolean isRoad()
    {
        return this.equals(ROAD);
    }

    /**
     * @return the 2-letter code for the gap type
     */
    public String getCode2()
    {
        String gap;
        switch (this)
        {
            case BRIDGE_START:
                gap = "BS";
                break;

            case BRIDGE_END:
                gap = "BE";
                break;

            case FERRY_START:
                gap = "FS";
                break;

            case FERRY_END:
                gap = "FS";
                break;

            case GAP_START:
                gap = "GS";
                break;

            case GAP_END:
                gap = "GE";
                break;

            default:
                gap = "";
                break;
        }
        return gap;
    }
    
    /**
     * @param code2 the 2-letter code to interpret
     * @return the corresponding enum
     */
    public static GapPoint getInstance(final String code2)
    {
        switch (code2)
        {
            case "GS":
                return GapPoint.GAP_START;

            case "GE":
                return GapPoint.GAP_END;

            case "BS":
                return GapPoint.BRIDGE_START;

            case "BE":
                return GapPoint.BRIDGE_END;

            case "FS":
                return GapPoint.FERRY_START;

            case "FE":
                return GapPoint.FERRY_END;

            default:
                return GapPoint.ROAD;
        }
    }
}
