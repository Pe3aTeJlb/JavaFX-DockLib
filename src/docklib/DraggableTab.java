package docklib;

import docklib.trash.DockTitleBar;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

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

    private DraggableTabPane originTabPane;
    private int originIndex;
    private Point2D dragOrigin;
    private boolean detached;
    private String animDirection = "";

    private DraggableTabPane lastInsertPane;

    private Stage floatStage;
    private boolean createNewFloatStage = true;

    //this means, u can detach System tab and attach it back or to another System tabPane
    private boolean detachable;

    private HashMap<Window, Node> dragNodes = new HashMap<Window, Node>();


    public DraggableTab(String text, DraggableTabPane tabPane) {
        this(text, tabPane, null, null);
    }

    public DraggableTab(String text, DraggableTabPane tabPane, String iconName) {
        this(text, tabPane, iconName, null);
    }

    public DraggableTab(String text, DraggableTabPane tabPane, String iconName, Node content) {

        this.tabGroup = tabPane.getTabGroup();
        detachable = tabGroup != TabGroup.System;

        tabPanes.add(tabPane);

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
        dragStagePane.setStyle("-fx-background-color:#DDDDDD;");

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

            if (!detached && dragDelta > 25) {

                detached = true;

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

            //hide/show system tab content
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

            //After detach, drag event will be continued on parent tabPane
            defineDragContinueEvent();

            //Detached tab released
            defineMouseReleaseEvent();


        });

        tabLabel.addEventFilter(MouseEvent.DRAG_DETECTED, event -> {
            originTabPane.startFullDrag();
        });


        this.setContextMenu(new DraggableTabContextMenu(this, tabGroup));
        this.setContent(content);

    }


    /**
     * The task that is to be executed when the dock event target is picked. This provides context for
     * what specific events and what order the events should be fired.
     *
     * @since DockFX 0.1
     */
    private abstract class EventTask {
        /**
         * The number of times this task has been executed.
         */
        protected int executions = 0;

        /**
         * Creates a default DockTitleBar with captions and dragging behavior.
         *
         * @param node The node that was chosen as the event target.
         * @param dragNode The node that was last event target.
         */
        public abstract void run(Node node, Node dragNode);

        /**
         * The number of times this task has been executed.
         *
         * @return The number of times this task has been executed.
         */
        public int getExecutions() {
            return executions;
        }

        /**
         * Reset the execution count to zero.
         */
        public void reset() {
            executions = 0;
        }
    }

    /**
     * Traverse the scene graph for all open stages and pick an event target for a dock event based on
     * the location. Once the event target is chosen run the event task with the target and the
     * previous target of the last dock event if one is cached. If an event target is not found fire
     * the explicit dock event on the stage root if one is provided.
     *
     * @param location The location of the dock event in screen coordinates.
     * @param eventTask The event task to be run when the event target is found.
     * @param explicit The explicit event to be fired on the stage root when no event target is found.
     */
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
            Stack<Parent> stack = new Stack<Parent>();
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

    public void defineDragContinueEvent(){

        originTabPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {

            if(event.getButton() != MouseButton.PRIMARY)
                return;

            if(!detached)
                return;

            dragStage.setX(event.getScreenX() - dragStage.getWidth() / 2);
            dragStage.setY(event.getScreenY() - dragStage.getHeight() / 2);

            //Make spread "Animation" at target tabpane while attach
            tabPanes.add(originTabPane);

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
            }

            /*
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
                        Event.fireEvent(node, dockEnterEvent.copyFor(this, node));

                        if (dragNode != null) {
                            Event.fireEvent(dragNode, dockExitEvent.copyFor(this, dragNode));
                        }

                        dragNodes.put(node.getScene().getWindow(), node);
                    }
                    Event.fireEvent(node, dockOverEvent.copyFor(this, node));
                }
            };

            this.pickEventTarget(new Point2D(event.getScreenX(), event.getScreenY()), eventTask,
                    dockExitEvent);
*/
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
                tabPanes.add(oldTabPane);
                InsertData insertData = getInsertData(screenPoint);

                if (insertData != null && tabGroup == insertData.getInsertPane().getTabGroup()) {

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
                        floatStage.hide();
                        floatStage.setScene(null);
                    }
                    createNewFloatStage = true;
                    detached = false;
                    return;
                }

                //undetachable tab was detached
                if (!detachable) {
                    detached = false;
                    originTabPane.getTabs().add(originIndex, DraggableTab.this);
                    return;
                }

                //Or Create stage for detached tab
                if(createNewFloatStage) {

                    final Stage newFloatStage = new Stage();
                    final DraggableTabPane pane = new DraggableTabPane(tabGroup);

                    tabPanes.add(pane);

                    newFloatStage.setOnHiding(hideEvent -> tabPanes.remove(pane));
                    pane.getTabs().add(DraggableTab.this);
                    pane.getTabs().addListener((ListChangeListener<Tab>) change -> {
                        if (pane.getTabs().isEmpty() && !detached) {
                            newFloatStage.hide();
                            newFloatStage.setScene(null);
                            createNewFloatStage = true;
                        } else if(pane.getTabs().isEmpty() && detached){
                            floatStage = newFloatStage;
                            createNewFloatStage = false;
                        }
                    });

                    detached = false;
                    newFloatStage.setScene(new Scene(pane));
                    newFloatStage.initStyle(StageStyle.DECORATED);
                    newFloatStage.setX(event.getScreenX());
                    newFloatStage.setY(event.getScreenY());
                    newFloatStage.show();
                    pane.requestLayout();
                    pane.requestFocus();

                } else {

                    detached = false;
                    originTabPane.getTabs().add(DraggableTab.this);
                    floatStage.setX(event.getScreenX());
                    floatStage.setY(event.getScreenY());

                }

            }

            /*
            DockEvent dockReleasedEvent =
                    new DockEvent(this, DockEvent.NULL_SOURCE_TARGET, DockEvent.DOCK_RELEASED, event.getX(),
                            event.getY(), event.getScreenX(), event.getScreenY(), null, originTabPane);

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
*/
            // Remove temporary event handler for bug mentioned above.
            /*
            DockPane dockPane = this.getDockNode().getDockPane();
            if (dockPane != null) {
                dockPane.removeEventFilter(MouseEvent.MOUSE_DRAGGED, this);
                dockPane.removeEventFilter(MouseEvent.MOUSE_RELEASED, this);
            }*/

        });

    }


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

        private TabViewMode viewMode;
        private ChangeListener<Boolean> focusListener;

        public DraggableTabContextMenu(DraggableTab tab, TabGroup tabGroup){

            super();

            switch (tabGroup){
                case System:    populateSystemMenu(tab); break;
                case WorkSpace: populateWorkspaceMenu(tab); break;
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


        public void populateWorkspaceMenu(DraggableTab tab){

            MenuItem a = new MenuItem("Close");
            //a.setOnAction(event -> setDockedPinned());

            MenuItem a1 = new MenuItem("Close others");
            //a.setOnAction(event -> setDockedPinned());

            MenuItem a2 = new MenuItem("Close all");
            // a.setOnAction(event -> setDockedPinned());

            MenuItem a3 = new MenuItem("Close all to the left");
            // a.setOnAction(event -> setDockedPinned());

            MenuItem a4 = new MenuItem("Close all to the right");
            //  a.setOnAction(event -> setDockedPinned());

            MenuItem a5 = new MenuItem("Split vertically");
            //   a.setOnAction(event -> setDockedPinned());

            MenuItem a6 = new MenuItem("Split horizontally");
            //   a.setOnAction(event -> setDockedPinned());


            MenuItem a7 = new MenuItem("Select next tab");
            // a.setOnAction(event -> setDockedPinned());

            MenuItem a8 = new MenuItem("Select previous tab");
            // a.setOnAction(event -> setDockedPinned());

            this.getItems().addAll(
                    new SeparatorMenuItem()
            );

        }

    }

}