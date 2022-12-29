package docklib.draggabletabpane;

import javafx.geometry.NodeOrientation;
import javafx.scene.Node;

public interface HeaderReachable {

    Node getTabHeaderArea();
    NodeOrientation getHeaderOrientation();

}
