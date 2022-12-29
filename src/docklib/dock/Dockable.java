package docklib.dock;

import javafx.scene.Node;
import javafx.scene.control.SplitPane;

public interface Dockable {

    boolean isWrappedInDockPane();
    void setDockPane(DockPane dockPane, SplitPane splitPane);
    DockPane getDockPane();
    void dock(Node node, DockAnchor dockAnchor);
    void undock();

}
