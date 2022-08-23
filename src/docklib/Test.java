package docklib;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class Test extends Application {

    @Override
    public void start(Stage primaryStage) {

        HBox hBox = new HBox();
        hBox.setAlignment(Pos.TOP_LEFT);
        Scene scene = new Scene(hBox, 800, 600);

        primaryStage.setTitle("Hello World");
        primaryStage.setScene(scene);

        DraggableTabPane draggableTabPane1 = new DraggableTabPane(primaryStage, TabGroup.System);
        draggableTabPane1.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        draggableTabPane1.setSide(Side.LEFT);
        draggableTabPane1.setRotateGraphic(true);

        DraggableTab tab1 = new DraggableTab("System Tab 1", draggableTabPane1, "icon.png");
        tab1.setContent(new Rectangle(500, 500, Color.ALICEBLUE));

        DraggableTab tab2 = new DraggableTab("System Tab 2", draggableTabPane1, "icon.png");
        tab2.setContent(new Rectangle(500, 500, Color.AQUA));

        DraggableTab tab3 = new DraggableTab("System Tab 3", draggableTabPane1, "icon.png");
        tab3.setContent(new Rectangle(500, 500, Color.AQUAMARINE));

        DraggableTab tab4 = new DraggableTab("System Tab 4", draggableTabPane1, "icon.png");
        tab4.setContent(new Rectangle(500, 500, Color.AZURE));

        draggableTabPane1.addAll(
                tab1,
                tab2,
                tab3,
                tab4
        );

        DraggableTabPane draggableTabPane2 = new DraggableTabPane(primaryStage, TabGroup.WorkSpace);
        draggableTabPane2.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        draggableTabPane2.setSide(Side.TOP);
        draggableTabPane2.setRotateGraphic(true);

        DraggableTab tab21 = new DraggableTab("WorkSpace Tab 1", draggableTabPane2, "icon.png");
        tab21.setContent(new Rectangle(500, 500, Color.BLACK));

        DraggableTab tab22 = new DraggableTab("WorkSpace Tab 2", draggableTabPane2, "icon.png");
        tab22.setContent(new Rectangle(500, 500, Color.BLANCHEDALMOND));

        DraggableTab tab23 = new DraggableTab("WorkSpace Tab 3", draggableTabPane2, "icon.png");
        tab23.setContent(new Rectangle(500, 500, Color.BURLYWOOD));

        DraggableTab tab24 = new DraggableTab("WorkSpace Tab 4", draggableTabPane2, "icon.png");
        tab24.setContent(new Rectangle(500, 500, Color.BLUE));

        draggableTabPane2.addAll(
                tab21,
                tab22,
                tab23,
                tab24
        );


        DraggableTabPane draggableTabPane3 = new DraggableTabPane(primaryStage, TabGroup.WorkSpace);
        draggableTabPane3.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        draggableTabPane3.setSide(Side.BOTTOM);
        draggableTabPane3.setRotateGraphic(true);

        DraggableTab tab31 = new DraggableTab("WorkSpace Tab 5", draggableTabPane3, "icon.png");
        tab31.setContent(new Rectangle(500, 500, Color.CORAL));

        DraggableTab tab32 = new DraggableTab("WorkSpace Tab 6", draggableTabPane3, "icon.png");
        tab32.setContent(new Rectangle(500, 500, Color.CORNSILK));

        DraggableTab tab33 = new DraggableTab("WorkSpace Tab 7", draggableTabPane3, "icon.png");
        tab33.setContent(new Rectangle(500, 500, Color.CYAN));

        DraggableTab tab34 = new DraggableTab("WorkSpace Tab 8", draggableTabPane3, "icon.png");
        tab34.setContent(new Rectangle(500, 500, Color.CRIMSON));

        draggableTabPane3.addAll(
                tab31,
                tab32,
                tab33,
                tab34
        );

        DraggableTabPane draggableTabPane4 = new DraggableTabPane(primaryStage, TabGroup.WorkSpace);
        draggableTabPane4.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        draggableTabPane4.setSide(Side.RIGHT);
        draggableTabPane4.setRotateGraphic(true);

        DraggableTab tab41 = new DraggableTab("WorkSpace Tab 9", draggableTabPane4, "icon.png");
        tab41.setContent(new Rectangle(500, 500, Color.RED));

        DraggableTab tab42 = new DraggableTab("WorkSpace Tab 10", draggableTabPane4, "icon.png");
        tab42.setContent(new Rectangle(500, 500, Color.ROYALBLUE));

        DraggableTab tab43 = new DraggableTab("WorkSpace Tab 11", draggableTabPane4, "icon.png");
        tab43.setContent(new Rectangle(500, 500, Color.ROSYBROWN));

        DraggableTab tab44 = new DraggableTab("WorkSpace Tab 12", draggableTabPane4, "icon.png");
        tab44.setContent(new Rectangle(500, 500, Color.ORANGE));

        draggableTabPane4.addAll(
                tab41,
                tab42,
                tab43,
                tab44
        );



        hBox.getChildren().addAll(
                draggableTabPane1,
                draggableTabPane2,
                draggableTabPane3,
                draggableTabPane4
        );

        HBox.setHgrow(draggableTabPane1, Priority.ALWAYS);
        HBox.setHgrow(draggableTabPane2, Priority.ALWAYS);
        HBox.setHgrow(draggableTabPane3, Priority.ALWAYS);
        HBox.setHgrow(draggableTabPane4, Priority.ALWAYS);

        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }

}
