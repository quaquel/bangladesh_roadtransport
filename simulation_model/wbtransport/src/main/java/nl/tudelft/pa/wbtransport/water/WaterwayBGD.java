package nl.tudelft.pa.wbtransport.water;

import java.util.Map;

import org.opentrafficsim.core.dsol.OTSDEVSSimulatorInterface;
import org.opentrafficsim.core.geometry.OTSLine3D;
import org.opentrafficsim.core.gtu.GTUType;
import org.opentrafficsim.core.network.LinkType;
import org.opentrafficsim.core.network.LongitudinalDirectionality;
import org.opentrafficsim.core.network.Network;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.core.network.OTSNode;
import org.opentrafficsim.water.network.Waterway;

/**
 * <p>
 * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opensource.org/licenses/BSD-3-Clause">BSD 3-Clause License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Jan 26, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class WaterwayBGD extends Waterway
{
    /** */
    private static final long serialVersionUID = 20150927L;

    /**
     * Construct a new waterway.
     * @param network the network.
     * @param id the waterway id
     * @param name the name
     * @param startNode start node (directional)
     * @param endNode end node (directional)
     * @param linkType Link type to indicate compatibility with GTU types
     * @param designLine the OTSLine3D design line of the Link
     * @param simulator the simulator to schedule events on
     * @param directionality to indicate the general direction of the waterway (FORWARD = in the direction of the design line;
     *            BACKWARD is in the opposite direction; BOTH is a waterway that can be used in both directions; NONE is a
     *            waterway that cannot be used for sailing.
     * @throws NetworkException when waterway with this id already exists
     */
    @SuppressWarnings("checkstyle:parameternumber")
    public WaterwayBGD(Network network, String id, String name, OTSNode startNode, OTSNode endNode, LinkType linkType,
            OTSLine3D designLine, OTSDEVSSimulatorInterface simulator, LongitudinalDirectionality directionality)
            throws NetworkException
    {
        super(network, id, name, startNode, endNode, linkType, designLine, simulator, directionality);
    }

    /**
     * Construct a new waterway.
     * @param network the network.
     * @param id the waterway id
     * @param name the name
     * @param startNode start node (directional)
     * @param endNode end node (directional)
     * @param linkType Link type to indicate compatibility with GTU types
     * @param designLine the OTSLine3D design line of the Link
     * @param simulator the simujlator to schedule events on
     * @param directionalityMap the directions for different type of ships; it might be that all or certain types of ships are
     *            only allowed to use a canal in one direction. Furthermore, the directions can limit waterways for certain
     *            classes of ships. Set the LongitudinalDirectionality to NONE for ships that are not allowed to sail this
     *            waterway.
     * @throws NetworkException when waterway with this id already exists
     */
    @SuppressWarnings("checkstyle:parameternumber")
    public WaterwayBGD(Network network, String id, String name, OTSNode startNode, OTSNode endNode, LinkType linkType,
            OTSLine3D designLine, OTSDEVSSimulatorInterface simulator,
            Map<GTUType, LongitudinalDirectionality> directionalityMap) throws NetworkException
    {
        super(network, id, name, startNode, endNode, linkType, designLine, simulator, directionalityMap);
    }

}
