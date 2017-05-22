package nl.tudelft.pa.wbtransport.test;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.UUID;

import javax.naming.NamingException;
import javax.swing.SwingUtilities;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.djunits.unit.DurationUnit;
import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Time;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opentrafficsim.base.modelproperties.PropertyException;
import org.opentrafficsim.core.dsol.OTSDEVSSimulatorInterface;
import org.opentrafficsim.core.dsol.OTSModelInterface;
import org.opentrafficsim.core.dsol.OTSSimTimeDouble;
import org.opentrafficsim.core.geometry.OTSGeometryException;
import org.opentrafficsim.core.geometry.OTSLine3D;
import org.opentrafficsim.core.geometry.OTSPoint3D;
import org.opentrafficsim.core.gtu.GTU;
import org.opentrafficsim.core.gtu.animation.GTUColorer;
import org.opentrafficsim.core.network.LinkType;
import org.opentrafficsim.core.network.LongitudinalDirectionality;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.core.network.Node;
import org.opentrafficsim.core.network.OTSNetwork;
import org.opentrafficsim.core.network.OTSNode;
import org.opentrafficsim.core.network.animation.LinkAnimation;
import org.opentrafficsim.core.network.animation.NodeAnimation;
import org.opentrafficsim.road.gtu.animation.DefaultCarAnimation;
import org.opentrafficsim.simulationengine.AbstractWrappableAnimation;
import org.opentrafficsim.simulationengine.OTSSimulationException;
import org.opentrafficsim.simulationengine.SimpleSimulatorInterface;
import org.opentrafficsim.water.network.Waterway;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import nl.tudelft.pa.wbtransport.ComparableLink;
import nl.tudelft.pa.wbtransport.bridge.BridgeBGD;
import nl.tudelft.pa.wbtransport.bridge.animation.BridgeAnimation;
import nl.tudelft.pa.wbtransport.bridge.animation.BridgeTextAnimation;
import nl.tudelft.pa.wbtransport.util.ExcelUtil;
import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.animation.D2.GisRenderable2D;
import nl.tudelft.simulation.dsol.simulators.SimulatorInterface;
import nl.tudelft.simulation.language.d3.DirectedPoint;
import nl.tudelft.simulation.language.io.URLResource;

