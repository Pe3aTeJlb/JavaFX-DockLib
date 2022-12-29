package docklib;

import docklib.dock.DockAnchor;
import docklib.dock.DockPane;
import docklib.dock.Dockable;
import docklib.draggabletabpane.DraggableTab;
import docklib.draggabletabpane.DraggableTabPane;
import docklib.draggabletabpane.DraggableTabPaneSkin;
import docklib.draggabletabpane.TabGroup;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;

public class DoubleSidedTabPane extends Control implements Dockable {

    private DraggableTabPane leftTabPane;
    private DraggableTabPane rightTabPane;
    private double prefExpandedSize = 0;

    public DoubleSidedTabPane(){

        super();

        leftTabPane = new DraggableTabPane(TabGroup.System);
        leftTabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        leftTabPane.setRotateGraphic(true);

        rightTabPane = new DraggableTabPane(TabGroup.System);
        rightTabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        rightTabPane.setRotateGraphic(true);

        setSelectionModel(leftTabPane.getSelectionModel());

        leftTabPane.collapsedProperty().addListener(change -> {
            if(leftTabPane.isCollapsed()) {
                collapse();
            } else {
                expand();
            }
        });

        rightTabPane.collapsedProperty().addListener(change -> {
            if(rightTabPane.isCollapsed()) {
                collapse();
            } else {
                expand();
            }
        });

    }

    public void addRight(DraggableTab... tabs){
        rightTabPane.addAll(tabs);
    }

    public void addLeft(DraggableTab... tabs){
        leftTabPane.addAll(tabs);
    }

    @Override protected Skin<?> createDefaultSkin() {
        return new DoubleSidedTabPaneSkin(this);
    }

    private void collapse(){

        if(leftTabPane.isCollapsed() && rightTabPane.isCollapsed()) {

            if(isWrappedInDockPane()){

                double[] dividers = split.getDividerPositions();
                int relativeIndex = split.getItems().indexOf(this);
                if(relativeIndex == split.getItems().size() - 1){
                    relativeIndex -= 1;
                }

                int finalRelativeIndex = relativeIndex;
                Platform.runLater(() ->{
                    for(int i = 0; i < split.getDividerPositions().length; i++) {
                        if(i != finalRelativeIndex) {
                            split.setDividerPositions(i, dividers[i]);
                        }
                    }
                });

            }

            if (getSide().isHorizontal()) {
                prefExpandedSize = this.getHeight();
                this.setMinHeight(((DraggableTabPaneSkin)leftTabPane.getSkin()).getTabHeaderAreaHeight());
                this.setMaxHeight(((DraggableTabPaneSkin)leftTabPane.getSkin()).getTabHeaderAreaHeight());
            } else {
                prefExpandedSize = this.getWidth();
                this.setMinWidth(((DraggableTabPaneSkin)leftTabPane.getSkin()).getTabHeaderAreaHeight());
                this.setMaxWidth(((DraggableTabPaneSkin)leftTabPane.getSkin()).getTabHeaderAreaHeight());
            }

        }

    }

    private void expand(){

        if(getSide().isHorizontal()){
            this.setMinHeight(((DraggableTabPaneSkin)leftTabPane.getSkin()).getTabHeaderAreaHeight() + 10);
            this.setPrefHeight(prefExpandedSize);
            this.setMaxHeight(TabPane.USE_COMPUTED_SIZE);
        } else {
            this.setMinWidth(((DraggableTabPaneSkin)leftTabPane.getSkin()).getTabHeaderAreaHeight() + 10);
            this.setPrefWidth(prefExpandedSize);
            this.setMaxWidth(TabPane.USE_COMPUTED_SIZE);
        }

        if(isWrappedInDockPane()) {

            double magnitude = 0;
            int relativeIndex;

            if (split.getItems().size() > 0) {

                relativeIndex = split.getItems().indexOf(this);
                boolean otherSide = false;
                if (relativeIndex == split.getItems().size() - 1) otherSide = true;

                if (split.getOrientation() == Orientation.HORIZONTAL) {

                    for (Node splitItem : split.getItems()) {
                        magnitude += splitItem.getLayoutBounds().getWidth();
                    }

                    if (otherSide) {
                        split.setDividerPosition(relativeIndex - 1, 1 - this.prefWidth(0) / magnitude);
                    } else {
                        split.setDividerPosition(relativeIndex, this.prefWidth(0) / magnitude);
                    }

                } else {

                    for (Node splitItem : split.getItems()) {
                        magnitude += splitItem.getLayoutBounds().getHeight();
                    }

                    if (otherSide) {
                        split.setDividerPosition(relativeIndex - 1, 1 - this.prefHeight(0) / magnitude);
                    } else {
                        split.setDividerPosition(relativeIndex, this.prefHeight(0) / magnitude);
                    }

                }

            }

        }

    }

