package docklib;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class DockPane extends StackPane implements EventHandler<DockEvent> {

    static List<DockPane> dockPanes = new ArrayList<>();

    private ObservableMap<Node, DockNodeEventHandler> dockNodeEventFilters = FXCollections.observableHashMap();

    private Node root;

    private Popup dockPopup;
    private DockAnchorButton dockAnchorButton;
    private Rectangle dockAreaHighlighter;

    private Node dockNodeTarget;
    private boolean receivedEnter = false;

    private Node dockAreaDrag;
    private DockAnchor dockAnchor;

    public DockPane(){

        super();
        DockPane.dockPanes.add(this);

        this.addEventHandler(DockEvent.ANY, this);
        this.addEventFilter(DockEvent.ANY, event -> {

            if (event.getEventType() == DockEvent.DOCK_ENTER) {
                this.receivedEnter = true;
            } else if (event.getEventType() == DockEvent.DOCK_OVER) {
                this.dockNodeTarget = null;
            }

        });

        dockPopup = new Popup();
        dockPopup.setAutoFix(false);
        dockPopup.hide();

        dockAreaHighlighter = new Rectangle(this.getWidth(),this.getHeight(), 10, 10);
        dockAreaHighlighter.setManaged(false);
        dockAreaHighlighter.setMouseTransparent(true);
        dockAreaHighlighter.setFill(new Color(0.4, 0.859, 1, 0.196));
        dockAreaHighlighter.setVisible(false);

        dockAnchorButton = new DockAnchorButton(false, DockAnchor.TOP);

        StackPane dockRootPane = new StackPane();
        dockRootPane.prefWidthProperty().bind(this.widthProperty());
        dockRootPane.prefHeightProperty().bind(this.heightProperty());
        dockRootPane.setMouseTransparent(true);
        dockRootPane.setAlignment(Pos.CENTER);

        dockRootPane.getChildren().addAll(dockAreaHighlighter, dockAnchorButton);

        dockPopup.getContent().addAll(dockRootPane);

    }


    //Dock to neighboring Node
    public void dock(Node node, DockAnchor dockAnchor, Node neighbor) {

        DockNodeEventHandler dockNodeEventHandler = new DockNodeEventHandler(node);
        dockNodeEventFilters.put(node, dockNodeEventHandler);
        node.addEventFilter(DockEvent.DOCK_OVER, dockNodeEventHandler);

        SplitPane split = (SplitPane) root;
        if (split == null) {
            split = new SplitPane();
            split.getItems().add(node);
            root = split;
            this.getChildren().add(root);
            return;
        }

        // find the parent of the neighbor
        if (neighbor != null && neighbor != root) {
            Stack<Parent> stack = new Stack<>();
            stack.push((Parent) root);
            while (!stack.isEmpty()) {
                Parent parent = stack.pop();

                ObservableList<Node> children = parent.getChildrenUnmodifiable();

                if (parent instanceof SplitPane) {
                    SplitPane splitPane = (SplitPane) parent;
                    children = splitPane.getItems();
                }

                for (int i = 0; i < children.size(); i++) {
                    if (children.get(i) == neighbor) {
                        split = (SplitPane) parent;
                    } else if (children.get(i) instanceof Parent) {
                        stack.push((Parent) children.get(i));
                    }
                }
            }
        }

        Orientation requestedOrientation = (dockAnchor == DockAnchor.LEFT || dockAnchor == DockAnchor.RIGHT)
                ? Orientation.HORIZONTAL : Orientation.VERTICAL;

        // if the orientation is different then reparent the split pane
        if (split.getOrientation() != requestedOrientation) {
            if (split.getItems().size() > 1) {
                SplitPane splitPane = new SplitPane();
                if (split == root && neighbor == root) {
                    this.getChildren().set(this.getChildren().indexOf(root), splitPane);
                    splitPane.getItems().add(split);
                    root = splitPane;
                } else {
                    split.getItems().set(split.getItems().indexOf(neighbor), splitPane);
                    splitPane.getItems().add(neighbor);
                }

                split = splitPane;
            }
            split.setOrientation(requestedOrientation);
        }

        // finally dock the node to the correct split pane
        ObservableList<Node> splitItems = split.getItems();

        double magnitude = 0;

        if (splitItems.size() > 0) {
            if (split.getOrientation() == Orientation.HORIZONTAL) {
                for (Node splitItem : splitItems) {
                    magnitude += splitItem.prefWidth(0);
                }
            } else {
                for (Node splitItem : splitItems) {
                    magnitude += splitItem.prefHeight(0);
                }
            }
        }

        if (dockAnchor == DockAnchor.LEFT || dockAnchor == DockAnchor.TOP) {
            int relativeIndex = 0;
            if (neighbor != null && neighbor != root && splitItems.contains(neighbor)) {
                relativeIndex = splitItems.indexOf(neighbor);
            }

            splitItems.add(relativeIndex, node);

            if (splitItems.size() > 1) {
                if (split.getOrientation() == Orientation.HORIZONTAL) {
                    split.setDividerPosition(relativeIndex,
                            node.prefWidth(0) / (magnitude + node.prefWidth(0)));
                } else {
                    split.setDividerPosition(relativeIndex,
                            node.prefHeight(0) / (magnitude + node.prefHeight(0)));
                }
            }
        } else if (dockAnchor == DockAnchor.RIGHT || dockAnchor == DockAnchor.BOTTOM) {
            int relativeIndex = splitItems.size();
            if (neighbor != null && neighbor != root && splitItems.contains(neighbor)) {
                relativeIndex = splitItems.indexOf(neighbor) + 1;
            }

            splitItems.add(relativeIndex, node);
            if (splitItems.size() > 1) {
                if (split.getOrientation() == Orientation.HORIZONTAL) {
                    split.setDividerPosition(relativeIndex - 1,
                            1 - node.prefWidth(0) / (magnitude + node.prefWidth(0)));
                } else {
                    split.setDividerPosition(relativeIndex - 1,
                            1 - node.prefHeight(0) / (magnitude + node.prefHeight(0)));
                }
            }
        }

    }

    //Dock to root
    public void dock(Node node, DockAnchor dockAnchor) {
        dock(node, dockAnchor, root);
    }

/*
    public void undock(DockNode node) {

        DockNodeEventHandler dockNodeEventHandler = dockNodeEventFilters.get(node);
        node.removeEventFilter(DockEvent.DOCK_OVER, dockNodeEventHandler);
        dockNodeEventFilters.remove(node);

        // depth first search to find the parent of the node
        Stack<Parent> findStack = new Stack<Parent>();
        findStack.push((Parent) root);
        while (!findStack.isEmpty()) {
            Parent parent = findStack.pop();

            ObservableList<Node> children = parent.getChildrenUnmodifiable();

            if (parent instanceof SplitPane) {
                SplitPane split = (SplitPane) parent;
                children = split.getItems();
            }

            for (int i = 0; i < children.size(); i++) {
                if (children.get(i) == node) {
                    children.remove(i);

                    // start from the root again and remove any SplitPane's with no children in them
                    Stack<Parent> clearStack = new Stack<Parent>();
                    clearStack.push((Parent) root);
                    while (!clearStack.isEmpty()) {
                        parent = clearStack.pop();

                        children = parent.getChildrenUnmodifiable();

                        if (parent instanceof SplitPane) {
                            SplitPane split = (SplitPane) parent;
                            children = split.getItems();
                        }

                        for (i = 0; i < children.size(); i++) {
                            if (children.get(i) instanceof SplitPane) {
                                SplitPane split = (SplitPane) children.get(i);
                                if (split.getItems().size() < 1) {
                                    children.remove(i);
                                    continue;
                                } else {
                                    clearStack.push(split);
                                }
                            }

                        }
                    }

                    return;
                } else if (children.get(i) instanceof Parent) {
                    findStack.push((Parent) children.get(i));
                }
            }
        }
    }

 */

    private class DockNodeEventHandler implements EventHandler<DockEvent> {

        private Node node = null;

        public DockNodeEventHandler(Node node) {
            this.node = node;
        }

        @Override
        public void handle(DockEvent event) {
            DockPane.this.dockNodeTarget = node;
        }

    }

    @Override
    public void handle(DockEvent event) {

        dockNodeTarget = (Node) event.getTarget();

        Scene scene = dockNodeTarget.getScene();
        Window window = dockNodeTarget.getScene().getWindow();

        Insets winInsets = new Insets(
                scene.getY(),
                window.getWidth()-scene.getWidth() - scene.getX(),
                window.getHeight()-scene.getHeight() - scene.getY(),
                scene.getX()
        );

        if (event.getEventType() == DockEvent.DOCK_ENTER) {

            if (!dockPopup.isShowing()) {
                Point2D topLeft = DockPane.this.localToScreen(0, 0);
                dockPopup.show(this, topLeft.getX(), topLeft.getY());
            }

        } else if (event.getEventType() == DockEvent.DOCK_OVER) {

            this.receivedEnter = false;

            int winDockThreshold = 25;

            //dockAreaDrag = dockNodeDrag;
            dockAreaDrag = this;

            //left try just window at least
            dockAnchor = null;

            if (event.getScreenX() - window.getX() - winInsets.getLeft() < winDockThreshold
                    && event.getScreenY() - window.getY() - winInsets.getTop() > winDockThreshold
                    && (window.getY() + window.getHeight() - winInsets.getBottom()) - event.getScreenY() > winDockThreshold) {

                dockAnchorButton.setDockAnchor(DockAnchor.LEFT);
                dockAnchor = DockAnchor.LEFT;

            } else if ((window.getX() + window.getWidth() - winInsets.getRight() - winInsets.getLeft()) - event.getScreenX() < winDockThreshold
                    && event.getScreenY() - window.getY() - winInsets.getTop() > winDockThreshold
                    && (window.getY() + window.getHeight() - winInsets.getBottom()) - event.getScreenY() > winDockThreshold) {

                dockAnchorButton.setDockAnchor(DockAnchor.RIGHT);
                dockAnchor = DockAnchor.RIGHT;

            } else if (event.getScreenY() - window.getY() - winInsets.getTop() < winDockThreshold
                    && event.getScreenX() - window.getX() - winInsets.getLeft() > winDockThreshold
                    && (window.getX() + window.getWidth() - winInsets.getRight() - event.getScreenX() > winDockThreshold)) {

                dockAnchorButton.setDockAnchor(DockAnchor.TOP);
                dockAnchor = DockAnchor.TOP;

            } else if ((window.getY()) + window.getHeight() - winInsets.getTop() - winInsets.getBottom() - event.getScreenY() < winDockThreshold
                    && event.getScreenX() - window.getX() > winDockThreshold
                    && (window.getX() + window.getWidth() - winInsets.getRight() - event.getScreenX() > winDockThreshold)) {

                dockAnchorButton.setDockAnchor(DockAnchor.BOTTOM);
                dockAnchor = DockAnchor.BOTTOM;

            } else {
                dockAnchor = null;
            }

            if (dockNodeTarget != null && dockAnchor != null) {

                //Reset area highlight
                dockAnchorButton.setTranslateX(0);
                dockAnchorButton.setTranslateY(0);
                dockAreaHighlighter.setTranslateX(0);
                dockAreaHighlighter.setTranslateY(0);

                //set highlight coord
                if(dockAnchor == DockAnchor.RIGHT){

                    dockAreaHighlighter.setTranslateX(dockAreaDrag.getLayoutBounds().getWidth() / 2);
                    dockAreaHighlighter.setWidth(dockAreaDrag.getLayoutBounds().getWidth() / 2);
                    dockAreaHighlighter.setHeight(dockAreaDrag.getLayoutBounds().getHeight());

                    dockAnchorButton.setTranslateX(dockAreaDrag.getLayoutBounds().getWidth() / 2 - dockAnchorButton.getLayoutBounds().getWidth() / 2);

                } else if(dockAnchor == DockAnchor.LEFT){

                    dockAreaHighlighter.setWidth(dockAreaDrag.getLayoutBounds().getWidth() / 2);
                    dockAreaHighlighter.setHeight(dockAreaDrag.getLayoutBounds().getHeight());

                    dockAnchorButton.setTranslateX(-dockAreaDrag.getLayoutBounds().getWidth() / 2 + dockAnchorButton.getLayoutBounds().getWidth() / 2);

                } else if(dockAnchor == DockAnchor.TOP){

                    dockAreaHighlighter.setWidth(dockAreaDrag.getLayoutBounds().getWidth());
                    dockAreaHighlighter.setHeight(dockAreaDrag.getLayoutBounds().getHeight() / 2);

                    dockAnchorButton.setTranslateY(-dockAreaDrag.getLayoutBounds().getHeight() / 2 + dockAnchorButton.getLayoutBounds().getHeight() / 2);

                } else {

                    dockAreaHighlighter.setTranslateY(dockAreaDrag.getLayoutBounds().getHeight() / 2);
                    dockAreaHighlighter.setWidth(dockAreaDrag.getLayoutBounds().getWidth());
                    dockAreaHighlighter.setHeight(dockAreaDrag.getLayoutBounds().getHeight() / 2);

                    dockAnchorButton.setTranslateY(dockAreaDrag.getLayoutBounds().getHeight() / 2 - dockAnchorButton.getLayoutBounds().getHeight() / 2);

                }

                //Upper-left corner of dragnode to local coords of dockpane
                Point2D DragNodeToDockPane = new Point2D(
                        this.localToScene(dockNodeTarget.localToScreen(dockNodeTarget.getLayoutBounds())).getMinX(),
                        this.localToScene(dockNodeTarget.localToScreen(dockNodeTarget.getLayoutBounds())).getMinY()
                );

                double posX = DragNodeToDockPane.getX();
                double posY = DragNodeToDockPane.getY();

                if (!dockPopup.isShowing()) {
                    dockPopup.show(DockPane.this, posX, posY);
                }

                //if (dockAnchorButton.contains(dockAnchorButton.screenToLocal(event.getScreenX(), event.getScreenY()))) {
                        dockAreaHighlighter.setVisible(true);
               // }

            } else {
                dockPopup.hide();
                dockAreaHighlighter.setVisible(false);
            }

        }

        if (event.getEventType() == DockEvent.DOCK_RELEASED && event.getContents() != null) {

           // if (dockAnchorButton.contains(dockAnchorButton.screenToLocal(event.getScreenX(), event.getScreenY()))) {
                this.dock(event.getContents(), dockAnchor, dockAreaDrag);
          //  }

        }

        if ((event.getEventType() == DockEvent.DOCK_EXIT && !this.receivedEnter) || event.getEventType() == DockEvent.DOCK_RELEASED) {
            if (dockPopup.isShowing()) {
                dockPopup.hide();
            }
        }

    }



    public static class DockAnchorButton extends Button {

        private boolean dockRoot;
        private DockAnchor dockAnchor;


        public DockAnchorButton(boolean dockRoot, DockAnchor dockAnchor) {
            super();
            this.dockRoot = dockRoot;
            this.dockAnchor = dockAnchor;
        }


        public void setDockRoot(boolean dockRoot) {
            this.dockRoot = dockRoot;
        }

        public boolean isDockRoot() {
            return dockRoot;
        }

        public void setDockAnchor(DockAnchor dockAnchor){

            switch (dockAnchor){
                case TOP:       this.setGraphic(IconsManager.getImageView("topAnchor.png")); break;
                case LEFT:      this.setGraphic(IconsManager.getImageView("leftAnchor.png")); break;
                case RIGHT:     this.setGraphic(IconsManager.getImageView("rightAnchor.png")); break;
                case BOTTOM:    this.setGraphic(IconsManager.getImageView("bottomAnchor.png")); break;
                case CENTER:    this.setGraphic(IconsManager.getImageView("centerAnchor.png")); break;
            }

            this.dockAnchor = dockAnchor;

        }

        public DockAnchor getDockAnchor() {
            return dockAnchor;
        }

    }

}
