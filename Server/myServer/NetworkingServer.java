package myServer;
import java.io.IOException;
import ray.networking.IGameConnection.ProtocolType;;

public class NetworkingServer {
    private GameServerUDP thisUDPServer;
    // private GameServerTCP thisTCPServer;

    public NetworkingServer(int serverPort, String protocol) {
        try {
            thisUDPServer = new GameServerUDP(serverPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length > 1) {
            int serverPort = Integer.parseInt(args[0]);
            String protocol = args[1];
            // NetworkingServer app = new NetworkingServer(serverPort, protocol);
            new NetworkingServer(serverPort, protocol);
        }
    }
}
