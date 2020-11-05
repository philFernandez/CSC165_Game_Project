
package myGameEngine;

import myGame.MyGame;
import net.java.games.input.Component.Identifier.Key;
import net.java.games.input.Event;
import ray.input.InputManager;
import ray.input.action.AbstractInputAction;
import ray.input.action.Action;
import ray.rage.scene.Camera;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class CameraOrbitController {
    private final Camera camera;
    private final SceneNode cameraN;
    private final SceneNode targetN;
    private float cameraAzimuth;
    private float cameraElevation;
    private float radius;
    private final Vector3 worldUpVect;
    private final MyGame game;

    public CameraOrbitController(final MyGame game, final String cameraName,
            final String targetName, final String controllerName) {
        this.game = game;
        final SceneManager sceneMangr = game.getEngine().getSceneManager();
        targetN = sceneMangr.getSceneNode(targetName);
        camera = sceneMangr.getCamera(cameraName);
        cameraN = sceneMangr.getSceneNode(camera.getName() + "Node");


        cameraAzimuth = 180.0f; // start directly behind target  (180 degrees)
        cameraElevation = 20.0f; // 20 degrees above target
        radius = 20.0f;
        worldUpVect = Vector3f.createFrom(0.0f, 1.0f, 0.0f);
        setupInput(game.getInputManager(), controllerName);
        updateCameraPosition();
    }

    public void updateCameraPosition() {
        final double theta = Math.toRadians(cameraAzimuth); // rotate around
        final double phi = Math.toRadians(cameraElevation); // altitude
        final double x = radius * Math.cos(phi) * Math.sin(theta);
        final double y = radius * Math.sin(phi);
        final double z = radius * Math.cos(phi) * Math.cos(theta);
        cameraN.setLocalPosition(Vector3f.createFrom((float) x, (float) y, (float) z)
                .add(targetN.getWorldPosition()));
        cameraN.lookAt(targetN, worldUpVect);
    }

    private void setupInput(final InputManager inputMangr, final String controllerName) {
        System.out.println("INPUT MAN : " + inputMangr);
        System.out.println("CONTROL : " + controllerName);
        final Action orbitAroundAction = new OrbitAroundAction();
        final Action orbitElevationAction = new OrbitElevationAction();
        final Action zoomAction = new ZoomAction();
        try { // setup for keyboard (top viewport)
            inputMangr.associateAction(controllerName, Key.RIGHT, orbitAroundAction,
                    InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            inputMangr.associateAction(controllerName, Key.LEFT, orbitAroundAction,
                    InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            inputMangr.associateAction(controllerName, Key.UP, orbitElevationAction,
                    InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            inputMangr.associateAction(controllerName, Key.DOWN, orbitElevationAction,
                    InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            inputMangr.associateAction(controllerName, Key.I, zoomAction,
                    InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            inputMangr.associateAction(controllerName, Key.O, zoomAction,
                    InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        } catch (final RuntimeException err) {
            err.printStackTrace();
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // The parameters for camera position are updated here in theses Actions. 
    // The call to updateCameraPosition is done in MyGame.update
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private class OrbitAroundAction extends AbstractInputAction {

        @Override
        public void performAction(final float time, final Event evt) {
            final float delta = game.getDelta() / 1000.f;
            final String component = evt.getComponent().getName();
            final float cmpntValue = evt.getValue();

            // Rotate at 180 degrees/second. 
            final float rotateAmount =
                    component.equals("Left") ? -180.0f * cmpntValue * delta
                            : 180.0f * cmpntValue * delta;

            // Give us a [-0.2, 0.2] axis deadzone
            if (cmpntValue < -0.2 || cmpntValue > 0.2)
                cameraAzimuth = (cameraAzimuth + rotateAmount) % 360;
        }
    }

    private class OrbitElevationAction extends AbstractInputAction {

        @Override
        public void performAction(final float time, final Event evt) {
            final float delta = game.getDelta() / 1000.f;
            final String component = evt.getComponent().getName();
            final float cmpntValue = evt.getValue();

            // Rotate at 180 degrees/second. 
            final float rotateAmount =
                    component.equals("Up") ? 180.0f * cmpntValue * delta
                            : -180.0f * cmpntValue * delta;

            final float elevation = cameraElevation + rotateAmount;

            // Don't allow camera to go below ground plane
            if ((cmpntValue < -0.2 || cmpntValue > 0.2)
                    && (elevation > 0 && elevation < 75))
                cameraElevation = elevation;

        }
    }

    private class ZoomAction extends AbstractInputAction {
        @Override
        public void performAction(final float time, final Event evt) {
            final float delta = game.getDelta() / 1000.f;
            final String component = evt.getComponent().getName();
            final float cmpntValue = evt.getValue();
            // zoom at 3 units of world distance/second (i think?)
            final float zoomAmount = component.equals("I") ? -3.0f * cmpntValue * delta
                    : 3.0f * cmpntValue * delta;

            final float zoom = radius + zoomAmount;

            if (zoom > 1 && zoom < 21) {
                radius = zoom;
            }
        }
    }
}
