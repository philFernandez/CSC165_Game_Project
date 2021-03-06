package myGame;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import myGameEngine.*;
import net.java.games.input.Event;
import net.java.games.input.Component.Identifier.Key;
import ray.input.GenericInputManager;
import ray.input.InputManager;
import ray.input.InputManager.INPUT_ACTION_TYPE;
import ray.input.action.AbstractInputAction;
import ray.input.action.Action;
import ray.networking.IGameConnection.ProtocolType;
import ray.rage.*;
import ray.rage.asset.texture.Texture;
import ray.rage.asset.texture.TextureManager;
import ray.rage.game.*;
import ray.rage.rendersystem.*;
import ray.rage.rendersystem.Renderable.*;
import ray.rage.scene.*;
import ray.rage.scene.Camera.Frustum.*;
import ray.rage.scene.SkeletalEntity.EndType;
import ray.rage.util.Configuration;
import ray.rml.*;
import ray.rage.rendersystem.gl4.GL4RenderSystem;
import ray.rage.rendersystem.states.RenderState;
import ray.rage.rendersystem.states.TextureState;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.awt.event.*;

public class MyGame extends VariableFrameRateGame {
    private String serverAddress;
    private int serverPort;
    private ProtocolType serverProtocol;
    private ProtocolClient protocolClient;
    private boolean isClientConnected;
    private Vector<UUID> gameObjectsToRemove;
    private Action moveFwd;
    private InputManager inputMangr;
    private CameraOrbitController cameraOrbitController;

    // javascript things ---------------------
    private long fileLastModified;
    protected File scriptFile1, scriptFile2, scriptFile3;
    protected ScriptEngine jsEngine;
    protected ColorAction colorAction;
    // ---------------------------------------

    // to minimize variable allocation in update()
    GL4RenderSystem rs;
    float elapsTime = 0.0f;
    String elapsTimeStr, counterStr, dispStr;
    int elapsTimeSec, counter = 0;
    float delta, previousTime;

    public MyGame(String serverAddress, int serverPort) {
        super();
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.serverProtocol = ProtocolType.UDP;
    }

    public static void main(String[] args) {
        // args: <server ip> <port number>
        MyGame game = new MyGame(args[0], Integer.parseInt(args[1]));
        try {
            game.startup();
            game.run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            game.shutdown();
            game.exit();
        }
    }

    private void executeScript(ScriptEngine engine, File scriptFileName) {
        try {
            FileReader fileReader = new FileReader(scriptFileName);
            engine.eval(fileReader);
            fileReader.close();
        } catch (FileNotFoundException e1) {
            System.out.println(scriptFileName + " not found " + e1);
        } catch (IOException e2) {
            System.out.println("IO problem with " + scriptFileName + e2);
        } catch (ScriptException e3) {
            System.out.println("Script Exception in " + scriptFileName + e3);
        } catch (NullPointerException e4) {
            System.out.println("Null Ptr in " + scriptFileName + e4);
        }
    }

