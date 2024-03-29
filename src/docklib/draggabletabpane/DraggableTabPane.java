package docklib.draggabletabpane;

import com.sun.javafx.tk.Toolkit;
import docklib.customsplitpane.CustomSplitPane;
import docklib.customsplitpane.SplitPaneSkin;
import docklib.dock.DockAnchor;
import docklib.dock.DockPane;
import docklib.dock.Dockable;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.HashSet;
import java.util.Set;

/*
 * Warning dont use this.getTabs().add to actually add tabs, this will break tabgroup logic
 * Use this.addTab instead
 */
public class DraggableTabPane extends TabPane implements Dockable {

    public static final Set<DraggableTabPane> tabPanes = new HashSet<>();

    private final TabGroup tabGroup;

    private BooleanProperty haveDetachedTab;

    private InvalidationListener skinListener;
    private InvalidationListener layoutListener;

    private boolean collapseOnInit = true;
    private double prefExpandedSize;
    private double DEFAULT_EXPAND_SIZE = 200;

    private Window owner;

    public DraggableTabPane(Window window){
        this(window, TabGroup.None, null);
    }

    public DraggableTabPane(Window window, TabGroup dockGroup){
        this(window, dockGroup, null);
    }

    public DraggableTabPane(Window window, DockPane dockPane){
        this(window, TabGroup.None, dockPane);
    }

    public DraggableTabPane(Window window, TabGroup dockGroup, DockPane dockPane){

        super();

        this.owner = window;
        this.tabGroup = dockGroup;
        this.dockPane = dockPane;
        this.prefExpandedSize = DEFAULT_EXPAND_SIZE;

        if(dockGroup == TabGroup.System) {

            this.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
            setCollapseOnInit(true);

        } else {
            this.setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
            this.setOnMouseClicked(event -> {
                if (isUnDockable() && (this.getScene().getWindow() instanceof Stage)){
                    ((Stage)this.getScene().getWindow()).titleProperty().bind(
                            ((DraggableTab)getSelectionModel().getSelectedItem()).getStageTitle());
                }
            });
        }

        //Double-listener
        //when tabpane have no tabs and no FLOAT DETACHED tab, undock it
        haveDetachedTab = new SimpleBooleanProperty(false);

        if (tabGroup != TabGroup.System) {

            this.getTabs().addListener((ListChangeListener<Tab>) change -> {
                if (!haveDetachedTab.get() && this.getTabs().isEmpty() && isWrappedInDockPane()) {
                    undock();
                }
            });

            haveDetachedTab.addListener((observableValue, oldVal, newVal) -> {
                if (oldVal && !newVal && this.getTabs().isEmpty() && isWrappedInDockPane()) {
                    undock();
                }
            });

        }

        this.setStyle("-fx-open-tab-animation: NONE; -fx-close-tab-animation: NONE;");

        tabPanes.add(this);

    }

    public void bindDetachedTab(SimpleBooleanProperty detachedProperty){
        if(this.getTabs().size() == 1) {
            haveDetachedTab.bind(detachedProperty);
        }
    }

