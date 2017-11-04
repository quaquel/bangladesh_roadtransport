'''


'''
from __future__ import (unicode_literals, print_function, absolute_import,
                        division)
import logging
import multiprocessing
import sys

from zmq.eventloop import ioloop, zmqstream
import zmq

from message_v2 import message_encode, message_decode
from _tracemalloc import stop
# Created on 30 Sep 2017
#
# .. codeauthor::jhkwakkel <j.h.kwakkel (at) tudelft (dot) nl>

__all__ = []

class ZmqProcess(multiprocessing.Process):
    """
    This is the base for all processes and offers utility functions
    for setup and creating new streams.

    """
    def __init__(self):
        super().__init__()

        self.context = None
        """The ØMQ :class:`~zmq.Context` instance."""

        self.loop = None
        """PyZMQ's event loop (:class:`~zmq.eventloop.ioloop.IOLoop`)."""


    def setup(self):
        """
        Creates a :attr:`context` and an event :attr:`loop` for the process.

        """
        self.context = zmq.Context()
        self.loop = ioloop.IOLoop.instance()


    def stream(self, sock_type, addr, bind, callback=None, subscribe=b''):
        """
        Creates a :class:`~zmq.eventloop.zmqstream.ZMQStream`.

        :param sock_type: The ØMQ socket type (e.g. ``zmq.REQ``)
        :param addr: Address to bind or connect to formatted as *host:port*,
                *(host, port)* or *host* (bind to random port).
                If *bind* is ``True``, *host* may be:

                - the wild-card ``*``, meaning all available interfaces,
                - the primary IPv4 address assigned to the interface, in its
                numeric representation or
                - the interface name as defined by the operating system.

                If *bind* is ``False``, *host* may be:

                - the DNS name of the peer or
                - the IPv4 address of the peer, in its numeric representation.

                If *addr* is just a host name without a port and *bind* is
                ``True``, the socket will be bound to a random port.
        :param bind: Binds to *addr* if ``True`` or tries to connect to it
                otherwise.
        :param callback: A callback for
                :meth:`~zmq.eventloop.zmqstream.ZMQStream.on_recv`, optional
        :param subscribe: Subscription pattern for *SUB* sockets, optional,
                defaults to ``b''``.
        :returns: A tuple containg the stream and the port number.

        """
        sock = self.context.socket(sock_type)
        

        # addr may be 'host:port' or ('host', port)
        if isinstance(addr, str):
            addr = addr.split(':')
        host, port = addr if len(addr) == 2 else (addr[0], None)

        # Bind/connect the socket
        if bind:
            if port:
                sock.bind('tcp://%s:%s' % (host, port))
            else:
                port = sock.bind_to_random_port('tcp://%s' % host)
        else:
            sock.connect('tcp://%s:%s' % (host, port))

        # Add a default subscription for SUB sockets
        if sock_type == zmq.SUB:  # @UndefinedVariable
            sock.setsockopt(zmq.SUBSCRIBE, subscribe)  # @UndefinedVariable

        # Create the stream and add the callback
        stream = zmqstream.ZMQStream(sock, self.loop)
        if callback:
            stream.on_recv(callback)

        return stream, int(port)


class FederateStarter(ZmqProcess):
    """
    Main processes for the Ponger. It handles ping requests and sends back
    a pong.

    """
    def __init__(self, federation_name, magic_nr, fm_port, fs_name, min_port,
                 max_port):
        super(FederateStarter, self).__init__()
        
        self.federation_name = federation_name
        self.magic_nr = magic_nr
        self.fm_port = fm_port
        self.fs_name = fs_name 
        self.min_port = min_port
        self.max_port = max_port
        self.no_messages = 0

        self.fm_stream = None
        self.model_stream = {} # must be a map
        self.initialize_logger()

    def initialize_logger(self):
        '''helper method for initializing logging'''
        
        logging.basicConfig(level=logging.INFO)
        logger = logging.getLogger(__name__)
        
        # create a file handler
#         handler = logging.StreamHandler(sys.stdout)
        handler = logging.FileHandler('federatestarter.log')
        handler.setLevel(logging.INFO)
        
        # # create a logging format
        formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
        handler.setFormatter(formatter)
        
        # add the handlers to the logger
        logger.addHandler(handler)
        logger.propagate = False
        self.log = logger

    def setup(self):
        """Sets up PyZMQ and creates all streams."""
        super().setup()

        # Create the stream and add the message handler
        self.fm_stream, _ = self.stream(zmq.ROUTER, 
                                        ('127.0.0.1', self.fm_port), 
                                        bind=True)
        self.fm_stream.on_recv(FMStreamHandler(self.log, 
                                                self.fm_stream, self.stop))
        self.log.info('federate starter running on port {}'.format(self.fm_port))

    def run(self):
        """Sets up everything and starts the event loop."""
        self.setup()
        self.loop.start()

    def stop(self):
        """Stops the event loop."""
        self.loop.stop()


class MessageHandler(object):
    """
    Base class for message handlers for a :class:`ZMQProcess`.

    Inheriting classes only need to implement a handler function for each
    message type.

    """
    def __init__(self, logger,stream, stop):
        object.__init__(self)
        self.logger = logger
        self._stop = stop
        self.stream = stream
        
    
    def __call__(self, msg):
        """
        Gets called when a messages is received by the stream this handlers is
        registered at. *msg* is a list as return by
        :meth:`zmq.core.socket.Socket.recv_multipart`.

        """
        # Try to JSON-decode the index "self._json_load" of the message
        address, _, b_message = msg
        message = message_decode(b_message)
        self.logger.info(message)
        
        message_type = message[4][1]
        print(message_type)
#         getattr(self, msg_type)(*msg)

class FMStreamHandler(MessageHandler):
    
    def handle_fm1(self):
        pass
    
    def handle_fm8(self):
        pass
     


def prepare_message(magic_no, sim_run_id, run_id, sender_id, fs_name, 
                    message_type, nr_messages, status, payload=[]):
        content = []
        content.append(('magic_no', magic_no))
        content.append(('sim_run_id', sim_run_id+'.'+str(run_id)))
        content.append(('sender_id', sender_id))
        content.append(('receiver_id', fs_name))
        content.append(('msg_type_id', message_type))
        content.append(('unique_msg_no', nr_messages))
        content.append(('msg_status_id', int(status).to_bytes(1, byteorder='big')))
        content.append(('no_fields', len(payload))) 
        
        for item in payload:
            content.append(('payload', item))

        return content    
if __name__ == '__main__':
    fm_port = 5555
    fs_name = "FS"
    magic_nr = "SIM01"
    min_port = 5000
    max_port = 6000
    
    fs = FederateStarter('federation_name', magic_nr, fm_port, 
                         fs_name, min_port, max_port)
    fs.start()
    
    # 
    ctx = zmq.Context()
    sock = ctx.socket(zmq.REQ)
    sock.connect('tcp://127.0.0.1:{}'.format(fm_port))
    
    identity = '1234'
    content = prepare_message(magic_nr, 'EMA', 1, identity,
                              fs_name, "FM.1", 1, 1, 
                              [])
    message = message_encode(content) 
    sock.send(message)
    