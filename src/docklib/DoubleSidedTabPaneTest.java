package docklib;

import docklib.dock.DockAnchor;
import docklib.dock.DockPane;
import docklib.draggabletabpane.DoubleSidedTabPane;
import docklib.draggabletabpane.DraggableTab;
import docklib.draggabletabpane.DraggableTabPane;
import docklib.draggabletabpane.TabGroup;
import docklib.utils.IconsManager;
import javafx.application.Application;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.Random;

public class DoubleSidedTabPaneTest extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        DoubleSidedTabPane systemTabPaneTop = new DoubleSidedTabPane(primaryStage);
        systemTabPaneTop.setSide(Side.TOP);

        DraggableTab tab41 = new DraggableTab("System Tab 1", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab42 = new DraggableTab("System Tab 2", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab43 = new DraggableTab("System Tab 3", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab44 = new DraggableTab("System Tab 4", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab45 = new DraggableTab("System Tab 5", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab46 = new DraggableTab("System Tab 6", IconsManager.getImage("icon.png"), new TextArea());

        systemTabPaneTop.addLeft(tab41, tab42, tab43);
        systemTabPaneTop.addRight(tab44, tab45, tab46);

        DoubleSidedTabPane systemTabPaneBottom = new DoubleSidedTabPane(primaryStage);
        systemTabPaneBottom.setSide(Side.BOTTOM);

        DraggableTab tab411 = new DraggableTab("System Tab 1", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab421 = new DraggableTab("System Tab 2", IconsManager.getImage("icon.png"), new Rectangle(100, 100, Color.ROYALBLUE));
        DraggableTab tab431 = new DraggableTab("System Tab 3", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab441 = new DraggableTab("System Tab 4", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab451 = new DraggableTab("System Tab 5", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab461 = new DraggableTab("System Tab 6", IconsManager.getImage("icon.png"), new TextArea());

        systemTabPaneBottom.addLeft(tab411, tab421, tab431);
        systemTabPaneBottom.addRight(tab441, tab451, tab461);


        DoubleSidedTabPane systemTabPaneLeft = new DoubleSidedTabPane(primaryStage);
        systemTabPaneLeft.setSide(Side.LEFT);
        systemTabPaneLeft.setCollapseOnInit(false);
        systemTabPaneLeft.setPrefExpandedSize(50);

        DraggableTab tab414 = new DraggableTab("System Tab 1", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab424 = new DraggableTab("System Tab 2", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab434 = new DraggableTab("System Tab 3", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab444 = new DraggableTab("System Tab 4", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab454 = new DraggableTab("System Tab 5", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab464 = new DraggableTab("System Tab 6", IconsManager.getImage("icon.png"), new TextArea());

        systemTabPaneLeft.addLeft(tab414, tab424, tab434);
        systemTabPaneLeft.addRight(tab444, tab454, tab464);


        DoubleSidedTabPane systemTabPaneRight = new DoubleSidedTabPane(primaryStage);
        systemTabPaneRight.setSide(Side.RIGHT);

        DraggableTab tab415 = new DraggableTab("System Tab 1", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab425 = new DraggableTab("System Tab 2", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab435 = new DraggableTab("System Tab 3", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab445 = new DraggableTab("System Tab 4", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab455 = new DraggableTab("System Tab 5", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab465 = new DraggableTab("System Tab 6", IconsManager.getImage("icon.png"), new TextArea());

        systemTabPaneRight.addLeft(tab415, tab425, tab435);
        systemTabPaneRight.addRight(tab445, tab455, tab465);


        DraggableTabPane workspaceTabPane = new DraggableTabPane(primaryStage, TabGroup.WorkSpace);
        workspaceTabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        workspaceTabPane.setSide(Side.TOP);
        workspaceTabPane.setRotateGraphic(true);
        workspaceTabPane.setUnDockable(false);

        DraggableTab tab410 = new DraggableTab("WorkSpace Tab 1", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab420 = new DraggableTab("WorkSpace Tab 2", IconsManager.getImage("icon.png"), new Rectangle(100, 100, Color.ROYALBLUE));
        DraggableTab tab430 = new DraggableTab("WorkSpace Tab 3", IconsManager.getImage("icon.png"), new Rectangle(100, 100, Color.ROSYBROWN));
        DraggableTab tab440 = new DraggableTab("WorkSpace Tab 4", IconsManager.getImage("icon.png"), new Rectangle(100, 100, Color.ORANGE));

        workspaceTabPane.addAll(
                tab410,
                tab420,
                tab430,
                tab440
        );


        DockPane dockPane = new DockPane(false);

        dockPane.dock(workspaceTabPane, DockAnchor.TOP);
        dockPane.dock(systemTabPaneLeft, DockAnchor.LEFT);
        dockPane.dock(systemTabPaneRight, DockAnchor.RIGHT);
    //    dockPane.dock(systemTabPaneTop, DockAnchor.TOP);
        dockPane.dock(systemTabPaneBottom, DockAnchor.BOTTOM);

        final Menu menu1 = new Menu("File");
        final Menu menu2 = new Menu("Options");
        final Menu menu3 = new Menu("Help");

        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(menu1, menu2, menu3);

        ToolBar toolBar = new ToolBar(
                new Button("New"),
                new Button("Open"),
                new Button("Save"),
                new Separator(),
                new Button("Clean"),
                new Button("Compile"),
                new Button("Run"),
                new Separator(),
                new Button("Debug"),
                new Button("Profile")
        );

        VBox vbox = new VBox();
        vbox.getChildren().addAll(menuBar, toolBar, dockPane);
        VBox.setVgrow(dockPane, Priority.ALWAYS);

        DockPane.initializeDefaultUserAgentStylesheet();

        primaryStage.setTitle("Demo scene");
        primaryStage.setScene(new Scene(vbox, 800, 500));
        primaryStage.sizeToScene();

        primaryStage.show();

    }

    private TreeView<String> generateRandomTree() {
        // create a demonstration tree view to use as the contents for a dock node
        TreeItem<String> root = new TreeItem<>("Root");
        TreeView<String> treeView = new TreeView<>(root);
        treeView.setShowRoot(false);

        // populate the prototype tree with some random nodes
        Random rand = new Random();
        for (int i = 4 + rand.nextInt(8); i > 0; i--) {
            TreeItem<String> treeItem = new TreeItem<>("Item " + i);
            root.getChildren().add(treeItem);
            for (int j = 2 + rand.nextInt(4); j > 0; j--) {
                TreeItem<String> childItem = new TreeItem<>("Child " + j);
                treeItem.getChildren().add(childItem);
            }
        }

        return treeView;
    }

}

