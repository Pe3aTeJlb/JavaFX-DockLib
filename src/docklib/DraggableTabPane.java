package docklib;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.skin.TabPaneSkin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/*
* Warning dont use this.getTabs().add to actually add tabs, this will break tabgroup logic
* Use this.addTab instead
*/
public class DraggableTabPane extends TabPane implements Dockable{

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
                skinListener = change -> ((CustomHeaderTabPaneSkin) this.getSkin()).getSkinHeightProperty().addListener(layoutListener);
                this.skinProperty().addListener(skinListener);
            }

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

    @Override protected Skin<?> createDefaultSkin() {
        if (tabGroup == TabGroup.System) {
            return new CustomHeaderTabPaneSkin(this);
        } else {
            return new TabPaneSkin(this);
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



    private ObjectProperty<Object> project;

    public final ObjectProperty<Object> projectProperty(){return project;}

    public void setProject(Object project){
        this.project.set(project);
    }

    public Object getProject(){
        return project.get();
    }

    public boolean sameProject(DraggableTabPane draggableTabPane){
        return draggableTabPane.sameProject(this.project);
    }

    private boolean sameProject(ObjectProperty<Object> project){
        return this.project.get() == project;
    }


    private BooleanProperty collapsed;
    private double prefExpandedSize = 0;

    public void setCollapseOnStart(boolean collapseOnStart) {
        this.collapseOnStart = collapseOnStart;
    }

    public void collapse(){

        if(collapseOnStart) {
            this.skinProperty().removeListener(skinListener);
            ((CustomHeaderTabPaneSkin) this.getSkin()).getSkinHeightProperty().removeListener(layoutListener);
        }

        if(isCollapsed())
            return;
/*
        int relativeIndex = 0;
        int otherSideIndex = 0;
        double divPos = 0;

        if (split.getItems().size() > 0) {

            relativeIndex = split.getItems().indexOf(this) == 0 ? 0 : split.getItems().indexOf(this) - 1;
            otherSideIndex = split.getDividers().size() - relativeIndex - 1;
            System.out.println("this "  + split.getItems().indexOf(this) + " " + relativeIndex + " " + split.getDividers().size() + " " +otherSideIndex);
            divPos = split.getDividers().get(otherSideIndex).getPosition();

            System.out.println("rel " + relativeIndex + " other " + otherSideIndex + " " + Arrays.toString(split.getDividerPositions()));

        }*/

        if(getSide().isHorizontal()) {
            prefExpandedSize = this.getHeight();
            this.setMaxHeight(((CustomHeaderTabPaneSkin)this.getSkin()).getTabHeaderAreaHeight());
            this.setMinHeight(((CustomHeaderTabPaneSkin)this.getSkin()).getTabHeaderAreaHeight());
        } else  {
            prefExpandedSize = this.getWidth();
            this.setMaxWidth(((CustomHeaderTabPaneSkin)this.getSkin()).getTabHeaderAreaHeight());
            this.setMinWidth(((CustomHeaderTabPaneSkin)this.getSkin()).getTabHeaderAreaHeight());
        }

        /*System.out.println(Arrays.toString(split.getDividerPositions()));
        split.setDividerPosition(otherSideIndex, divPos);
        System.out.println(Arrays.toString(split.getDividerPositions()));
*/
        collapsedProperty().set(true);

        this.setFocused(false);

    }

    public void expand(){

        if(!isCollapsed())
            return;

        if(getSide().isHorizontal()){
            this.setMinHeight(((CustomHeaderTabPaneSkin)this.getSkin()).getTabHeaderAreaHeight());
            //this.setPrefHeight(prefExpandedSize);
            this.setMaxHeight(TabPane.USE_COMPUTED_SIZE);
        } else {
            this.setMinWidth(((CustomHeaderTabPaneSkin)this.getSkin()).getTabHeaderAreaHeight());
            //this.setPrefWidth(prefExpandedSize);
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
                        magnitude += splitItem.prefWidth(0);
                    }

                    if(otherSide){
                        split.setDividerPosition(relativeIndex - 1, 1 - this.prefWidth(0) / magnitude);
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

    }

    public final boolean isCollapsed() {
        return this.collapsed != null && this.collapsed.get();
    }

    public final BooleanProperty collapsedProperty() {
        if (this.collapsed == null) {
            this.collapsed = new SimpleBooleanProperty(this, "rotateGraphic", false);
        }

        return this.collapsed;
    }

}