/**
 * <p>
 * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opensource.org/licenses/BSD-3-Clause">BSD 3-Clause License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Jan 5, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class TestWaterwayModelParser extends AbstractWrappableAnimation
{
    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private GisWaterwayImport gisWaterwayImport;

    /**
     * Main program.
     * @param args String[]; the command line arguments (not used)
     * @throws SimRuntimeException should never happen
     */
    public static void main(final String[] args) throws SimRuntimeException
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    TestWaterwayModelParser waterwayModel = new TestWaterwayModelParser();
                    // 1 hour simulation run for testing
                    waterwayModel.buildAnimator(Time.ZERO, Duration.ZERO, new Duration(60.0, DurationUnit.MINUTE),
                            new ArrayList<org.opentrafficsim.base.modelproperties.Property<?>>(), null, true);
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
    public final String shortName()
    {
        return "TestWaterwayModelParser";
    }

    /** {@inheritDoc} */
    @Override
    public final String description()
    {
        return "TestWaterwayModelParser";
    }

    /** {@inheritDoc} */
    @Override
    public final void stopTimersThreads()
    {
        super.stopTimersThreads();
    }

    /** {@inheritDoc} */
    @Override
    protected final void addTabs(final SimpleSimulatorInterface simulator)
    {
        return;
    }

    /** {@inheritDoc} */
    @Override
    protected final OTSModelInterface makeModel(final GTUColorer colorer)
    {
        this.gisWaterwayImport = new GisWaterwayImport();
        return this.gisWaterwayImport;
    }

    /** {@inheritDoc} */
    @Override
    protected final java.awt.geom.Rectangle2D.Double makeAnimationRectangle()
    {
        return new Rectangle2D.Double(87.8, 20.2, 5.0, 6.6);
    }

    /** {@inheritDoc} */
    @Override
    protected void addAnimationToggles()
    {
        this.addToggleAnimationButtonText("Node", OTSNode.class, "Show/hide nodes", false);
        this.addToggleAnimationButtonText("NodeId", NodeAnimation.Text.class, "Show/hide node Ids", false);
        this.addToggleAnimationButtonText("Link", ComparableLink.class, "Show/hide links", false);
        this.addToggleAnimationButtonText("LinkId", LinkAnimation.Text.class, "Show/hide link Ids", false);
        this.addToggleAnimationButtonText("GTU", GTU.class, "Show/hide ships", true);
        this.addToggleAnimationButtonText("GTUId", DefaultCarAnimation.Text.class, "Show/hide ship Ids", false);
        this.addToggleAnimationButtonText("Bridge", BridgeBGD.class, "Show/hide bridges", true);
        this.addToggleAnimationButtonText("BridgeId", BridgeTextAnimation.class, "Show/hide bridge Ids", false);

        this.panel.addToggleText(" ");
        this.panel.addToggleText(" GIS Layers");
        this.panel.addToggleGISButtonText("roads", "Roads", this.gisWaterwayImport.getGisMap(),
                "Turn GIS road map layer on or off");
        this.panel.addToggleGISButtonText("buildings", "Buildings", this.gisWaterwayImport.getGisMap(),
                "Turn GIS building map layer on or off");
        this.panel.addToggleGISButtonText("railways", "Railways", this.gisWaterwayImport.getGisMap(),
                "Turn GIS rail map layer on or off");
        this.panel.addToggleGISButtonText("waterways", "Waterways", this.gisWaterwayImport.getGisMap(),
                "Turn GIS waterway map layer on or off");
        this.panel.addToggleGISButtonText("wfp-river", "Rivers", this.gisWaterwayImport.getGisMap(),
                "Turn GIS waterway map layer of the WFP on or off");
        this.panel.hideGISLayer("buildings");
    }

    /** {@inheritDoc} */
    @Override
    public final String toString()
    {
        return "TestWaterwayModelParser []";
    }

    /**
     * Model to test the Esri Shape File Format parser.
     * <p>
     * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. <br>
     * All rights reserved. BSD-style license. See <a href="http://opentrafficsim.org/docs/license.html">OpenTrafficSim
     * License</a>.
     * <p>
     * $LastChangedDate: 2016-08-26 16:34:41 +0200 (Fri, 26 Aug 2016) $, @version $Revision: 2150 $, by $Author: gtamminga $,
     * initial version un 27, 2015 <br>
     * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
     */
    protected static class GisWaterwayImport implements OTSModelInterface
    {
        /** */
        private static final long serialVersionUID = 20141121L;

        /** The simulator. */
        private OTSDEVSSimulatorInterface simulator;

        /** The network. */
        private final OTSNetwork network = new OTSNetwork("Bangladesh waterway test network");

        /** the GIS map. */
        protected GisRenderable2D gisMap;

        /** {@inheritDoc} */
        @Override
        public final void constructModel(final SimulatorInterface<Time, Duration, OTSSimTimeDouble> pSimulator)
                throws SimRuntimeException
        {

            this.simulator = (OTSDEVSSimulatorInterface) pSimulator;

            try
            {
                getWaterways("infrastructure/water/WaterwaysSailable/", "waterways_53routes_routable_v05");

                readBridges("/infrastructure/Bridges.xlsx");
            }
            catch (Exception nwe)
            {
                nwe.printStackTrace();
            }

            // background
            URL gisURL = URLResource.getResource("/gis/map.xml");
            System.out.println("GIS-map file: " + gisURL.toString());
            this.gisMap = new GisRenderable2D(this.simulator, gisURL);
        }

        /**
         * @return gisMap
         */
        public final GisRenderable2D getGisMap()
        {
            return this.gisMap;
        }

        /**
         * Import a list of waterway (link) elements from a shape file
         * @param initialDir dir
         * @param fileName file name
         * @throws NetworkException on read error
         * @throws OTSGeometryException when design line not proper
         */
        private void getWaterways(String initialDir, String fileName) throws NetworkException, OTSGeometryException
        {
            FileDataStore dataStoreLink = null;
            try
            {
                dataStoreLink = newDatastore(initialDir, fileName);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            // open and read shape file links
            SimpleFeatureIterator featureIterator = getFeatureIterator(dataStoreLink);

            // loop through the features and first retrieve the geometry
            while (featureIterator.hasNext())
            {
                Feature feature = featureIterator.next();
                try
                {
                    Waterway ww = getPropertiesWW(feature);
                    System.out.println(ww);
                }
                catch (OTSGeometryException ge)
                {
                    ge.printStackTrace();
                }
            }
        }

        /**
         * @param initialDir initial directory
         * @param fileName file name without .shp
         * @return shape file data store
         * @throws IOException on read error
         */
        private FileDataStore newDatastore(String initialDir, final String fileName) throws IOException
        {
            try
            {
                URL url = TestWaterwayModelParser.class.getResource("/");
                File file = new File(url.getFile() + initialDir);
                String fn = file.getCanonicalPath();
                fn = fn.replace('\\', '/');
                File iniDir = new File(fn);
                file = new File(iniDir, fileName + ".shp");

                FileDataStore dataStoreLink = FileDataStoreFinder.getDataStore(file);
                return dataStoreLink;

            }
            catch (IOException exception)
            {
                exception.printStackTrace();
            }
            return null;

        }

        /**
         * Return an iterator.
         * @param dataStore the shape file store
         * @return iterator
         */
        private SimpleFeatureIterator getFeatureIterator(FileDataStore dataStore)
        {
            try
            {
                String[] typeNameLink = dataStore.getTypeNames();
                SimpleFeatureSource sourceLink;
                sourceLink = dataStore.getFeatureSource(typeNameLink[0]);
                SimpleFeatureCollection featuresLink = sourceLink.getFeatures();
                return featuresLink.features();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        /** counter. */
        static int count = 0;

        /**
         * @param feature the feature from the shape file
         * @return one road element with properties
         * @throws NetworkException on error
         * @throws OTSGeometryException when design line not proper
         */
        private Waterway getPropertiesWW(final Feature feature) throws NetworkException, OTSGeometryException
        {
            Geometry theGeom = (Geometry) feature.getDefaultGeometryProperty().getValue();
            Coordinate[] coordinates = theGeom.getCoordinates();

            // {osm_id=6, null=0, fix_id=2, length=4, name=5, width=8, Class=1, id=3, type=7, the_geom=0, Linestring=9}

            Property property = feature.getProperty("osm_id");
            String osmId = property.getValue().toString();
            property = feature.getProperty("fix_id");
            String fixId = property.getValue().toString();
            property = feature.getProperty("length");
            double length = parseDouble(property);
            property = feature.getProperty("name");
            String name = property.getValue().toString();
            name = name.length() == 0 ? UUID.randomUUID().toString() : name;
            property = feature.getProperty("width");
            double width = parseDouble(property);
            property = feature.getProperty("Class");
            double wwClass = parseDouble(property);
            property = feature.getProperty("id");
            String id = property.getValue().toString();
            property = feature.getProperty("type");
            String type = property.getValue().toString();

            String wwIdBegin = name + ".b";
            String wwIdEnd = name + ".e";

            OTSNode startNode = null;
            OTSPoint3D ptBegin = new OTSPoint3D(coordinates[0]);
            while (this.network.getNodeMap().keySet().contains(wwIdBegin))
            {
                wwIdBegin += "$";
            }
            for (Node node : this.network.getNodeMap().values())
            {
                if (node.getPoint().equals(ptBegin))
                {
                    wwIdBegin = node.getId();
                    ptBegin = node.getPoint();
                    startNode = (OTSNode) node;
                    break;
                }
            }
            if (startNode == null)
            {
                startNode = new OTSNode(this.network, wwIdBegin, ptBegin);
            }

            OTSNode endNode = null;
            OTSPoint3D ptEnd = new OTSPoint3D(coordinates[coordinates.length - 1]);
            while (this.network.getNodeMap().keySet().contains(wwIdEnd))
            {
                wwIdEnd += "$";
            }
            for (Node node : this.network.getNodeMap().values())
            {
                if (node.getPoint().equals(ptEnd))
                {
                    wwIdEnd = node.getId();
                    ptEnd = node.getPoint();
                    endNode = (OTSNode) node;
                    break;
                }
            }
            if (endNode == null)
            {
                endNode = new OTSNode(this.network, wwIdEnd, ptEnd);
            }

            while (this.network.getLinkMap().keySet().contains(name))
            {
                name += "$";
            }

            OTSLine3D designLine = new OTSLine3D(coordinates);

            Waterway ww = new Waterway(this.network, name, name, startNode, endNode, LinkType.ALL, designLine, this.simulator,
                    LongitudinalDirectionality.DIR_BOTH);
            try
            {
                new LinkAnimation(ww, this.simulator, (float) ((5.0 - wwClass) * 0.005));
            }
            catch (RemoteException | NamingException exception)
            {
                exception.printStackTrace();
            }
            return ww;
        }

        /**
         * @param property the property
         * @return a double
         */
        private Double parseDouble(Property property)
        {
            if (property.getValue() != null)
            {
                if (property.getValue().toString() != null && property.getValue().toString().length() > 0)
                {
                    return Double.parseDouble(property.getValue().toString());
                }
            }
            return Double.NaN;
        }

        private void readBridges(final String filename) throws Exception
        {
            FileInputStream fis;
            if (new File(filename).canRead())
                fis = new FileInputStream(filename);
            else
                fis = new FileInputStream(TestWaterwayModelParser.class.getResource(filename).getFile());
            XSSFWorkbook wbNuts = new XSSFWorkbook(fis);

            XSSFSheet sheet1 = wbNuts.getSheet("Bridge types and attributes");
            boolean firstRow = true;
            for (Row row : sheet1)
            {
                if (!firstRow)
                {
                    try
                    {
                        String roadNo = ExcelUtil.cellValue(row, "M");
                        String condition = ExcelUtil.cellValue(row, "D");
                        String type = ExcelUtil.cellValue(row, "C");
                        double width = ExcelUtil.cellValueDouble(row, "E");
                        double length = ExcelUtil.cellValueDouble(row, "F");
                        int constructionYear = ExcelUtil.cellValueInt(row, "G");
                        int numberOfSpans = ExcelUtil.cellValueInt(row, "H");

                        String name = ExcelUtil.cellValue(row, "B");
                        double latH = ExcelUtil.cellValueDouble(row, "S");
                        double latM = ExcelUtil.cellValueDouble(row, "T");
                        double latS = ExcelUtil.cellValueDouble(row, "U");
                        double lat = latH + latM / 60.0 + latS / 3600.0;
                        double lonH = ExcelUtil.cellValueDouble(row, "V");
                        double lonM = ExcelUtil.cellValueDouble(row, "W");
                        double lonS = ExcelUtil.cellValueDouble(row, "X");
                        double lon = lonH + lonM / 60.0 + lonS / 3600.0;
                        BridgeBGD b = new BridgeBGD(name, new DirectedPoint(lon, lat, 5.0), roadNo, condition, type, width,
                                length, constructionYear, numberOfSpans);

                        Color color;
                        switch (condition)
                        {
                            case "A":
                                color = Color.GREEN;
                                break;

                            case "B":
                                color = Color.ORANGE;
                                break;

                            case "C":
                                color = Color.RED;
                                break;

                            case "D":
                                color = Color.BLACK;
                                break;

                            default:
                                color = Color.DARK_GRAY;
                                break;
                        }

                        new BridgeAnimation(b, this.simulator, color);
                    }
                    catch (Exception e)
                    {
                        System.err.println(e.getMessage());
                    }
                }
                firstRow = false;
            }
        }

        /** {@inheritDoc} */
        @Override
        public SimulatorInterface<Time, Duration, OTSSimTimeDouble> getSimulator()
        {
            return this.simulator;
        }

        /** {@inheritDoc} */
        @Override
        public OTSNetwork getNetwork()
        {
            return this.network;
        }

        /** {@inheritDoc} */
        @Override
        public final String toString()
        {
            return "GisWaterwayImport [simulator=" + this.simulator + "]";
        }

    }

}
