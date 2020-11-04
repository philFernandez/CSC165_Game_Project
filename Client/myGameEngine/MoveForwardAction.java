package myGameEngine;

import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.Node;
import ray.rml.Degreef;

public class MoveForwardAction extends AbstractInputAction {
    private Node avN;
    private ProtocolClient protocolClient;

    public MoveForwardAction(Node n, ProtocolClient p) {
        avN = n;
        protocolClient = p;
    }

    @Override
    public void performAction(float time, Event event) {
        String component = event.getComponent().toString();
        switch (component) {
            case "W":
                avN.moveForward(0.05f);
                break;
            case "A":
                avN.moveLeft(0.05f);
                break;
            case "S":
                avN.moveBackward(0.05f);
                break;
            case "D":
                avN.moveRight(0.05f);
                break;
            case "Up":
                avN.pitch(Degreef.createFrom(-1.0f));
                break;
            case "Down":
                avN.pitch(Degreef.createFrom(1.0f));
                break;
            case "Right":
                avN.yaw(Degreef.createFrom(1.0f));
                break;
            case "Left":
                avN.yaw(Degreef.createFrom(-1.0f));
                break;

        }
        // have to to protocol client to call the server and tell 
        // other clients that this client has moved, so they can 
        // update their ghost avatar representing this client
        if (component == "W" || component == "S" || component == "A" || component == "D")
            protocolClient.sendMoveMessage(avN.getWorldPosition());
        else
            protocolClient.sendRotateMessage(avN.getWorldRotation());
    }

}
