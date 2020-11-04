
var JavaPackages = new JavaImporter(
    Packages.ray.rage.scene.Light,
    Packages.java.awt.Color
);

with (JavaPackages) {
    var toggle = false;
    function updateAmbientColor(thisLight) {
        if (!toggle) {
            thisLight.setAmbient(java.awt.Color.blue);
        }
        else {
            thisLight.setAmbient(new Color(0.3, 0.3, 0.3));
        }
        toggle = !toggle;
    }
}