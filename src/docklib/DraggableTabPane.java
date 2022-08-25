package docklib;

import javafx.geometry.Side;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import java.util.HashSet;
import java.util.Set;

public class DraggableTabPane extends TabPane {

    public static final Set<DraggableTabPane> tabPanes = new HashSet<>();

    private TabGroup tabGroup;

    private double prefWidth, prefHeight;
    private boolean collapsed = false;


    public DraggableTabPane(){

        super();

        this.tabGroup = TabGroup.None;
        this.setTabClosingPolicy(TabClosingPolicy.ALL_TABS);

    }

    public DraggableTabPane(TabGroup dockGroup){

        super();

        this.tabGroup = dockGroup;
        if(dockGroup == TabGroup.System) {
            this.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
        } else {
            this.setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
        }

    }

    public void addTab(DraggableTab tab){
        if(tab.getTabGroup() == tabGroup){
               this.getTabs().add(tab);
        }
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

    public void addAll(DraggableTab... tabs){
        this.getTabs().addAll(tabs);
    }

    public TabGroup getTabGroup(){
        return tabGroup;
    }

    public boolean isCollapsed(){
        return collapsed;
    }

}
