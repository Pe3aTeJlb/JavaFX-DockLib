package docklib;

import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.TabPane;

import java.util.HashSet;
import java.util.Set;

public class DraggableTabPane extends TabPane {

    public static final Set<DraggableTabPane> tabPanes = new HashSet<>();

    private final Object project;
    private final TabGroup tabGroup;
    private DockPane dockPane;

    private double prefWidth, prefHeight;
    private boolean collapsed = false;


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
        } else {
            this.setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
        }
        tabPanes.add(this);

    }

    /*
    public void addTab(DraggableTab tab){
        if(tab.getTabGroup() == tabGroup &&
                ((DraggableTabPane)tab.getTabPane()).sameProject(this)){
               this.getTabs().add(tab);
        }
    }
*/

    public void addTab(DraggableTabPane originTabPane, DraggableTab tab){
        if(tab.getTabGroup() == tabGroup &&
                originTabPane.sameProject(this)){
            this.getTabs().add(tab);
            //tab.updateOriginTabPane(this);
        }
    }

    public void addAll(DraggableTab... tabs){
        for(DraggableTab tab : tabs){
            //tab.updateOriginTabPane(this);
        }
        this.getTabs().addAll(tabs);
    }


    public void collapse(){

        if(collapsed)
            return;

        if(getSide() == Side.TOP || getSide() == Side.BOTTOM) {
            prefHeight = this.getPrefHeight();
            prefWidth = this.getPrefWidth();
            this.setMaxHeight(1.315 * this.getTabMinHeight());
        } else  {
            prefHeight = this.getPrefHeight();
            prefWidth = this.getPrefWidth();
            this.setMaxWidth(1.315 * this.getTabMinHeight());
        }

        collapsed = true;

    }

    public void show(){

        if(!collapsed)
            return;

        this.setMaxHeight(prefHeight);
        this.setMaxWidth(prefWidth);

        collapsed = false;

    }


    public boolean sameProject(DraggableTabPane draggableTabPane){
        return draggableTabPane.sameProject(this.project);
    }

    private boolean sameProject(Object project){
        return this.project == project;
    }

    public void dock(Node node, DockAnchor dockAnchor){
        if(dockPane != null){
            dockPane.dock(node, dockAnchor, this);
        }
    }

    public void undock(){
        if(dockPane != null){
            dockPane.undock(this);
        }
    }


    public TabGroup getTabGroup(){
        return tabGroup;
    }

    public boolean isCollapsed(){
        return collapsed;
    }


    public boolean isWrappedInDockPane(){
        return dockPane != null;
    }

    public void setDockPane(DockPane dockPane){
        this.dockPane = dockPane;
    }

    public DockPane getDockPane(){
        return dockPane;
    }

}
