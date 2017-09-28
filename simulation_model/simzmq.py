'''
Created on 20 feb. 2017

@author: sibeleker

This is module specifies a ModelStructureInterface for controlling a model via 
ZeroMQ. Therefore, the functions do not directLy command a model 
(e.g. a java model), but sends corresponding messages to ZeroMQ.

'''
from __future__ import (unicode_literals, print_function, division)


import os
import time
import zmq

from random import randint
from zmq.error import ZMQError

from ema_workbench.em_framework.model import AbstractModel, SingleReplication
from ema_workbench.util import EMAError, ema_logging
from ema_workbench.util.ema_logging import debug, warning, method_logger

from message_v2 import message_encode, message_decode

type_code = {'bytes' : 0,
             'int16' : 1,
             'int32' : 2,
             'int64' : 3,
             'int' : 3, 
             'float32' : 4,
             'float64' : 5,
             'float' : 5,
             'bool' : 6,
             'char_8' : 7,
             'char_16' : 8,
             'str' : 9,
             'str_16' : 10,
             'ndarray' : 16}


field_types = {'magic_no' : 'str',
               'sim_run_id' : 'str',
               'sender_id' : 'str',
               'receiver_id' : 'str',
               'msg_type_id' : 'str',
               'msg_status_id' : 'bytes', #but int for a bytearray
               'unique_msg_no' : 'int64', 
               'no_fields' : 'int16'}
  
field_names = ['magic_no', 'sim_run_id',
               'sender_id', 'receiver_id',
               'msg_type_id', 'unique_msg_no',
               'msg_status_id', 'no_fields', 
               'payload']    

# messages = {'FM.1' : FederateStart,
#             'FM.2' : SimRunControl,
#             'FM.3' : SetValue,
#             'FM.4' : StartSimulation,
#             'FM.5' : RequestStatus,
#             'FM.6' : RequestStatistics,
#             'FM.7' : SimulationReset,
#             'FM.8' : KillFederate}

class WorkingDirectoryModel(AbstractModel):
    '''TODO:: to be moved to ema_workbench'''
    
    @property
    def working_directory(self):
        return self._working_directory

    @working_directory.setter
    def working_directory(self, path):
        wd = os.path.abspath(path)
        debug('setting working directory to '+ wd)
        self._working_directory = wd

    def __init__(self, name, wd=None):
        """interface to the model

        Parameters
        ----------
        name : str
               name of the modelInterface. The name should contain only
               alpha-numerical characters.        
        working_directory : str
                            working_directory for the model. 

        Raises
        ------
        EMAError 
            if name contains non alpha-numerical characters
        AssertionError
            if working_directory does not exist

        """
        super(WorkingDirectoryModel, self).__init__(name)
        assert os.path.exists(wd)

        self.working_directory = wd

    def as_dict(self):
        model_specs = super(WorkingDirectoryModel, self).as_dict()
        model_specs['working_directory'] = self.working_directory
        return model_specs


