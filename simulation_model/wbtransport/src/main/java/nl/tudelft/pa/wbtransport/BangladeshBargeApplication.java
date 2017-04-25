package nl.tudelft.pa.wbtransport;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.naming.NamingException;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.djunits.unit.UNITS;
import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Time;
import org.opentrafficsim.base.modelproperties.BooleanProperty;
import org.opentrafficsim.base.modelproperties.Property;
import org.opentrafficsim.base.modelproperties.PropertyException;
import org.opentrafficsim.core.dsol.OTSDEVSSimulatorInterface;
import org.opentrafficsim.core.dsol.OTSModelInterface;
import org.opentrafficsim.core.dsol.OTSSimTimeDouble;
import org.opentrafficsim.core.gtu.GTU;
import org.opentrafficsim.core.gtu.animation.GTUColorer;
import org.opentrafficsim.core.network.OTSLink;
import org.opentrafficsim.core.network.OTSNetwork;
import org.opentrafficsim.core.network.OTSNode;
import org.opentrafficsim.core.network.animation.LinkAnimation;
import org.opentrafficsim.core.network.animation.NodeAnimation;
import org.opentrafficsim.road.gtu.animation.DefaultCarAnimation;
import org.opentrafficsim.simulationengine.AbstractWrappableAnimation;
import org.opentrafficsim.simulationengine.OTSSimulationException;
import org.opentrafficsim.simulationengine.SimpleSimulatorInterface;
import org.opentrafficsim.water.network.Waterway;

import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.animation.D2.GisRenderable2D;
import nl.tudelft.simulation.dsol.gui.swing.HTMLPanel;
import nl.tudelft.simulation.dsol.gui.swing.TablePanel;
import nl.tudelft.simulation.dsol.simulators.SimulatorInterface;
import nl.tudelft.simulation.language.io.URLResource;

