package docklib;

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

public class Demo extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        DraggableTabPane systemTabPaneLeft = new DraggableTabPane(TabGroup.System);
        systemTabPaneLeft.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        systemTabPaneLeft.setSide(Side.LEFT);
        systemTabPaneLeft.setRotateGraphic(true);

        DraggableTab tab1 = new DraggableTab("System Tab 1", "icon.png", generateRandomTree());
        DraggableTab tab2 = new DraggableTab("System Tab 2", "icon.png", new Rectangle(100, 100, Color.AQUA));

        systemTabPaneLeft.addAll(
                tab1,
                tab2
        );

        DraggableTabPane systemTabPaneRight = new DraggableTabPane(TabGroup.System);
        systemTabPaneRight.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        systemTabPaneRight.setSide(Side.RIGHT);
        systemTabPaneRight.setRotateGraphic(true);

        DraggableTab tab21 = new DraggableTab("System Tab 5", "icon.png", generateRandomTree());
        DraggableTab tab22 = new DraggableTab("System Tab 6", "icon.png", new Rectangle(100, 100, Color.BLANCHEDALMOND));

        systemTabPaneRight.addAll(
                tab21,
                tab22
        );


        DraggableTabPane systemTabPaneBottom = new DraggableTabPane(TabGroup.System);
        systemTabPaneBottom.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        systemTabPaneBottom.setSide(Side.BOTTOM);
        systemTabPaneBottom.setRotateGraphic(true);

        DraggableTab tab31 = new DraggableTab("System Tab 9", "icon.png", new TextArea());
        DraggableTab tab32 = new DraggableTab("System Tab 10", "icon.png", new Rectangle(100, 100, Color.CORNSILK));

        systemTabPaneBottom.addAll(
                tab31,
                tab32
        );

        DraggableTabPane workspaceTabPane = new DraggableTabPane(TabGroup.WorkSpace);
        workspaceTabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        workspaceTabPane.setSide(Side.TOP);
        workspaceTabPane.setRotateGraphic(true);

        DraggableTab tab41 = new DraggableTab("WorkSpace Tab 1", "icon.png", new TextArea());
        DraggableTab tab42 = new DraggableTab("WorkSpace Tab 2", "icon.png", new Rectangle(100, 100, Color.ROYALBLUE));
        DraggableTab tab43 = new DraggableTab("WorkSpace Tab 3", "icon.png", new Rectangle(100, 100, Color.ROSYBROWN));
        DraggableTab tab44 = new DraggableTab("WorkSpace Tab 4", "icon.png", new Rectangle(100, 100, Color.ORANGE));

        workspaceTabPane.addAll(
                tab41,
                tab42,
                tab43,
                tab44
        );

        DockPane dockPane = new DockPane(false);
        dockPane.dock(systemTabPaneLeft, DockAnchor.LEFT);
        dockPane.dock(workspaceTabPane, DockAnchor.RIGHT);
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