class SimZMQModel(SingleReplication, WorkingDirectoryModel):
    #TODO:: who is responsible for replications, I assume dsol
    # thus should use SingleReplicaton here
    
    # TODO:: have a separate function which handles the check of return 
    # message

    @property
    def redirectStdin(self):
        return  os.path.join(self.working_directory, self._stdin)
    
    @redirectStdin.setter
    def redirectStdin(self, stdin):
        self._stdin = stdin
    
    @property
    def redirectStdout(self):
        return os.path.join(self.working_directory, self._stdout)

    @redirectStdout.setter
    def redirectStdout(self, stdout):
        self._stdout = stdout
    
    @property
    def redirectStderr(self):
        return os.path.join(self.working_directory, self._stderr)

    @redirectStderr.setter
    def redirectStderr(self, stderr):
        self._stderr = stderr

    @property
    def run_setup(self):
        raise NotImplementedError

    def __init__(self, name, wd, software_code, args_before, args_after,
                 fullPathModelFile, 
                 redirectStdin, redirectStdout, redirectStderr, 
                 ip, federatestarter_port, federatestarter_name, 
                 receiver_tag, magic_nr, sim_run_id, sender_id, 
                 deleteWorkingDirectory=False, deleteStdin=False, 
                 deleteStdout=False, deleteStderr=False,):
        '''
        specify the model file etc, does not send any message to zeromq
        
        Parameters
        ----------
        name : str
        wd  : str
        software_code: str
        args_before : str
        args_after : str
        fullPathModelFile : str
        redirectStdin : 
        redirectStdout : 
        redirectStderr : 
        model_file : str
        ip : str , {'localhost', {remote_ip}}
        federatestarter_port : int,
                  port number of the federate starter
        federatestarter_name : str,
        receiver_tag : base name for identifying receiver
        magic_nr : str
                   the magic nr that will be used in the simulations, 
                   e.g "SIM01"
        sim_run_id : str
                     sim_un_id will show the simulation and replication number, 
                     but the root should be e.g. "EMA"
        sender_id : str
        local_directory : str 
        deleteWorkingDirectory : bool, optional
        deleteStdin : bool, optional
        deleteStdout : bool, optional
        deleteStderr : bool, optional
        '''
        super(SimZMQModel, self).__init__(name, wd)
        self.model_file = fullPathModelFile
        
        # have to define the working directory, if you want cleanup() to be 
        # called in finalization
        self.ip_toconnect = ip
        self.fs_port = federatestarter_port   
        self.fs_receiver = federatestarter_name
        
        self.receiver_tag = receiver_tag
        
        # ===unique message fields===
        self.magic_no = magic_nr
        self.sim_run_id = sim_run_id
        self.sender_id = sender_id
        
        # severytime this SimZMQModel object sends a message, 
        # this variable will be incremented
        self.nr_messages = 0 
        
        # === model instance specifications ===
        self.software_code = software_code
        self.args_before = args_before
        self.args_after = args_after  
        self.redirectStdin = redirectStdin 
        self.redirectStdout = redirectStdout
        self.redirectStderr = redirectStderr
        
        self.del_wd = deleteWorkingDirectory
        self.del_stdout = deleteStdout
        self.del_stdin = deleteStdin
        self.del_stderr = deleteStderr
               
                   
    def model_init(self, policy):
        super(SimZMQModel, self).model_init(policy)
        
        self.context = zmq.Context()  
        self.fs_socket = self.context.socket(zmq.REQ)  # @UndefinedVariable
        
        # TODO:: should this not be a UUID (using uuid library)?
        identity = u"%04x-%04x" % (randint(0, 0x10000), randint(0, 0x10000))
        self.fs_socket.setsockopt_string(zmq.IDENTITY, identity) # @UndefinedVariable
        try:
            self.fs_socket.connect("tcp://{}:{}".format(self.ip_toconnect, 
                                                        self.fs_port))
        except ZMQError as e:
            raise e
        
    
    @method_logger
    def start_new_model(self):        
        # TODO:: bit of a hack due to lack of reset on dsol
        ema_logging.info("starting new model")

        #TODO:: hack, m_reiver and base should be two seperate attributes
        self.instance_id = u"%04x-%04x" % (randint(0, 0x10000), randint(0, 0x10000))
        self.m_receiver = self.receiver_tag + '.' + str(self.instance_id)  

        self.sender_id = self.sender_id + '.' + str(self.instance_id) 
        
        #TODO:: why is the port number even in here
        args_after = '{m_receiver} 5556 {directory}'.format(
                    m_receiver=self.m_receiver, directory=self.args_after)  

        
        # ===send the federate starter message===
        payload = [self.m_receiver, self.software_code, self.args_before, 
                   self.model_file, args_after, self.working_directory, 
                   self.redirectStdin, self.redirectStdout, self.redirectStderr, 
                   self.del_wd, self.del_stdout, self.del_stdin]
        
        m_port = self.start_federate(payload)
        
        self.m_socket = self.context.socket(zmq.REQ) # @UndefinedVariable
        
        # TODO:: should this not be a UUID (using uuid library)?
        identity = u"%04x-%04x" % (randint(0, 0x10000), randint(0, 0x10000))
        self.m_socket.setsockopt_string(zmq.IDENTITY, identity) # @UndefinedVariable
        self.m_socket.connect("tcp://{}:{}".format(self.ip_toconnect, 
                                                   m_port))
        
        # ===send the Simulation Run Control message===
        self.sim_run_control()
        ema_logging.info("new model started")
              
    @method_logger
    def run_experiment(self, experiment):
        '''
        scenario : Scenario instance
                   keyword arguments for running the model. Scenario is 
                   dict-like  with  the names of the uncertainties as key, and 
                   the values to which to set these uncertainties.
        policy :  Policy instance
        experiment_id : the number of simulation we will be running. 
                        Message structure requires this. Starts with 0!
                        IT REQUIRES MODIFYING THE EXPERIMENT_RUNNER.run_experiment()
        '''
        run_id = int(experiment.id) # TODO:: check if id is unique
         
        #1) === SETTING THE PARAMETER VALUES ONE BY ONE ===
        for key, value in experiment.items():
            payload = [key, value]
            self.set_value(run_id, payload)   
         
        #2) === RUN THE SIMULATION ===
        self.StartSimulation(run_id)
             
        # 3) === REQUEST STATUS ===
        wait = True
        while wait:
            wait = self.RequestStatus(run_id)
            time.sleep(2) # TODO:: needed?
         
        #4) === COLLECT THE SIMULATION RESULTS ===
        results = {}
        for outcome in self.outcomes:
            variable = outcome.variable_name[0]
            v_type = type(outcome).__name__.split("O")[0]
             
            results[variable] = self.RequestStatistics(run_id, variable, 
                                                       v_type)
         
        return results
    
    
    @method_logger
    def reset_model(self):
        """ Method for reseting the model to its initial state. The default
        implementation only sets the outputs to an empty dict. 

        """
        super(SimZMQModel, self).reset_model()
        self.KillFederate()
        self.m_socket.close()
        
    
    @method_logger
    def cleanup(self):
        try:
            self.fs_socket.close()
