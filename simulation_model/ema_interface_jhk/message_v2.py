'''
Created on 6 mrt. 2017

@author: sibeleker
'''
import struct
from zmq import ZMQError

from ema_workbench.util import EMAError
import numpy as np

type_code = {#scalar types
            'bytes'    : 0,
            'int16'    : 1,
            'int32'    : 2,
            'int64'    : 3,
            'int'      : 3, 
            'float32'  : 4,
            'float64'  : 5,
            'float'    : 5,
            'bool'     : 6,
            'char_8'   : 7,
            'char_16'  : 8,
            'str'      : 9,
            'str_16'   : 10,
            #array types 
            'bytearray'     : 11,
            'int16_array'   : 12,
            'int32_array'   : 13,
            'int64_array'   : 14,
            'float32_array' : 15,
            'float64_array' : 16,
            'ndarray'       : 16,
            'boolean_array' : 17,
            #matrix types
            'byte_matrix'    : 18,
            'int16_matrix'   : 19,
            'int32_matrix'   : 20,
            'int64_matrix'   : 21,
            'float32_matrix' : 22,
            'float64_matrix' : 23,
            'boolean_matrix' : 24,
            #unit types
            'float32_unit'         : 25,
            'float64_unit'         : 26,
            'float32_unit_array'   : 27,
            'float64_unit_array'   : 28,
            'float32_unit_matrix'  : 29,
            'float64_unit_matrix'  : 30,
            'float32_unit2_matrix' : 31,
            'float64_unit2_matrix' : 32
            }

field_types = {'magic_no'      : 'str',
               'sim_run_id'    : 'str',
               'sender_id'     : 'str',
               'receiver_id'   : 'str',
               'msg_type_id'   : 'str',
               'msg_status_id' : 'bytes', #but int for a bytearray
               'unique_msg_no' : 'int64', 
               'no_fields'     : 'int16'}
  
field_names = ['magic_no', 'sim_run_id',
               'sender_id', 'receiver_id',
               'msg_type_id', 'unique_msg_no',
               'msg_status_id', 'no_fields', 'payload']    
    
content = [('magic_no', "SIM01"), #10
           ('sim_run_id', "IDVV.14.2"),   #14
           ('sender_id', "EMA.1"),        #10  
           ('receiver_id', "MC.1"),       #9
           ('msg_type_id', "DSOL.3"),     #11
           ('unique_msg_no', 1234),
           ('msg_status_id', int(2).to_bytes(1, byteorder='big')),         
           ('no_fields', 1),              #3
           ('payload', 0.5),
           ('payload', 23),
           ('payload', True)]
           #('payload', np.array([np.int16(5502), 0.65, 0.123]))]              #9   



def message_encode(content):    
    '''
    takes a list of tuples [(field_name, field_value)]
    the order of the elements in the list is fixed - based on the protocol with Alexander
    returns a bytearray
    '''
    message = bytearray()
    
    for field in content:

        key = field[0]
        value = field[1]
        
        try:
            tp = type_code[field_types[key]]
        except KeyError:
            if key == 'payload':
                #print("payload type", value.__class__.__name__)
                tp = type_code[value.__class__.__name__]
        
        if tp == 0:
            message.append(0)
            message.extend(value) # if type==byte, no need for conversion
            
        elif tp == 1:
            message.append(1)
            v = int(value).to_bytes(2, byteorder='big')
            message.extend(v) 
            
        elif tp == 2:
            message.append(2)
            v = value.to_bytes(4, byteorder='big')
            message.extend(v)
            
        elif tp ==3:
            message.append(3)
            v = value.to_bytes(8, byteorder='big')
            message.extend(v)
            
        elif tp == 4:
            message.append(4)
            s = struct.pack('>f', value)
            #s = struct.unpack('>i', s) #unpack returns a tuple, so take the first item
            #v = s[0].to_bytes(4, byteorder='big')
            #message.extend(v)
            message.extend(s)
            
        elif tp == 5:
            message.append(5)
            s = struct.pack('>d', value)
            #s = struct.unpack('>l', s)
            #v = s[0].to_bytes(8, byteorder='big')
            #message.extend(v)
            message.extend(s)
        elif tp == 6:
            message.append(6)
            v = struct.pack('>?', value)
            message.extend(v)
            
        elif tp == 9:
            message.append(9)
            l = len(value).to_bytes(4, byteorder='big')
            message.extend(l)
            for i, c in enumerate(value.encode('UTF-8')):
                message.append(c)
#         elif tp == 10:
#             message.append(10)
#             l = len(value).to_bytes(4, byteorder='big')
#             message.extend(l)
#             for c in enumerate(value.encode('UTF-16')):
#                 message.append(c)

        elif tp == 16:
            message.append(16)
            l = len(value).to_bytes(4, byteorder='big') #array_length as 32-bit integer
            message.extend(l)
            for j in range(len(value)):
                message.extend(struct.pack('>d', value[j])) #convert each double to bytes and append
        else:
            raise ZMQError("Unknown data type " + value.__class__.__name__ + " for encoding the ZeroMQ message")
    
    return message
