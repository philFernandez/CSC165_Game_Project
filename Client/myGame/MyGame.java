package myGame;

import java.awt.*;
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
import ray.rage.game.*;
import ray.rage.rendersystem.*;
import ray.rage.rendersystem.Renderable.*;
import ray.rage.scene.*;
import ray.rage.scene.Camera.Frustum.*;
import ray.rage.scene.controllers.RotationController;
import ray.rml.*;
import ray.rage.rendersystem.gl4.GL4RenderSystem;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class MyGame extends VariableFrameRateGame {
    private String serverAddress;
    private int serverPort;
    private ProtocolType serverProtocol;
    private ProtocolClient protocolClient;
    private boolean isClientConnected;
    private Vector<UUID> gameObjectsToRemove;
    private Action moveFwd;
    private InputManager inputMangr;

    // javascript things ---------------------
    private long fileLastModified;
    protected File scriptFile1, scriptFile2, scriptFile3;
    protected ScriptEngine jsEngine;
    protected ColorAction colorAction;
    private RotationController rc;
    // ---------------------------------------

    // to minimize variable allocation in update()
    GL4RenderSystem rs;
    float elapsTime = 0.0f;
    String elapsTimeStr, counterStr, dispStr;
    int elapsTimeSec, counter = 0;

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
    protected void setupCameras(SceneManager sm, RenderWindow rw) {
        SceneNode rootNode = sm.getRootSceneNode();
        Camera camera = sm.createCamera("MainCamera", Projection.PERSPECTIVE);
        rw.getViewport(0).setCamera(camera);

        camera.setRt((Vector3f) Vector3f.createFrom(1.0f, 0.0f, 0.0f));
        camera.setUp((Vector3f) Vector3f.createFrom(0.0f, 1.0f, 0.0f));
        camera.setFd((Vector3f) Vector3f.createFrom(0.0f, 0.0f, -1.0f));

        camera.setPo((Vector3f) Vector3f.createFrom(0.0f, 0.0f, 0.0f));

        SceneNode cameraNode = rootNode.createChildSceneNode(camera.getName() + "Node");
        cameraNode.attachObject(camera);
    }


    @Override
    protected void setupScene(Engine eng, SceneManager sm) throws IOException {
        inputMangr = new GenericInputManager();
        Entity dolphinE = sm.createEntity("myDolphin", "dolphinHighPoly.obj");
        dolphinE.setPrimitive(Primitive.TRIANGLES);

        SceneNode dolphinN =
                sm.getRootSceneNode().createChildSceneNode(dolphinE.getName() + "Node");
        dolphinN.moveBackward(2.0f);
        dolphinN.attachObject(dolphinE);

        // prepare script engine
        ScriptEngineManager factory = new ScriptEngineManager();
        jsEngine = factory.getEngineByName("js");
        // use spin speed setting from the first script to init dolphin rotation
        scriptFile1 = new File("jsScripts/InitParams.js");
        this.executeScript(jsEngine, scriptFile1);
        rc = new RotationController(Vector3f.createUnitVectorY(),
                ((Double) (jsEngine.get("spinSpeed"))).floatValue());
        rc.addNode(dolphinN);
        sm.addController(rc);

        // add the light specified in the second script to the game world
        scriptFile2 = new File("jsScripts/CreateLight.js");
        jsEngine.put("sm", sm);
        this.executeScript(jsEngine, scriptFile2);

        SceneNode plightNode = sm.getRootSceneNode().createChildSceneNode("plightNode");
        plightNode.attachObject((Light) jsEngine.get("plight"));

        scriptFile3 = new File("jsScripts/UpdateLightColor.js");
        this.executeScript(jsEngine, scriptFile3);
        String kbName = inputMangr.getKeyboardName();
        colorAction = new ColorAction(sm);
        inputMangr.associateAction(kbName, Key.SPACE, colorAction,
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);


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
        return getEngine().getSceneManager().getSceneNode("myDolphinNode")
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
                    .createEntity(avatar.getId().toString(), "cube.obj");
            ghostE.setPrimitive(Primitive.TRIANGLES);
            SceneNode ghostN = getEngine().getSceneManager().getRootSceneNode()
                    .createChildSceneNode(avatar.getId().toString());
            ghostN.attachObject(ghostE);
            ghostN.scale(0.3f, 0.3f, 0.3f);
            avatar.setNode(ghostN);
            avatar.setEntity(ghostE);
            avatar.setPosition(avatar.getInitPosition());
        }
    }

    public void removeGhostAvatarFromGameWorld(GhostAvatar avatar) {
        if (avatar != null)
            gameObjectsToRemove.add(avatar.getId());
    }



    @Override
    protected void update(Engine engine) {
        // build and set HUD
        rs = (GL4RenderSystem) engine.getRenderSystem();
        elapsTime += engine.getElapsedTimeMillis();
        elapsTimeSec = Math.round(elapsTime / 1000.0f);
        elapsTimeStr = Integer.toString(elapsTimeSec);
        counterStr = Integer.toString(counter);
        dispStr = "Time = " + elapsTimeStr + "   Keyboard hits = " + counterStr;
        rs.setHUD(dispStr, 15, 15);
        processNetworking(elapsTime);
        inputMangr.update(elapsTime);

        // Should update on the fly? but doesn't
        long modTime = scriptFile1.lastModified();
        if (modTime > fileLastModified) {
            fileLastModified = modTime;
            this.executeScript(jsEngine, scriptFile1);
            rc.setSpeed(((Double) (jsEngine.get("spinSpeed"))).floatValue());
        }
    }

    public void setIsConnected(boolean isConnected) {
        isClientConnected = isConnected;
    }

    protected void setupInputs() {
        SceneNode playerNode =
                getEngine().getSceneManager().getSceneNode("myDolphinNode");
        moveFwd = new MoveForwardAction(playerNode, protocolClient);
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
            inputMangr.associateAction(keyboard, Key.RIGHT, moveFwd,
                    INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            inputMangr.associateAction(keyboard, Key.LEFT, moveFwd,
                    INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            inputMangr.associateAction(keyboard, Key.UP, moveFwd,
                    INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            inputMangr.associateAction(keyboard, Key.DOWN, moveFwd,
                    INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
            inputMangr.associateAction(keyboard, Key.ESCAPE,
                    sendCloseConnectionPacketAction, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
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

}
