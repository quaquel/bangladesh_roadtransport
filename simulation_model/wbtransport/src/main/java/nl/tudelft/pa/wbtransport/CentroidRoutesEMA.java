package nl.tudelft.pa.wbtransport;

import java.rmi.RemoteException;

import javax.naming.NamingException;

import org.djunits.unit.DurationUnit;
import org.djunits.unit.TimeUnit;
import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Time;
import org.opentrafficsim.core.dsol.OTSDEVSSimulator;
import org.opentrafficsim.core.dsol.OTSReplication;
import org.opentrafficsim.core.dsol.OTSSimTimeDouble;
import org.sim0mq.Sim0MQException;
import org.sim0mq.message.MessageStatus;
import org.sim0mq.message.SimulationMessage;
import org.sim0mq.message.TypedMessage;
import org.zeromq.ZMQ;

import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.experiment.ReplicationMode;

/**
 * <p>
 * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opensource.org/licenses/BSD-3-Clause">BSD 3-Clause License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Jan 5, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class CentroidRoutesEMA
{
    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private OTSDEVSSimulator simulator;

    /** */
    private CentroidRoutesModel model;
    
    /** the socket. */
    private ZMQ.Socket fsSocket;

    /** the context. */
    private ZMQ.Context fsContext;

    /** federation run id. */
    private Object federationRunId;

    /** modelId unique Id of the model that is used as the sender/receiver when communicating. */
    private String modelId;

    /** runtime. */
    private Duration runTime;

    /** warmup. */
    private Duration warmupTime;

    /** message count. */
    private long messageCount = 0;

    /**
     * @param args modelId portnumber
     * @throws SimRuntimeException on error
     * @throws RemoteException on error
     * @throws NamingException on error
     * @throws Sim0MQException on error
     */
    public static void main(final String[] args) throws SimRuntimeException, RemoteException, NamingException, Sim0MQException
    {
        if (args.length < 3)
        {
            System.err.println("Use as MM1Queue41Application modelId sim0mqPortNumber fileFolder");
            System.exit(-1);
        }

        String modelId = args[0];

        String sPort = args[1];
        int port = 0;
        try
        {
            port = Integer.parseInt(sPort);
        }
        catch (NumberFormatException nfe)
        {
            System.err.println("Use as FederateStarter portNumber, where portNumber is a number");
            System.exit(-1);
        }
        if (port == 0 || port > 65535)
        {
            System.err.println("PortNumber should be between 1 and 65535");
            System.exit(-1);
        }

        new CentroidRoutesEMA(modelId, port, args[2]);
    }

    /**
     * Main program.
     * @param modelId unique Id of the model that is used as the sender/receiver when communicating
     * @param port the sim0mq port number on which the model listens
     * @param fileFolder Folder of the files
     * @throws SimRuntimeException on error
     * @throws RemoteException on error
     * @throws NamingException on error
     * @throws Sim0MQException on error
     */
    public CentroidRoutesEMA(final String modelId, final int port, final String fileFolder)
            throws SimRuntimeException, RemoteException, NamingException, Sim0MQException
    {
        this.modelId = modelId.trim();
        this.model = new CentroidRoutesModel();
        this.model.fileFolder = fileFolder;
        this.simulator = new OTSDEVSSimulator();
        startListener(port);
    }

    /**
     * Start listening on a port.
     * @param port the sim0mq port number on which the model listens
     * @throws Sim0MQException on error
     */
    protected void startListener(final int port) throws Sim0MQException
    {
        this.fsContext = ZMQ.context(1);

        this.fsSocket = this.fsContext.socket(ZMQ.ROUTER);
        this.fsSocket.bind("tcp://*:" + port);

        System.out.println("Model started. Listening at port: " + port);
        System.out.flush();

        while (!Thread.currentThread().isInterrupted())
        {
            // Wait for next request from the client -- first the identity (String) and the delimiter (#0)
            String identity = this.fsSocket.recvStr();
            this.fsSocket.recvStr();

            byte[] request = this.fsSocket.recv(0);
            System.out.println(TypedMessage.printBytes(request));
            Object[] fields = SimulationMessage.decode(request);

            System.out.println("Received " + SimulationMessage.print(fields));
            System.out.flush();

            this.federationRunId = fields[1];
            String senderId = fields[2].toString();
            String receiverId = fields[3].toString();
            String messageId = fields[4].toString();
            long uniqueId = ((Long) fields[5]).longValue();

            if (receiverId.equals(this.modelId))
            {
                System.err.println("Received: " + messageId + ", payload = " + SimulationMessage.listPayload(fields));
                switch (messageId)
                {
                    case "FS.1":
                    case "FM.5":
                        processRequestStatus(identity, senderId, uniqueId);
                        break;

                    case "FM.2":
                        processSimRunControl(identity, senderId, uniqueId, fields);
                        break;

                    case "FM.3":
                        processSetParameter(identity, senderId, uniqueId, fields);
                        break;

                    case "FM.4":
                        processSimStart(identity, senderId, uniqueId);
                        break;

                    case "FM.6":
                        processRequestStatistics(identity, senderId, uniqueId, fields);
                        break;

                    case "FS.3":
                        processKillFederate();
                        break;

                    default:
                        // wrong message
                        System.err.println("Received unknown message -- not processed: " + messageId);
                }
            }
            else
            {
                // wrong receiver
                System.err.println(
                        "Received message not intended for " + this.modelId + " but for " + receiverId + " -- not processed: ");
            }
        }
    }

    /**
     * Process FS.1 message and send MC.1 message back.
     * @param identity reply id for REQ-ROUTER pattern
     * @param receiverId the receiver of the response
     * @param replyToMessageId the message to which this is the reply
     * @throws Sim0MQException on error
     */
    private void processRequestStatus(final String identity, final String receiverId, final long replyToMessageId)
            throws Sim0MQException
    {
        String status = "started";
        if (this.simulator.isRunning())
        {
            status = "running";
        }
        else if (this.simulator.getSimulatorTime() != null && this.simulator.getReplication() != null
                && this.simulator.getReplication().getTreatment() != null)
        {
            if (this.simulator.getSimulatorTime().ge(this.simulator.getReplication().getTreatment().getEndTime()))
            {
                status = "ended";
            }
            else
            {
                status = "error";
            }
        }
        this.fsSocket.sendMore(identity);
        this.fsSocket.sendMore("");
        byte[] mc1Message = SimulationMessage.encode(this.federationRunId, this.modelId, receiverId, "MC.1",
                ++this.messageCount, MessageStatus.NEW, replyToMessageId, status, "");
        this.fsSocket.send(mc1Message, 0);

        System.out.println("Sent MC.1");
        System.out.flush();
    }

    /**
     * Process FM.2 message and send MC.2 message back.
     * @param identity reply id for REQ-ROUTER pattern
     * @param receiverId the receiver of the response
     * @param replyToMessageId the message to which this is the reply
     * @param fields the message
     * @throws Sim0MQException on error
     */
    private void processSimRunControl(final String identity, final String receiverId, final long replyToMessageId,
            final Object[] fields) throws Sim0MQException
    {
        boolean status = true;
        String error = "";
        try
        {
            Object runTimeField = fields[8];
            if (runTimeField instanceof Number)
            {
                this.runTime = new Duration(((Number) fields[8]).doubleValue(), DurationUnit.SI);
            }
            else if (runTimeField instanceof Duration)
            {
                this.runTime = (Duration) runTimeField;
            }
            else
            {
                throw new Sim0MQException("runTimeField " + runTimeField + " neither Number nor Duration");
            }

            Object warmupField = fields[8];
            if (warmupField instanceof Number)
            {
                this.warmupTime = new Duration(((Number) fields[9]).doubleValue(), DurationUnit.SI);
            }
            else if (warmupField instanceof Duration)
            {
                this.warmupTime = (Duration) warmupField;
            }
            else
            {
                throw new Sim0MQException("warmupField " + warmupField + " neither Number nor Duration");
            }
        }
        catch (Exception e)
        {
            status = false;
            error = e.getMessage();
        }
        byte[] mc2Message = SimulationMessage.encode(this.federationRunId, this.modelId, receiverId, "MC.2",
                ++this.messageCount, MessageStatus.NEW, replyToMessageId, status, error);
        this.fsSocket.sendMore(identity);
        this.fsSocket.sendMore("");
        this.fsSocket.send(mc2Message, 0);

        System.out.println("Sent MC.2");
        System.out.flush();
    }

    /**
     * Process FM.3 message and send MC.2 message back.
     * @param identity reply id for REQ-ROUTER pattern
     * @param receiverId the receiver of the response
     * @param replyToMessageId the message to which this is the reply
     * @throws Sim0MQException on error
     */
    private void processSimStart(final String identity, final String receiverId, final long replyToMessageId)
            throws Sim0MQException
    {
        boolean status = true;
        String error = "";
        try
        {
            OTSReplication replication = new OTSReplication("rep1", new OTSSimTimeDouble(new Time(0.0, TimeUnit.BASE)),
                    this.warmupTime, this.runTime, this.model);
            this.simulator.initialize(replication, ReplicationMode.TERMINATING);
            this.simulator.scheduleEventRel(new Duration(30.0, DurationUnit.DAY), this, this, "terminate", null);

            this.simulator.start();
        }
        catch (Exception e)
        {
            status = false;
            error = e.getMessage();
            System.err.println("----- ERROR OCCURED WHEN STARTING THE MODEL: " + e.getMessage());
            e.printStackTrace();
            System.err.println("----- ERROR OCCURED WHEN STARTING THE MODEL");
        }

        byte[] mc2Message = SimulationMessage.encode(this.federationRunId, this.modelId, receiverId, "MC.2",
                ++this.messageCount, MessageStatus.NEW, replyToMessageId, status, error);
        this.fsSocket.sendMore(identity);
        this.fsSocket.sendMore("");
        this.fsSocket.send(mc2Message, 0);

        System.out.println("Sent MC.2");
        System.out.flush();
    }

    /**
     * Process FM.4 message and send MC.2 message back.
     * @param identity reply id for REQ-ROUTER pattern
     * @param receiverId the receiver of the response
     * @param replyToMessageId the message to which this is the reply
     * @param fields the message
     * @throws Sim0MQException on error
     */
    private void processSetParameter(final String identity, final String receiverId, final long replyToMessageId,
            final Object[] fields) throws Sim0MQException
    {
        boolean status = true;
        String error = "";
        try
        {
            String parameterName = fields[8].toString();
            Object parameterValueField = fields[9];

            if (parameterName.equalsIgnoreCase("Flood_area"))
            {
                this.model.parameterFloodArea = parameterValueField.toString();
            }
            else
            {
                this.model.parameters.put(parameterName.toUpperCase(), Double.parseDouble(parameterValueField.toString()));
            }
        }
        catch (Exception e)
        {
            status = false;
            error = e.getMessage();
        }

        byte[] mc2Message = SimulationMessage.encode(this.federationRunId, this.modelId, receiverId, "MC.2",
                ++this.messageCount, MessageStatus.NEW, replyToMessageId, status, error);
        this.fsSocket.sendMore(identity);
        this.fsSocket.sendMore("");
        this.fsSocket.send(mc2Message, 0);

        System.out.println("Sent MC.2");
        System.out.flush();
    }

    /**
     * Process FM.5 message and send MC.3 or MC.4 message back.
     * @param identity reply id for REQ-ROUTER pattern
     * @param receiverId the receiver of the response
     * @param replyToMessageId the message to which this is the reply
     * @param fields the message
     * @throws Sim0MQException on error
     */
    private void processRequestStatistics(final String identity, final String receiverId, final long replyToMessageId,
            final Object[] fields) throws Sim0MQException
    {
        boolean ok = true;
        String error = "";
        String variableName = fields[8].toString();
        double variableValue = Double.NaN;
        try
        {
            String[] var = variableName.split("_");
            String product = var[0];
            switch (var[1])
            {
                case "TransportCost":
                    variableValue = this.model.outputTransportCost.get(product);
                    break;

                case "TravelTime":
                    variableValue = this.model.outputTravelTime.get(product);
                    break;

                case "UnsatisfiedDemand":
                    variableValue = this.model.outputUnsatisfiedDemand.get(product);
                    break;

                default:
                    ok = false;
                    error = "Parameter " + variableName + " unknown";
                    break;
            }
        }
        catch (Exception e)
        {
            ok = false;
            error = e.getMessage();
        }

        if (Double.isNaN(variableValue))
        {
            ok = false;
            error = "Parameter " + variableName + " not set to a value";
        }

        if (ok)
        {
            byte[] mc3Message = SimulationMessage.encode(this.federationRunId, this.modelId, receiverId, "MC.3",
                    ++this.messageCount, MessageStatus.NEW, variableName, variableValue);
            this.fsSocket.sendMore(identity);
            this.fsSocket.sendMore("");
            this.fsSocket.send(mc3Message, 0);

            System.out.println("Sent MC.3");
            System.out.flush();
        }
        else
        {
            byte[] mc4Message = SimulationMessage.encode(this.federationRunId, this.modelId, receiverId, "MC.4",
                    ++this.messageCount, MessageStatus.NEW, variableName, error);
            this.fsSocket.sendMore(identity);
            this.fsSocket.sendMore("");
            this.fsSocket.send(mc4Message, 0);

            System.out.println("Sent MC.4");
            System.out.flush();
        }
    }

    /**
     * Process FS.3 message.
     */
    private void processKillFederate()
    {
        this.fsSocket.close();
        this.fsContext.term();
        System.exit(0);
    }

    /** stop the simulation. */
    protected final void terminate()
    {
        System.out.println("Model terminated.");
        System.out.println("Key Performance Indicators are: ");
        // TODO
    }
}
