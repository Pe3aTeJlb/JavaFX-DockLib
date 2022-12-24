package docklib.utils;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class IconsManager {

    private static final String path = "docklib/resources";

    private static final String logoPath = "docklib/resources";

    public static final Image StageIcon = new Image("docklib/resources/icon.png");

    public static ImageView getIcon(String name) {
        return getImageView(name);
    }

    public static ImageView getImageView(String name) {

        Image img = new Image(path + "/" + name);
        return new ImageView(img);

    }

    public static Image getImage(String name){

        Image img = new Image(path + "/" + name);
        return img;

    }

    public static Image getLogo(String name){

        Image img = new Image(logoPath + "/" + name);
        return img;

    }

}
