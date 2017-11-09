'''
Created on 19 apr. 2017

@author: sibeleker
'''
from __future__ import unicode_literals

import logging
import random
import shutil
import subprocess
import sys
import time
import uuid
import zmq

from zmq.error import ZMQError

from message_v2 import message_encode, message_decode


class FederateStarter(object):
    
    def __init__(self, federation_name, magic_no, fm_port, sender_id, min_port,
                 max_port):
        '''
        
        Parameters
        -----------
        federation name : str
                          an identifier for the federation
        fm_port : int
                  port number to which the federation manager(s) will connect
        sender_id : str
                    the sender id of the federate starter to be used in the 
                    messages, in string format, e.g. FS
        min_port : int
                   lower bound of the port number range from which the federate
                   starter will select one for each model instance
        max_port : int
                   upper bound of the port number range from which the federate 
                   starter will select one for each model instance
        
        '''
        self.initialize_logger()
        
        self.federation_name = federation_name
        self.magic_no = magic_no
        self.fm_port = fm_port
        self.sender_id = sender_id 
        self.min_port = min_port
        self.max_port = max_port
        self.nr_messages = 0

        self.portsinuse = set()
        self.model_processes = {} 
        
        self.context = zmq.Context()   
        self.fm_socket = self.context.socket(zmq.ROUTER)  # @UndefinedVariable
        self.fm_socket.bind("tcp://*:{}".format(fm_port))
        self.log.info("Running the Federate Starter on port: %s" % fm_port)
        self.loop()
        
    def send(self, message, socket, address=None):
        if address is None:
            socket.send(message)
        else:
            socket.send_multipart([address, b'', message])
        self.nr_messages += 1

    def loop(self):
        while True:
            address, _, b_message = self.fm_socket.recv_multipart()
            message = message_decode(b_message)        
            
            #checking if the message arrived at the right place
            self.check_received_message(message, ["FM.1", "FM.8"]  ) 

            message_type = message[4][1]
            sender = message[2][1]
            self.log.info("{} received from {}".format(message_type, sender))
            
            if message_type == "FM.1": #FEDERATE START MESSAGE
                self._handle_fm1(address, message)
            elif message_type == "FM.8": #FEDERATE KILL 
                self._handle_fm8(address, message)
            else:
                ValueError("unknown message type: {}".format(message_type))
            self.log.info("{} handled for {} \n".format(message_type, sender))


    def initialize_logger(self):
        '''helper method for initializing logging'''
        
        logging.basicConfig(level=logging.INFO)
        logger = logging.getLogger(__name__)
        
        # create a file handler
#         handler = logging.StreamHandler(sys.stdout)
        handler = logging.FileHandler('federatestarter.log', mode='w')
        handler.setLevel(logging.INFO)
        
        # # create a logging format
        formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
        handler.setFormatter(formatter)
        
        # add the handlers to the logger
        logger.addHandler(handler)
        logger.propagate = False
        self.log = logger


    def _handle_fm1(self, address, message):
        simrunid = message[1][1] 
        fm_id = message[2][1]
        payload = [x[1] for x in message[8:]]
    
        # === instantiate a model ===
        model_id, m_port = self.start_federate(simrunid, payload)
    
        # === send a message back to the federate manager ===       
        content = self.prepare_message(sim_run_id=simrunid, 
                                       receiver=fm_id, 
                                       message_type='FS.2',
                                       status=1,
                                       payload=[model_id, 'started', 
                                                m_port, ''])
                    
        message = message_encode(content)
        self.send(message, self.fm_socket, address)
        self.log.info("model started successfully")
        
    def _handle_fm8(self, address, message):
        payload = message[8][1] # instance id to be killed
        sim_run_id = message[1][1] 
        fm_id = message[2][1]
        self.kill_federate(payload, sim_run_id, fm_id, address)

                              
    def start_federate(self, simrunid, payload):
        softwareCode = payload[1]
        args_before = payload[2] # TODO:: split on space
        model_file = payload[3]
        
        # may need to be changed depending on what the model initialization 
        # requires
        args_after = payload[4].split(' ') 

        # TODO:: should be part of the attributes of the federate
        # might be different between federates
        working_directory = payload[5]
        # redirectStdin = payload[6]
        redirectStdout = payload[7]
        redirectStderr = payload[8]
        delete_working_directory = payload[9]
        #deleteStdout = payload[10]
        #deleteStdin = payload[11]
        
        instance_id = args_after[0]
        
        #assign a port number
        m_port = self.get__random_port()
        self.portsinuse.add(m_port)     
        
        #the chosen port can be seized by another process in the meantime    
        identity = str(uuid.uuid4())
        
        #instantiate the model
        #args after to include the input data directory
        #data_dir = argsAfter[-1]
        with open(redirectStdout, 'w') as f1, open(redirectStderr, 'w') as f2:
            try:
                args = [softwareCode, args_before, '-Xmx4G', model_file, 
                        str(instance_id), str(m_port), args_after[-1]]
                process = subprocess.Popen(args, stdout=f1, stderr=f2)
            except Exception as e:
                self.log.info("Error in {} {}: ".format(instance_id, e))
                raise
            else:
                self.log.info("started process pid: {}, "
                              "listening on {}".format(process.pid, m_port))

        m_socket = self.context.socket(zmq.REQ)  # @UndefinedVariable
        m_socket.setsockopt_string(zmq.IDENTITY, identity) # @UndefinedVariable
        m_socket.connect("tcp://localhost:{}".format(m_port))   

        self.wait_for_started_model(simrunid, instance_id, m_socket)
        
        m_socket.close()
        
        #add the model id to the list
        self.model_processes[instance_id] = (process, m_port, 
                                    working_directory,delete_working_directory)

        return instance_id, m_port


    def wait_for_started_model(self, simrunid, receiverid, socket):
        #ask the status of the model with FS.1 message
        time.sleep(5)
        content = self.prepare_message(sim_run_id=simrunid, 
                               receiver=receiverid, 
                               message_type='FS.1',
                               status=1,
                               payload=[])
        message = message_encode(content)
        
        #receive status
        while True:
            self.send(message, socket)
            bmsg = socket.recv()  # @UndefinedVariable
            message = message_decode(bmsg)
            self.check_received_message(message, "MC.1")

            payload = [x[1] for x in message[8:]]
            status = payload[1]
            if status == 'started':
                break    
            else:
                time.sleep(1)        

    def get__random_port(self):
        while True:
            m_port = random.randint(self.min_port, self.max_port) 
            
            if m_port in self.portsinuse: 
                continue
            
            #check if it is in use:
            dummy_socket = self.context.socket(zmq.REP)  # @UndefinedVariable
            try:
                dummy_socket.bind('tcp://127.0.0.1:{}'.format(m_port))
            except ZMQError as e:
                self.log.info("Trying to assign port {} but {}".format(m_port, str(e)))
            else:
                dummy_socket.unbind('tcp://127.0.0.1:{}'.format(m_port))
                dummy_socket.close()
                return m_port

             
    def kill_federate(self, model_id, sim_run_id, fm_id, address):
        try:
            process, port, wd, remove = self.model_processes.pop(model_id)
        except KeyError:
            raise ValueError(("Model {} is unknown to the "
                              "FederateStarter").format(model_id))
        else:
            #send an fs.3 message to the model as a REQ with id
            content = self.prepare_message(sim_run_id=sim_run_id, 
                                       receiver=model_id, 
                                       message_type='FS.3',
                                       status=1,
                                       payload=[])
            message = message_encode(content)
            model_killed = False
            

            socket = self.context.socket(zmq.REQ)  # @UndefinedVariable
