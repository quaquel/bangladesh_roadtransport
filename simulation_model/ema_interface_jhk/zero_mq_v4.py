'''
Created on 20 feb. 2017

@author: sibeleker

This is module specifies a ModelStructureInterface for controlling a model via ZeroMQ. 
Therefore, the functions do not directLy command a model (e.g. a java model), but sends corresponding messages to ZeroMQ.

'''
from __future__ import (unicode_literals, print_function, division)


import zmq
import time

from random import randint
from zmq.error import ZMQError

from ema_workbench.em_framework.model import AbstractModel, Replicator
from ema_workbench.util import EMAError
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

class ZeroMQModel(Replicator, AbstractModel):
    
    def __init__(self, name, softwareCode, argsBefore, fullPathModelFile, 
                 argsAfter, fs_workingDirectory, redirectStdin, redirectStdout, 
                 redirectStderr, deleteWorkingDirectory, deleteStdin, 
                 deleteStdout, deleteStderr, ip, fs_port, fs_receiver, 
                 m_receiver, magic_no, sim_run_id, sender_id, local_directory):
        '''
        specify the model file etc, does not send any message to zeromq
        
        Parameters
        ----------
        name
        softwareCode
        argsBefore
        fullPathModelFil, 
        argsAfter
        fs_workingDirectory
        redirectStdin
        redirectStdout, 
        redirectStderr
        deleteWorkingDirectory
        deleteStdin, 
        deleteStdout
        deleteStderr
        ip
        fs_port
        fs_receiver, 
        m_receiver
        magic_no
        sim_run_id
        sender_id
        local_directory
        
        name : str
               name of the model, e.g. "WB_transport"
        wd : str
             working directory to the .jar file (wherever the model is)
        model_file : the .jar file to run OR SOMETHING ELSE
        ip : if local, ip = 'localhost' else ip='{remote ip}'
        fs_port : port number of the federate starter to connect
        m_port : port number of the model (that cna be alist for multiple model instances)
        magic_no : the magic number that will be used in the simulations, e.g "SIM01"
        sim_run_id : sim_un_id will show the simulation and replication number, but the root should be e.g. "EMA"
        id : the unique id of this bus component, that will be used as 'sender id', e.g. "EMA01"
         
        '''
        super(ZeroMQModel, self).__init__(name)
        self.model_file = fullPathModelFile
        self.working_directory = local_directory # have to define the working directory, if you want cleanup() to be called in finalization
        self.ip_toconnect = ip
        self.fs_port = fs_port   
        self.fs_receiver = fs_receiver
        self.m_receiver = m_receiver
        
        self.instance_id = None
        
        # ===unique message fields===
        self.magic_no = magic_no
        self.sim_run_id = sim_run_id
        self.sender_id = sender_id
        
        # severytime this ZeroMQModel object sends a message, 
        # this variable will be incremented
        self.no_messages = 0 
        
        # === model instance specifications ===
        self.softwareCode = softwareCode
        self.argsBefore = argsBefore
        self.argsAfter = argsAfter  
        self.fs_workingDirectory = fs_workingDirectory #m_port, 
        self.redirectStdin = redirectStdin 
        self.redirectStdout = redirectStdout
        self.redirectStderr = redirectStderr
        self.deleteWorkingDirectory = deleteWorkingDirectory
        self.deleteStdout = deleteStdout
        self.deleteStdin = deleteStdin
               
                   
    def model_init(self, policy):
        super(ZeroMQModel, self).model_init(policy)
        
        self.context = zmq.Context()  
        self.fs_socket = self.context.socket(zmq.REQ)  # @UndefinedVariable
        #print("in model_init, setting the identity")
        identity = u"%04x-%04x" % (randint(0, 0x10000), randint(0, 0x10000))
        self.fs_socket.setsockopt_string(zmq.IDENTITY, identity) # @UndefinedVariable
        try:
            #print("trying to connect to the federate starter")
            self.fs_socket.connect("tcp://{}:{}".format(self.ip_toconnect, self.fs_port))
            print("connected to the federate starter")
        except ZMQError as e:
            print(e)
        
        self.m_socket = self.context.socket(zmq.REQ) # @UndefinedVariable
        identity = u"%04x-%04x" % (randint(0, 0x10000), randint(0, 0x10000))
        self.m_socket.setsockopt_string(zmq.IDENTITY, identity) # @UndefinedVariable
        
        self.m_receiver = self.m_receiver + str(self.instance_id)  
        self.sender_id = self.sender_id + '.' + str(self.instance_id) 
        
        # ===send the federate starter message===
        payload = [self.m_receiver, self.softwareCode, self.argsBefore, 
                   self.model_file, self.argsAfter, self.fs_workingDirectory, 
                   self.redirectStdin, self.redirectStdout, self.redirectStderr, 
                   self.deleteWorkingDirectory, self.deleteStdout, 
                   self.deleteStdin]
        
        self.FederateStart(payload)
        self.m_socket.connect("tcp://{}:{}".format(self.ip_toconnect, 
                                                   self.m_port))
        
        # ===send the Simulation Run Control message===
        self.SimRunControl()
              
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
        run_id = experiment.name + 1 #to avoid 0
         
        #1) === SETTING THE PARAMETER VALUES ONE BY ONE ===
        for key, value in experiment.items():
            payload = [key, value]
            self.SetValue(run_id, payload)   
         
        #2) === RUN THE SIMULATION ===
        self.StartSimulation(run_id)
             
        # 3) === REQUEST STATUS ===
        wait = True
        while wait:
            wait = self.RequestStatus(run_id)
            time.sleep(2)
         
        #4) === COLLECT THE SIMULATION RESULTS ===
        results = {}
        for outcome in self.outcomes:
            variable = outcome.variable_name[0]
            v_type = type(outcome).__name__.split("O")[0]
             
            results[variable] = self.RequestStatistics(run_id, variable, 
                                                       v_type)
         
        debug('setting results to output')
        self.output = results

    def cleanup(self):
        print("in cleanup {}".format(self.sender_id))
        self.KillFederate()
        self.fs_socket.close()
        self.m_socket.close()
        self.context.term()
        
    def FederateStart(self, payload):
        run_id = 0 # This is normally the experiment number. For federate start, it is 0.
        content = self.prepare_message(run_id=run_id,
                                       receiver=self.fs_receiver, 
                                       type="FM.1",
                                       status=1,
                                       payload=payload)
        message = message_encode(content) 
        # === send the message ===
        try:
            self.fs_socket.send(message)
            self.no_messages += 1
            #print("federate start msg has been sent.")
        except TypeError as e1:
            raise EMAError(str(e1))
        except ZMQError as e2:
            raise EMAError(str(e2))
        
        # ===receive acknowledgement===
        try:
            r_msg = self.fs_socket.recv()
            r_message = message_decode(r_msg)
            print("FederateStarted : ", r_message)
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
                self.m_port = payload[-1] 
        except ZMQError as e:
            debug("model initialization message could not be received.")
            raise EMAError(str(e))
                  
    def SimRunControl(self):
        run_id = 0
        content = self.prepare_message(run_id=run_id,
                                       receiver=self.m_receiver, 
                                       type='FM.2',
                                       status=1,
                                       payload=[constant.value for constant in self.constants])
            
        message = message_encode(content) 
        try:
            self.m_socket.send(message)
            self.no_messages += 1
        except TypeError as e1:
            raise EMAError(str(e1))
        except ZMQError as e2:
            raise EMAError(str(e2))
        
        try:
            r_msg = self.m_socket.recv()
            r_message = message_decode(r_msg)
            print("Ackn for SimRunControl: ", r_message)
            expected_type = "MC.2"
            error, fields = self.check_received_message(r_message, run_id, expected_type)
            if error:
                raise EMAError("Wrong message : the field(s) '{}' does not match.".format(', '.join(fields)))
            else:
                payload = [x[1] for x in r_message[8:]]
                #payload has 3 fields: uniqueid, status, error
                if payload[0] == self.no_messages-1: #unique_message_id, since no other message was sent after the SimRunControl message, this should be equal to the previous value.
                    if payload[1]:
                        debug("Model has been initialized successfully.")
                    else:
                        EMAError("Error in sim run control: ", payload[2])
        except ZMQError as e:
            debug("SimRunControl acknowledgement could not be received.")
            raise EMAError(str(e))
        
    def SetValue(self, run_id, payload):
        content = self.prepare_message(run_id=run_id,
                                           receiver=self.m_receiver, 
                                           type="FM.3",
                                           status=1,
                                           payload=payload)
            
        message = message_encode(content)
       
        try:
            self.m_socket.send(message)
            print("set value ", payload)
            self.no_messages += 1
        except TypeError as e1:
            raise EMAError(str(e1))
        except ZMQError as e2:
            raise EMAError(str(e2))

        try:
            r_msg = self.m_socket.recv()
            r_message = message_decode(r_msg)
            print("Ack SetValue : ",r_message)
            expected_type = "MC.2"
            error, fields = self.check_received_message(r_message, run_id, expected_type)
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
        
    def StartSimulation(self, run_id):
        content = self.prepare_message(run_id=run_id,
                                       receiver=self.m_receiver, 
                                       type="FM.4",
                                       status=1,
                                       payload=[]) 
            
        message = message_encode(content)
        try:
            self.m_socket.send(message)
            print("start simulation has been sent")
            self.no_messages += 1
        except TypeError as e1:
            raise EMAError(str(e1))
        except ZMQError as e2:
            raise EMAError(str(e2))

        try:
            r_msg = self.m_socket.recv()
            r_message = message_decode(r_msg)
            print("Ack start simulation: ", r_message)
            expected_type = "MC.2"
            error, fields = self.check_received_message(r_message, run_id, expected_type)
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
        
    def RequestStatus(self, run_id):
        content = self.prepare_message(run_id=run_id,
                                       receiver=self.m_receiver, 
                                       type="FM.5",
                                       status=1,
                                       payload=[]) 
            
        message = message_encode(content)
        try:
            self.m_socket.send(message)
            print("request status has bee nsent")
            self.no_messages += 1
        except TypeError as e1:
            raise EMAError(str(e1))
        except ZMQError as e2:
            raise EMAError(str(e2))
        
        try:
            r_msg = self.m_socket.recv()
            r_message = message_decode(r_msg)
            print("Status : ", r_message)
            expected_type = "MC.1"
            error, fields = self.check_received_message(r_message, run_id, expected_type)
            if error:
                raise EMAError("Wrong message : the field(s) '{}' does not match.".format(', '.join(fields)))
            else:
                payload = [x[1] for x in r_message[8:]]
                if payload[0] == self.no_messages-1:
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
    
    def RequestStatistics(self, run_id, variable, v_type):
        content = self.prepare_message(run_id=run_id,
                                           receiver=self.m_receiver, 
                                           type="FM.6",
                                           status=1,
                                           payload=[variable])
            
        message = message_encode(content)        
        try:
            self.m_socket.send(message)
            self.no_messages += 1
        except TypeError as e1:
            raise EMAError(str(e1))
        except ZMQError as e2:
            raise EMAError(str(e2))
        
        #receive the output
        try:
            r_msg = self.m_socket.recv()
            r_message = message_decode(r_msg)
            print("Statistics : ", r_message)
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
        
    def SimulationReset(self):
        #do something here
        pass
    
    def KillAll(self):
        run_id = 0
        content = self.prepare_message(run_id=run_id,
                                       receiver=self.fs_receiver, 
                                       type="FM.9",
                                       status=1,
                                       payload=[])
        message = message_encode(content)   
        try:
            self.fs_socket.send(message)
            self.no_messages += 1
        except TypeError as e1:
            raise EMAError(str(e1))
        except ZMQError as e2:
            raise EMAError(str(e2))
        try:
            r_msg = self.fs_socket.recv()
            r_message = message_decode(r_msg)
            print("Federate killed : ", r_message)
            expected_type = "FS.5"
            error, fields = self.check_received_message(r_message, run_id, expected_type)
            if error:
                raise EMAError("Wrong message : the field(s) '{}' does not match.".format(', '.join(fields)))
            else:
                #payload = [r_message[-3][1], r_message[-2][1], r_message[-1][1]]
                payload = r_message[8:][1]
                if payload[0]:
                    debug("Federate has been terminated successfully.")
                else:
                    EMAError("Error in terminating the federate: ", payload[1])
                    
        except ZMQError as e:
            raise EMAError(str(e))
    
    def KillFederate(self):
        run_id = 0
        content = self.prepare_message(run_id=run_id,
                                       receiver=self.fs_receiver, 
                                       type="FM.8",
                                       status=1,
                                       payload=[self.m_receiver])
        
        message = message_encode(content)   
        try:
            self.fs_socket.send(message)
            self.no_messages += 1
        except TypeError as e1:
            raise EMAError(str(e1))
        except ZMQError as e2:
            raise EMAError(str(e2))
        try:
            r_msg = self.fs_socket.recv()
            r_message = message_decode(r_msg)
            expected_type = "FS.4"
            error, fields = self.check_received_message(r_message, run_id, expected_type)
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
            
    def prepare_message(self, run_id=None, receiver=None, type=None, status=None, payload=[]):
        content = []
        content.append(('magic_no', self.magic_no))
        content.append(('sim_run_id', self.sim_run_id+'.'+str(run_id)))
        content.append(('sender_id', self.sender_id))
        content.append(('receiver_id', receiver))
        content.append(('msg_type_id', type))
        content.append(('unique_msg_no', self.no_messages))
        content.append(('msg_status_id', int(status).to_bytes(1, byteorder='big')))
        content.append(('no_fields', len(payload))) # F1: "load federate", F2: directory, F3: file_name
        
        for item in payload:
            content.append(('payload', item))
        #print(content[1])
        return content    

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
                wrong_fields.append(message[4][0])
                error = True
        else:
            if message[4][1] not in expected_type:
                wrong_fields.append(message[4][0])
                error = True
        return error, wrong_fields    
        



