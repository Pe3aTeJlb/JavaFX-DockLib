package docklib;

import docklib.dock.DockAnchor;
import docklib.dock.DockPane;
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

public class Demo extends Application {


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        DraggableTabPane workspaceTabPane = new DraggableTabPane(TabGroup.WorkSpace);
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

        TabPane tabPane = new TabPane();

        Tab tab1 = new Tab("tab 1 tab 1 tab 1", new Rectangle(100, 100, Color.ROYALBLUE));
        Tab tab2 = new Tab("tab 2 tab 2 tab 2 ", new Rectangle(100, 100, Color.ROSYBROWN));
        Tab tab4 = new Tab("tab 3 tab 3 tab 3", new Rectangle(100, 100, Color.ORANGE));
        Tab tab5 = new Tab("tab 3 tab 3 tab 3", new Rectangle(100, 100, Color.ORANGE));
        Tab tab6 = new Tab("tab 3 tab 3 tab 3", new Rectangle(100, 100, Color.ORANGE));
        Tab tab7 = new Tab("tab 3 tab 3 tab 3", new Rectangle(100, 100, Color.ORANGE));
        Tab tab8 = new Tab("tab 3 tab 3 tab 3", new Rectangle(100, 100, Color.ORANGE));
        Tab tab9 = new Tab("tab 3 tab 3 tab 3", new Rectangle(100, 100, Color.ORANGE));
        Tab tab11 = new Tab("tab 3 tab 3 tab 3", new Rectangle(100, 100, Color.ORANGE));
        Tab tab22 = new Tab("tab 3 tab 3 tab 3", new Rectangle(100, 100, Color.ORANGE));
        Tab tab33 = new Tab("tab 3 tab 3 tab 3", new Rectangle(100, 100, Color.ORANGE));
        Tab tab44 = new Tab("tab 3 tab 3 tab 3", new Rectangle(100, 100, Color.ORANGE));


        tabPane.getTabs().addAll(
                tab1,
                tab2,
                tab4,
                tab5,
                tab6,
                tab7,
                tab8,
                tab9,
                tab11,
                tab22,
                tab33,
                tab44

        );


        DockPane dockPane = new DockPane(false);

        dockPane.dock(workspaceTabPane, DockAnchor.TOP);

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