/**
 * First Java model for sailing barges in Bangladesh.
 * <p>
 * Copyright (c) 2013-2016 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="http://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * <p>
 * $LastChangedDate: 2016-12-13 02:02:22 +0100 (Tue, 13 Dec 2016) $, @version $Revision: 2930 $, by $Author: wjschakel $,
 * initial version 12 nov. 2014 <br>
 * @author <a href="http://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class BangladeshBargeApplication extends AbstractWrappableAnimation implements UNITS
{
    /** */
    private static final long serialVersionUID = 1L;

    /** The model. */
    private BangladeshBargeModel model;

    /**
     * Create a ContourPlots simulation.
     * @throws PropertyException when the provided properties could not be handled 
     */
    public BangladeshBargeApplication() throws PropertyException
    {
        //
    }

    /** {@inheritDoc} */
    @Override
    public final void stopTimersThreads()
    {
        super.stopTimersThreads();
        this.model = null;
    }

    /**
     * Main program.
     * @param args String[]; the command line arguments (not used)
     * @throws SimRuntimeException when simulation cannot be created with given parameters
     */
    public static void main(final String[] args) throws SimRuntimeException
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            @SuppressWarnings("synthetic-access")
            @Override
            public void run()
            {
                try
                {
                    BangladeshBargeApplication bangladeshBargeApplication = new BangladeshBargeApplication();
                    List<Property<?>> localProperties = bangladeshBargeApplication.getProperties();
                    
                    bangladeshBargeApplication.buildAnimator(Time.ZERO, Duration.ZERO, new Duration(30, DAY),
                            localProperties, null, true);
                    bangladeshBargeApplication.panel.getTabbedPane().addTab("info", bangladeshBargeApplication.makeInfoPane());
                }
                catch (SimRuntimeException | NamingException | OTSSimulationException | PropertyException exception)
                {
                    exception.printStackTrace();
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    protected final void addAnimationToggles()
    {
        addToggleAnimationButtonText("Node", OTSNode.class, "Show/hide nodes", false);
        addToggleAnimationButtonText("NodeId", NodeAnimation.Text.class, "Show/hide node Ids", false);
        addToggleAnimationButtonText("Link", OTSLink.class, "Show/hide links", false);
        addToggleAnimationButtonText("LinkId", LinkAnimation.Text.class, "Show/hide link Ids", false);
        addToggleAnimationButtonText("Waterway", Waterway.class, "Show/hide waterways", true);
        // addToggleAnimationButtonText("WaterwayId", WaterwayAnimation.Text.class, "Show/hide lane Ids", false);
        addToggleAnimationButtonText("Bridge", BridgeBGD.class, "Show/hide bridges", false);
        addToggleAnimationButtonText("BridgeId", BridgeTextAnimation.class, "Show/hide bride Ids", false);
        addToggleAnimationButtonText("Ship", GTU.class, "Show/hide ships", true);
        addToggleAnimationButtonText("ShipId", DefaultCarAnimation.Text.class, "Show/hide ship Ids", false);
    }

    /** {@inheritDoc} */
    @Override
    protected final OTSModelInterface makeModel(final GTUColorer colorer)
    {
        this.model = new BangladeshBargeModel(this.savedUserModifiedProperties);
        return this.model;
    }

    /**
     * @return an info pane to be added to the tabbed pane.
     */
    protected final JComponent makeInfoPane()
    {
        // Make the info tab
        String helpSource = "/" + BangladeshBargeModel.class.getPackage().getName().replace('.', '/') + "/BangladeshWB.html";
        URL page = BangladeshBargeModel.class.getResource(helpSource);
        if (page != null)
        {
            try
            {
                HTMLPanel htmlPanel = new HTMLPanel(page);
                return new JScrollPane(htmlPanel);
            }
            catch (IOException exception)
            {
                exception.printStackTrace();
            }
        }
        return new JPanel();
    }

    /** {@inheritDoc} */
    @Override
    protected final void addTabs(final SimpleSimulatorInterface simulator) throws OTSSimulationException, PropertyException
    {
        // Make the tab with the plots
        ArrayList<BooleanProperty> graphs = new ArrayList<>();
        int graphCount = graphs.size();
        int columns = (int) Math.ceil(Math.sqrt(graphCount));
        int rows = 0 == columns ? 0 : (int) Math.ceil(graphCount * 1.0 / columns);
        TablePanel charts = new TablePanel(columns, rows);
        // fill the graphs
        addTab(getTabCount(), "statistics", charts);
    }

    /** {@inheritDoc} */
    @Override
    protected Rectangle2D makeAnimationRectangle()
    {
        return new Rectangle2D.Double(87.8, 20.2, 5.0, 6.6);
    }

    /** {@inheritDoc} */
    @Override
    public final String shortName()
    {
        return "Bangladesh Barge application";
    }

    /** {@inheritDoc} */
    @Override
    public final String description()
    {
        return "<html><h1>Bangladesh Barge Application</H1>"
                + "First model implementation of barge sailing in Bangladesh for the World Bank project.";
    }

}

/**
 * Barge model for Bangladesh.
 * <p>
 * Copyright (c) 2013-2016 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="http://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * <p>
 * $LastChangedDate: 2016-12-13 02:02:22 +0100 (Tue, 13 Dec 2016) $, @version $Revision: 2930 $, by $Author: wjschakel $,
 * initial version ug 1, 2014 <br>
 * @author <a href="http://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
class BangladeshBargeModel implements OTSModelInterface, UNITS
{
    /** */
    private static final long serialVersionUID = 20140815L;

    /** the properties for the model. */
    private List<Property<?>> properties;

    /** The simulator. */
    private OTSDEVSSimulatorInterface simulator;

    /** The network. */
    private final OTSNetwork network = new OTSNetwork("bg_network");

    /** Number of barges created. */
    private int bargesCreated = 0;

    /** The random number generator used to decide what kind of GTU to generate. */
    private Random randomGenerator = new Random(12345);

    /**
     * @param properties the user settable properties
     */
    BangladeshBargeModel(final List<Property<?>> properties)
    {
        this.properties = properties;
    }
    /** {@inheritDoc} */
    @Override
    public final void constructModel(final SimulatorInterface<Time, Duration, OTSSimTimeDouble> theSimulator)
            throws SimRuntimeException, RemoteException
    {
        this.simulator = (OTSDEVSSimulatorInterface) theSimulator;
        try
        {
            // create the model
            URL wwtURL = URLResource.getResource("/infrastructure/water/WaterwayTypes.xlsx");
            WaterwayTypeReader.read(wwtURL);
            URL routeURL = URLResource.getResource("/infrastructure/water/WaterwaysSailable/waterways_53routes_routable_v02.shp");
            RouteReader.read(routeURL);
            
            // background
            URL gisURL = URLResource.getResource("/gis/map.xml");
            System.out.println("GIS-map file: " + gisURL.toString());
            new GisRenderable2D(this.simulator, gisURL);
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final SimulatorInterface<Time, Duration, OTSSimTimeDouble> getSimulator() throws RemoteException
    {
        return this.simulator;
    }

    /** {@inheritDoc} */
    @Override
    public OTSNetwork getNetwork()
    {
        return this.network;
    }

}
