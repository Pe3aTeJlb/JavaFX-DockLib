package docklib;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.util.HashSet;
import java.util.Set;

/*
* Warning dont use this.getTabs().add to actually add tabs, this will break tabgroup logic
* Use this.addTab instead
*/
public class DraggableTabPane extends TabPane {

    public static final Set<DraggableTabPane> tabPanes = new HashSet<>();

    private final Object project;
    private final TabGroup tabGroup;

    private DockPane dockPane;
    private SplitPane split;

    private double prefWidth, prefHeight;
    private boolean collapsed = false;
    private BooleanProperty haveDetachedTab;

    private InvalidationListener showingListener;

    public DraggableTabPane(){
        this(null, TabGroup.None, null);
    }

    public DraggableTabPane(Object project){
        this(project, TabGroup.None, null);
    }

    public DraggableTabPane(TabGroup dockGroup){
        this(null, dockGroup, null);
    }

    public DraggableTabPane(DockPane dockPane){
        this(null, TabGroup.None, dockPane);
    }

    public DraggableTabPane(Object project, DockPane dockPane){
        this(project, TabGroup.None, dockPane);
    }

    public DraggableTabPane(TabGroup dockGroup, DockPane dockPane){
        this(null, dockGroup, dockPane);
    }

    public DraggableTabPane(Object project, TabGroup dockGroup, DockPane dockPane){

        super();

        this.project = project;
        this.tabGroup = dockGroup;
        this.dockPane = dockPane;
        if(dockGroup == TabGroup.System) {
            this.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
            showingListener = observable -> collapse();
            this.widthProperty().addListener(showingListener);
        } else {
            this.setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
        }

        //Double-listener
        //when tabpane have no tabs and no FLOAT DETACHED tab, undock it
        this.getTabs().addListener((ListChangeListener<Tab>) change -> {
            if(!haveDetachedTab.get() && this.getTabs().isEmpty()){
                undock();
            }
        });

        haveDetachedTab = new SimpleBooleanProperty(false);
        haveDetachedTab.addListener((observableValue, oldVal, newVal) -> {
            if(oldVal && !newVal && this.getTabs().isEmpty()){
                undock();
            }
        });

        tabPanes.add(this);

    }

    public void bindDetachedTab(SimpleBooleanProperty detachedProperty){
        if(this.getTabs().size() == 1) {
            haveDetachedTab.bind(detachedProperty);
        }
    }


    public void addTab(DraggableTab tab){
        if(tab.getOriginTabPane() == null){
            this.getTabs().add(tab);
            tab.setTabProperties(this);
        } else {
            if (tab.getTabGroup() == tabGroup &&
                    tab.getOriginTabPane().sameProject(this)) {
                this.getTabs().add(tab);
            }
        }
    }

    public void addAll(DraggableTab... tabs){
        for(DraggableTab tab : tabs){
            this.addTab(tab);
        }
    }


    public TabGroup getTabGroup(){
        return tabGroup;
    }

    public boolean isCollapsed(){
        return collapsed;
    }

    public void collapse(){

        this.widthProperty().removeListener(showingListener);

        if(collapsed)
            return;

        if(getSide() == Side.TOP || getSide() == Side.BOTTOM) {
            prefHeight = this.getPrefHeight();
            prefWidth = this.getPrefWidth();
            this.setMaxHeight(1.315 * this.getTabMinHeight());
            this.setMinHeight(1.315 * this.getTabMinHeight());
        } else  {
            prefHeight = this.getPrefHeight();
            prefWidth = this.getPrefWidth();
            this.setMaxWidth(1.315 * this.getTabMinHeight());
            this.setMinWidth(1.315 * this.getTabMinHeight());
        }

        this.setFocused(false);
        //this.getSelectionModel().clearSelection();
        collapsed = true;

    }

    public void show(){

        if(!collapsed)
            return;

        this.setMaxHeight(prefHeight);
        this.setMaxWidth(prefWidth);

        if(isWrappedInDockPane()){

            double magnitude = 0;
            int relativeIndex = 0;

            if (split.getItems().size() > 0) {

                relativeIndex = split.getItems().indexOf(this);
                boolean otherSide = false;
                if(relativeIndex == split.getItems().size() - 1) otherSide = true;

                if (split.getOrientation() == Orientation.HORIZONTAL) {

                    for (Node splitItem : split.getItems()) {
                        magnitude += splitItem.prefWidth(0);
                    }

                    if(otherSide){
                        split.setDividerPosition(relativeIndex - 1 , 1 - this.prefWidth(0) / magnitude);
                    } else {
                        split.setDividerPosition(relativeIndex, this.prefWidth(0) / magnitude);
                    }

                } else {

                    for (Node splitItem : split.getItems()) {
                        magnitude += splitItem.prefHeight(0);
                    }

                    if(otherSide){
                        split.setDividerPosition(relativeIndex - 1, 1 - this.prefHeight(0) / magnitude);
                    } else {
                        split.setDividerPosition(relativeIndex, this.prefHeight(0) / magnitude);
                    }

                }

            }

        }

        collapsed = false;

    }



    public Object getProject(){
        return project;
    }

    public boolean sameProject(DraggableTabPane draggableTabPane){
        return draggableTabPane.sameProject(this.project);
    }

    private boolean sameProject(Object project){
        return this.project == project;
    }


    public boolean isWrappedInDockPane(){
        return this.dockPane != null;
    }

    public void setDockPane(DockPane dockPane, SplitPane splitPane){
        this.dockPane = dockPane;
        this.split = splitPane;
    }

    public DockPane getDockPane(){
        return dockPane;
    }

    public void dock(Node node, DockAnchor dockAnchor){
        if(dockPane != null){
            dockPane.dock(node, dockAnchor, this);
        }
    }

    public void undock(){
        haveDetachedTab.unbind();
        if(dockPane != null){
            dockPane.undock(this);
        }
    }

}
