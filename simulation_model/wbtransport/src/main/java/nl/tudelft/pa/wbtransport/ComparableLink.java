package nl.tudelft.pa.wbtransport;

import java.util.Map;

import org.opentrafficsim.core.dsol.OTSSimulatorInterface;
import org.opentrafficsim.core.geometry.OTSLine3D;
import org.opentrafficsim.core.gtu.GTUType;
import org.opentrafficsim.core.network.LinkType;
import org.opentrafficsim.core.network.LongitudinalDirectionality;
import org.opentrafficsim.core.network.Network;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.core.network.Node;
import org.opentrafficsim.core.network.OTSLink;

/**
 * <p>
 * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opensource.org/licenses/BSD-3-Clause">BSD 3-Clause License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version May 18, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class ComparableLink extends OTSLink implements Comparable<ComparableLink>
{

    public ComparableLink(Network newNetwork, OTSSimulatorInterface newSimulator, boolean animation, OTSLink link)
            throws NetworkException
    {
        super(newNetwork, newSimulator, animation, link);
    }

    public ComparableLink(Network network, String id, Node startNode, Node endNode, LinkType linkType, OTSLine3D designLine,
            OTSSimulatorInterface simulator, Map<GTUType, LongitudinalDirectionality> directionalityMap) throws NetworkException
    {
        super(network, id, startNode, endNode, linkType, designLine, simulator, directionalityMap);
    }

    public ComparableLink(Network network, String id, Node startNode, Node endNode, LinkType linkType, OTSLine3D designLine,
            OTSSimulatorInterface simulator, LongitudinalDirectionality directionality) throws NetworkException
    {
        super(network, id, startNode, endNode, linkType, designLine, simulator, directionality);
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(ComparableLink w)
    {
        return this.getId().compareTo(w.getId());
    }

}
