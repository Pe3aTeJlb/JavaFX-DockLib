package docklib;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;

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

    private Node dockNodeDrag;
    private boolean receivedEnter = false;

    private Node dockAreaDrag;
    private DockAnchor dockAnchorDrag;

    public DockPane(){

        super();
        DockPane.dockPanes.add(this);

        this.addEventHandler(DockEvent.ANY, this);
        this.addEventFilter(DockEvent.ANY, event -> {

            if (event.getEventType() == DockEvent.DOCK_ENTER) {
                this.receivedEnter = true;
            } else if (event.getEventType() == DockEvent.DOCK_OVER) {
                this.dockNodeDrag = null;
            }
        });


        dockPopup = new Popup();
        dockPopup.setAutoFix(false);
        dockPopup.hide();

        dockAreaHighlighter = new Rectangle();
        dockAreaHighlighter.setManaged(false);
        dockAreaHighlighter.setMouseTransparent(true);
        dockAreaHighlighter.setFill(Color.RED);
        dockAreaHighlighter.setVisible(false);

        dockAnchorButton = new DockAnchorButton(false, DockAnchor.TOP);

        StackPane dockRootPane = new StackPane();
        dockRootPane.prefWidthProperty().bind(this.widthProperty());
        dockRootPane.prefHeightProperty().bind(this.heightProperty());

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
            if (neighbor != null && neighbor != root) {
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
            if (neighbor != null && neighbor != root) {
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

    private class DockNodeEventHandler implements EventHandler<DockEvent> {

        private Node node = null;

        public DockNodeEventHandler(Node node) {
            this.node = node;
        }

        @Override
        public void handle(DockEvent event) {
            DockPane.this.dockNodeDrag = node;
        }

    }

    @Override
    public void handle(DockEvent event) {

        if (event.getEventType() == DockEvent.DOCK_ENTER) {

            if (!dockPopup.isShowing()) {
                Point2D topLeft = DockPane.this.localToScreen(0, 0);
                dockPopup.show(this, topLeft.getX(), topLeft.getY());
            }

        } else if (event.getEventType() == DockEvent.DOCK_OVER) {

            this.receivedEnter = false;

            dockAnchorDrag = null;
            dockAreaDrag = dockNodeDrag;

            //Set focus css
            if (dockAnchorButton.contains(dockAnchorButton.screenToLocal(event.getScreenX(), event.getScreenY()))) {
                dockAnchorDrag = dockAnchorButton.getDockAnchor();
                if (dockAnchorButton.isDockRoot()) {
                    dockAreaDrag = root;
                }
                dockAnchorButton.requestFocus();
            }

            if (dockAnchorDrag != null) {

                Point2D originToScene = dockAreaDrag.localToScene(0, 0).subtract(this.localToScene(0, 0));
                dockAnchorButton.setDockAnchor(dockAnchorDrag);

                dockAreaHighlighter.setVisible(true);
                dockAreaHighlighter.relocate(originToScene.getX(), originToScene.getY());
                if (dockAnchorDrag == DockAnchor.RIGHT) {
                    dockAreaHighlighter.setTranslateX(dockAreaDrag.getLayoutBounds().getWidth() / 2);
                } else {
                    dockAreaHighlighter.setTranslateX(0);
                }

                if (dockAnchorDrag == DockAnchor.BOTTOM) {
                    dockAreaHighlighter.setTranslateY(dockAreaDrag.getLayoutBounds().getHeight() / 2);
                } else {
                    dockAreaHighlighter.setTranslateY(0);
                }

                if (dockAnchorDrag == DockAnchor.LEFT || dockAnchorDrag == DockAnchor.RIGHT) {
                    dockAreaHighlighter.setWidth(dockAreaDrag.getLayoutBounds().getWidth() / 2);
                } else {
                    dockAreaHighlighter.setWidth(dockAreaDrag.getLayoutBounds().getWidth());
                }

                if (dockAnchorDrag == DockAnchor.TOP || dockAnchorDrag == DockAnchor.BOTTOM) {
                    dockAreaHighlighter.setHeight(dockAreaDrag.getLayoutBounds().getHeight() / 2);
                } else {
                    dockAreaHighlighter.setHeight(dockAreaDrag.getLayoutBounds().getHeight());
                }

            } else {
                dockAreaHighlighter.setVisible(false);
            }

            if (dockNodeDrag != null) {
                Point2D originToScreen = dockNodeDrag.localToScreen(0, 0);

                double posX = originToScreen.getX() + dockNodeDrag.getLayoutBounds().getWidth() / 2
                        - dockAreaHighlighter.getWidth() / 2;
                double posY = originToScreen.getY() + dockNodeDrag.getLayoutBounds().getHeight() / 2
                        - dockAreaHighlighter.getHeight() / 2;

                if (!dockPopup.isShowing()) {
                    dockPopup.show(DockPane.this, posX, posY);
                } else {
                    dockPopup.setX(posX);
                    dockPopup.setY(posY);
                }

                // set visible after moving the popup
                dockAreaHighlighter.setVisible(true);
            } else {
                dockAreaHighlighter.setVisible(false);
            }

        }

        if (event.getEventType() == DockEvent.DOCK_RELEASED && event.getContents() != null) {
            /*
            if (dockAnchorDrag != null && dockIndicatorOverlay.isShowing()) {
                DockNode dockNode = (DockNode) event.getContents();
                dockNode.dock(this, dockAnchorDrag, dockAreaDrag);
            }

             */
        }

        if ((event.getEventType() == DockEvent.DOCK_EXIT && !this.receivedEnter) || event.getEventType() == DockEvent.DOCK_RELEASED) {
            if (dockPopup.isShowing()) {
                dockPopup.hide();
            }
        }

    }


    public class DockAnchorButton extends Button {

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