# if __name__ == "__main__":       
#     ema_logging.log_to_stderr(ema_logging.INFO)
#     Process(target=Server, args=("*", "5556", )).start()
#     time.sleep(2)
#     ip = 'localhost'
#     port = '5556'
#     model = ZeroMQModel("lakemodel", "model.jar", ip, port)  
#     model.uncertainties = [RealParameter('b', 0.1, 0.45),
#                            RealParameter('q', 2.0, 4.5)]
#     model.outcomes = [ScalarOutcome('max_P',),
#                       ScalarOutcome('utility')]
#     #input = {'b' : 0.4, 'q' : 3}
#     #output = model.run_model(scenario=Scenario(**input), policy=Policy({})) 
#     #print("output ", output)
#     model.time_horizon = 5
# 
#     model.levers = [RealParameter(str(i), 0, 0.1) for i in 
#                          range(model.time_horizon)]
# 
#     policies = samplers.sample_levers(model, 2, 
#                                       sampler=samplers.MonteCarloSampler(),
#                                       name=util.counter)
#     
#     # perform experiments
#     nr_experiments = 2
#     
#     results = perform_experiments(model, nr_experiments, 
#                                   policies, parallel=False)
#     fn = './zero_mq_trial.tar.gz'
#     save_results(results, fn)
#     experiment, outcomes = results
#     print(outcomes['max_P'])  
#     