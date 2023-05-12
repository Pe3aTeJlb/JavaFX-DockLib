package docklib;

import docklib.dock.DockAnchor;
import docklib.dock.DockPane;
import docklib.draggabletabpane.DraggableTab;
import docklib.draggabletabpane.DraggableTabPane;
import docklib.draggabletabpane.TabGroup;
import docklib.utils.IconsManager;
import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.Random;

public class DraggableTabsTest extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        DraggableTabPane systemTabPaneLeft = new DraggableTabPane(primaryStage, TabGroup.System);
        systemTabPaneLeft.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        systemTabPaneLeft.setSide(Side.LEFT);
        systemTabPaneLeft.setRotateGraphic(true);

        systemTabPaneLeft.setCollapseOnInit(false);
        systemTabPaneLeft.setPrefExpandedSize(100);

        DraggableTab tab1 = new DraggableTab("", IconsManager.getImage("icon.png"), generateRandomTree());
        DraggableTab tab2 = new DraggableTab("System Tab 2", IconsManager.getImage("icon.png"), new Rectangle(100, 100, Color.AQUA));

        systemTabPaneLeft.addAll(
                tab1,
                tab2
        );

        DraggableTabPane systemTabPaneRight = new DraggableTabPane(primaryStage, TabGroup.System);
        systemTabPaneRight.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        systemTabPaneRight.setSide(Side.RIGHT);
        systemTabPaneRight.setRotateGraphic(true);

        DraggableTab tab21 = new DraggableTab("System Tab 5", IconsManager.getImage("icon.png"), generateRandomTree());
        DraggableTab tab22 = new DraggableTab("System Tab 6", IconsManager.getImage("icon.png"), new Rectangle(100, 100, Color.BLANCHEDALMOND));

        systemTabPaneRight.addAll(
                tab21,
                tab22
        );


        DraggableTabPane systemTabPaneBottom = new DraggableTabPane(primaryStage, TabGroup.System);
        systemTabPaneBottom.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        systemTabPaneBottom.setSide(Side.BOTTOM);
        systemTabPaneBottom.setRotateGraphic(true);

        DraggableTab tab31 = new DraggableTab("System Tab 9", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab32 = new DraggableTab("System Tab 10", IconsManager.getImage("icon.png"), new Rectangle(100, 100, Color.CORNSILK));
        DraggableTab tab33 = new DraggableTab("System Tab 10", IconsManager.getImage("icon.png"), new Rectangle(100, 100, Color.CORNSILK));
        DraggableTab tab34 = new DraggableTab("System Tab 10", IconsManager.getImage("icon.png"), new Rectangle(100, 100, Color.CORNSILK));
        DraggableTab tab35 = new DraggableTab("System Tab 10", IconsManager.getImage("icon.png"), new Rectangle(100, 100, Color.CORNSILK));
        DraggableTab tab36 = new DraggableTab("System Tab 10", IconsManager.getImage("icon.png"), new Rectangle(100, 100, Color.CORNSILK));
        DraggableTab tab37 = new DraggableTab("System Tab 10", IconsManager.getImage("icon.png"), new Rectangle(100, 100, Color.CORNSILK));
        DraggableTab tab38 = new DraggableTab("System Tab 10", IconsManager.getImage("icon.png"), new Rectangle(100, 100, Color.CORNSILK));
        DraggableTab tab39 = new DraggableTab("System Tab 10", IconsManager.getImage("icon.png"), new Rectangle(100, 100, Color.CORNSILK));

        systemTabPaneBottom.addAll(
                tab31,
                tab32,
                tab33,
                tab34,
                tab35,
                tab36,
                tab37,
                tab38,
                tab39
        );

        DraggableTabPane systemTabPaneTop = new DraggableTabPane(primaryStage, TabGroup.System);
        systemTabPaneTop.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        systemTabPaneTop.setSide(Side.TOP);
        systemTabPaneTop.setRotateGraphic(true);

        DraggableTab tab313 = new DraggableTab("System Tab 9", IconsManager.getImage("icon.png"), new TextArea());
        DraggableTab tab323 = new DraggableTab("System Tab 10", IconsManager.getImage("icon.png"), new Rectangle(100, 100, Color.CORNSILK));
        DraggableTab tab333 = new DraggableTab("System Tab 10", IconsManager.getImage("icon.png"), new Rectangle(100, 100, Color.CORNSILK));

        systemTabPaneTop.getTabs().addAll(
                tab313,
                tab323,
                tab333
        );

        DraggableTabPane workspaceTabPane = new DraggableTabPane(primaryStage, TabGroup.WorkSpace);
        workspaceTabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        workspaceTabPane.setSide(Side.TOP);
        workspaceTabPane.setRotateGraphic(true);
        workspaceTabPane.setUnDockable(false);


        VBox vBox = new VBox();
        Canvas pane = new Canvas(300, 300);
        SimpleIntegerProperty sip = new SimpleIntegerProperty();


        HBox box = new HBox();
        box.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label();
        label.textProperty().bind(
                sip.asString()
        );
        box.getChildren().addAll(label);

        pane.setOnMouseMoved(event -> {
            //System.out.println("moved");
            sip.set((int)event.getScreenX());
            label.requestLayout();
        });


        vBox.getChildren().addAll(pane,box);

        DraggableTab tab410 = new DraggableTab("WorkSpace Tab 1", IconsManager.getImage("icon.png"), vBox);
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


