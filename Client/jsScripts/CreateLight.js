var JavaPackages = new JavaImporter(
    Packages.ray.rage.scene.SceneManager,
    Packages.ray.rage.scene.Light,
    Packages.ray.rage.scene.Light.Type,
    Packages.ray.rage.scene.Light.Type.POINT,
    Packages.java.awt.Color
);

// creates a RAGE object - a light
with (JavaPackages) {
    var plight = sceneMangr.createLight("testLamp1", Light.Type.SPOT);
    plight.setAmbient(new Color(0.3, 0.3, 0.3));
    plight.setDiffuse(new Color(0.7, 0.7, 0.7));
    plight.setSpecular(new Color(1.0, 1.0, 1.0));
    plight.setRange(5);
}
