package nl.tudelft.pa.wbtransport.water;

import java.util.Map;

import org.opentrafficsim.core.dsol.OTSSimulatorInterface;
import org.opentrafficsim.core.geometry.OTSLine3D;
import org.opentrafficsim.core.gtu.GTUType;
import org.opentrafficsim.core.network.LinkType;
import org.opentrafficsim.core.network.LongitudinalDirectionality;
import org.opentrafficsim.core.network.Network;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.core.network.Node;

import nl.tudelft.pa.wbtransport.ComparableLink;

/**
 * <p>
 * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opensource.org/licenses/BSD-3-Clause">BSD 3-Clause License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Feb 19, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class Waterway extends ComparableLink
{
    /** */
    private static final long serialVersionUID = 20170219L;

    /**
     * Construct a new link.
     * @param id the link id
     * @param network the network to which the link belongs
     * @param startNode start node (directional)
     * @param endNode end node (directional)
     * @param linkType Link type to indicate compatibility with GTU types
     * @param designLine the OTSLine3D design line of the Link
     * @param simulator the simulator on which events can be scheduled
     * @param directionalityMap the directions (FORWARD, BACKWARD, BOTH, NONE) that GTUtypes can traverse this link
     * @throws NetworkException if link already exists in the network, if name of the link is not unique, or if the start node
     *             or the end node of the link are not registered in the network.
     */
    public Waterway(Network network, String id, Node startNode, Node endNode, LinkType linkType, OTSLine3D designLine,
            OTSSimulatorInterface simulator, Map<GTUType, LongitudinalDirectionality> directionalityMap) throws NetworkException
    {
        super(network, id, startNode, endNode, linkType, designLine, simulator, directionalityMap);
    }

    /**
     * Construct a new link, with a directionality for all GTUs as provided.
     * @param id the link id
     * @param network the network to which the link belongs
     * @param startNode start node (directional)
     * @param endNode end node (directional)
     * @param linkType Link type to indicate compatibility with GTU types
     * @param designLine the OTSLine3D design line of the Link
     * @param simulator the simulator on which events can be scheduled
     * @param directionality the directionality for all GTUs
     * @throws NetworkException if link already exists in the network, if name of the link is not unique, or if the start node
     *             or the end node of the link are not registered in the network.
     */
    public Waterway(Network network, String id, Node startNode, Node endNode, LinkType linkType, OTSLine3D designLine,
            OTSSimulatorInterface simulator, LongitudinalDirectionality directionality) throws NetworkException
    {
        super(network, id, startNode, endNode, linkType, designLine, simulator, directionality);
    }

    /**
     * Clone a link for a new network.
     * @param newNetwork the new network to which the clone belongs
     * @param newSimulator the new simulator for this network
     * @param animation whether to (re)create animation or not. Could be used in subclasses.
     * @param link the link to clone from
     * @throws NetworkException if link already exists in the network, if name of the link is not unique, or if the start node
     *             or the end node of the link are not registered in the network.
     */
    protected Waterway(Network newNetwork, OTSSimulatorInterface newSimulator, boolean animation, ComparableLink link)
            throws NetworkException
    {
        super(newNetwork, newSimulator, animation, link);
    }

}
