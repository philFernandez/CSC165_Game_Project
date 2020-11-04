package myGameEngine;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.UUID;
import myGame.MyGame;
import ray.networking.client.GameConnectionClient;
import ray.rml.Matrix3;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class ProtocolClient extends GameConnectionClient {
    private MyGame game;
    private UUID id;
    private HashMap<UUID, GhostAvatar> ghostAvatarMap;

    public ProtocolClient(InetAddress remoteAddr, int remotePort,
            ProtocolType protocolType, MyGame game) throws IOException {
        super(remoteAddr, remotePort, protocolType);
        this.game = game;
        this.id = UUID.randomUUID();
        this.ghostAvatarMap = new HashMap<UUID, GhostAvatar>();
    }

    @Override
    protected void processPacket(Object msg) {
        String strMessage = (String) msg;
        String[] messageTokens = strMessage.split(",");
        if (messageTokens.length > 0) {
            // _1A_ (see sendJoinMessage below)
            if (messageTokens[0].compareTo("join") == 0) { // receive join
                // format: join, success OR join, failure
                // Join message is response from server telling wheather or 
                // not join request from you (client) was successful
                if (messageTokens[1].compareTo("success") == 0) {
                    System.out.println(strMessage + ": JOIN SUCCESS");
                    game.setIsConnected(true);
                    // sends message back to server telling where 
                    // this client wants to join. server then tells 
                    // other clients where to put their ghost for you
                    sendCreateMessage(game.getPlayerPosition());
                }
                if (messageTokens[1].compareTo("failure") == 0) {
                    System.out.println(strMessage + ": JOIN FAILURE");
                    game.setIsConnected(false);
                }
            }
            if (messageTokens[0].compareTo("bye") == 0) { // receive bye
                System.out.println(strMessage + ": BYE");
                // format: bye, remoteId
                // this is server telling this client about another client that left the 
                // game? So remove that client's avatar
                UUID ghostID = UUID.fromString(messageTokens[1]);
                game.removeGhostAvatarFromGameWorld(ghostAvatarMap.get(ghostID));
                ghostAvatarMap.remove(ghostID);
            }


            // DETAILS FROM (dsft) is for after when this new client comes in, it 
            // gets the details of all the other clients to create ghost avatars
            // CREATE  is for when another client comes in, the server will send 
            // the position for that new client, then this client can make ghost avatar
            if ((messageTokens[0].compareTo("dsfr") == 0) // receive  'details from'
                    || (messageTokens[0].compareTo("create") == 0)) {
                System.out.println(strMessage + ": DEETS FROM/CREATE");
                // format: create, remoteId, x, y, z OR dsfr, remoteId, x, y, z
                UUID ghostID = UUID.fromString(messageTokens[1]);
                Vector3 ghostPosition =
                        Vector3f.createFrom(Float.parseFloat(messageTokens[2]),
                                Float.parseFloat(messageTokens[3]),
                                Float.parseFloat(messageTokens[4]));
                try {
                    // put the ghost avatar of the newly joined client into 
                    // this client's game world
                    GhostAvatar avatar = new GhostAvatar(ghostID, ghostPosition);
                    ghostAvatarMap.put(ghostID, avatar);

                    game.addGhostAvatarToGameWorld(avatar);
                } catch (IOException e) {
                    System.out.println("error creating ghost avatar");
                }
            }

            // server sent request for details about this client
            if (messageTokens[0].compareTo("wsds") == 0) { // receive 'wants details'
                System.out.println(strMessage + ": WANTS DEETS");
                // format: wsds,remoteId
                // the uuid of the other client who is requesting details
                UUID remoteID = UUID.fromString(messageTokens[1]);
                sendDetailsForMessage(remoteID, game.getPlayerPosition());
            }

            if (messageTokens[0].compareTo("move") == 0) {
                // format: move,remoteId,x,y,z
                System.out.println(strMessage + ": MOVE");
                UUID remoteID = UUID.fromString(messageTokens[1]);

                ghostAvatarMap.get(remoteID).setPosition(
                        Float.parseFloat(messageTokens[2]),
                        Float.parseFloat(messageTokens[3]),
                        Float.parseFloat(messageTokens[4]));
            }

            if (messageTokens[0].compareTo("rot") == 0) {
                // format: 
                // rot,remoteId,m{0,0},m{1,0},m{2,0},m{0,1},m{1,1},m{2,1},m{0,2},m{1,2},m{2,2}
                System.out.println(strMessage + ": ROT");
                UUID remoteID = UUID.fromString(messageTokens[1]);
                ghostAvatarMap.get(remoteID).setRotation(
                        Float.parseFloat(messageTokens[2]),
                        Float.parseFloat(messageTokens[3]),
                        Float.parseFloat(messageTokens[4]),
                        Float.parseFloat(messageTokens[5]),
                        Float.parseFloat(messageTokens[6]),
                        Float.parseFloat(messageTokens[7]),
                        Float.parseFloat(messageTokens[8]),
                        Float.parseFloat(messageTokens[9]),
                        Float.parseFloat(messageTokens[10]));
            }

        }
    }

    // These methods are for sending to the server about this client 

    // This gets called from MyGame.java during scene setup 
    // the server should respond with a messge (See _1A_ in processPacket above)
    public void sendJoinMessage() { // format: join, localId
        try {
            sendPacket(new String("join," + id.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendCreateMessage(Vector3 pos) { // format: (create, localId, x, y, z)
        try {
            String message = new String("create," + id.toString());
            message += "," + pos.x() + "," + pos.y() + "," + pos.z();
            sendPacket(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Send when leaving
    public void sendByeMessage() {
        try {
            String message = new String("bye," + id.toString());
            sendPacket(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // When receiving "wants details"  send message back to server
    public void sendDetailsForMessage(UUID remoteID, Vector3 pos) {
        try {
            // remoteID is the id of the client that requested the details
            String message = new String("dsfr," + remoteID.toString());
            message +=
                    "," + id.toString() + "," + pos.x() + "," + pos.y() + "," + pos.z();
            sendPacket(message);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // When my avatar moves this method needs to be called and send position 
    // to server so that other clients can get that position info 
    // This has to be called from any move action
    public void sendMoveMessage(Vector3 pos) {
        try {
            String message = new String("move," + id.toString());
            message += "," + pos.x() + "," + pos.y() + "," + pos.z();
            sendPacket(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendRotateMessage(Matrix3 rotMat) {
        try {
            float[] values = rotMat.toFloatArray();
            String message = new String("rot," + id.toString());
            message += "," + values[0] + "," + values[1] + "," + values[2] + ","
                    + values[3] + "," + values[4] + "," + values[5] + "," + values[6]
                    + "," + values[7] + "," + values[8];
            sendPacket(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
