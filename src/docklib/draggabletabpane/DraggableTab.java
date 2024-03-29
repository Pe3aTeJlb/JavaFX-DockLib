package docklib.draggabletabpane;

import docklib.dock.DockAnchor;
import docklib.dock.DockEvent;
import docklib.dock.DockPane;
import docklib.utils.IconsManager;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static docklib.dock.DockPane.dockPanes;
import static docklib.draggabletabpane.DraggableTabPane.tabPanes;

public class DraggableTab extends Tab {

    private Label tabLabel;
    private Label dragText;
    private SimpleStringProperty stageTitle = new SimpleStringProperty();
    private String type;

    //Tech stage for event handling
    private Stage dragStage;
    private TabGroup tabGroup;

    //originTabPane refresh only when click on label or close it in purpose to provide proper behaviour while detached
    private DraggableTabPane originTabPane;
    private int originIndex;
    private Point2D dragOrigin;
    private SimpleBooleanProperty detached;
    private String animDirection = "";
    private boolean selectedBeforeClick;

    private DraggableTabPane lastInsertPane;

    private Stage floatStage;
    private boolean createNewFloatStage = true;

    //this means, u can detach System tab and attach it back or to another System tabPane
    private boolean detachable;

    //For docking
    private HashMap<Window, Node> dragNodes = new HashMap<>();
    private Window targetWindow;
    private boolean callbackReceived;

    public void setTabProperties(DraggableTabPane draggableTabPane){
        originTabPane = draggableTabPane;
        tabGroup = draggableTabPane.getTabGroup();
        detachable = tabGroup != TabGroup.System;
        if (this.getContextMenu() == null) {
            this.setContextMenu(new DraggableTabContextMenu(this, tabGroup));
        }
    }

    public DraggableTab(String text) {
        this(text, null, null);
    }

    public DraggableTab(String text, Image image) {
        this(text, image, null);
    }

    public DraggableTab(String text, Node content) {
        this(text, null, content);
    }

    public DraggableTab(StringBinding binding, Image image, Node content){

        this("", image, content);
        stageTitle.bind(binding);
        tabLabel.textProperty().bind(binding);
        dragText.textProperty().bind(binding);

    }