#             m_socket.setsockopt_string(zmq.IDENTITY, identity) # @UndefinedVariable
            socket.connect("tcp://localhost:{}".format(port))            
            
            try:            
                self.send(message, socket)
            except ZMQError:
                raise
            else:
                model_killed = True
                self.log.info(("killed federate {}, " 
                               "listening on port").format(model_id, port))
            
            time.sleep(3)
            #check if the model is running, if so, kill forcibly
            alive = process.poll()
            
            if alive is None:
                self.log.info("forcefully trying to kill process")
                process.kill()
                model_killed = True
            #clean the directory
            if remove:
                shutil.rmtree(wd)
            
            socket.close()
            self.portsinuse.remove(port)
            
            #send a message FS.4 to the federate manager
            content = self.prepare_message(sim_run_id=sim_run_id, 
                                       receiver=fm_id, 
                                       message_type='FS.4',
                                       status=1,
                                       payload=[model_id, model_killed, ''])
        
            message = message_encode(content)
            self.send(message, self.fm_socket, address)


    def prepare_message(self, sim_run_id=None, receiver=None, 
                        message_type=None, status=None, payload=[]):
        '''
        
        Parameters
        ----------
        sim_run_id :
        receiver :
        message_type : str, {'FS.1', 'FS.2', 'FS.3', 'FS.4'}
        status :
        payload :
        
        
        '''
        content = []
        content.append(('magic_no', self.magic_no))
        content.append(('sim_run_id', sim_run_id))
        content.append(('sender_id', self.sender_id))
        content.append(('receiver_id', receiver))
        content.append(('msg_type_id', message_type))
        content.append(('unique_msg_no', self.nr_messages))
        content.append(('msg_status_id', int(status).to_bytes(1, byteorder='big')))
        content.append(('no_fields', len(payload))) # F1: "load federate", F2: directory, F3: file_name
        
        for item in payload:
            content.append(('payload', item))
        return content   


    def check_received_message(self, message, expected_type):
        '''
        
        Parameters
        ----------
        message : str
        expected_type : str
        
        
        Return
        ------
        bool, list of str
        
        Raises
        ------
        ValueError if check fails
        
        '''
        error = False
        wrong_fields = []
        if message[0][1] != self.magic_no: #is it my simulation?
            error = True
            wrong_fields.append(message[0][0])
        if message[3][1] != self.sender_id: #is it intended for me?
            error = True
            wrong_fields.append(message[3][0])
    
        if isinstance(expected_type, str):
            if message[4][1] != expected_type:
                wrong_fields.append(message[4][0])
                error = True
        else:
            if message[4][1] not in expected_type:
                wrong_fields.append(message[4][0])
                error = True
                
        if error:
            raise ValueError(("Wrong message : the field(s) '{}' " 
                              "does not match.").format(', '.join(wrong_fields)))



#     def find_free_port(self):
#         #http://stackoverflow.com/questions/1365265/on-localhost-how-to-pick-a-free-port-number
#         tcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
#         tcp.bind(('', 0))
#         _, port = tcp.getsockname()
#         tcp.close()
#         return port 
    
    
if __name__ == "__main__":  
    ip = 'localhost'
    fs_port = '5555'
    min_port = 5000
    max_port = 6000
    FederateStarter('federation_name', "SIM01", fs_port, 'FS', min_port, max_port)