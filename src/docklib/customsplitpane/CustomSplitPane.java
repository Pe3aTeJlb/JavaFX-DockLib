package docklib.customsplitpane;

import javafx.scene.control.Skin;
import javafx.scene.control.SplitPane;

public class CustomSplitPane extends SplitPane {

    public CustomSplitPane(){
        super();
    }

    @Override protected Skin<?> createDefaultSkin() {
        return new SplitPaneSkin(this);
    }

}
