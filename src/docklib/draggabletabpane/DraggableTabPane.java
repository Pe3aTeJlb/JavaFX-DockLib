package docklib.draggabletabpane;

import docklib.dock.DockAnchor;
import docklib.dock.DockPane;
import docklib.dock.Dockable;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.skin.TabPaneSkin;

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

    public boolean collapseOnStart = true;

    public DraggableTabPane(){
        this(TabGroup.None, null);
    }

    public DraggableTabPane(TabGroup dockGroup){
        this(dockGroup, null);
    }

    public DraggableTabPane(DockPane dockPane){
        this(TabGroup.None, dockPane);
    }

    public DraggableTabPane(TabGroup dockGroup, DockPane dockPane){

        super();

        this.tabGroup = dockGroup;
        this.dockPane = dockPane;
        if(dockGroup == TabGroup.System) {

            this.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);

            if(collapseOnStart) {
                layoutListener = change -> collapse();
                skinListener = change -> ((DraggableTabPaneSkin) this.getSkin()).getSkinHeightProperty().addListener(layoutListener);
                this.skinProperty().addListener(skinListener);
            }

        } else {
            this.setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
        }

        //Double-listener
        //when tabpane have no tabs and no FLOAT DETACHED tab, undock it
        haveDetachedTab = new SimpleBooleanProperty(false);
        /*
        this.getTabs().addListener((ListChangeListener<Tab>) change -> {
            if(!haveDetachedTab.get() && this.getTabs().isEmpty()){
                undock();
            }
        });


        haveDetachedTab.addListener((observableValue, oldVal, newVal) -> {
            if(oldVal && !newVal && this.getTabs().isEmpty()){
                undock();
            }
        });

         */

        this.setStyle("-fx-open-tab-animation: NONE; -fx-close-tab-animation: NONE;");

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

    @Override protected Skin<?> createDefaultSkin() {
        if (tabGroup == TabGroup.System) {
            return new DraggableTabPaneSkin(this);
        } else {
            return new CustomTabPaneSkin(this);
        }
    }



    //Dock interface

    private DockPane dockPane;
    private SplitPane split;

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



    private ObjectProperty<Object> projectProperty;

    public final ObjectProperty<Object> projectProperty(){
        if (this.projectProperty == null) {
            this.projectProperty = new SimpleObjectProperty<>(this, "project", false);
        }

        return projectProperty;
    }

    public void setProject(Object project){
        this.projectProperty.set(project);
    }

    public Object getProject(){
        return projectProperty.get();
    }

    public boolean sameProject(DraggableTabPane draggableTabPane){
        return draggableTabPane.sameProject(this.projectProperty);
    }

    private boolean sameProject(Object project){
        //return projectProperty().get() == project;
        return true;
    }


    private BooleanProperty collapsed;
    private double prefExpandedSize = 0;

    public void setCollapseOnStart(boolean collapseOnStart) {
        this.collapseOnStart = collapseOnStart;
    }

    public void collapse(){

        if(collapseOnStart) {
            this.skinProperty().removeListener(skinListener);
            ((DraggableTabPaneSkin) this.getSkin()).getSkinHeightProperty().removeListener(layoutListener);
        }

        if(isCollapsed())
            return;

        if(getSide().isHorizontal()) {
            prefExpandedSize = this.getHeight();
            this.setMaxHeight(((DraggableTabPaneSkin)this.getSkin()).getTabHeaderAreaHeight());
            this.setMinHeight(((DraggableTabPaneSkin)this.getSkin()).getTabHeaderAreaHeight());
        } else  {
            prefExpandedSize = this.getWidth();
            this.setMaxWidth(((DraggableTabPaneSkin)this.getSkin()).getTabHeaderAreaHeight());
            this.setMinWidth(((DraggableTabPaneSkin)this.getSkin()).getTabHeaderAreaHeight());
        }

        collapsedProperty().set(true);

        this.setFocused(false);

    }

    public void expand(){

        if(!isCollapsed())
            return;

        if(getSide().isHorizontal()){
            this.setMinHeight(((DraggableTabPaneSkin)this.getSkin()).getTabHeaderAreaHeight() + 10);
            this.setPrefHeight(prefExpandedSize);
            this.setMaxHeight(TabPane.USE_COMPUTED_SIZE);
        } else {
            this.setMinWidth(((DraggableTabPaneSkin)this.getSkin()).getTabHeaderAreaHeight() + 10);
            this.setPrefWidth(prefExpandedSize);
            this.setMaxWidth(TabPane.USE_COMPUTED_SIZE);
        }

        collapsedProperty().set(false);

        if(isWrappedInDockPane()){

            double magnitude = 0;
            int relativeIndex;

            if (split.getItems().size() > 0) {

                relativeIndex = split.getItems().indexOf(this);
                boolean otherSide = false;
                if(relativeIndex == split.getItems().size() - 1) otherSide = true;

                if (split.getOrientation() == Orientation.HORIZONTAL) {

                    for (Node splitItem : split.getItems()) {
                        magnitude += splitItem.getLayoutBounds().getWidth();
                    }

                    if(otherSide){
                        split.setDividerPosition(relativeIndex - 1, 1 - this.getPrefWidth() / magnitude);
                    } else {
                        split.setDividerPosition(relativeIndex, this.getPrefWidth() / magnitude);
                    }

                } else {

                    for (Node splitItem : split.getItems()) {
                        magnitude += splitItem.getLayoutBounds().getHeight();
                    }

                    if(otherSide){
                        split.setDividerPosition(relativeIndex - 1, 1 - this.getPrefHeight() / magnitude);
                    } else {
                        split.setDividerPosition(relativeIndex, this.getPrefHeight() / magnitude);
                    }

                }

            }

        }

    }

    public final boolean isCollapsed() {
        return this.collapsed != null && this.collapsed.get();
    }

    public final BooleanProperty collapsedProperty() {
        if (this.collapsed == null) {
            this.collapsed = new SimpleBooleanProperty(this, "collapsed", false);
        }

        return this.collapsed;
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