    public Window getOwnerWindow() {
        return owner;
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

    @Override protected Skin<?> createDefaultSkin() {
        if (tabGroup == TabGroup.System) {
            return new DraggableTabPaneSkin(this);
        } else {
            return new CustomTabPaneSkin(this);
        }
    }



    //Dock interface

    private DockPane dockPane;
    private CustomSplitPane split;
    private BooleanProperty unDockable;

    public final void setUnDockable(boolean val){
        this.unDockableProperty().set(val);
    }

    public final boolean isUnDockable(){
        return this.unDockable == null ? true : this.unDockable.get();
    }

    public BooleanProperty unDockableProperty(){
        if (this.unDockable == null) {
            this.unDockable = new SimpleBooleanProperty(this, "unDockable", true);
        }

        return this.unDockable;
    }

    public boolean isWrappedInDockPane(){
        return this.dockPane != null;
    }

    public void setDockPane(DockPane dockPane, SplitPane splitPane){
        this.dockPane = dockPane;
        this.split = (CustomSplitPane) splitPane;
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
        if(dockPane != null && isUnDockable()){
            Event.fireEvent(this, new Event(UNDOCKING));
            dockPane.undock(this);
            tabPanes.remove(this);
            Event.fireEvent(this, new Event(UNDOCK));
        }
    }



    private ObjectProperty<Object> projectProperty;

    public final ObjectProperty<Object> projectProperty(){
        if (this.projectProperty == null) {
            this.projectProperty = new SimpleObjectProperty<>(this, "project", false);
        }

        return projectProperty;
    }

    public void setProject(Object project){
        projectProperty().set(project);
    }

    public Object getProject(){
        return projectProperty().get();
    }

    public boolean sameProject(DraggableTabPane draggableTabPane){
        return draggableTabPane.sameProject(projectProperty().get());
    }

    private boolean sameProject(Object project){
        return projectProperty().get() == project;
    }


    private BooleanProperty collapsed;

    public void setPrefExpandedSize(double size){
        prefExpandedSize = size;
    }

    public double getPrefExpandedSize(){
        return prefExpandedSize;
    }

    public void setCollapseOnInit(boolean collapseOnInit) {

        if (skinListener != null){
            this.skinProperty().removeListener(skinListener);
        }

        if (collapseOnInit){
            layoutListener = change -> {
                collapse();
            };
        } else {
            layoutListener = change -> {
                collapse();
                expand();
            };
        }

        skinListener = change -> {
            ((DraggableTabPaneSkin) this.getSkin()).getSkinHeightProperty().addListener(layoutListener);
        };
        this.skinProperty().addListener(skinListener);

    }

    public void collapse(){

        if(collapseOnInit) {
            this.skinProperty().removeListener(skinListener);
            ((DraggableTabPaneSkin) this.getSkin()).getSkinHeightProperty().removeListener(layoutListener);
        }

        if(isCollapsed())
            return;


        if(isWrappedInDockPane()){

            double[] dividers = split.getDividerPositions();
            int relativeIndex = split.getItems().indexOf(this);
            if (relativeIndex == split.getItems().size() - 1) {
                relativeIndex -= 1;
            }

            if(!collapseOnInit) {
                dividers[relativeIndex] = 1;
                Platform.runLater(() -> split.setDividerPositions(dividers));
            }

            SplitPaneSkin.ContentDivider divider = ((SplitPaneSkin)split.getSkin()).getContentDividers().get(relativeIndex);
            divider.setPrefWidth(0);
            divider.setVisible(false);

        }

        if(getSide().isHorizontal()) {
            if(!collapseOnInit) {
                prefExpandedSize = this.getHeight();
            }
            this.setMaxHeight(((DraggableTabPaneSkin)this.getSkin()).getTabHeaderAreaHeight());
            this.setMinHeight(((DraggableTabPaneSkin)this.getSkin()).getTabHeaderAreaHeight());
        } else  {
            if(!collapseOnInit) {
                prefExpandedSize = this.getWidth();
            }
            this.setMaxWidth(((DraggableTabPaneSkin)this.getSkin()).getTabHeaderAreaHeight());
            this.setMinWidth(((DraggableTabPaneSkin)this.getSkin()).getTabHeaderAreaHeight());
        }

        if(collapseOnInit) {
            collapseOnInit = false;
        }

        this.setFocused(false);

        this.collapsedProperty().set(true);

        this.requestParentLayout();

    }

    public void expand(){

        if(!isCollapsed())
            return;

        if(getSide().isHorizontal()){
            this.setMinHeight(((DraggableTabPaneSkin)this.getSkin()).getTabHeaderAreaHeight() + 10);
            this.setPrefHeight(prefExpandedSize);
            if (isWrappedInDockPane()) {
                this.setMaxHeight(TabPane.USE_PREF_SIZE);
                Platform.runLater(() ->  this.setMaxHeight(TabPane.USE_COMPUTED_SIZE));
            } else {
                this.setMaxHeight(TabPane.USE_COMPUTED_SIZE);
            }
        } else {
            this.setMinWidth(((DraggableTabPaneSkin)this.getSkin()).getTabHeaderAreaHeight() + 10);
            this.setPrefWidth(prefExpandedSize);
            if (isWrappedInDockPane()) {
                this.setMaxWidth(TabPane.USE_PREF_SIZE);
                Platform.runLater(() ->  this.setMaxWidth(TabPane.USE_COMPUTED_SIZE));
            } else {
                this.setMaxWidth(TabPane.USE_COMPUTED_SIZE);
            }
        }

        this.collapsedProperty().set(false);

        if(isWrappedInDockPane()){

            double magnitude = 0;
            int relativeIndex;

            if (split.getItems().size() > 0) {

                relativeIndex = split.getItems().indexOf(this);
                boolean otherSide = false;
                if(relativeIndex == split.getItems().size() - 1){
                    relativeIndex -= 1;
                    otherSide = true;
                }

                if (split.getOrientation() == Orientation.HORIZONTAL) {

                    for (Node splitItem : split.getItems()) {
                        magnitude += splitItem.getLayoutBounds().getWidth();
                    }

                    if(otherSide){
                        split.setDividerPosition(relativeIndex, 1 - this.getPrefWidth() / magnitude);
                    } else {
                        split.setDividerPosition(relativeIndex, this.getPrefWidth() / magnitude);
                    }

                } else {

                    for (Node splitItem : split.getItems()) {
                        magnitude += splitItem.getLayoutBounds().getHeight();
                    }

                    if(otherSide){
                        split.setDividerPosition(relativeIndex, 1 - this.getPrefHeight() / magnitude);
                    } else {
                        split.setDividerPosition(relativeIndex, this.getPrefHeight() / magnitude);
                    }

                }

                SplitPaneSkin.ContentDivider divider = ((SplitPaneSkin)split.getSkin()).getContentDividers().get(relativeIndex);
                divider.setPrefWidth(SplitPane.USE_COMPUTED_SIZE);
                divider.setVisible(true);

            }

        }

    }

    public final boolean isCollapsed() {
        return collapsedProperty().get();
    }

    public final BooleanProperty collapsedProperty() {
        if (this.collapsed == null) {
            this.collapsed = new SimpleBooleanProperty(this, "collapsed", false);
        }

        return this.collapsed;
    }


    public NodeOrientation getHeaderOrientation(){
        return ((HeaderReachable)getSkin()).getHeaderOrientation();
    }

    public static final EventType<DraggableTabEvent> ANY = new EventType<>(Event.ANY, "DRAGGABLETABPANE_EVENT");
    public static final EventType<DraggableTabEvent> UNDOCKING = new EventType<>(ANY, "UNDOCKING");
    public static final EventType<DraggableTabEvent> UNDOCK = new EventType<>(ANY, "UNDOCK");

    //Custom Events

    public void setOnUndocking(EventHandler<Event> var1) {
        this.addEventHandler(UNDOCKING, var1);
    }

    public void setOnUndock(EventHandler<Event> var1) {
        this.addEventHandler(UNDOCK, var1);
    }

/*
    private StringProperty tabAnimation;

    public void setTabAnimation(String val){
        tabAnimationProperty().set(val);
    }

    public final StringProperty tabAnimationProperty(){
        if (this.tabAnimation == null) {
            this.tabAnimation = new SimpleStringProperty(this, "tabAnimation", "-fx-open-tab-animation: NONE; -fx-close-tab-animation: NONE");
        }
        return this.tabAnimation;
    }*/

}
