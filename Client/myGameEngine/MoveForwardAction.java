package myGameEngine;

import myGame.MyGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.Node;
import ray.rml.Degreef;

public class MoveForwardAction extends AbstractInputAction {
    private Node avN;
    private ProtocolClient protocolClient;
    private MyGame game;

    public MoveForwardAction(Node n, ProtocolClient p, MyGame game) {
        avN = n;
        protocolClient = p;
        this.game = game;
    }

    @Override
    public void performAction(float time, Event event) {
        String component = event.getComponent().toString();
        switch (component) {
            case "W":
                avN.moveForward(0.1f);
                break;
            case "A":
                avN.yaw(Degreef.createFrom(-1.0f));
                break;
            case "S":
                avN.moveBackward(0.1f);
                break;
            case "D":
                avN.yaw(Degreef.createFrom(1.0f));
                break;
            case "Up":
                avN.pitch(Degreef.createFrom(-1.0f));
                break;

        }
        // have to to protocol client to call the server and tell 
        // other clients that this client has moved, so they can 
        // update their ghost avatar representing this client
        if (component == "W" || component == "S") {
            game.updateVerticalPosition();
            protocolClient.sendMoveMessage(avN.getWorldPosition());

        } else if (component == "A" || component == "D") {
            protocolClient.sendRotateMessage(avN.getWorldRotation());
        }
    }

}
