package docklib;

import docklib.trash.SplitPaneSkin;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.control.Control;
import javafx.scene.control.SkinBase;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class DoubleSidedTabPaneSkin extends SkinBase<DoubleSidedTabPane> {


    private Rectangle clipRect;
    private DraggableTabPane leftTabPane, rightTabPane;
    private DraggableTabPane collapseTarget;
    private CustomHeaderTabPaneSkin defaultLeftTabPaneSkin, defaultRightTabPaneSkin;
    private CustomHeaderTabPaneSkin rtlLeftTabPaneSkin, rtlRightTabPaneSkin;
    private boolean init = true;

    /**
     * Creates a new DoubleSidedTabPaneSkin instance, installing the necessary child
     * nodes into the Control {@link Control#//getChildren() children} list.
     *
     * @param control The control that this skin should be installed onto.
     */
    protected DoubleSidedTabPaneSkin(DoubleSidedTabPane control) {

        super(control);

        leftTabPane = control.getLeftTabPane();
        rightTabPane = control.getRightTabPane();

        //Set Skins
        defaultLeftTabPaneSkin = (CustomHeaderTabPaneSkin) leftTabPane.getSkin();
        defaultRightTabPaneSkin = (CustomHeaderTabPaneSkin) rightTabPane.getSkin();

        rtlLeftTabPaneSkin = new CustomHeaderTabPaneSkin(leftTabPane, NodeOrientation.RIGHT_TO_LEFT);
        rtlRightTabPaneSkin = new CustomHeaderTabPaneSkin(rightTabPane, NodeOrientation.RIGHT_TO_LEFT);

        setSideSkin(getSkinnable().getSide());

        //CLip rect
        clipRect = new Rectangle(control.getWidth(), control.getHeight());
        getSkinnable().setClip(clipRect);

        getChildren().addAll(leftTabPane, rightTabPane);

        //Property Listeners

        registerChangeListener(control.tabDragPolicyProperty(), e -> {
            leftTabPane.setTabDragPolicy(getSkinnable().getTabDragPolicy());
            rightTabPane.setTabDragPolicy(getSkinnable().getTabDragPolicy());
        });

        registerChangeListener(control.tabClosingPolicyProperty(), e -> {
            leftTabPane.setTabClosingPolicy(getSkinnable().getTabClosingPolicy());
            rightTabPane.setTabClosingPolicy(getSkinnable().getTabClosingPolicy());
        });

        registerChangeListener(control.selectionModelProperty(), e -> {
            leftTabPane.setSelectionModel(getSkinnable().selectionModelProperty().get());
            rightTabPane.setSelectionModel(getSkinnable().selectionModelProperty().get());
        });

        registerChangeListener(control.rotateGraphicProperty(), e -> {
            leftTabPane.setRotateGraphic(getSkinnable().rotateGraphicProperty().get());
            rightTabPane.setRotateGraphic(getSkinnable().rotateGraphicProperty().get());
        });

        registerChangeListener(control.sideProperty(), e -> getSkinnable().requestLayout());

        //Collapse property listeners

        registerChangeListener(control.getLeftTabPane().collapsedProperty(), e -> getSkinnable().requestLayout());
        registerChangeListener(control.getRightTabPane().collapsedProperty(), e -> getSkinnable().requestLayout());

        //Size property listeners

        registerChangeListener(control.widthProperty(), e -> {

            clipRect.setWidth(getSkinnable().getWidth());

            Side side = getSkinnable().getSide();

            if (side.isHorizontal()){

                leftTabPane.setPrefWidth(clipRect.getWidth() * 0.5);
                rightTabPane.setPrefWidth(clipRect.getWidth() * 0.5);

            } else {

                calculateCollapse();

            }

        });

        registerChangeListener(control.heightProperty(), e -> {

            clipRect.setHeight(getSkinnable().getHeight());

            Side side = getSkinnable().getSide();

            if(side.isHorizontal()){

                calculateCollapse();

            } else {

                leftTabPane.setPrefHeight(clipRect.getHeight() * 0.5);
                rightTabPane.setPrefHeight(clipRect.getHeight() * 0.5);

            }

        });

    }

    /*
    private void swapTabPanes(){
        Collections.swap(this.getChildren(), this.getChildren().indexOf(leftTabPane), this.getChildren().indexOf(rightTabPane));
    }


 */

    @Override protected void layoutChildren(final double x, final double y,
                                            final double w, final double h) {

        DoubleSidedTabPane tabPane = getSkinnable();
        Side side = tabPane.getSide();

        leftTabPane.setSide(side);
        rightTabPane.setSide(side);

        setSideSkin(side);

        switch (side) {

            case TOP:

                leftTabPane.setMaxWidth(TabPane.USE_PREF_SIZE);
                rightTabPane.setMaxWidth(TabPane.USE_PREF_SIZE);

                calculateCollapse();

                //layout should be in exact this order
                layoutInArea(leftTabPane, x, y, w, h, 0, HPos.LEFT, VPos.TOP);
                layoutInArea(rightTabPane, x, y, w, h, 0, HPos.RIGHT, VPos.TOP);

                break;

            case BOTTOM:

                leftTabPane.setMaxWidth(TabPane.USE_PREF_SIZE);
                rightTabPane.setMaxWidth(TabPane.USE_PREF_SIZE);

                calculateCollapse();

                layoutInArea(leftTabPane, x, y, w, h, 0, HPos.LEFT, VPos.BOTTOM);
                layoutInArea(rightTabPane, x, y, w, h, 0, HPos.RIGHT, VPos.BOTTOM);

                break;

            case LEFT:

                leftTabPane.setMaxHeight(TabPane.USE_PREF_SIZE);
                rightTabPane.setMaxHeight(TabPane.USE_PREF_SIZE);

                calculateCollapse();

                layoutInArea(rightTabPane, x, y, w, h, 0, HPos.LEFT, VPos.TOP);
                layoutInArea(leftTabPane, x, y, w, h, 0, HPos.LEFT, VPos.BOTTOM);

                break;

            case RIGHT:

                leftTabPane.setMaxHeight(TabPane.USE_PREF_SIZE);
                rightTabPane.setMaxHeight(TabPane.USE_PREF_SIZE);

                calculateCollapse();

                layoutInArea(leftTabPane, x, y, w, h, 0, HPos.RIGHT, VPos.TOP);
                layoutInArea(rightTabPane, x, y, w, h, 0, HPos.RIGHT, VPos.BOTTOM);

                break;

        }

    }


    private void calculateCollapse(){

        Side side = leftTabPane.getSide();

        if(side.isHorizontal()){

            //collapse DoubleSidedTabPane
            if(leftTabPane.isCollapsed() && rightTabPane.isCollapsed()){

                leftTabPane.setPrefWidth(clipRect.getWidth() * 0.5);
                rightTabPane.setPrefWidth(clipRect.getWidth() * 0.5);

            } else if(leftTabPane.isCollapsed() && !rightTabPane.isCollapsed()) {

                leftTabPane.setPrefWidth(clipRect.getWidth() * 0.5);
                rightTabPane.setPrefWidth(clipRect.getWidth());

                leftTabPane.setViewOrder(0);
                rightTabPane.setViewOrder(1);

            } else if (!leftTabPane.isCollapsed() && rightTabPane.isCollapsed()){

                leftTabPane.setPrefWidth(clipRect.getWidth());
                rightTabPane.setPrefWidth(clipRect.getWidth() * 0.5);

                leftTabPane.setViewOrder(1);
                rightTabPane.setViewOrder(0);

            } else {

                leftTabPane.setPrefWidth(clipRect.getWidth() * 0.5);
                rightTabPane.setPrefWidth(clipRect.getWidth() * 0.5);

            }

        } else {

            //collapse DoubleSidedTabPane
            if(leftTabPane.isCollapsed() && rightTabPane.isCollapsed()){

                leftTabPane.setPrefHeight(clipRect.getHeight() * 0.5);
                rightTabPane.setPrefHeight(clipRect.getHeight() * 0.5);

            } else if(leftTabPane.isCollapsed() && !rightTabPane.isCollapsed()) {

                leftTabPane.setPrefHeight(clipRect.getHeight() * 0.5);
                rightTabPane.setPrefHeight(clipRect.getHeight());

                leftTabPane.setViewOrder(0);
                rightTabPane.setViewOrder(1);

            } else if(!leftTabPane.isCollapsed() && rightTabPane.isCollapsed()){

                leftTabPane.setPrefHeight(clipRect.getHeight());
                rightTabPane.setPrefHeight(clipRect.getHeight() * 0.5);

                leftTabPane.setViewOrder(1);
                rightTabPane.setViewOrder(0);

            } else {

                leftTabPane.setPrefHeight(clipRect.getHeight() * 0.5);
                rightTabPane.setPrefHeight(clipRect.getHeight() * 0.5);

            }

        }

    }

    private void setSideSkin(Side side){

        boolean sameSide = side == leftTabPane.getSide();

        if ((side.isHorizontal() || side == Side.RIGHT) && (!sameSide || init)){
            leftTabPane.setSkin(defaultLeftTabPaneSkin);
            rightTabPane.setSkin(rtlRightTabPaneSkin);
        } else if (side == Side.LEFT && !sameSide){
            leftTabPane.setSkin(rtlLeftTabPaneSkin);
            rightTabPane.setSkin(defaultRightTabPaneSkin);
        }

        init = false;

    }

}