def message_decode(message):
    '''
    takes a bytearray
    returns a list of tuples (field_name, field_value)
    '''
    content = []
    msg_size = len(message)
    no_fields = 9
    i = 0
    j = 0
    while j < no_fields:
        tp = message[i]
        try:
            key = field_names[j]
        except IndexError:
            key = field_names[-1]
        
        try:
            if key == 'no_fields':
                exp_tp = [type_code['int16'], type_code['int32'], type_code['bytes']]
                if tp not in exp_tp:
                    print(key, tp, exp_tp)
                    raise EMAError("The type code of the message field {} does not match.".format(key))

            else:
                exp_tp = type_code[field_types[key]]
                if tp != exp_tp:
                    print(key, tp, exp_tp)
                    raise EMAError("The type code of the message field {} does not match.".format(key))
        except KeyError:
            if key == 'payload':
                pass 
        
        if tp == 0: #byte
            size = 1
            v = message[(i+1):(i+1+size)]
            #value = int.from_bytes(v, byteorder='big')
            content.append((key,v))
  
        elif tp == 1: #short
            size = 2
            v = message[(i+1):(i+1+size)]
            value = int.from_bytes(v, byteorder='big')
            content.append((key,value))
            
        elif tp == 2: #int32
            size = 4
            v = message[(i+1):(i+1+size)]
            value = int.from_bytes(v, byteorder='big')
            content.append((key,value))
            
        elif tp ==3: #int64
            size = 8
            v = message[(i+1):(i+1+size)]
            value = int.from_bytes(v, byteorder='big')
            content.append((key,value))
            
        elif tp == 4: #float32
            size = 4
            v = message[(i+1):(i+1+size)]
            value = struct.unpack('>f', v)[0]
            content.append((key,value))
            
        elif tp == 5: #float64
            size = 8
            v = message[(i+1):(i+1+size)]
            value = struct.unpack('>d', v)[0]
            content.append((key,value))
            
        elif tp == 6: #bool
            size = 1
            v = message[(i+1):(i+1+size)]
            value = struct.unpack('>?', v)[0]
            content.append((key,value))
            #value = v.decode()
            #content.append((key,value))
            
        elif tp == 7: #char-8
            size = 1
            v = message[(i+1):(i+1+size)]
            value = v.decode() #decode('UTF-8')
            content.append((key, value))
            
        elif tp == 8:
            size = 2
            v = message[(i+1):(i+1+size)]
            value = v.decode() # decode('UTF-16')
            content.append((key, value))
            
        elif tp == 9: #string
            l_size = 4
            v = message[(i+1):(i+1+l_size)]
            size = int.from_bytes(v, byteorder='big')
            i += l_size
            v = message[(i+1):(i+1+size)]
            value = v.decode()
            content.append((key,value))
        
        elif tp == 10: #16-bit string
            l_size = 4
            v = message[(i+1):(i+1+l_size)]
            size = int.from_bytes(v, byteorder='big')
            i += l_size
            v = message[(i+1):(i+1+size)]
            value = v.decode('UTF-16')
            content.append((key,value))
            
        elif tp == 16: #double array
            l_size = 4
            size = 8 #64-bit
            v = message[(i+1):(i+1+l_size)]
            array_length = int.from_bytes(v, byteorder='big') 
            value = np.zeros(array_length)
            i += l_size
            for j in range(array_length):
                v = message[(i+1):(i+1+size)]
                value[j] = struct.unpack('>d', v)[0]   
                if j != array_length-1:
                    i = i+size
            content.append((key,value))
            
                  
        else:
            raise ZMQError("Unknown type code " + str(tp) + " for decoding the ZeroMQ message")
        if key == 'no_fields' and value > 1:
            no_fields += value-1
        j += 1   
        i = i+1+size
        
        if i == msg_size:
            break
        
    return content

if __name__ == "__main__":       

    #Process(target=Server, args=("*", "5555", )).start()
    
    #context = zmq.Context()       
    #client_socket = context.socket(zmq.REQ)
    
    #client_socket.connect("tcp://130.161.3.179:5556")
    #print("connected to the socket")
    #client_socket.send_string("hello server")
    
    message = message_encode(content)
    #client_socket.send(message)
    
    #r_msg = client_socket.recv()
    #print("client received: ", r_msg)
    #client_socket.send(message_encode(content))
    #r_msg = client_socket.recv()
    r_message = message_decode(message)
    print("client received: ", message)
    print("in other words: ", r_message)
    #a = np.int16(5502)
    #print(a.__class__.__name__)
    payload = r_message[8:][1]
    print(payload)
    #client_socket.send_string("kill yourself")
    
    #client_socket.close()
    #context.term()




