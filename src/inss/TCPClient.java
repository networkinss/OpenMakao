package inss;

import java.net.*;

/* For checking the port */
class TCPClient {
	private Socket server = null;

	protected boolean ConnectTo(String host, int port) {
		boolean canConnect = false;
		try {
			server = new Socket(InetAddress.getByName(host), port);
			canConnect = true;
		}catch(Exception e){
			if(OpenMakao.debug >= 3){
				e.printStackTrace();
			}
		}finally{
			try{
				if(null != server){
					server.close();
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return canConnect;
	}
}
