package docklib.dock;

import com.sun.javafx.css.StyleManager;
import docklib.customsplitpane.CustomSplitPane;
import docklib.draggabletabpane.DraggableTab;
import docklib.draggabletabpane.DraggableTabPane;
import docklib.utils.IconsManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class DockPane extends StackPane implements EventHandler<DockEvent> {

    public static List<DockPane> dockPanes = new ArrayList<>();

    private SplitPane root;
    private Node promptNode;

    private Popup winDockPopup, nodeDockPopup;
    private double CIRCLE_RADIUS = 100;
    private DockAnchorButton dockAnchorButton;
    private StackPane nodeDockSelector;
    private ObservableList<DockAnchorButton> gridPaneBtns;
    private Rectangle winDockAreaIndicator, nodeDockAreaIndicator;

    private Node dockNodeTarget;
    private boolean receivedEnter = false;
    private DockAnchor dockAnchor;

    private boolean winInteractive;
    private boolean useDockPaneBoundaryForSideDock = false;

    public static class DockAnchorButton extends Button {

        private DockAnchor dockAnchor;

        public DockAnchorButton() {
            super();
            this.setMouseTransparent(true);
            this.getStyleClass().add("node-dock-selector-button");
            this.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true);
        }

        public DockAnchorButton(DockAnchor dockAnchor) {
            super();
            this.setMouseTransparent(true);
            this.dockAnchor = dockAnchor;
            this.setDockAnchor(this.dockAnchor);
            this.getStyleClass().add("node-dock-selector-button");
        }

        public void setDockAnchor(DockAnchor dockAnchor){

            switch (dockAnchor){
                case TOP:       this.setGraphic(IconsManager.getImageView("topAnchor.png")); break;
                case LEFT:      this.setGraphic(IconsManager.getImageView("leftAnchor.png")); break;
                case RIGHT:     this.setGraphic(IconsManager.getImageView("rightAnchor.png")); break;
                case BOTTOM:    this.setGraphic(IconsManager.getImageView("bottomAnchor.png")); break;
                case CENTER:    this.setGraphic(IconsManager.getImageView("centerAnchor.png")); break;
            }

        }

        public DockAnchor getDockAnchor(){
            return dockAnchor;
        }

    }

    public static void initializeDefaultUserAgentStylesheet() {
        StyleManager.getInstance()
                .addUserAgentStylesheet(DockPane.class.getResource("/docklib/resources/docklib.css").toExternalForm());
    }

    public DockPane(){
        this(null, true);
    }

    public DockPane(Node promptNode){
        this(promptNode,true);
    }

    public DockPane(boolean winInteractive){
        this(null,winInteractive);
    }

    public DockPane(Node promptNode, boolean winInteractive){

        super();

        this.winInteractive = winInteractive;

        setPromptNode(promptNode);

        this.addEventHandler(DockEvent.ANY, this);
        this.addEventFilter(DockEvent.ANY, event -> {
            if (event.getEventType() == DockEvent.DOCK_ENTER) {
                this.receivedEnter = true;
                Collections.swap(dockPanes, dockPanes.indexOf(this), 0);
            } else if (event.getEventType() == DockEvent.DOCK_OVER) {
                this.dockNodeTarget = null;
            }
        });

        //WinDockPopup
        winDockPopup = new Popup();
        winDockPopup.setAutoFix(false);
        winDockPopup.hide();

        winDockAreaIndicator = new Rectangle();
        winDockAreaIndicator.setManaged(false);
        winDockAreaIndicator.setMouseTransparent(true);
        winDockAreaIndicator.setVisible(false);

        dockAnchorButton = new DockAnchorButton();

        StackPane dockRootPane = new StackPane();
        dockRootPane.prefWidthProperty().bind(this.widthProperty());
        dockRootPane.prefHeightProperty().bind(this.heightProperty());
        dockRootPane.setMouseTransparent(true);
        dockRootPane.setAlignment(Pos.CENTER);

        dockRootPane.getChildren().addAll(winDockAreaIndicator, dockAnchorButton);

        winDockPopup.getContent().addAll(dockRootPane);

        //nodeDock
        nodeDockPopup = new Popup();
        nodeDockPopup.setAutoFix(false);
        nodeDockPopup.hide();

        DockAnchorButton dockTopBtn = new DockAnchorButton(DockAnchor.TOP);
        DockAnchorButton dockBottomBtn = new DockAnchorButton(DockAnchor.BOTTOM);
        DockAnchorButton dockLeftBtn = new DockAnchorButton(DockAnchor.LEFT);
        DockAnchorButton dockRightBtn = new DockAnchorButton(DockAnchor.RIGHT);
        DockAnchorButton dockCenterBtn = new DockAnchorButton(DockAnchor.CENTER);

        gridPaneBtns = FXCollections.observableArrayList(dockTopBtn, dockBottomBtn, dockLeftBtn, dockRightBtn, dockCenterBtn);

        nodeDockSelector = new StackPane();
        nodeDockSelector.setAlignment(Pos.CENTER);

        Circle back = new Circle();
        back.getStyleClass().add("node-dock-selector-back");
        back.setRadius(CIRCLE_RADIUS);

        GridPane gridPane = new GridPane();
        gridPane.getStyleClass().add("node-dock-selector");
        gridPane.setPrefSize(nodeDockSelector.getPrefWidth(), nodeDockSelector.getPrefHeight());
        gridPane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        gridPane.add(back, 1, 1);
        gridPane.add(dockTopBtn, 1, 0);
        gridPane.add(dockRightBtn, 2, 1);
        gridPane.add(dockBottomBtn, 1, 2);
        gridPane.add(dockLeftBtn, 0, 1);
        gridPane.add(dockCenterBtn, 1, 1);

        for(ColumnConstraints c: gridPane.getColumnConstraints()){
            c.setHgrow(Priority.ALWAYS);
        }
        for(RowConstraints c: gridPane.getRowConstraints()){
            c.setVgrow(Priority.ALWAYS);
        }

        nodeDockSelector.getChildren().addAll(back, gridPane);

        nodeDockAreaIndicator = new Rectangle();
        nodeDockAreaIndicator.setManaged(false);
        nodeDockAreaIndicator.setMouseTransparent(true);
        nodeDockAreaIndicator.setVisible(false);

        nodeDockPopup.getContent().addAll(nodeDockAreaIndicator, nodeDockSelector);

        winDockAreaIndicator.getStyleClass().add("dock-area-indicator");
        nodeDockAreaIndicator.getStyleClass().add("dock-area-indicator");

        //this.getScene().getWindow().setOnHidden(event -> dockPanes.remove(this));
        dockPanes.add(this);

    }

    public void setUseDockPaneBoundaryForSideDock(boolean val){
        useDockPaneBoundaryForSideDock = val;
    }

    public boolean getUseDockPaneBoundaryForSideDock(){
        return useDockPaneBoundaryForSideDock;
    }

    public void setPromptNode(Node node){
        if (node != null){
            if (promptNode != null){
                this.getChildren().remove(0);
            }
            this.promptNode = node;
            this.getChildren().add(0,node);
        }
    }


    public SplitPane getRoot(){
        return root;
    }

    //Dock to neighboring Node
    public void dock(Node node, DockAnchor dockAnchor, Node neighbor) {

        if(dockAnchor == DockAnchor.CENTER) {
            if (node instanceof DraggableTabPane && neighbor instanceof DraggableTabPane) {
                ((DraggableTabPane) neighbor).addTab((DraggableTab)((DraggableTabPane) node).getTabs().get(0));
                return;
            } else {
                dockAnchor = DockAnchor.LEFT;
            }
        }

        SplitPane split = (SplitPane) root;
        if (split == null) {
            split = new CustomSplitPane();
            split.getItems().add(node);
            if (node instanceof Dockable) {
                ((Dockable) node).setDockPane(this, split);
            }
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
                SplitPane splitPane = new CustomSplitPane();
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

                split.setDividerPosition(relativeIndex, (relativeIndex + 1) * (1.0 / splitItems.size()));
/*
                if (split.getOrientation() == Orientation.HORIZONTAL) {
                    split.setDividerPosition(relativeIndex -1,
                            node.getLayoutBounds().getWidth() / (magnitude + node.getLayoutBounds().getWidth()));
                } else {
                    split.setDividerPosition(relativeIndex -1,
                            node.getLayoutBounds().getHeight() / (magnitude + node.getLayoutBounds().getHeight()));
                }*/

            }
        } else if (dockAnchor == DockAnchor.RIGHT || dockAnchor == DockAnchor.BOTTOM) {
            int relativeIndex = splitItems.size();
            if (neighbor != null && neighbor != root && splitItems.contains(neighbor)) {
                relativeIndex = splitItems.indexOf(neighbor) + 1;
            }

            splitItems.add(relativeIndex, node);

            if (splitItems.size() > 1) {

                split.setDividerPosition(relativeIndex, relativeIndex * 1.0 / splitItems.size());
/*
                if (split.getOrientation() == Orientation.HORIZONTAL) {
                    split.setDividerPosition(relativeIndex,
                            1 - node.getLayoutBounds().getWidth() / (magnitude + node.getLayoutBounds().getWidth()));
                } else {
                    split.setDividerPosition(relativeIndex,
                            1 - node.getLayoutBounds().getHeight() / (magnitude + node.getLayoutBounds().getHeight()));
                }*/

            }
        }

        //Dock center in dragtabpane = add tab to neighbor tabpane
        if (node instanceof Dockable) {
            ((Dockable) node).setDockPane(this, split);
        }

    }

    //Dock to root
    public void dock(Node node, DockAnchor dockAnchor) {
        dock(node, dockAnchor, root);
    }

    public void undock(Node node) {

        // depth first search to find the parent of the node
        Stack<Parent> findStack = new Stack<>();
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

                    haveEmptyPanes(this);
                    if (root.getParent() == null) { root = null; }
                    //drawTree(root, "");
                    return;

                } else if (children.get(i) instanceof Parent) {
                    findStack.push((Parent) children.get(i));
                }
            }
        }

    }

    private boolean haveEmptyPanes(Node parent){

        ObservableList<Node> children;

        if (parent instanceof DockPane){
            DockPane split = (DockPane) parent;
            children = split.getChildren();
            children.removeIf(this::haveEmptyPanes);
            return children.isEmpty();
        } else if (parent instanceof SplitPane) {
            SplitPane split = (SplitPane) parent;
            children = split.getItems();
            children.removeIf(this::haveEmptyPanes);
            return children.isEmpty();
        } else {
            return false;
        }

    }

    private void drawTree(Node parent, String dash){

        System.out.println(dash + parent);
        ObservableList<Node> children;
        if (parent instanceof SplitPane) {
            SplitPane split = (SplitPane) parent;
            children = split.getItems();
            for (Node n: children){
                drawTree(n, dash+"    ");
            }
        }

    }



    @Override
    public void handle(DockEvent event) {

        if(event.getTarget() == null)
            return;

        dockNodeTarget = (Node) event.getTarget();

        Node targetRoot = (Node) event.getTarget();
        //check, if dockNo relate to this dockpane
        while (!(targetRoot.getParent() instanceof DockPane)){
            targetRoot = targetRoot.getParent();
        }
        targetRoot = targetRoot.getParent();
        if(targetRoot != this) return;


        //Fine the parent like DockPane or DraggableTabPane for this target
        if(!(event.getTarget() instanceof DockPane) && !(event.getTarget() instanceof DraggableTabPane)) {
            while (!(dockNodeTarget.getParent() instanceof DockPane) && !(dockNodeTarget.getParent() instanceof DraggableTabPane)) {
                dockNodeTarget = dockNodeTarget.getParent();
            }
            dockNodeTarget = dockNodeTarget.getParent();
        }

        if ((dockNodeTarget instanceof DraggableTabPane) && !((DraggableTabPane)dockNodeTarget).isUnDockable()){
            return;
        }

        //if it is TabPane check tabgroup

        if(dockNodeTarget instanceof DraggableTabPane){
            if(((DraggableTabPane)event.getContents()).getTabGroup()
                    != ((DraggableTabPane)dockNodeTarget).getTabGroup() ||
                    !((DraggableTabPane)event.getContents()).sameProject((DraggableTabPane)dockNodeTarget)){
                return;
            }
            if(((DraggableTabPane)dockNodeTarget).getTabs().isEmpty()){
                setCenterDockOnly();
            } else {
                setDockAvailable();
            }
        }

        Scene scene = dockNodeTarget.getScene();
        Window window = dockNodeTarget.getScene().getWindow();
        Insets winInsets = new Insets(
                scene.getY(),
                window.getWidth()-scene.getWidth() - scene.getX(),
                window.getHeight()-scene.getHeight() - scene.getY(),
                scene.getX()
        );

        Bounds screenDockBounds = this.localToScreen(this.getLayoutBounds());

        if (event.getEventType() == DockEvent.DOCK_ENTER) {
/*
            if (!winDockPopup.isShowing()) {
                Point2D topLeft = DockPane.this.localToScreen(0, 0);
                winDockPopup.show(this, topLeft.getX(), topLeft.getY());
            }*/

        } else if (event.getEventType() == DockEvent.DOCK_OVER) {

            this.receivedEnter = false;
            dockAnchor = null;

            int winThreshold = 25;
            int dockThreshold = 25;
            //Check if at window border
            if(winInteractive && event.getScreenX() - window.getX() - winInsets.getLeft() < winThreshold){
                dockAnchor = DockAnchor.LEFT;
                dockNodeTarget = this;
                showWinDockPopup(dockNodeTarget,dockAnchor);
            } else if(winInteractive && (window.getX() + window.getWidth() - winInsets.getRight() - winInsets.getLeft()) - event.getScreenX() < winThreshold){
                dockAnchor = DockAnchor.RIGHT;
                dockNodeTarget = this;
                showWinDockPopup(dockNodeTarget, dockAnchor);
            } else if(winInteractive && event.getScreenY() - window.getY() - winInsets.getTop() < winThreshold){
                dockAnchor = DockAnchor.TOP;
                dockNodeTarget = this;
                showWinDockPopup(dockNodeTarget, dockAnchor);
            } else if(winInteractive && (window.getY()) + window.getHeight() - winInsets.getTop() - winInsets.getBottom() - event.getScreenY() < winThreshold){
                dockAnchor = DockAnchor.BOTTOM;
                dockNodeTarget = this;
                showWinDockPopup(dockNodeTarget,dockAnchor);
            } else if (useDockPaneBoundaryForSideDock && event.getScreenX() - screenDockBounds.getMinX() < dockThreshold){
                dockAnchor = DockAnchor.LEFT;
                dockNodeTarget = this;
                showDockPaneDockPopup(dockNodeTarget,dockAnchor);
            } else if (useDockPaneBoundaryForSideDock && screenDockBounds.getMaxX() - event.getScreenX() < dockThreshold){
                dockAnchor = DockAnchor.RIGHT;
                dockNodeTarget = this;
                showDockPaneDockPopup(dockNodeTarget, dockAnchor);
            } else if (useDockPaneBoundaryForSideDock && screenDockBounds.getMaxY() - event.getScreenY() < dockThreshold){
                dockAnchor = DockAnchor.BOTTOM;
                dockNodeTarget = this;
                showDockPaneDockPopup(dockNodeTarget, dockAnchor);
            } else {

                // else check if we at node center

                hideWinDockPopup();

                Point2D eventPos = new Point2D(event.getScreenX(), event.getScreenY());
                Point2D dockTargetNodeCenter = new Point2D(
                        dockNodeTarget.localToScreen(dockNodeTarget.getLayoutBounds()).getCenterX(),
                        dockNodeTarget.localToScreen(dockNodeTarget.getLayoutBounds()).getCenterY()
                );

                if(eventPos.distance(dockTargetNodeCenter) < CIRCLE_RADIUS){
                    showNodeDockPopup(dockNodeTarget, event);
                } else {
                    hideNodeDockPopup();
                    hideWinDockPopup();
                }

            }

        }

        if (event.getEventType() == DockEvent.DOCK_RELEASED && event.getContents() != null) {
            //Dock to targetNode else to this pane
            if(nodeDockPopup.isShowing() && dockAnchor != null &&
                    nodeDockSelector.contains(nodeDockSelector.screenToLocal(event.getScreenX(), event.getScreenY()))) {
                this.dock(event.getContents(), dockAnchor, dockNodeTarget);
                ((DraggableTab)((DraggableTabPane)event.getContents()).getTabs().get(0)).dockEventCallback(true, event);
            } else if(winDockPopup.isShowing() &&
                    winDockAreaIndicator.contains(winDockAreaIndicator.screenToLocal(event.getScreenX(), event.getScreenY()))) {
                this.dock(event.getContents(), dockAnchor);
                ((DraggableTab)((DraggableTabPane)event.getContents()).getTabs().get(0)).dockEventCallback(true, event);
            } else {
                //detach tab
                ((DraggableTab)((DraggableTabPane)event.getContents()).getTabs().get(0)).dockEventCallback(false, event);
            }

            hideWinDockPopup();
            hideNodeDockPopup();

        }

        if (event.getEventType() == DockEvent.DOCK_EXIT && !this.receivedEnter) {
            hideWinDockPopup();
            hideNodeDockPopup();
        }

    }

    private void setCenterDockOnly(){
        for(int i = 0; i < gridPaneBtns.size() - 1; i++){
            gridPaneBtns.get(i).setVisible(false);
        }
    }

    private void setDockAvailable(){
        for(int i = 0; i < gridPaneBtns.size() - 1; i++){
            gridPaneBtns.get(i).setVisible(true);
        }
    }

    public void showWinDockPopup(Node dockNodeTarget, DockAnchor dockAnchor){

        hideNodeDockPopup();

        dockAnchorButton.setDockAnchor(dockAnchor);

        dockAnchorButton.setTranslateX(0);
        dockAnchorButton.setTranslateY(0);
        winDockAreaIndicator.setTranslateX(0);
        winDockAreaIndicator.setTranslateY(0);

        //set highlight coord
        if(dockAnchor == DockAnchor.RIGHT){

            winDockAreaIndicator.setTranslateX(dockNodeTarget.getLayoutBounds().getWidth() / 2);
            winDockAreaIndicator.setWidth(dockNodeTarget.getLayoutBounds().getWidth() / 2);
            winDockAreaIndicator.setHeight(dockNodeTarget.getLayoutBounds().getHeight());

            dockAnchorButton.setTranslateX(dockNodeTarget.getLayoutBounds().getWidth() / 2 - dockAnchorButton.getLayoutBounds().getWidth() / 2);

        } else if(dockAnchor == DockAnchor.LEFT){

            winDockAreaIndicator.setWidth(dockNodeTarget.getLayoutBounds().getWidth() / 2);
            winDockAreaIndicator.setHeight(dockNodeTarget.getLayoutBounds().getHeight());

            dockAnchorButton.setTranslateX(-dockNodeTarget.getLayoutBounds().getWidth() / 2 + dockAnchorButton.getLayoutBounds().getWidth() / 2);

        } else if(dockAnchor == DockAnchor.TOP){

            winDockAreaIndicator.setWidth(dockNodeTarget.getLayoutBounds().getWidth());
            winDockAreaIndicator.setHeight(dockNodeTarget.getLayoutBounds().getHeight() / 2);

            dockAnchorButton.setTranslateY(-dockNodeTarget.getLayoutBounds().getHeight() / 2 + dockAnchorButton.getLayoutBounds().getHeight() / 2);

        } else {

            winDockAreaIndicator.setTranslateY(dockNodeTarget.getLayoutBounds().getHeight() / 2);
            winDockAreaIndicator.setWidth(dockNodeTarget.getLayoutBounds().getWidth());
            winDockAreaIndicator.setHeight(dockNodeTarget.getLayoutBounds().getHeight() / 2);

            dockAnchorButton.setTranslateY(dockNodeTarget.getLayoutBounds().getHeight() / 2 - dockAnchorButton.getLayoutBounds().getHeight() / 2);

        }

        //Upper-left corner of dragnode to local coords of dockpane
        Point2D DragNodeToDockPane = new Point2D(
                this.localToScene(dockNodeTarget.localToScreen(dockNodeTarget.getLayoutBounds())).getMinX(),
                this.localToScene(dockNodeTarget.localToScreen(dockNodeTarget.getLayoutBounds())).getMinY()
        );

        double posX = DragNodeToDockPane.getX();
        double posY = DragNodeToDockPane.getY();

        if (!winDockPopup.isShowing()) {
            winDockPopup.show(DockPane.this, posX, posY);
        }

        winDockAreaIndicator.setVisible(true);

    }

    public void showDockPaneDockPopup(Node dockNodeTarget, DockAnchor dockAnchor){

        if(!useDockPaneBoundaryForSideDock)
            return;

        hideNodeDockPopup();

        dockAnchorButton.setDockAnchor(dockAnchor);

        dockAnchorButton.setTranslateX(0);
        dockAnchorButton.setTranslateY(0);
        winDockAreaIndicator.setTranslateX(0);
        winDockAreaIndicator.setTranslateY(0);

        //set highlight coord
        if(dockAnchor == DockAnchor.RIGHT){

            winDockAreaIndicator.setTranslateX(dockNodeTarget.getLayoutBounds().getWidth() / 2);
            winDockAreaIndicator.setWidth(dockNodeTarget.getLayoutBounds().getWidth() / 2);
            winDockAreaIndicator.setHeight(dockNodeTarget.getLayoutBounds().getHeight());

            dockAnchorButton.setTranslateX(dockNodeTarget.getLayoutBounds().getWidth() / 2 - dockAnchorButton.getLayoutBounds().getWidth() / 2);

        } else if(dockAnchor == DockAnchor.LEFT){

            winDockAreaIndicator.setWidth(dockNodeTarget.getLayoutBounds().getWidth() / 2);
            winDockAreaIndicator.setHeight(dockNodeTarget.getLayoutBounds().getHeight());

            dockAnchorButton.setTranslateX(-dockNodeTarget.getLayoutBounds().getWidth() / 2 + dockAnchorButton.getLayoutBounds().getWidth() / 2);

        } else if(dockAnchor == DockAnchor.TOP){

            winDockAreaIndicator.setWidth(dockNodeTarget.getLayoutBounds().getWidth());
            winDockAreaIndicator.setHeight(dockNodeTarget.getLayoutBounds().getHeight() / 2);

            dockAnchorButton.setTranslateY(-dockNodeTarget.getLayoutBounds().getHeight() / 2 + dockAnchorButton.getLayoutBounds().getHeight() / 2);

        } else {

            winDockAreaIndicator.setTranslateY(dockNodeTarget.getLayoutBounds().getHeight() / 2);
            winDockAreaIndicator.setWidth(dockNodeTarget.getLayoutBounds().getWidth());
            winDockAreaIndicator.setHeight(dockNodeTarget.getLayoutBounds().getHeight() / 2);

            dockAnchorButton.setTranslateY(dockNodeTarget.getLayoutBounds().getHeight() / 2 - dockAnchorButton.getLayoutBounds().getHeight() / 2);

        }

        Bounds b = this.localToScreen(this.getLayoutBounds());

        if (!winDockPopup.isShowing()) {
            winDockPopup.show(DockPane.this, b.getMinX(), b.getMinY());
        }

        winDockAreaIndicator.setVisible(true);

    }

    public void hideWinDockPopup(){
        winDockPopup.hide();
        winDockAreaIndicator.setVisible(false);
    }

    public void showNodeDockPopup(Node dockNodeTarget, DockEvent event){

        hideWinDockPopup();

        for (DockAnchorButton btn : gridPaneBtns) {
            if(btn.isVisible()) {
                if (btn.contains(btn.screenToLocal(event.getScreenX(), event.getScreenY()))) {
                    dockAnchor = btn.getDockAnchor();
                    btn.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true);
                    break;
                } else {
                    btn.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), false);
                }
            }
        }

        // nodeDockPopup.setWidth(dockNodeTarget.getLayoutBounds().getWidth());
        // nodeDockPopup.setHeight(dockNodeTarget.getLayoutBounds().getHeight());

        double shitDragCountermeasure = 0.5;

        nodeDockAreaIndicator.setTranslateX(0);
        nodeDockAreaIndicator.setTranslateY(0);
        nodeDockSelector.setTranslateX(dockNodeTarget.getLayoutBounds().getWidth() / 2 - nodeDockSelector.getLayoutBounds().getWidth() / 2);
        nodeDockSelector.setTranslateY(dockNodeTarget.getLayoutBounds().getHeight() / 2 - nodeDockSelector.getLayoutBounds().getHeight() / 2);

        if(dockAnchor != null) {

            //set highlight coord
            if (dockAnchor == DockAnchor.RIGHT) {

                nodeDockAreaIndicator.setTranslateX(dockNodeTarget.getLayoutBounds().getWidth() * 0.75 - shitDragCountermeasure);
                nodeDockAreaIndicator.setTranslateY(shitDragCountermeasure);

                nodeDockAreaIndicator.setWidth(dockNodeTarget.getLayoutBounds().getWidth() / 4);
                nodeDockAreaIndicator.setHeight(dockNodeTarget.getLayoutBounds().getHeight());

            } else if (dockAnchor == DockAnchor.LEFT) {

                nodeDockAreaIndicator.setTranslateX(shitDragCountermeasure);
                nodeDockAreaIndicator.setTranslateY(shitDragCountermeasure);

                nodeDockAreaIndicator.setWidth(dockNodeTarget.getLayoutBounds().getWidth() / 4);
                nodeDockAreaIndicator.setHeight(dockNodeTarget.getLayoutBounds().getHeight());

            } else if (dockAnchor == DockAnchor.TOP) {

                nodeDockAreaIndicator.setTranslateX(shitDragCountermeasure);
                nodeDockAreaIndicator.setTranslateY(shitDragCountermeasure);

                nodeDockAreaIndicator.setWidth(dockNodeTarget.getLayoutBounds().getWidth());
                nodeDockAreaIndicator.setHeight(dockNodeTarget.getLayoutBounds().getHeight() / 4);

            } else if (dockAnchor == DockAnchor.BOTTOM) {

                nodeDockAreaIndicator.setTranslateX(shitDragCountermeasure);
                nodeDockAreaIndicator.setTranslateY(dockNodeTarget.getLayoutBounds().getHeight() * 0.75 - shitDragCountermeasure);

                nodeDockAreaIndicator.setWidth(dockNodeTarget.getLayoutBounds().getWidth());
                nodeDockAreaIndicator.setHeight(dockNodeTarget.getLayoutBounds().getHeight() / 4);

            } else {

                nodeDockAreaIndicator.setTranslateX(shitDragCountermeasure);
                nodeDockAreaIndicator.setTranslateY(shitDragCountermeasure);

                nodeDockAreaIndicator.setWidth(dockNodeTarget.getLayoutBounds().getWidth());
                nodeDockAreaIndicator.setHeight(dockNodeTarget.getLayoutBounds().getHeight());


            }

            nodeDockAreaIndicator.setVisible(true);

        } else {
            nodeDockAreaIndicator.setVisible(false);
        }

        if(!nodeDockPopup.isShowing()) {
            nodeDockPopup.show(this, dockNodeTarget.localToScreen(dockNodeTarget.getLayoutBounds()).getMinX(),
                    dockNodeTarget.localToScreen(dockNodeTarget.getLayoutBounds()).getMinY());
        }

    }

    public void hideNodeDockPopup(){
        nodeDockPopup.hide();
        nodeDockAreaIndicator.setVisible(false);
    }

}