    public DraggableTab(String text, Image image, Node content) {

        /*
        originTabPane = tabPane;
        tabGroup = tabPane.getTabGroup();
        detachable = tabGroup != TabGroup.System;
         */
        detached = new SimpleBooleanProperty(false);

        stageTitle.set(text);
        tabLabel = new Label(text);
        if(image != null) {
            tabLabel.setGraphic(new ImageView(image));
        }
        this.setGraphic(tabLabel);

        //Prepare dragStage, which shown when tab dragged
        dragStage = new Stage();
        dragStage.initStyle(StageStyle.UNDECORATED);
        dragStage.setAlwaysOnTop(true);

        StackPane dragStagePane = new StackPane();
        //dragStagePane.setStyle("-fx-background-color:#DDDDDD;");

        dragText = new Label(text);
        if(image != null) {
            dragText.setGraphic(new ImageView(image));
        }

        StackPane.setAlignment(dragText, Pos.CENTER);
        dragStagePane.getChildren().add(dragText);
        dragStage.setScene(new Scene(dragStagePane));

        //Define drag events

        //Detach tab from tabpane
        tabLabel.setOnMouseDragged(event -> {

            if(event.getButton() != MouseButton.PRIMARY)
                return;

            double dragDelta = 0;

            switch (this.getTabPane().getSide()){
                case TOP:
                    animDirection = ((DraggableTabPane)this.getTabPane()).getHeaderOrientation() == NodeOrientation.LEFT_TO_RIGHT ? "" : "-";
                    dragDelta = Math.abs(event.getScreenY() - dragOrigin.getY()); break;
                case LEFT:
                    animDirection = ((DraggableTabPane)this.getTabPane()).getHeaderOrientation() == NodeOrientation.LEFT_TO_RIGHT ? "-" : "";
                    dragDelta = Math.abs(event.getScreenX() - dragOrigin.getX());
                    break;
                case RIGHT:
                    animDirection = ((DraggableTabPane)this.getTabPane()).getHeaderOrientation() == NodeOrientation.LEFT_TO_RIGHT ? "" : "-";
                    dragDelta = Math.abs(dragOrigin.getX() - event.getScreenX());
                    break;
                case BOTTOM:
                    animDirection = ((DraggableTabPane)this.getTabPane()).getHeaderOrientation() == NodeOrientation.LEFT_TO_RIGHT ? "-" : "";
                    dragDelta = Math.abs(dragOrigin.getY() - event.getScreenY());
                    break;
            }

            if (!detached.get() && dragDelta > 25) {

                dragStage.setWidth(dragText.getWidth() + 10);
                dragStage.setHeight(dragText.getHeight() + 10);
                dragStage.setX(event.getScreenX()-dragStage.getWidth()/2);
                dragStage.setY(event.getScreenY()-dragStage.getHeight()/2);
                dragStage.show();
                dragStage.setWidth(dragText.getWidth() + 10);
                dragStage.setHeight(dragText.getHeight() + 10);

                dragText.requestFocus();

                originIndex = this.getTabPane().getTabs().indexOf(this);
                //delete origin tab and share drag event with tabpane (check tabLabel drag detect)
                ((DraggableTabPane)this.getTabPane()).bindDetachedTab(detached);
                detached.set(true);

                if(!originTabPane.isCollapsed()) {
                    collapseSystemTab();
                }

                this.getTabPane().getTabs().remove(this);

                Event.fireEvent(this.tabLabel, new Event(DraggableTabEvent.DETACHED));

            }

        });

        /*Detach/Attach logic*/
        tabLabel.setOnMousePressed(event -> {

            if(event.getButton() != MouseButton.PRIMARY)
                return;

            //Define tabPane of this tab
            originTabPane = (DraggableTabPane) this.getTabPane();
            dragOrigin = new Point2D(event.getScreenX(), event.getScreenY());
            targetWindow = null;

            selectedBeforeClick = isSelected();

            //After detach, drag event will be continued on parent tabPane
            defineDragContinueEvent();

            //Detached tab released
            defineMouseReleaseEvent();

        });

        tabLabel.setOnMouseReleased(event -> {

            if(event.getButton() != MouseButton.PRIMARY)
                return;

            //hide/show system tab content
            collapseSystemTab();

        });

        tabLabel.addEventFilter(MouseEvent.DRAG_DETECTED, event -> {
            originTabPane.requestFocus();
            originTabPane.startFullDrag();
        });

        this.setContent(content);
        this.getContent().addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (((DraggableTabPane)getTabPane()).isUnDockable() && (getTabPane().getScene().getWindow() instanceof Stage)){
                ((Stage)getTabPane().getScene().getWindow()).titleProperty().bind(stageTitle);
            }
        });
    }

    public void collapseSystemTab(){

        if(tabGroup == TabGroup.System){

            if(floatStage != null) {

                if (floatStage.isShowing()) {
                    floatStage.hide();
                }else{
                    floatStage.show();
                }

            } else {

                if(originTabPane.isCollapsed()) {
                    originTabPane.expand();
                } else if(selectedBeforeClick){
                    originTabPane.collapse();
                }

            }

        }

    }

    public void defineDragContinueEvent(){

        originTabPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {

            if (!detached.get()){
                return;
            }

            if(event.getButton() != MouseButton.PRIMARY)
                return;

            dragStage.setX(event.getScreenX() - dragStage.getWidth() / 2);
            dragStage.setY(event.getScreenY() - dragStage.getHeight() / 2);

            //Make spread "Animation" at target tabpane while attach

            Point2D screenPoint = new Point2D(event.getScreenX(), event.getScreenY());
            InsertData data = getInsertData(screenPoint);

            //reset tab css in last pointed tabPane
            if(data == null || data.getInsertPane().getTabs().isEmpty()) {
                if(lastInsertPane != null) {
                    for (Tab tab : lastInsertPane.getTabs()) {
                        tab.setStyle("-fx-translate-x: 0;");
                    }
                }
            }

            //Create slide animation in pointed tabPane
            if (data != null && !data.getInsertPane().getTabs().isEmpty()) {

                if(tabGroup != data.getInsertPane().getTabGroup()){
                    return;
                }

                if (!originTabPane.sameProject(data.getInsertPane())){
                    return;
                }

                lastInsertPane = data.insertPane;

                switch (data.insertPane.getSide()){
                    case TOP:
                    case RIGHT:     animDirection = "";  break;
                    case LEFT:
                    case BOTTOM:    animDirection = "-"; break;
                }

                int index = data.getIndex();
                //no anim
                if (index == data.getInsertPane().getTabs().size()) {
                    return;
                }

                //Anim depends on insert index
                if (index != data.getInsertPane().getTabs().size()) {
                    for (int i = 0; i < data.getInsertPane().getTabs().size(); i++) {
                        if (i < index) {
                            data.getInsertPane().getTabs().get(i).setStyle("-fx-translate-x: 0;");
                        } else {
                            data.getInsertPane().getTabs().get(i).setStyle("-fx-translate-x: " + animDirection + (tabLabel.getWidth()+15) + " ;");
                        }
                    }
                }

            } else if(tabGroup != TabGroup.System){
                //Dock events
                DockEvent dockEnterEvent =
                        new DockEvent(this, DockEvent.NULL_SOURCE_TARGET, DockEvent.DOCK_ENTER, event.getX(),
                                event.getY(), event.getScreenX(), event.getScreenY(), null, originTabPane);
                DockEvent dockOverEvent =
                        new DockEvent(this, DockEvent.NULL_SOURCE_TARGET, DockEvent.DOCK_OVER, event.getX(),
                                event.getY(), event.getScreenX(), event.getScreenY(), null, originTabPane);
                DockEvent dockExitEvent =
                        new DockEvent(this, DockEvent.NULL_SOURCE_TARGET, DockEvent.DOCK_EXIT, event.getX(),
                                event.getY(), event.getScreenX(), event.getScreenY(), null, originTabPane);

                EventTask eventTask = new EventTask() {
                    @Override
                    public void run(Node node, Node dragNode) {
                        executions++;

                        if (dragNode != node) {
                            Event.fireEvent(node, dockEnterEvent.copyFor(originTabPane, node));
                            if (dragNode != null) {
                                Event.fireEvent(dragNode, dockExitEvent.copyFor(originTabPane, dragNode));
                            }
                            targetWindow = node.getScene().getWindow();
                            dragNodes.put(node.getScene().getWindow(), node);
                        }
                        Event.fireEvent(node, dockOverEvent.copyFor(originTabPane, node));
                    }
                };

                this.pickEventTarget(new Point2D(event.getScreenX(), event.getScreenY()), eventTask,
                        dockExitEvent);

            }
        });

    }

    public void defineMouseReleaseEvent(){

        originTabPane.setOnMouseReleased(event -> {

            if(event.getButton() != MouseButton.PRIMARY)
                return;

            if(!detached.get())
                return;

            dragStage.hide();

            if (!event.isStillSincePress()) {

                //Insert tab into tabPane
                Point2D screenPoint = new Point2D(event.getScreenX(), event.getScreenY());
                DraggableTabPane oldTabPane = originTabPane;
                int oldIndex = originIndex;

                InsertData insertData = getInsertData(screenPoint);
                if (insertData != null && tabGroup == insertData.getInsertPane().getTabGroup()
                        && originTabPane.sameProject(insertData.getInsertPane())) {
                    int addIndex = insertData.getIndex();
                    /*
                    if(oldTabPane == insertData.getInsertPane() && oldTabPane.getTabs().size() == 1) {
                        //TODO
                        return;
                    }*/
                    oldTabPane.getTabs().remove(DraggableTab.this);
                    if(oldIndex < addIndex && oldTabPane == insertData.getInsertPane()) {
                        //addIndex--;
                    }
                    if (addIndex > insertData.getInsertPane().getTabs().size()) {
                        addIndex = insertData.getInsertPane().getTabs().size();
                    }
                    insertData.getInsertPane().getTabs().add(addIndex, DraggableTab.this);
                    insertData.getInsertPane().getSelectionModel().select(addIndex);
                    for(Tab tab: insertData.getInsertPane().getTabs()){
                        tab.setStyle("-fx-translate-x: 0;");
                    }

                    //terminate origin stage if it is empty
                    if(!insertData.getInsertPane().equals(originTabPane) && originTabPane.getTabs().isEmpty()){
                        if(floatStage != null) {
                            floatStage.hide();
                            floatStage.setScene(null);
                        }
                    }
                    createNewFloatStage = true;
                    detached.set(false);

                    //fireDockEvent(event, null);
                    Event.fireEvent(this.tabLabel, new Event(DraggableTabEvent.ATTACHED));
                    return;
                }

                //undetachable tab was detached
                if (!detachable) {
                    detached.set(false);
                    originTabPane.getTabs().add(originIndex, DraggableTab.this);
                    Event.fireEvent(this.tabLabel, new Event(DraggableTabEvent.DETACH_INTERRUPTED));
                    return;
                }

                //Or Create stage for detached tab
                if (dragNodes.get(targetWindow) != null) {
                    //at this moment this tab is deleted from origin tabpane but we need it reference
                    //to check if it relate to the same project
                    DraggableTabPane draggableTabPane = new DraggableTabPane(originTabPane.getOwnerWindow(), tabGroup);
                    draggableTabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
                    if(originTabPane.getProject() != null) draggableTabPane.setProject(originTabPane.getProject());
                    draggableTabPane.addTab(this);

                    fireDockEvent(event, draggableTabPane);
                    if(!callbackReceived){
                        detachTab(event);
                    }
                    callbackReceived = false;

                } else {
                    detachTab(event);
                }

                detached.set(false);

            }

        });

    }

    /* Dock event */

    public void dockEventCallback(boolean docked, DockEvent event){

        callbackReceived = true;
        if(docked){
            //terminate float-stage if it is empty
            if(originTabPane.getTabs().isEmpty()){
                if(floatStage != null) {
                    floatStage.close();
                    floatStage.setScene(null);
                }
            }
            createNewFloatStage = true;
            detached.set(false);
        } else {
            detachTab(event);
        }

    }

    private abstract static class EventTask {

        protected int executions = 0;

        public abstract void run(Node node, Node dragNode);

        public int getExecutions() {
            return executions;
        }

        public void reset() {
            executions = 0;
        }

    }

    private void pickEventTarget(Point2D screenPoint, EventTask eventTask, Event explicit) {

        for (DockPane dockPane: dockPanes){

            if(dockPane.getScene() == null) {
                continue;
            }

            Window window = dockPane.getScene().getWindow();
            if (!(window instanceof Stage)) continue;
            Stage targetStage = (Stage) window;

            eventTask.reset();

            Parent root = targetStage.getScene().getRoot();
            Node dragNode = dragNodes.get(targetStage);

            if(root.contains(root.screenToLocal(screenPoint))
                    && !root.isMouseTransparent()) {

                Stack<Parent> stack = new Stack<>();
                stack.push(root);

                // depth first traversal to find the deepest node or parent with no children
                // that intersects the point of interest
                while (!stack.isEmpty()) {
                    Parent parent = stack.pop();
                    // if this parent contains the mouse click in screen coordinates in its local bounds
                    // then traverse its children
                    boolean notFired = true;
                    for (Node node : parent.getChildrenUnmodifiable()) {
                        if (node.contains(node.screenToLocal(screenPoint)) && !node.isMouseTransparent()) {
                            if (node instanceof Parent) {
                                stack.push((Parent) node);
                            } else {
                                eventTask.run(node, dragNode);
                            }
                            notFired = false;
                            break;
                        }
                    }
                    // if none of the children fired the event or there were no children
                    // fire it with the parent as the target to receive the event
                    if (notFired) {
                        eventTask.run(parent, dragNode);
                    }
                }

                return;
            }

            if (explicit != null && dragNode != null && eventTask.getExecutions() < 1) {
                Event.fireEvent(dragNode, explicit.copyFor(this, dragNode));
                dragNodes.put(targetStage, null);
            }

        }

    }

    /*
    private void pickEventTarget(Point2D location, EventTask eventTask, Event explicit) {

        List<DockPane> dockPanes = new ArrayList<>(DockPane.dockPanes);

        // fire the dock over event for the active stages
        for (DockPane dockPane : dockPanes) {

            Window window = dockPane.getScene().getWindow();
            if (!(window instanceof Stage)) continue;
            Stage targetStage = (Stage) window;

            // obviously this title bar does not need to receive its own events
            // though users of this library may want to know when their
            // dock node is being dragged by subclassing it or attaching
            // an event listener in which case a new event can be defined or
            // this continue behavior can be removed
            //if (targetStage == this.dockNode.getStage())
            //    continue;

            eventTask.reset();

            Node dragNode = dragNodes.get(targetStage);

            Parent root = targetStage.getScene().getRoot();
            Stack<Parent> stack = new Stack<>();
            if (root.contains(root.screenToLocal(location.getX(), location.getY()))
                    && !root.isMouseTransparent()) {
                stack.push(root);
            }
            // depth first traversal to find the deepest node or parent with no children
            // that intersects the point of interest
            while (!stack.isEmpty()) {
                Parent parent = stack.pop();
                // if this parent contains the mouse click in screen coordinates in its local bounds
                // then traverse its children
                boolean notFired = true;
                for (Node node : parent.getChildrenUnmodifiable()) {
                    if (node.contains(node.screenToLocal(location.getX(), location.getY()))
                            && !node.isMouseTransparent()) {
                        if (node instanceof Parent) {
                            stack.push((Parent) node);
                        } else {
                            eventTask.run(node, dragNode);
                        }
                        notFired = false;
                        break;
                    }
                }
                // if none of the children fired the event or there were no children
                // fire it with the parent as the target to receive the event
                if (notFired) {
                    eventTask.run(parent, dragNode);
                }
            }

            if (explicit != null && dragNode != null && eventTask.getExecutions() < 1) {
                Event.fireEvent(dragNode, explicit.copyFor(this, dragNode));
                dragNodes.put(targetStage, null);
            }

        }

    }
     */

    public void fireDockEvent(MouseEvent event, DraggableTabPane draggableTabPane){

        DockEvent dockReleasedEvent =
                new DockEvent(this, DockEvent.NULL_SOURCE_TARGET, DockEvent.DOCK_RELEASED, event.getX(),
                        event.getY(), event.getScreenX(), event.getScreenY(), null, draggableTabPane
                );

        EventTask eventTask = new EventTask() {
            @Override
            public void run(Node node, Node dragNode) {
                executions++;
                if (dragNode != node) {
                    Event.fireEvent(node, dockReleasedEvent.copyFor(originTabPane, node));
                }
                Event.fireEvent(node, dockReleasedEvent.copyFor(originTabPane, node));
            }
        };

        this.pickEventTarget(new Point2D(event.getScreenX(), event.getScreenY()), eventTask, null);

        dragNodes.clear();

        // Remove temporary event handler for bug mentioned above.
                    /*
                    DockPane dockPane = this.getDockNode().getDockPane();
                    if (dockPane != null) {
                        dockPane.removeEventFilter(MouseEvent.MOUSE_DRAGGED, this);
                        dockPane.removeEventFilter(MouseEvent.MOUSE_RELEASED, this);
                    }*/

    }


    private void detachTab(MouseEvent event){
        detachTab(event.getScreenX(), event.getScreenY());
    }

    private void detachTab(DockEvent event){
        detachTab(event.getScreenX(), event.getScreenY());
    }

    private void detachTab(double screenX, double screenY){

        if(createNewFloatStage) {

            final Stage newFloatStage = new Stage();
            newFloatStage.getIcons().add(IconsManager.StageIcon);
            newFloatStage.titleProperty().bind(stageTitle);
            originTabPane.getOwnerWindow().addEventHandler(WindowEvent.WINDOW_HIDING, windowEvent -> newFloatStage.close());

            final DraggableTabPane pane = new DraggableTabPane(originTabPane.getOwnerWindow(), tabGroup);
            pane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
            if(originTabPane.getProject() != null) pane.setProject(originTabPane.getProject());
            pane.addTab(this);

            DockPane newDockPane = new DockPane();
            newDockPane.dock(pane, DockAnchor.CENTER);
            newFloatStage.setScene(new Scene(newDockPane));
            newFloatStage.initStyle(StageStyle.DECORATED);
            newFloatStage.setX(screenX);
            newFloatStage.setY(screenY);
            newFloatStage.show();

            newDockPane.getChildren().addListener((ListChangeListener<Node>) change -> {

                if(newDockPane.getChildren().isEmpty()){
                    newFloatStage.close();
                    newFloatStage.setScene(null);
                    createNewFloatStage = true;
                } else {
                    floatStage = newFloatStage;
                    createNewFloatStage = false;
                }

            });

            pane.requestLayout();
            pane.requestFocus();

            Event.fireEvent(this.tabLabel, new Event(DraggableTabEvent.INTO_SEPARATED_WINDOW));

        } else {

            //If u detached tab from float stage to free space
            //and that stage contains only this tab, return it back and just move float stage
            originTabPane.getTabs().add(DraggableTab.this);
            floatStage.setX(screenX);
            floatStage.setY(screenY);

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


    /* ScreenSpace intersect calculation */

    private InsertData getInsertData(Point2D screenPoint) {

        for(DraggableTabPane tabPane : tabPanes) {

            Side side = tabPane.getSide();

            //tabpane hidden or haven't been added to scene graph, etc.
            //skip this tabpane
            if(tabPane.getScene() == null) {
                continue;
            }

            Rectangle2D headerScreenBounds = getAbsoluteRect(tabPane);
            if(headerScreenBounds.contains(screenPoint)) {

                int tabInsertIndex = 0;

                if(!tabPane.getTabs().isEmpty()) {

                    if(tabPane.getScene() == null) {
                        continue;
                    }

                    Rectangle2D firstTabRect = getAbsoluteRect(tabPane.getTabs().get(0));
                    Rectangle2D lastTabRect = getAbsoluteRect(tabPane.getTabs().get(tabPane.getTabs().size() - 1));

                    if(side.isHorizontal()) {

                        if (screenPoint.getY() > headerScreenBounds.getMaxY() || screenPoint.getY() < headerScreenBounds.getMinY()) {
                            return null;
                        }

                        if(tabPane.getHeaderOrientation() == NodeOrientation.LEFT_TO_RIGHT) {

                            if (screenPoint.getX() < (firstTabRect.getMinX() + firstTabRect.getWidth() / 2)) {
                                tabInsertIndex = 0;
                            } else if (screenPoint.getX() > (lastTabRect.getMaxX() - lastTabRect.getWidth() / 2)) {
                                tabInsertIndex = tabPane.getTabs().size();
                            } else {

                                for (int i = 0; i < tabPane.getTabs().size() - 1; i++) {
                                    Tab leftTab = tabPane.getTabs().get(i);
                                    Tab rightTab = tabPane.getTabs().get(i + 1);
                                    if (leftTab instanceof DraggableTab && rightTab instanceof DraggableTab) {
                                        Rectangle2D leftTabRect = getAbsoluteRect(leftTab);
                                        Rectangle2D rightTabRect = getAbsoluteRect(rightTab);
                                        if (betweenX(leftTabRect, rightTabRect, screenPoint.getX())) {
                                            tabInsertIndex = i + 1;
                                            break;
                                        }
                                    }
                                }

                            }

                        } else {

                            if (screenPoint.getX() > (firstTabRect.getMinX() + firstTabRect.getWidth() / 2)) {
                                tabInsertIndex = 0;
                            } else if (screenPoint.getX() < (lastTabRect.getMinX() + lastTabRect.getWidth() / 2)) {
                                tabInsertIndex = tabPane.getTabs().size();
                            } else {
                                for (int i = 0; i < tabPane.getTabs().size() - 1; i++) {
                                    Tab rightTab = tabPane.getTabs().get(i);
                                    Tab leftTab = tabPane.getTabs().get(i + 1);
                                    if (leftTab instanceof DraggableTab && rightTab instanceof DraggableTab) {
                                        Rectangle2D leftTabRect = getAbsoluteRect(leftTab);
                                        Rectangle2D rightTabRect = getAbsoluteRect(rightTab);
                                        if (betweenX(leftTabRect, rightTabRect, screenPoint.getX())) {
                                            tabInsertIndex = i + 1;
                                            break;
                                        }
                                    }
                                }

                            }

                        }

                    } else {

                        if (screenPoint.getX() > headerScreenBounds.getMaxX() || screenPoint.getX() < headerScreenBounds.getMinX()) {
                            return null;
                        }

                        if(tabPane.getHeaderOrientation() == NodeOrientation.LEFT_TO_RIGHT) {

                            if (screenPoint.getY() < (firstTabRect.getMinY() + firstTabRect.getHeight() / 2)) {
                                tabInsertIndex = 0;
                            } else if (screenPoint.getY() > (lastTabRect.getMaxY() - lastTabRect.getHeight() / 2)) {
                                tabInsertIndex = tabPane.getTabs().size();
                            } else {

                                for (int i = 0; i < tabPane.getTabs().size() - 1; i++) {
                                    Tab leftTab = tabPane.getTabs().get(i);
                                    Tab rightTab = tabPane.getTabs().get(i + 1);
                                    if (leftTab instanceof DraggableTab && rightTab instanceof DraggableTab) {
                                        Rectangle2D leftTabRect = getAbsoluteRect(leftTab);
                                        Rectangle2D rightTabRect = getAbsoluteRect(rightTab);
                                        if (betweenY(leftTabRect, rightTabRect, screenPoint.getY())) {
                                            tabInsertIndex = i + 1;
                                            break;
                                        }
                                    }
                                }

                            }

                        } else {

                            if (screenPoint.getY() > (firstTabRect.getMinY() + firstTabRect.getHeight() / 2)) {
                                tabInsertIndex = 0;
                            } else if (screenPoint.getY() < (lastTabRect.getMinY() + lastTabRect.getHeight() / 2)) {
                                tabInsertIndex = tabPane.getTabs().size();
                            } else {

                                for (int i = 0; i < tabPane.getTabs().size() - 1; i++) {
                                    Tab rightTab = tabPane.getTabs().get(i);
                                    Tab leftTab = tabPane.getTabs().get(i + 1);
                                    if (leftTab instanceof DraggableTab && rightTab instanceof DraggableTab) {
                                        Rectangle2D leftTabRect = getAbsoluteRect(leftTab);
                                        Rectangle2D rightTabRect = getAbsoluteRect(rightTab);
                                        if (betweenY(leftTabRect, rightTabRect, screenPoint.getY())) {
                                            tabInsertIndex = i + 1;
                                            break;
                                        }
                                    }
                                }

                            }

                        }

                    }

                }

                return new InsertData(tabInsertIndex, tabPane);

            }

        }

        return null;

    }

    private Rectangle2D getAbsoluteRect(DraggableTabPane node) {

        Node header = ((HeaderReachable)node.getSkin()).getTabHeaderArea();
        Bounds headerScreenBounds = header.localToScreen(header.getLayoutBounds());
        /*
        return new Rectangle2D(
                node.localToScene(node.getLayoutBounds().getMinX(), node.getLayoutBounds().getMinY()).getX() + node.getScene().getWindow().getX(),
                node.localToScene(node.getLayoutBounds().getMinX(), node.getLayoutBounds().getMinY()).getY() + node.getScene().getWindow().getY(),
                node.getWidth(),
                node.getHeight()
        );*/
        return new Rectangle2D(
                headerScreenBounds.getMinX(),
                headerScreenBounds.getMinY(),
                headerScreenBounds.getWidth(),
                headerScreenBounds.getHeight()
        );

    }

    private Rectangle2D getAbsoluteRect(Tab tab) {
        Control node = ((DraggableTab) tab).getLabel();
        return getAbsoluteRect(node, tab.getTabPane().getSide());
    }

    private Rectangle2D getAbsoluteRect(Control node, Side side){

        //Pls, don't ask me why there is no symmetry, just put up with these coefficients
        //Thank God, it works
        Bounds nodeScreenBounds = node.localToScreen(node.getLayoutBounds());
        return new Rectangle2D(
                nodeScreenBounds.getMinX(),
                nodeScreenBounds.getMinY(),
                node.getWidth(),
                node.getHeight()
        );

        /*
        switch (side){
            case TOP:
                break;
            case BOTTOM:
                break;
            case LEFT:
                break;
            case RIGHT:
                break;
        }

        if(side == Side.TOP ) {
            return new Rectangle2D(
                    node.localToScene(node.getLayoutBounds().getMinX(), node.getLayoutBounds().getMinY()).getX() + node.getScene().getWindow().getX(),
                    node.localToScreen(node.getLayoutBounds().getMinX(), node.getLayoutBounds().getMinY()).getY() - node.getHeight(),
                    node.getWidth(),
                    2 * node.getHeight()
            );
        } else if(side == Side.BOTTOM) {
            return new Rectangle2D(
                    node.localToScreen(node.getLayoutBounds().getMinX(), node.getLayoutBounds().getMinY()).getX() ,
                    (node.localToScreen(node.getLayoutBounds().getMinX(), node.getLayoutBounds().getMinY()).getY() - 2 * node.getHeight()),
                    node.getWidth(),
                    2 * node.getHeight()
            );
        } else if(side == Side.LEFT) {
            return new Rectangle2D(
                    node.localToScreen(node.getLayoutBounds().getMinX(), node.getLayoutBounds().getMaxY()).getX() - node.getHeight()/2,
                    node.localToScreen(node.getLayoutBounds().getMinX(), node.getLayoutBounds().getMaxY()).getY() - node.getWidth(),
                    2*node.getHeight(),
                    node.getWidth()
            );
        } else if(side == Side.RIGHT) {
            return new Rectangle2D(
                    node.localToScreen(node.getLayoutBounds().getMinX(), node.getLayoutBounds().getMinY()).getX() - 2 * node.getHeight(),
                    node.localToScreen(node.getLayoutBounds().getMinX(), node.getLayoutBounds().getMaxY()).getY() + node.getWidth()/2,
                    2 * node.getHeight(),
                    node.getWidth()
            );
        }

        return null;*/

    }

    private boolean betweenX(Rectangle2D r1, Rectangle2D r2, double xPoint) {
        double leftBound = r1.getMinX() + r1.getWidth() / 2;
        double rightBound = r2.getMinX() + r2.getWidth() / 2;
        return xPoint >= leftBound && xPoint <= rightBound;
    }

    private boolean betweenY(Rectangle2D r1, Rectangle2D r2, double YPoint) {
        double lowerBound = r1.getMinY() + r1.getHeight() / 2;
        double upperBound = r2.getMinY() + r2.getHeight() / 2;
        return YPoint >= lowerBound && YPoint <= upperBound;
    }

    private static class InsertData {

        private final int index;
        private final DraggableTabPane insertPane;

        public InsertData(int index, DraggableTabPane insertPane) {
            this.index = index;
            this.insertPane = insertPane;
        }

        public int getIndex() {
            return index;
        }

        public DraggableTabPane getInsertPane() {
            return insertPane;
        }

    }



    //Custom Events

    public void setOnDetached(EventHandler<Event> var1) {
        this.tabLabel.addEventHandler(DraggableTabEvent.DETACHED, var1);
    }

    public void setOnAttached(EventHandler<Event> var1) {
        this.tabLabel.addEventHandler(DraggableTabEvent.ATTACHED, var1);
    }

    public void setOnIntoSeparatedWindow(EventHandler<Event> var1) {
        this.tabLabel.addEventHandler(DraggableTabEvent.INTO_SEPARATED_WINDOW, var1);
    }



    /* Getters/Setters */

    public DraggableTabPane getOriginTabPane(){
        return originTabPane;
    }

    public TabGroup getTabGroup(){
        return tabGroup;
    }


    public void setDetachable(boolean detachable) {
        this.detachable = detachable;
    }

    public boolean isDetachable() {
        return this.detachable;
    }


    public void setTabText(String text) {
        this.setText(text);
        tabLabel.setText(text);
        dragText.setText(text);
    }

    private Label getLabel() {
        return tabLabel;
    }


    public void setStageTitle(String text){
        stageTitle.set(text);
    }

    public void setStageTitle(StringBinding textBind){
        stageTitle.bind(textBind);
    }

    public SimpleStringProperty getStageTitle() {
        return stageTitle;
    }


    public void setType(String type){
        this.type = type;
    }

    public String getType(){
        return type;
    }



    public void close(){
        Event.fireEvent(this, new Event(Tab.TAB_CLOSE_REQUEST_EVENT));
        getTabPane().getTabs().remove(this);
        Event.fireEvent(this, new Event(Tab.CLOSED_EVENT));
    }



    static Map<String, StringBinding> localizationPack = null;

    public static void setLocalizationPack(Map<String, StringBinding> locale){
        localizationPack = locale;
    }

    private class DraggableTabContextMenu extends ContextMenu{

        private DraggableTab tab;
        private TabViewMode viewMode;
        private ChangeListener<Boolean> focusListener;
        private AtomicReference<DraggableTabPane> tabPaneRef;


        public DraggableTabContextMenu(DraggableTab tab, TabGroup tabGroup){

            super();

            this.tab = tab;
            this.tabPaneRef = new AtomicReference<>();
            this.tabPaneRef.set((DraggableTabPane) tab.getTabPane());

            switch (tabGroup){
                case System:    populateSystemMenu(tab); break;
                case WorkSpace: populateWorkspaceMenu(); break;
                case None:      break;
            }

        }

        public void populateSystemMenu(DraggableTab tab){

            viewMode = TabViewMode.DockPinned;

            focusListener = (ov, onHidden, onShown) -> {
                if(onHidden){
                    ((DraggableTabPane)tab.getTabPane()).collapse();
                }
            };

            MenuItem dockPinnedItem = new MenuItem("dock pinned");
            dockPinnedItem.setOnAction(event -> setDockedPinned(tab));

            MenuItem dockUnpinnedItem = new MenuItem("dock unpinned");
            dockUnpinnedItem.setOnAction(event -> setDockedUnpinned(tab));

            MenuItem floatItem = new MenuItem("float");
            floatItem.setOnAction(event -> setFloating(tab));

            MenuItem windowItem = new MenuItem("window");
            windowItem.setOnAction(event -> setWindowed(tab));

            MenuItem closeItem = new MenuItem("close");
            closeItem.setOnAction(event -> {
                Event.fireEvent(tab, new Event(Tab.TAB_CLOSE_REQUEST_EVENT));
                terminateFloatStage(null);
                tab.getTabPane().focusedProperty().removeListener(focusListener);
                tab.getTabPane().getTabs().remove(tab);
                Event.fireEvent(tab, new Event(Tab.CLOSED_EVENT));
            });

            if (localizationPack != null){
                dockPinnedItem.textProperty().bind(localizationPack.get("dockPinnedItem"));
                floatItem.textProperty().bind(localizationPack.get("floatItem"));
                windowItem.textProperty().bind(localizationPack.get("windowItem"));
                closeItem.textProperty().bind(localizationPack.get("closeItem"));
            }

            this.getItems().addAll(
                    dockPinnedItem,
                    //dockUnpinnedItem,
                    floatItem,
                    windowItem,
                    new SeparatorMenuItem(),
                    closeItem
            );

        }

        private void setDockedPinned(DraggableTab tab){

            if(viewMode == TabViewMode.DockPinned)
                return;

            //idk default behaviour lolxd
            terminateFloatStage(tab);

            tab.getTabPane().focusedProperty().removeListener(focusListener);
            ((DraggableTabPane)tab.getTabPane()).expand();

            viewMode = TabViewMode.DockPinned;

        }

        private void setDockedUnpinned(DraggableTab tab){

            if(viewMode == TabViewMode.DockUnpinned)
                return;

            terminateFloatStage(tab);
            tab.getTabPane().focusedProperty().addListener(focusListener);

            viewMode = TabViewMode.DockUnpinned;

        }

        private void setFloating(DraggableTab tab){

            if(viewMode == TabViewMode.Float)
                return;

            terminateFloatStage(null);

            Node content = tab.getContent();
            ((DraggableTabPane)tab.getTabPane()).collapse();

            floatStage = new Stage();
            floatStage.getIcons().add(IconsManager.StageIcon);
            floatStage.titleProperty().bind(stageTitle);
            ((DraggableTabPane) tab.getTabPane()).getOwnerWindow().addEventHandler(WindowEvent.WINDOW_HIDING, windowEvent -> floatStage.close());

            AnchorPane anchorPane = new AnchorPane();
            anchorPane.getChildren().add(content);

            AnchorPane.setLeftAnchor(content, 0.0);
            AnchorPane.setTopAnchor(content, 0.0);
            AnchorPane.setRightAnchor(content, 0.0);
            AnchorPane.setBottomAnchor(content, 0.0);

            floatStage.setScene(new Scene(anchorPane));

            floatStage.setX(content.localToScreen(content.getLayoutBounds().getMinX(), content.getLayoutBounds().getMinY()).getX());
            floatStage.setY(content.localToScreen(content.getLayoutBounds().getMinX(), content.getLayoutBounds().getMinY()).getY());
            floatStage.initStyle(StageStyle.UTILITY);
            floatStage.setAlwaysOnTop(true);
            floatStage.show();

            floatStage.requestFocus();

            viewMode = TabViewMode.Float;

        }

        private void setWindowed(DraggableTab tab){

            if(viewMode == TabViewMode.Window)
                return;

            terminateFloatStage(null);

            Node content = tab.getContent();
            ((DraggableTabPane)tab.getTabPane()).collapse();

            floatStage = new Stage();
            floatStage.getIcons().add(IconsManager.StageIcon);
            floatStage.titleProperty().bind(stageTitle);
            ((DraggableTabPane) tab.getTabPane()).getOwnerWindow().addEventHandler(WindowEvent.WINDOW_HIDING, windowEvent -> floatStage.close());


            AnchorPane anchorPane = new AnchorPane();
            anchorPane.getChildren().add(content);

            AnchorPane.setLeftAnchor(content, 0.0);
            AnchorPane.setTopAnchor(content, 0.0);
            AnchorPane.setRightAnchor(content, 0.0);
            AnchorPane.setBottomAnchor(content, 0.0);

            floatStage.setScene(new Scene(anchorPane));

            floatStage.setX(content.localToScreen(content.getLayoutBounds().getMinX(), content.getLayoutBounds().getMinY()).getX());
            floatStage.setY(content.localToScreen(content.getLayoutBounds().getMinX(), content.getLayoutBounds().getMinY()).getY());
            floatStage.initStyle(StageStyle.DECORATED);
            floatStage.show();

            floatStage.requestFocus();

            viewMode = TabViewMode.Window;

        }

        private void terminateFloatStage(DraggableTab tab){

            if(floatStage != null) {
                if(tab != null) tab.setContent(floatStage.getScene().getRoot());
                floatStage.hide();
                floatStage.setScene(null);
                floatStage = null;
            }

        }


        public void populateWorkspaceMenu(){

            MenuItem closeItem = new MenuItem("Close");
            closeItem.setOnAction(event -> {
                Event.fireEvent(tab, new Event(Tab.TAB_CLOSE_REQUEST_EVENT));
                tabPaneRef.get().getTabs().remove(tab);
                Event.fireEvent(tab, new Event(Tab.CLOSED_EVENT));
            });

            MenuItem closeOthersItem = new MenuItem("Close others");
            closeOthersItem.setOnAction(event -> {
                ArrayList<Tab> tabsToClose = new ArrayList<>(tabPaneRef.get().getTabs());
                for (Tab t: tabsToClose){
                    if (t != tab){
                        Event.fireEvent(t, new Event(Tab.TAB_CLOSE_REQUEST_EVENT));
                        tabPaneRef.get().getTabs().remove(t);
                        Event.fireEvent(t, new Event(Tab.CLOSED_EVENT));
                    }
                }
            });

            MenuItem closeAllItems = new MenuItem("Close all");
            closeAllItems.setOnAction(event -> {
                ArrayList<Tab> tabsToClose = new ArrayList<>(tabPaneRef.get().getTabs());
                for (Tab t: tabsToClose){
                    Event.fireEvent(t, new Event(Tab.TAB_CLOSE_REQUEST_EVENT));
                    tabPaneRef.get().getTabs().remove(t);
                    Event.fireEvent(t, new Event(Tab.CLOSED_EVENT));
                }
            });

            MenuItem closeToTheLeftItem = new MenuItem("Close all to the left");
            closeToTheLeftItem.setOnAction(event -> {
                int index = tabPaneRef.get().getTabs().indexOf(tab);
                List<Tab> tabsToClose = new ArrayList<>(tabPaneRef.get().getTabs().subList(0, index));
                for (Tab t: tabsToClose){
                    Event.fireEvent(t, new Event(Tab.TAB_CLOSE_REQUEST_EVENT));
                    tabPaneRef.get().getTabs().remove(t);
                    Event.fireEvent(t, new Event(Tab.CLOSED_EVENT));
                }
            });

            MenuItem closeToTheRightItem = new MenuItem("Close all to the right");
            closeToTheRightItem.setOnAction(event -> {
                int index = tabPaneRef.get().getTabs().indexOf(tab);
                List<Tab> tabs = new ArrayList<>(tabPaneRef.get().getTabs().subList(index+1, tabPaneRef.get().getTabs().size()));
                for (Tab t: tabs){
                    Event.fireEvent(t, new Event(Tab.TAB_CLOSE_REQUEST_EVENT));
                    tabPaneRef.get().getTabs().remove(t);
                    Event.fireEvent(t, new Event(Tab.CLOSED_EVENT));
                }
            });

            MenuItem splitVerticallyItem = new MenuItem("Split vertically");
            splitVerticallyItem.setOnAction(event -> {
                tabPaneRef.get().getTabs().remove(tab);
                DraggableTabPane draggableTabPane = new DraggableTabPane(tabPaneRef.get().getOwnerWindow(), tabGroup);
                draggableTabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
                if(tabPaneRef.get().getProject() != null) draggableTabPane.setProject(tabPaneRef.get().getProject());
                draggableTabPane.addTab(tab);
                tabPaneRef.get().dock(draggableTabPane, DockAnchor.BOTTOM);
            });

            MenuItem splitHorizontallyItem = new MenuItem("Split horizontally");
            splitHorizontallyItem.setOnAction(event -> {
                tabPaneRef.get().getTabs().remove(tab);
                DraggableTabPane draggableTabPane = new DraggableTabPane(tabPaneRef.get().getOwnerWindow(), tabGroup);
                draggableTabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
                if(tabPaneRef.get().getProject() != null) draggableTabPane.setProject(tabPaneRef.get().getProject());
                draggableTabPane.addTab(tab);
                tabPaneRef.get().dock(draggableTabPane, DockAnchor.RIGHT);
            });


            MenuItem selectNextTabItem = new MenuItem("Select next tab");
            selectNextTabItem.setOnAction(event -> {
                int index = tabPaneRef.get().getSelectionModel().getSelectedIndex();
                if(index == tabPaneRef.get().getTabs().size()-1){
                    tabPaneRef.get().getSelectionModel().selectFirst();
                } else {
                    tabPaneRef.get().getSelectionModel().selectNext();
                }
            });
            selectNextTabItem.setAccelerator(KeyCodeCombination.keyCombination("Alt+Right"));

            MenuItem selectPreviousTabItem = new MenuItem("Select previous tab");
            selectPreviousTabItem.setOnAction(event -> {
                int index = tabPaneRef.get().getSelectionModel().getSelectedIndex();
                if(index == 0){
                    tabPaneRef.get().getSelectionModel().selectLast();
                } else {
                    tabPaneRef.get().getSelectionModel().selectPrevious();
                }
            });
            selectPreviousTabItem.setAccelerator(KeyCodeCombination.keyCombination("Alt+Left"));

            if (localizationPack != null){
                closeItem.textProperty().bind(localizationPack.get("closeItem"));
                closeOthersItem.textProperty().bind(localizationPack.get("closeOthersItem"));
                closeAllItems.textProperty().bind(localizationPack.get("closeAllItems"));
                closeToTheLeftItem.textProperty().bind(localizationPack.get("closeToTheLeftItem"));
                closeToTheRightItem.textProperty().bind(localizationPack.get("closeToTheRightItem"));
                splitVerticallyItem.textProperty().bind(localizationPack.get("splitVerticallyItem"));
                splitHorizontallyItem.textProperty().bind(localizationPack.get("splitHorizontallyItem"));
                selectNextTabItem.textProperty().bind(localizationPack.get("selectNextTabItem"));
                selectPreviousTabItem.textProperty().bind(localizationPack.get("selectPreviousTabItem"));
            }

            this.setOnShowing(event ->{
                tabPaneRef.set((DraggableTabPane) this.tab.getTabPane());
                closeOthersItem.setDisable(tabPaneRef.get().getTabs().size() == 1);
                closeToTheLeftItem.setDisable(tabPaneRef.get().getTabs().indexOf(tab) == 0);
                closeToTheRightItem.setDisable(tabPaneRef.get().getTabs().indexOf(tab) == tabPaneRef.get().getTabs().size()-1);
                splitVerticallyItem.setDisable(!tabPaneRef.get().isWrappedInDockPane() || tabPaneRef.get().getTabs().size() == 1);
                splitHorizontallyItem.setDisable(!tabPaneRef.get().isWrappedInDockPane() || tabPaneRef.get().getTabs().size() == 1);
                selectNextTabItem.setDisable(tabPaneRef.get().getTabs().size() == 1);
                selectPreviousTabItem.setDisable(tabPaneRef.get().getTabs().size() == 1);
            });

            this.getItems().addAll(
                    closeItem,
                    closeOthersItem,
                    closeAllItems,
                    closeToTheLeftItem,
                    closeToTheRightItem,
                    new SeparatorMenuItem(),
                    splitVerticallyItem,
                    splitHorizontallyItem,
                    new SeparatorMenuItem(),
                    selectNextTabItem,
                    selectPreviousTabItem
            );

        }

    }

}