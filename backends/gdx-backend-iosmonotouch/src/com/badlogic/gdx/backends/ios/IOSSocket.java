package com.badlogic.gdx.backends.ios;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import cli.System.Net.Sockets.LingerOption;
import cli.System.Net.Sockets.NetworkStream;
import cli.System.Net.Sockets.TcpClient;

import com.badlogic.gdx.Net.Protocol;
import com.badlogic.gdx.net.Socket;
import com.badlogic.gdx.net.SocketHints;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * The iOS socket implementation using System.Net.Sockets.TcpClient (Microsoft).
 * 
 * @author noblemaster
 */
public class IOSSocket implements Socket {

	/** Our server if the socket was created via server socket. null if it's a client socket only. */
	private IOSServerSocket server;

	/** Our client or null for disposed, aka closed. */
	private TcpClient client;
	
	private NetworkStream stream;
	private IOSStreamInput inputStream;
	private IOSStreamOutput outputStream;
	
	
	public IOSSocket(Protocol protocol, String host, int port, SocketHints hints) {
		if (protocol == Protocol.TCP) {
			try {
				// create and connect the socket
				// NOTE: there is no connection timeout setting available - will assume there is some sort of default!?
				client = new TcpClient(host, port);
				setupConnection(hints); 
			}
			catch (Exception e) {
				throw new GdxRuntimeException("Error making a socket connection to " + host + ":" + port, e);
			}
		}
		else {
			throw new GdxRuntimeException("Socket protocol " + protocol + " is not supported under iOS backend.");
		}
	}
	
	public IOSSocket(IOSServerSocket server, TcpClient client, SocketHints hints) {
		this.server = server;
		this.client = client;
		setupConnection(hints);
	}
	
	private void setupConnection(SocketHints hints) {
		// apply hints as needed
		if (hints != null) {
			try {	
				// NOTE: traffic parameter settings/class cannot be set via iOS
				client.set_NoDelay(hints.tcpNoDelay);
				client.set_SendTimeout(hints.keepAlive ? 0 : 30000);  // milliseconds -> 0=no timeout
				client.set_SendBufferSize(hints.sendBufferSize);
				client.set_ReceiveTimeout(hints.keepAlive ? 0 : 30000);  // milliseconds -> 0=no timeout
				client.set_ReceiveBufferSize(hints.receiveBufferSize);
				client.set_LingerState(new LingerOption(hints.linger, hints.lingerDuration));
			}
			catch (Exception e) {
				throw new GdxRuntimeException("Error setting socket hints." , e);
			}
		}
		
		// create our streams!
		stream = client.GetStream();
		inputStream = new IOSStreamInput(stream, false);
		outputStream = new IOSStreamOutput(stream, false);
	}
	
	@Override
	public boolean isConnected () {
		if (client != null) {
			return client.get_Connected();
		}
		else {
			return false;
		}
	}

	@Override
	public InputStream getInputStream () {
		return inputStream;
	}

	@Override
	public OutputStream getOutputStream () {
		return outputStream;
	}

	@Override
	public void dispose() {
		// close stream
	   if (stream != null) {
			try {
				stream.Close();
			}
			catch (Exception e) {
				throw new GdxRuntimeException("Error closing stream.", e);
			}
			stream = null;
	   }
		   
		// remove from server as needed
		if (server != null) {
			try {
				server.dispose(this);
			}
			catch (Exception e) {
				throw new GdxRuntimeException("Error closing socket on server.", e);
			}
			server = null;
		}
			
		// dispose client
		if (client != null) {
			try {
				client.Close();
				client.Dispose();
			}
			catch (Exception e) {
				throw new GdxRuntimeException("Error closing socket.", e);
			}
			client = null;
		}
	}
}