    // call this out of setupScene?
    private void setupNetworking() {
        gameObjectsToRemove = new Vector<UUID>();
        isClientConnected = false;
        try {
            protocolClient = new ProtocolClient(InetAddress.getByName(serverAddress),
                    serverPort, serverProtocol, this);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (protocolClient == null)
            System.out.println("missing protocol host");
        else {
            // ask client protocol to send initial join message
            // to server, with a unique identifier for this client
            protocolClient.sendJoinMessage();
        }
    }

    @Override
    protected void setupWindow(RenderSystem rs, GraphicsEnvironment ge) {
        rs.createRenderWindow(new DisplayMode(1000, 700, 24, 60), false);
    }


    @Override
    protected void setupCameras(SceneManager sceneMangr, RenderWindow renderWin) {
        Camera camera = sceneMangr.createCamera("MainCamera", Projection.PERSPECTIVE);
        renderWin.getViewport(0).setCamera(camera);
        SceneNode cameraN = sceneMangr.getRootSceneNode()
                .createChildSceneNode(camera.getName() + "Node");
        cameraN.attachObject(camera);
        camera.setMode('r');
        camera.getFrustum().setFarClipDistance(500.0f);
    }

    private void setupOrbitCamera(Engine engine, SceneManager sceneMangr) {
        cameraOrbitController = new CameraOrbitController(this, "MainCamera",
                "playerAvatarNode", inputMangr.getKeyboardName());
    }


    private SceneNode playerAvatarN;
    private Tessellation tessE;
    private double avatarYPos;


    @Override
    protected void setupScene(Engine engine, SceneManager sceneMangr)
            throws IOException {
        inputMangr = new GenericInputManager();
        Configuration conf = getEngine().getConfiguration();
        TextureManager textureMangr = getEngine().getTextureManager();
        // point texture path to skyboxes dir
        textureMangr.setBaseDirectoryPath(conf.valueOf("assets.skyboxes.path"));
        Texture skyboxFront = textureMangr.getAssetByPath("front.png");
        Texture skyboxBack = textureMangr.getAssetByPath("back.png");
        Texture skyboxLeft = textureMangr.getAssetByPath("left.png");
        Texture skyboxRight = textureMangr.getAssetByPath("right.png");
        Texture skyboxTop = textureMangr.getAssetByPath("top.png");
        Texture skyboxBottom = textureMangr.getAssetByPath("bottom.png");
        // point texture path back to textures dir
        textureMangr.setBaseDirectoryPath(conf.valueOf("assets.textures.path"));
        AffineTransform xform = new AffineTransform();
        xform.translate(0, skyboxFront.getImage().getHeight());
        xform.scale(1d, -1d);
        skyboxFront.transform(xform);
        skyboxBack.transform(xform);
        skyboxLeft.transform(xform);
        skyboxRight.transform(xform);
        skyboxTop.transform(xform);
        skyboxBottom.transform(xform);
        SkyBox skyBox = sceneMangr.createSkyBox("skyBox");
        skyBox.setTexture(skyboxFront, SkyBox.Face.FRONT);
        skyBox.setTexture(skyboxBack, SkyBox.Face.BACK);
        skyBox.setTexture(skyboxLeft, SkyBox.Face.LEFT);
        skyBox.setTexture(skyboxRight, SkyBox.Face.RIGHT);
        skyBox.setTexture(skyboxTop, SkyBox.Face.TOP);
        skyBox.setTexture(skyboxBottom, SkyBox.Face.BOTTOM);
        sceneMangr.setActiveSkyBox(skyBox);

        ScriptEngineManager factory = new ScriptEngineManager();
        jsEngine = factory.getEngineByName("js");
        // use spin speed setting from the first script to init dolphin rotation
        scriptFile1 = new File("jsScripts/InitParams.js");
        this.executeScript(jsEngine, scriptFile1);

        // SkeletalEntity roboEntity = sceneMangr.createSkeletalEntity("playerAvatar",
        //         "myrobot.rkm", "myrobot.rks");
        // Texture tex = sceneMangr.getTextureManager().getAssetByPath("myrobot.jpg");
        // TextureState tstate = (TextureState) sceneMangr.getRenderSystem()
        //         .createRenderState(RenderState.Type.TEXTURE);
        // tstate.setTexture(tex);
        // roboEntity.setRenderState(tstate);

        // SceneNode roboNode =
        //         sceneMangr.getRootSceneNode().createChildSceneNode("playerAvatarNode");
        // roboNode.attachObject(roboEntity);
        // roboNode.translate(0, 0.5f, 0);

        // roboEntity.loadAnimation("waveAnimation", "myrobot.rka");
        // roboEntity.playAnimation("waveAnimation", 0.5f, EndType.LOOP, 0);

        Entity carE = sceneMangr.createEntity("playerAvatar", "car.obj");
        SceneNode carN =
                sceneMangr.getRootSceneNode().createChildSceneNode("playerAvatarNode");
        carN.attachObject(carE);



        // prepare script engine

        // add the light specified in the second script to the game world
        scriptFile2 = new File("jsScripts/CreateLight.js");
        jsEngine.put("sceneMangr", sceneMangr);
        this.executeScript(jsEngine, scriptFile2);

        SceneNode plightNode =
                sceneMangr.getRootSceneNode().createChildSceneNode("plightNode");
        plightNode.attachObject((Light) jsEngine.get("plight"));

        scriptFile3 = new File("jsScripts/UpdateLightColor.js");
        this.executeScript(jsEngine, scriptFile3);
        String kbName = inputMangr.getKeyboardName();
        colorAction = new ColorAction(sceneMangr);
        inputMangr.associateAction(kbName, Key.SPACE, colorAction,
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        // 2^{patches} : min=5, def=7, warnings start at 10
        int patches = ((Integer) jsEngine.get("patches")).intValue();
        double subdivisions = ((Double) jsEngine.get("subdivisions")).floatValue();

        tessE = sceneMangr.createTessellation("tessE", patches);
        // subdivisions per patch: min=0, try up to 32
        tessE.setSubdivisions((float) subdivisions);
        SceneNode tessN = sceneMangr.getRootSceneNode().createChildSceneNode("tessN");
        tessN.attachObject(tessE);
        // to move it X and Z must both be positive or negative
        // tessN.translate(Vector3f.createFrom(-6.2f, -2.2f, 2.7f));
        // tessN.yaw(Degreef.createFrom(37.2f));

        tessN.scale(1000f, 1000f, 1000f);
        tessE.setNormalMap(this.getEngine(), "NormalMap2.jpg");
        tessE.setHeightMap(this.getEngine(), "texture2.jpg");
        tessE.setTexture(this.getEngine(), "grassy.jpg");


        setupOrbitCamera(engine, sceneMangr);
        setupNetworking();
        setupInputs();
    }



    protected void processNetworking(float elapsTime) {
        // process packets received by client from server
        if (protocolClient != null) {
            // process packets is defined in protocolClient super class?
            // it calls processPacket in protocolClient for avery packet received
            protocolClient.processPackets();
        }
        // remove ghost avatars for players who have left game
        Iterator<UUID> it = gameObjectsToRemove.iterator();
        while (it.hasNext()) {
            getEngine().getSceneManager().destroySceneNode(it.next().toString());
        }
        gameObjectsToRemove.clear();
    }

    public Vector3 getPlayerPosition() {
        return getEngine().getSceneManager().getSceneNode("playerAvatarNode")
                .getWorldPosition();
    }

    /*
    add new ghost avatar into world. In futre part of the protocol would include
    what type of avatar the other client chose, then depending on that you would
    instantiate the correct type of avatar. Is this called from GhostAvater.java?
    */
    public void addGhostAvatarToGameWorld(GhostAvatar avatar) throws IOException {
        if (avatar != null) {
            Entity ghostE = getEngine().getSceneManager()
                    .createEntity(avatar.getId().toString(), "cube_nomat.obj");
            ghostE.setPrimitive(Primitive.TRIANGLES);
            SceneNode ghostN = getEngine().getSceneManager().getRootSceneNode()
                    .createChildSceneNode(avatar.getId().toString());
            ghostN.attachObject(ghostE);
            Texture ghostAvatarTexture =
                    getEngine().getTextureManager().getAssetByPath("ghost-brick.jpg");
            RenderSystem renderSys = getEngine().getSceneManager().getRenderSystem();
            TextureState textureState =
                    (TextureState) renderSys.createRenderState(RenderState.Type.TEXTURE);
            textureState.setTexture(ghostAvatarTexture);
            ghostE.setRenderState(textureState);
            avatar.setNode(ghostN);
            avatar.setEntity(ghostE);
            avatar.setPosition(avatar.getInitPosition());
        }
    }

    public void removeGhostAvatarFromGameWorld(GhostAvatar avatar) {
        if (avatar != null)
            gameObjectsToRemove.add(avatar.getId());
    }

    private void updateParameterScript() {
        // Should update on the fly? but doesn't
        SceneNode playerAvatarN =
                getEngine().getSceneManager().getSceneNode("playerAvatarNode");
        long modTime = scriptFile1.lastModified();
        if (modTime > fileLastModified) {
            fileLastModified = modTime;
            this.executeScript(jsEngine, scriptFile1);
            double xPos = ((Double) (jsEngine.get("playerAvatarPOSx"))).floatValue();
            avatarYPos = ((Double) (jsEngine.get("playerAvatarPOSy"))).floatValue();
            double zPos = ((Double) (jsEngine.get("playerAvatarPOSz"))).floatValue();
            playerAvatarN.setLocalPosition((float) xPos, (float) avatarYPos,
                    (float) zPos);
            playerAvatarN.moveBackward(
                    ((Double) (jsEngine.get("avatarMoveBack"))).floatValue());
            double subdivisions = ((Double) jsEngine.get("subdivisions")).floatValue();
            tessE.setSubdivisions((float) subdivisions);
        }
    }

    public void updateVerticalPosition() {
        SceneNode playerAvatarN =
                getEngine().getSceneManager().getSceneNode("playerAvatarNode");
        SceneNode tessN = getEngine().getSceneManager().getSceneNode("tessN");
        Tessellation tessE = ((Tessellation) tessN.getAttachedObject("tessE"));

        // Figure out Avatar's position relative to plane
        Vector3 worldAvatarPosition = playerAvatarN.getWorldPosition();
        Vector3 localAvatarPosition = playerAvatarN.getLocalPosition();

        // use avatar World coords to get coords for height
        Vector3 newAvatarPosition = Vector3f.createFrom(
                // X coord stays the same
                localAvatarPosition.x(),
                // Y coord is varying 
                // (add avatarYpos to keep origionally assigned Y distance from x=0)
                tessE.getWorldHeight(worldAvatarPosition.x(), worldAvatarPosition.z())
                        + (float) avatarYPos,
                // Z coord stays the same
                localAvatarPosition.z());
        playerAvatarN.setLocalPosition(newAvatarPosition);
    }


    @Override
    protected void update(Engine engine) {
        rs = (GL4RenderSystem) engine.getRenderSystem();
        elapsTime += engine.getElapsedTimeMillis();
        delta = elapsTime - previousTime;
        previousTime = elapsTime;
        elapsTimeSec = Math.round(elapsTime / 1000.0f);
        elapsTimeStr = Integer.toString(elapsTimeSec);
        counterStr = Integer.toString(counter);
        dispStr = "Time = " + elapsTimeStr + "   Keyboard hits = " + counterStr;
        rs.setHUD("Early Beta", 15, 15);
        processNetworking(elapsTime);
        inputMangr.update(elapsTime);
        cameraOrbitController.updateCameraPosition();
        updateParameterScript();
        // SkeletalEntity entity =
        //         (SkeletalEntity) engine.getSceneManager().getEntity("playerAvatar");
        // entity.update();
    }

    public void setIsConnected(boolean isConnected) {
        isClientConnected = isConnected;
    }

    protected void setupInputs() {
        SceneNode playerNode =
                getEngine().getSceneManager().getSceneNode("playerAvatarNode");
        moveFwd = new MoveForwardAction(playerNode, protocolClient, this);
        Action sendCloseConnectionPacketAction = new Action() {
            @Override
            public void performAction(float time, Event event) {
                if (protocolClient != null && isClientConnected) {
                    protocolClient.sendByeMessage();
                    exit();
                }
            }
        };
        String keyboard = inputMangr.getKeyboardName();

        try {
            inputMangr.associateAction(keyboard, Key.W, moveFwd,
                    INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            inputMangr.associateAction(keyboard, Key.A, moveFwd,
                    INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            inputMangr.associateAction(keyboard, Key.S, moveFwd,
                    INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            inputMangr.associateAction(keyboard, Key.D, moveFwd,
                    INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            inputMangr.associateAction(keyboard, Key.H, moveFwd,
                    INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            inputMangr.associateAction(keyboard, Key.Q, sendCloseConnectionPacketAction,
                    INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }


    // -------------------Nested ColorAction class---------------------------
    private class ColorAction extends AbstractInputAction {
        private SceneManager sceneMangr;

        private ColorAction(SceneManager sm) {
            sceneMangr = sm;
        }

        @Override
        public void performAction(float time, Event event) {
            Invocable invocableEngine = (Invocable) jsEngine;
            Light light = sceneMangr.getLight("testLamp1");

            // invoke the script function
            try {
                invocableEngine.invokeFunction("updateAmbientColor", light);
            } catch (ScriptException e1) {
                System.out.println("ScriptException in " + scriptFile3 + e1);
            } catch (NoSuchMethodException e2) {
                System.out.println("No such method in " + scriptFile3 + e2);
            } catch (NullPointerException e3) {
                System.out.println("Null ptr exception reading " + scriptFile3 + e3);
            }
        }
    }
    // ----------------------------------------------------------------------


    public InputManager getInputManager() {
        return inputMangr;
    }

    public float getDelta() {
        return delta;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_V:
                doAnimation("waveAnimation", false);
                break;
            case KeyEvent.VK_C:
                doAnimation("clapAnimation", false);
                break;
            case KeyEvent.VK_P:
                doAnimation("animation", true);
                break;
        }
        super.keyPressed(e);
    }

    private void doAnimation(String animation, boolean stop) {
        SkeletalEntity playerEntity =
                (SkeletalEntity) getEngine().getSceneManager().getEntity("playerAvatar");
        playerEntity.stopAnimation();
        if (!stop) {
            System.out.println("I do " + animation);
            playerEntity.playAnimation(animation, 0.5f, EndType.LOOP, 0);
        }
    }

}
