package myGameEngine;

import java.util.UUID;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneNode;
import ray.rml.Matrix3;
import ray.rml.Matrix3f;
import ray.rml.Vector3;
import ray.rml.Vector3f;

// these will be actual Entities/Nodes representing other players
public class GhostAvatar {
    private UUID id;
    private SceneNode node;
    private Entity entity;
    private Vector3 initialPosition;

    // probably need to do something with position in the constructor?
    // like set the nodes position with it?
    public GhostAvatar(UUID id, Vector3 position) {
        this.id = id;
        initialPosition = position;
        // node and entity are defined in MyGame.addGhostAvatarToGameWorld
        // and then they are set via a GhostAvatar instance calling its setters
    }

    public UUID getId() {
        return id;
    }

    public Entity getEntity() {
        return entity;
    }

    public SceneNode getNode() {
        return node;
    }

    public Vector3 getPosition() {
        return node.getLocalPosition();
    }

    public Matrix3 getRotation() {
        return node.getLocalRotation();
    }

    public void setRotation(float m1, float m2, float m3, float m4, float m5, float m6,
            float m7, float m8, float m9) {
        float[] values = {m1, m2, m3, m4, m5, m6, m7, m8, m9};
        setRotation(values);
    }

    public void setRotation(float[] values) {
        node.setLocalRotation(Matrix3f.createFrom(values));
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public void setNode(SceneNode node) {
        this.node = node;
    }

    public void setPosition(Vector3 position) {
        setPosition(position.x(), position.y(), position.z());
    }

    public void setPosition(float x, float y, float z) {
        node.setLocalPosition(Vector3f.createFrom(x, y, z));
    }

    // Can't set node position until the node actually exists, 
    // so save init position when object is instantiated, and
    // use it to set the node's position after it is created in Game
    public Vector3 getInitPosition() {
        return initialPosition;
    }
}
