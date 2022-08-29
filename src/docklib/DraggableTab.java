package docklib;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;

import static docklib.DraggableTabPane.tabPanes;

/* !!!Warning!!!
* This realisation doesn't use initOwner() to share window lifecycle cause then we can't minimize detached tabs,
* so, u have to write u own frame/window manager to handle this problem or just accept this fact
*/
public class DraggableTab extends Tab {

    private Label tabLabel;
    private Label dragText;

    //Tech stage for event handling
    private Stage dragStage;
    private TabGroup tabGroup;

    //originTabPane refresh only when click on label or close it in purpose to provide proper behaviour while detached
    private DraggableTabPane originTabPane;
    private int originIndex;
    private Point2D dragOrigin;
    private SimpleBooleanProperty detached;
    private String animDirection = "";

    private DraggableTabPane lastInsertPane;

    private Stage floatStage;
    private boolean createNewFloatStage = true;

    //this means, u can detach System tab and attach it back or to another System tabPane
    private boolean detachable;

    //For docking
    private HashMap<Window, Node> dragNodes = new HashMap<>();
    private Window targetWindow;

    public void updateOriginTabPane(DraggableTabPane draggableTabPane){
        originTabPane = draggableTabPane;
        tabGroup = draggableTabPane.getTabGroup();
        if(this.getContextMenu() == null){

        }
    }

    public DraggableTab(String text, DraggableTabPane tabPane) {
        this(text, tabPane, null, null);
    }

    public DraggableTab(String text, DraggableTabPane tabPane, String iconName) {
        this(text, tabPane, iconName, null);
    }

    public DraggableTab(String text, DraggableTabPane tabPane, String iconName, Node content) {

        originTabPane = tabPane;
        tabGroup = tabPane.getTabGroup();
        detachable = tabGroup != TabGroup.System;
        detached = new SimpleBooleanProperty(false);

        tabLabel = new Label(text);
        if(iconName != null) {
            tabLabel.setGraphic(IconsManager.getImageView(iconName));
        }
        setGraphic(tabLabel);

        //Prepare dragStage, which shown when tab dragged
        dragStage = new Stage();
        dragStage.initStyle(StageStyle.UNDECORATED);
        dragStage.setAlwaysOnTop(true);

        StackPane dragStagePane = new StackPane();
        //dragStagePane.setStyle("-fx-background-color:#DDDDDD;");

        dragText = new Label(text);
        if(iconName != null) {
            dragText.setGraphic(IconsManager.getImageView(iconName));
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
                case TOP:       animDirection = ""; dragDelta = Math.abs(event.getScreenY() - dragOrigin.getY()); break;
                case LEFT:      animDirection = "-"; dragDelta = Math.abs(event.getScreenX() - dragOrigin.getX()); break;
                case RIGHT:     animDirection = ""; dragDelta = Math.abs(dragOrigin.getX() - event.getScreenX()); break;
                case BOTTOM:    animDirection = "-"; dragDelta = Math.abs(dragOrigin.getY() - event.getScreenY()); break;
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

                this.getTabPane().getTabs().remove(this);

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

            //hide/show system tab content
            collapseSystemTab();

            //After detach, drag event will be continued on parent tabPane
            defineDragContinueEvent();

            //Detached tab released
            defineMouseReleaseEvent();

        });

        tabLabel.addEventFilter(MouseEvent.DRAG_DETECTED, event -> {
            originTabPane.requestFocus();
            originTabPane.startFullDrag();
        });

        this.setContextMenu(new DraggableTabContextMenu(this, tabGroup));
        this.setContent(content);

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