    public DraggableTabPane getLeftTabPane(){
        return leftTabPane;
    }

    public DraggableTabPane getRightTabPane(){
        return rightTabPane;
    }


    /*Side Property*/

    private ObjectProperty<Side> side;

    /**
     * <p>The position to place the tabs in this TabPane. Whenever this changes
     * the TabPane will immediately update the location of the tabs to reflect
     * this.</p>
     *
     * @param value the side
     */
    public final void setSide(Side value) {
        sideProperty().set(value);
    }

    /**
     * The current position of the tabs in the TabPane.  The default position
     * for the tabs is Side.Top.
     *
     * @return The current position of the tabs in the TabPane.
     */
    public final Side getSide() {
        return side == null ? Side.TOP : side.get();
    }

    /**
     * The position of the tabs in the TabPane.
     * @return the side property
     */
    public final ObjectProperty<Side> sideProperty() {
        if (side == null) {
            side = new ObjectPropertyBase<>(Side.TOP) {
                @Override protected void invalidated() {
                }

                @Override
                public Object getBean() {
                    return DoubleSidedTabPane.this;
                }

                @Override
                public String getName() {
                    return "side";
                }
            };
        }
        return side;
    }



    /*TabDragPolicy Property*/

    private ObjectProperty<TabPane.TabDragPolicy> tabDragPolicy;

    public final ObjectProperty<TabPane.TabDragPolicy> tabDragPolicyProperty() {
        if (this.tabDragPolicy == null) {
            this.tabDragPolicy = new SimpleObjectProperty(this, "tabDragPolicy", TabPane.TabDragPolicy.FIXED);
        }

        return this.tabDragPolicy;
    }

    public final void setTabDragPolicy(TabPane.TabDragPolicy var1) {
        this.tabDragPolicyProperty().set(var1);
    }

    public final TabPane.TabDragPolicy getTabDragPolicy() {
        return (TabPane.TabDragPolicy)this.tabDragPolicyProperty().get();
    }


    /*TabClosingPolicy Property*/

    private ObjectProperty<TabPane.TabClosingPolicy> tabClosingPolicy;

    public final void setTabClosingPolicy(TabPane.TabClosingPolicy var1) {
        this.tabClosingPolicyProperty().set(var1);
    }

    public final TabPane.TabClosingPolicy getTabClosingPolicy() {
        return this.tabClosingPolicy == null ? TabPane.TabClosingPolicy.SELECTED_TAB : (TabPane.TabClosingPolicy)this.tabClosingPolicy.get();
    }

    public final ObjectProperty<TabPane.TabClosingPolicy> tabClosingPolicyProperty() {
        if (this.tabClosingPolicy == null) {
            this.tabClosingPolicy = new SimpleObjectProperty(this, "tabClosingPolicy", TabPane.TabClosingPolicy.SELECTED_TAB);
        }

        return this.tabClosingPolicy;
    }

    /*Selection Model Property*/

    private ObjectProperty<SingleSelectionModel<Tab>> selectionModel = new SimpleObjectProperty<SingleSelectionModel<Tab>>(this, "selectionModel");

    public final void setSelectionModel(SingleSelectionModel<Tab> value) { selectionModel.set(value); }

    public final SingleSelectionModel<Tab> getSelectionModel() { return selectionModel.get(); }

    public final ObjectProperty<SingleSelectionModel<Tab>> selectionModelProperty() { return selectionModel; }

    /*Rotate graphics property*/

    private BooleanProperty rotateGraphic;

    public final void setRotateGraphic(boolean var1) {
        this.rotateGraphicProperty().set(var1);
    }

    public final boolean isRotateGraphic() {
        return this.rotateGraphic == null ? false : this.rotateGraphic.get();
    }

    public final BooleanProperty rotateGraphicProperty() {
        if (this.rotateGraphic == null) {
            this.rotateGraphic = new SimpleBooleanProperty(this, "rotateGraphic", false);
        }

        return this.rotateGraphic;
    }

    //Dockable

    private DockPane dockPane;
    private SplitPane split;

    @Override
    public boolean isWrappedInDockPane() {
        return this.dockPane != null;
    }

    @Override
    public void setDockPane(DockPane dockPane, SplitPane splitPane) {
        this.dockPane = dockPane;
        this.split = splitPane;
    }

    @Override
    public DockPane getDockPane() {
        return dockPane;
    }

    @Override
    public void dock(Node node, DockAnchor dockAnchor) { }

    @Override
    public void undock() { }

}