#             self.context.term()
        except AttributeError as e:
            # typically only happens if the number of experiments is lower
            # than the number of cores in cases of running in parallel.
            # TODO:: in ema_workbench number of processes should be the
            # minimum of nr. cores and nr. of experiments
            ema_logging.warning(str(e))
    
    @method_logger
    def send_to_fs(self, message):
        # TODO logger
        self._send(self.fs_socket, message)
    
    @method_logger            
    def send_to_model(self, message):
        self._send(self.m_socket, message)
    
    @method_logger
    def _send(self, socket, message):
        try:
            socket.send(message)
            self.nr_messages += 1
        except TypeError as e1:
            raise EMAError(str(e1))
        except ZMQError as e2:
            raise EMAError(str(e2))
        
    @method_logger
    def start_federate(self, payload):
        # This is normally the experiment number. For federate start, it is 0.
        run_id = 0 
        content = self.prepare_message(run_id=run_id,
                                       receiver=self.fs_receiver, 
                                       message_type="FM.1",
                                       status=1,
                                       payload=payload)
        message = message_encode(content) 
        
        # === send the message ===
        self.send_to_fs(message)
        
        # ===receive acknowledgement===
        try:
            r_msg = self.fs_socket.recv()
            r_message = message_decode(r_msg)
            debug("FederateStarted : " + str(r_message))
            expected_type = "FS.2"
            error, fields = self.check_received_message(r_message, run_id, expected_type)
            if error:
                raise EMAError("Wrong message : the field(s) '{}' does not match.".format(', '.join(fields)))
            else:
                payload = [x[1] for x in r_message[8:]]
                if payload[0] == self.m_receiver:
                    if payload[1] in ['started', 'running', 'ended']:
                        debug("Federate {} has been started successfully.".format(payload[0]))
                    else:
                        EMAError("Error in starting federate {} : ".format(payload[0]), payload[2])
                m_port = payload[2] 
        except ZMQError as e:
            debug("model initialization message could not be received.")
            raise EMAError(str(e))
        return m_port
    
    @method_logger            
    def sim_run_control(self):
        run_id = 0
        
        payload = self.run_setup
        payload.append(self.n_replications) # TODO:: correctly implement FM.2
        payload.append(0)
        
        content = self.prepare_message(run_id=run_id,
                                       receiver=self.m_receiver, 
                                       message_type='FM.2',
                                       status=1,
                                       payload=payload)
            
        message = message_encode(content) 
        self.send_to_model(message)
        
        try:
            r_msg = self.m_socket.recv()
            r_message = message_decode(r_msg)
            expected_type = "MC.2"
            error, fields = self.check_received_message(r_message, run_id, expected_type)
            if error:
                raise EMAError("Wrong message : the field(s) '{}' does not match.".format(', '.join(fields)))
            else:
                payload = [x[1] for x in r_message[8:]]
                #payload has 3 fields: uniqueid, status, error
                if payload[0] == self.nr_messages-1: #unique_message_id, since no other message was sent after the sim_run_control message, this should be equal to the previous value.
                    if payload[1]:
                        debug("Model has been initialized successfully.")
                    else:
                        EMAError("Error in sim run control: ", payload[2])
        except ZMQError as e:
            debug("sim_run_control acknowledgement could not be received.")
            raise EMAError(str(e))
    
    @method_logger 
    def set_value(self, run_id, payload):
        content = self.prepare_message(run_id=run_id,
                                       receiver=self.m_receiver, 
                                       message_type="FM.3",
                                       status=1,
                                       payload=payload)
            
        message = message_encode(content)
        self.send_to_model(message)

        try:
            r_msg = self.m_socket.recv()
            r_message = message_decode(r_msg)
            expected_type = "MC.2"
            error, fields = self.check_received_message(r_message, run_id, 
                                                        expected_type)
            if error:
                raise EMAError("Wrong message : the field(s) '{}' does not match.".format(', '.join(fields)))
            else:
                r_payload = [x[1] for x in r_message[8:]]
                if r_payload[1]:
                    debug("Parameter {} has been set successfully for simulation {}.".format(payload[0], run_id))
                else:
                    warning("Error in setting parameter {} in simulation {}: ".format(payload[0], run_id), r_payload[2])
        except ZMQError as e:
            debug("Parameter setting acknowledgement could not be received.")
            raise EMAError(str(e))
    
    @method_logger 
    def StartSimulation(self, run_id):
        content = self.prepare_message(run_id=run_id,
                                       receiver=self.m_receiver, 
                                       message_type="FM.4",
                                       status=1,
                                       payload=[]) 
            
        message = message_encode(content)
        self.send_to_model(message)

        try:
            r_msg = self.m_socket.recv()
            r_message = message_decode(r_msg)
            expected_type = "MC.2"
            error, fields = self.check_received_message(r_message, run_id, 
                                                        expected_type)
            if error:
                raise EMAError("Wrong message : the field(s) '{}' does not match.".format(', '.join(fields)))
            else:
                payload = [x[1] for x in r_message[8:]]
                if payload[1]:
                    debug("Simulation {} has been started successfully.".format(run_id))
                else:
                    EMAError("Error in starting simulation {} : ".format(run_id), payload[2])
        except ZMQError as e:
                debug("Sim start acknowledgement could not be received.")
                raise EMAError(str(e))
    
    @method_logger
    def RequestStatus(self, run_id):
        content = self.prepare_message(run_id=run_id,
                                       receiver=self.m_receiver, 
                                       message_type="FM.5",
                                       status=1,
                                       payload=[]) 
            
        message = message_encode(content)
        self.send_to_model(message)
        
        try:
            r_msg = self.m_socket.recv()
            r_message = message_decode(r_msg)
            expected_type = "MC.1"
            error, fields = self.check_received_message(r_message, run_id, expected_type)
            if error:
                raise EMAError("Wrong message : the field(s) '{}' does not match.".format(', '.join(fields)))
            else:
                payload = [x[1] for x in r_message[8:]]
                if payload[0] == self.nr_messages-1:
                    if payload[1] in ['started', 'running']:
                        wait = True
                    elif payload[1] == 'ended':
                        wait = False
                    elif payload[1] == 'error':
                        wait = False        
                        EMAError("Error in simulation {} : ".format(run_id), payload[2])
        except ZMQError as e:
                debug("Status could not be received.")
                raise EMAError(str(e))
        return wait
    
    @method_logger
    def RequestStatistics(self, run_id, variable, v_type):
        content = self.prepare_message(run_id=run_id,
                                       receiver=self.m_receiver, 
                                       message_type="FM.6",
                                       status=1,
                                       payload=[variable])
            
        message = message_encode(content)
        self.send_to_model(message)        
        
        #receive the output
        try:
            r_msg = self.m_socket.recv()
            r_message = message_decode(r_msg)
            if r_message[4][1] == "MC.3": #output has been received
                debug("The output for {} has been received".format(variable))
                error, fields = self.check_received_message(r_message, run_id, "MC.3")
                if error:
                    raise EMAError("Wrong message : the field(s) '{}' does not match.".format(', '.join(fields)))
                else:
                    o_value = r_message[-1][1]
                    o_name = r_message[-2][1]
                    
                    if o_name != variable:
                        EMAError("Conflict in collecting the output for {} in simulation {}".format(variable, run_id))
                    #error=True if the length of the value does not match with the outcome type and the rung length if timeseries
                    error = False
                    if v_type == 'TimeSeries' and not isinstance(o_value, list):
                        error = True
                    elif v_type == 'Scalar' and isinstance(o_value, list):
                        error = True
                    if error:
                        warning("Run {} not completed.".format(run_id))
            
            elif r_message[4][1] == "MC.4": #error message has been recievied
                error, fields = self.check_received_message(r_message, run_id, "MC.4")
                if error:
                    raise EMAError("Wrong message : the field(s) '{}' does not match.".format(', '.join(fields)))
                else:
                    err_msg = r_message[-1][1]
                    o_name = r_message[-2][1]
                    raise EMAError("Error in collecting output for {} in simulation {}: ".format(variable, run_id), err_msg)           
        
        except ZMQError as e:
            raise EMAError(str(e))   
        
        return o_value 
    
    @method_logger
    def SimulationReset(self):
        #TODO:: do something here
        
        
        pass
    
    @method_logger
    def KillAll(self):
        run_id = 0
        content = self.prepare_message(run_id=run_id,
                                       receiver=self.fs_receiver, 
                                       message_type="FM.9",
                                       status=1,
                                       payload=[])
        message = message_encode(content)   
        
        self.send_to_fs(message)
        
        try:
            r_msg = self.fs_socket.recv()
            r_message = message_decode(r_msg)
            expected_type = "FS.5"
            error, fields = self.check_received_message(r_message, run_id, 
                                                        expected_type)
            if error:
                raise EMAError("Wrong message : the field(s) '{}' does not match.".format(', '.join(fields)))
            else:
                payload = r_message[8:][1]
                if payload[0]:
                    debug("Federate has been terminated successfully.")
                else:
                    EMAError("Error in terminating the federate: ", payload[1])
                    
        except ZMQError as e:
            raise EMAError(str(e))
    
    @method_logger
    def KillFederate(self):
        run_id = 0
        content = self.prepare_message(run_id=run_id, receiver=self.fs_receiver, 
                                       message_type="FM.8", status=1, 
                                       payload=[self.m_receiver])
        
        message = message_encode(content)   
        self.send_to_fs(message)
        
        try:
            r_msg = self.fs_socket.recv()
            r_message = message_decode(r_msg)
            expected_type = "FS.4"
            error, fields = self.check_received_message(r_message, run_id, 
                                                        expected_type)
            if error:
                raise EMAError("Wrong message : the field(s) '{}' does not match.".format(', '.join(fields)))
            else:
                #payload = [r_message[-3][1], r_message[-2][1], r_message[-1][1]]
                payload = r_message[8:][1]
                if payload[1]:
                    debug("Federate {} has been terminated successfully.".format(payload[0]))
                else:
                    EMAError("Error in terminating the federate {} : ".format(payload[0]), payload[2])
                    
        except ZMQError as e:
            raise EMAError(str(e))
        
    @method_logger   
    def prepare_message(self, run_id=None, receiver=None, message_type=None, 
                        status=None, payload=[]):
        content = []
        content.append(('magic_no', self.magic_no))
        content.append(('sim_run_id', self.sim_run_id+'.'+str(run_id)))
        content.append(('sender_id', self.sender_id))
        content.append(('receiver_id', receiver))
        content.append(('msg_type_id', message_type))
        content.append(('unique_msg_no', self.nr_messages))
        content.append(('msg_status_id', int(status).to_bytes(1, byteorder='big')))
        content.append(('no_fields', len(payload))) # F1: "load federate", F2: directory, F3: file_name
        
        for item in payload:
            content.append(('payload', item))

        return content    

    @method_logger
    def check_received_message(self, message, run_id, expected_type):
        '''
        check magic no
        check receiver id
        check sim_run_id
        
        return message type, error
        '''
        error = False
        wrong_fields = []
        if message[0][1] != self.magic_no: #is it my simulation?
            error = True
            wrong_fields.append(message[0][0])
        if message[3][1] != self.sender_id: #is it intended for me?
            error = True
            wrong_fields.append(message[3][0])
        
        m_run_id = message[1][1].split('.')[1]
        if int(m_run_id) != run_id:
            error = True
            wrong_fields.append(message[1][0])

        if isinstance(expected_type, str):
            if message[4][1] != expected_type:
                error = True
                wrong_fields.append(message[4][0])
        else:
            if message[4][1] not in expected_type:
                error = True
                wrong_fields.append(message[4][0])
        return error, wrong_fields    
        