                if(this.isSelected() || originTabPane.isCollapsed()) {

                    if (originTabPane.isCollapsed()) {
                        originTabPane.show();
                    } else {
                        originTabPane.collapse();
                    }
                }

            }

        }

    }

    public void defineDragContinueEvent(){

        originTabPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {

            if(event.getButton() != MouseButton.PRIMARY)
                return;

            if(!detached.get())
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

            } else {
                //Dock events
                DockEvent dockEnterEvent =
                        new DockEvent(this, DockEvent.NULL_SOURCE_TARGET, DockEvent.DOCK_ENTER, event.getX(),
                                event.getY(), event.getScreenX(), event.getScreenY(), null);
                DockEvent dockOverEvent =
                        new DockEvent(this, DockEvent.NULL_SOURCE_TARGET, DockEvent.DOCK_OVER, event.getX(),
                                event.getY(), event.getScreenX(), event.getScreenY(), null);
                DockEvent dockExitEvent =
                        new DockEvent(this, DockEvent.NULL_SOURCE_TARGET, DockEvent.DOCK_EXIT, event.getX(),
                                event.getY(), event.getScreenX(), event.getScreenY(), null);

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

            dragStage.hide();

            if (!event.isStillSincePress()) {

                //Insert tab into tabPane
                Point2D screenPoint = new Point2D(event.getScreenX(), event.getScreenY());
                DraggableTabPane oldTabPane = originTabPane;
                int oldIndex = originIndex;

                InsertData insertData = getInsertData(screenPoint);

                if (insertData != null && tabGroup == insertData.getInsertPane().getTabGroup() && originTabPane.sameProject(insertData.getInsertPane())) {
                    int addIndex = insertData.getIndex();
                    if(oldTabPane == insertData.getInsertPane() && oldTabPane.getTabs().size() == 1) {
                        return;
                    }
                    oldTabPane.getTabs().remove(DraggableTab.this);
                    if(oldIndex < addIndex && oldTabPane == insertData.getInsertPane()) {
                        //addIndex--;
                    }
                    if (addIndex > insertData.getInsertPane().getTabs().size()) {
                        addIndex = insertData.getInsertPane().getTabs().size();
                    }
                    insertData.getInsertPane().getTabs().add(addIndex, DraggableTab.this);
                    insertData.getInsertPane().selectionModelProperty().get().select(addIndex);
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

                    fireDockEvent(event, null);

                    return;
                }

                //undetachable tab was detached
                if (!detachable) {
                    detached.set(false);
                    originTabPane.getTabs().add(originIndex, DraggableTab.this);
                    return;
                }

                //Or Create stage for detached tab
                if (dragNodes.get(targetWindow) != null) {
                    //at this moment this tab is deleted from origin tabpane but we need it reference
                    //to check if it relate to the same project
                    DraggableTabPane draggableTabPane = new DraggableTabPane(tabGroup);
                    draggableTabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
                    draggableTabPane.addTab(originTabPane, this);

                    fireDockEvent(event, draggableTabPane);

                } else {
                    detachTab(event);
                }

                detached.set(false);

            }

        });

    }

    /* Dock event */

    public void dockEventCallback(boolean docked, DockEvent event){

        if(docked){
            //terminate float-stage if it is empty
            if(originTabPane.getTabs().isEmpty()){
                if(floatStage != null) {
                    floatStage.hide();
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

    private void pickEventTarget(Point2D location, EventTask eventTask, Event explicit) {

        List<DockPane> dockPanes = DockPane.dockPanes;

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
            final DraggableTabPane pane = new DraggableTabPane(tabGroup);
            pane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);

            newFloatStage.setOnHiding(hideEvent -> tabPanes.remove(pane));
            pane.getTabs().add(DraggableTab.this);
            pane.getTabs().addListener((ListChangeListener<Tab>) change -> {
                if (pane.getTabs().isEmpty() && !detached.get()) {
                    //calls when tabpane contains no tabs and have no detached floating tabs at the moment
                    newFloatStage.close();
                    newFloatStage.setScene(null);
                    createNewFloatStage = true;
                } else if (pane.getTabs().isEmpty() && detached.get()) {
                    floatStage = newFloatStage;
                    createNewFloatStage = false;
                }
            });

            newFloatStage.setScene(new Scene(pane));
            newFloatStage.initStyle(StageStyle.DECORATED);
            newFloatStage.setX(screenX);
            newFloatStage.setY(screenY);
            newFloatStage.show();
            pane.requestLayout();
            pane.requestFocus();

        } else {

            //If u detached tab from float stage to free space
            //and that stage contains only this tab, return it back and just move float stage
            originTabPane.getTabs().add(DraggableTab.this);
            floatStage.setX(screenX);
            floatStage.setY(screenY);

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

            Rectangle2D tabAbsolute = getAbsoluteRect(tabPane);
            if(tabAbsolute.contains(screenPoint)) {

                int tabInsertIndex = 0;

                if(!tabPane.getTabs().isEmpty()) {

                    if(tabPane.getScene() == null) {
                        continue;
                    }
                    Rectangle2D firstTabRect = getAbsoluteRect(tabPane.getTabs().get(0));

                    if(side == Side.TOP) {

                        if (screenPoint.getY() > firstTabRect.getMaxY() || screenPoint.getY() < firstTabRect.getMinY()) {
                            return null;
                        }

                        Rectangle2D lastTabRect = getAbsoluteRect(tabPane.getTabs().get(tabPane.getTabs().size() - 1));

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

                    } else if(side == Side.BOTTOM){

                        if (screenPoint.getY() < firstTabRect.getMinY() || screenPoint.getY() > firstTabRect.getMaxY()) {
                            return null;
                        }

                        Rectangle2D lastTabRect = getAbsoluteRect(tabPane.getTabs().get(tabPane.getTabs().size() - 1));

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

                    } else if(side == Side.LEFT) {

                        if (screenPoint.getX() > firstTabRect.getMaxX() || screenPoint.getX() < firstTabRect.getMinX()) {
                            return null;
                        }

                        Rectangle2D lastTabRect = getAbsoluteRect(tabPane.getTabs().get(tabPane.getTabs().size() - 1));

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

                    } else if(side == Side.RIGHT){

                        if (screenPoint.getX() > firstTabRect.getMaxX() || screenPoint.getX() < firstTabRect.getMinX()) {
                            return null;
                        }

                        Rectangle2D lastTabRect = getAbsoluteRect(tabPane.getTabs().get(tabPane.getTabs().size() - 1));

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

                    }


                }

                return new InsertData(tabInsertIndex, tabPane);

            }

        }

        return null;

    }

    private Rectangle2D getAbsoluteRect(Control node) {

        return new Rectangle2D(
                node.localToScene(node.getLayoutBounds().getMinX(), node.getLayoutBounds().getMinY()).getX() + node.getScene().getWindow().getX(),
                node.localToScene(node.getLayoutBounds().getMinX(), node.getLayoutBounds().getMinY()).getY() + node.getScene().getWindow().getY(),
                node.getWidth(),
                node.getHeight()
        );

    }

    private Rectangle2D getAbsoluteRect(Control node, Side side){

        //Pls, don't ask me why there is no symmetry, just put up with these coefficients
        //Thank God, it works
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

        return null;

    }

    private Rectangle2D getAbsoluteRect(Tab tab) {
        Control node = ((DraggableTab) tab).getLabel();
        return getAbsoluteRect(node, tab.getTabPane().getSide());
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


    /* Getters/Setters */

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
        tabLabel.setText(text);
        dragText.setText(text);
    }

    private Label getLabel() {
        return tabLabel;
    }


    private class DraggableTabContextMenu extends ContextMenu{

        private DraggableTab tab;
        private TabViewMode viewMode;
        private ChangeListener<Boolean> focusListener;

        public DraggableTabContextMenu(DraggableTab tab, TabGroup tabGroup){

            super();

            this.tab = tab;

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
                terminateFloatStage(null);
                tab.getTabPane().focusedProperty().removeListener(focusListener);
                tab.getTabPane().getTabs().remove(tab);
            });

            this.getItems().addAll(
                    dockPinnedItem,
                    dockUnpinnedItem,
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
            ((DraggableTabPane)tab.getTabPane()).show();

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

            AtomicReference<DraggableTabPane> tabPaneRef = new AtomicReference<>();

            MenuItem closeItem = new MenuItem("Close");
            closeItem.setOnAction(event -> tabPaneRef.get().getTabs().remove(tab));

            MenuItem closeOthersItem = new MenuItem("Close others");
            closeOthersItem.setOnAction(event -> {
                tabPaneRef.get().getTabs().clear();
                tabPaneRef.get().getTabs().add(tab);
            });

            MenuItem closeAllItems = new MenuItem("Close all");
            closeAllItems.setOnAction(event -> tabPaneRef.get().getTabs().clear());

            MenuItem closeToTheLeftItem = new MenuItem("Close all to the left");
            closeToTheLeftItem.setOnAction(event -> {
                int index = tabPaneRef.get().getTabs().indexOf(tab);
                List<Tab> tabs = new ArrayList<>(tabPaneRef.get().getTabs().subList(index, tabPaneRef.get().getTabs().size()));
                tabPaneRef.get().getTabs().setAll(tabs);
            });

            MenuItem closeToTheRightItem = new MenuItem("Close all to the right");
            closeToTheRightItem.setOnAction(event -> {
                int index = tabPaneRef.get().getTabs().indexOf(tab);
                List<Tab> tabs = new ArrayList<>(tabPaneRef.get().getTabs().subList(0, index + 1));
                tabPaneRef.get().getTabs().setAll(tabs);
            });

            MenuItem splitVerticallyItem = new MenuItem("Split vertically");
            splitVerticallyItem.setOnAction(event -> {
                tabPaneRef.get().getTabs().remove(tab);
                DraggableTabPane draggableTabPane = new DraggableTabPane(tabGroup);
                draggableTabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
                draggableTabPane.addTab(tabPaneRef.get(), tab);
                tabPaneRef.get().dock(draggableTabPane, DockAnchor.BOTTOM);
            });

            MenuItem splitHorizontallyItem = new MenuItem("Split horizontally");
            splitHorizontallyItem.setOnAction(event -> {
                tabPaneRef.get().getTabs().remove(tab);
                DraggableTabPane draggableTabPane = new DraggableTabPane(tabGroup);
                draggableTabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
                draggableTabPane.addTab(tabPaneRef.get(), tab);
                tabPaneRef.get().dock(draggableTabPane, DockAnchor.RIGHT);
            });


            MenuItem selectNextTabItem = new MenuItem("Select next tab");
            selectNextTabItem.setOnAction(event -> {
                int index = tabPaneRef.get().getTabs().indexOf(tab);
                if(index == tabPaneRef.get().getTabs().size()){
                    System.out.println("right1");
                    tabPaneRef.get().getSelectionModel().select(0);
                } else {
                    System.out.println("righ2");
                    tabPaneRef.get().getSelectionModel().select(index + 1);
                }
            });
            selectNextTabItem.setAccelerator(KeyCodeCombination.keyCombination("Alt+Right"));

            MenuItem selectPreviousTabItem = new MenuItem("Select previous tab");
            selectPreviousTabItem.setOnAction(event -> {
                int index = tabPaneRef.get().getTabs().indexOf(tab);
                if(index == 0){
                    System.out.println("left1");
                    tabPaneRef.get().getSelectionModel().select(tabPaneRef.get().getTabs().size() - 1);
                } else {
                    System.out.println("left2");
                    tabPaneRef.get().getSelectionModel().select(index - 1);
                }
            });
            selectPreviousTabItem.setAccelerator(KeyCodeCombination.keyCombination("Alt+Left"));

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