package myServer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import ray.networking.server.GameConnectionServer;
import ray.networking.server.IClientInfo;

public class GameServerUDP extends GameConnectionServer<UUID> {

    public GameServerUDP(int localPort) throws IOException {
        super(localPort, ProtocolType.UDP);
    }

    /**
     * Receives messages from clients
     */
    @Override
    public void processPacket(Object obj, InetAddress senderIP, int senderPort) {
        String message = (String) obj;
        String[] messageTokens = message.split(",");

        if (messageTokens.length > 0) {
            // case: server receives JOIN message
            // format: join,localid
            if (messageTokens[0].compareTo("join") == 0) {
                try {
                    IClientInfo clientInfo =
                            getServerSocket().createClientInfo(senderIP, senderPort);
                    UUID clientID = UUID.fromString(messageTokens[1]);
                    addClient(clientInfo, clientID);
                    sendJoinedMessage(clientID, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // case: server receives CREATE message
            // format: create, localid, x, y, z
            if (messageTokens[0].compareTo("create") == 0) {
                UUID clientID = UUID.fromString(messageTokens[1]);
                String[] pos = {messageTokens[2], messageTokens[3], messageTokens[4]};
                sendCreateMessages(clientID, pos);
                sendWantsDetailsMessages(clientID);
            }
            // case: server receives BYE message
            // format: bye,localid
            if (messageTokens[0].compareTo("bye") == 0) {
                UUID clientID = UUID.fromString(messageTokens[1]);
                sendByeMessages(clientID);
                removeClient(clientID);
            }
            // case: server receives DETAILS-FOR message
            // format: dsfr,remoteId,localID,x,y,z
            if (messageTokens[0].compareTo("dsfr") == 0) {
                UUID remoteID = UUID.fromString(messageTokens[1]);
                UUID clientID = UUID.fromString(messageTokens[2]);
                String[] pos = {messageTokens[3], messageTokens[4], messageTokens[5]};
                sendDetailsMessages(clientID, remoteID, pos);
            }
            // case: server receives MOVE message
            if (messageTokens[0].compareTo("move") == 0) {
                // format: move,localid,x,y,z
                UUID clientID = UUID.fromString(messageTokens[1]);
                String[] pos = {messageTokens[2], messageTokens[3], messageTokens[4]};
                sendMoveMessages(clientID, pos);
            }
            // case: server receives ROT message
            if (messageTokens[0].compareTo("rot") == 0) {
                UUID clientID = UUID.fromString(messageTokens[1]);
                String[] rotMatrix =
                        {messageTokens[2], messageTokens[3], messageTokens[4],
                                messageTokens[5], messageTokens[6], messageTokens[7],
                                messageTokens[8], messageTokens[9], messageTokens[10]};
                sendRotationMessages(clientID, rotMatrix);
            }
        }
    }

    /**
     * Sends messages to clients
     * @param clientID
     * @param success
     */
    public void sendJoinedMessage(UUID clientID, boolean success) {
        // format: join,success OR join,failure
        try {
            String message = new String("join,");
            if (success)
                message += "success";
            else
                message += "failure";
            sendPacket(message, clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends message to all other clients that new client joined, 
     * with new client's avatar position. Lets other clients know
     * where to place ghost avatar for new client
     * @param clientID
     * @param pos
     */
    public void sendCreateMessages(UUID clientID, String[] pos) {
        // format: create,remoteid,x,y,z
        try {
            String message = new String("create," + clientID.toString());
            message += "," + pos[0];
            message += "," + pos[1];
            message += "," + pos[2];
            // sends message to every client except for clientID
            forwardPacketToAll(message, clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendDetailsMessages(UUID clientID, UUID remoteID, String[] pos) {
        try {
            String message = new String("dsfr," + clientID.toString());
            message += "," + pos[0];
            message += "," + pos[1];
            message += "," + pos[2];
            // send response from other clients to client who requested "wants details"
            sendPacket(message, remoteID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tells the new client where all of the other client's avatars are positioned
     * @param clientID
     */
    public void sendWantsDetailsMessages(UUID clientID) {
        try {
            String message = new String("wsds," + clientID.toString());
            forwardPacketToAll(message, clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMoveMessages(UUID clientID, String[] pos) {
        try {
            String message = new String("move," + clientID.toString());
            message += "," + pos[0];
            message += "," + pos[1];
            message += "," + pos[2];
            // send the move mesage to all other clients exept for client 
            // that initiated it
            forwardPacketToAll(message, clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendRotationMessages(UUID cliendID, String[] rotMat) {
        try {
            String message = new String("rot," + cliendID.toString());
            message += "," + rotMat[0];
            message += "," + rotMat[1];
            message += "," + rotMat[2];
            message += "," + rotMat[3];
            message += "," + rotMat[4];
            message += "," + rotMat[5];
            message += "," + rotMat[6];
            message += "," + rotMat[7];
            message += "," + rotMat[8];
            // send rotate msg to all other clients except for client that 
            // intiated it
            forwardPacketToAll(message, cliendID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Send bye message to all connected clients except for the client that 
     * sent it (the client with cliendID is the one who sent it)
     * @param clientID
     */
    public void sendByeMessages(UUID clientID) {
        try {
            String message = new String("bye," + clientID.toString());
            forwardPacketToAll(message, clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
