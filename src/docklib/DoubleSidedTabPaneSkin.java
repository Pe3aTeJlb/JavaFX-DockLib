package docklib;

import docklib.draggabletabpane.DraggableTabPane;
import docklib.draggabletabpane.DraggableTabPaneSkin;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.*;
import javafx.scene.Cursor;
import javafx.scene.control.Control;
import javafx.scene.control.SkinBase;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class DoubleSidedTabPaneSkin extends SkinBase<DoubleSidedTabPane> {

    private Rectangle clipRect;
    private DraggableTabPane leftTabPane, rightTabPane;
    private DraggableTabPaneSkin leftSkin, rightSkin;
    private ContentDivider contentDivider;
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

        leftTabPane.setPickOnBounds(false);
        rightTabPane.setPickOnBounds(false);

        //Set Skins

        contentDivider = new ContentDivider();
        contentDivider.setViewOrder(0);

        //CLip rect
        clipRect = new Rectangle(control.getWidth(), control.getHeight());
        getSkinnable().setClip(clipRect);

        getChildren().addAll(leftTabPane, rightTabPane, contentDivider);

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
            contentDivider.recalculatePos();
        });

        registerChangeListener(control.heightProperty(), e -> {
            clipRect.setHeight(getSkinnable().getHeight());
            contentDivider.recalculatePos();
        });

    }

    @Override protected void layoutChildren(final double x, final double y,
                                            final double w, final double h) {

        Side side = getSkinnable().getSide();

        leftTabPane.setSide(side);
        rightTabPane.setSide(side);

        if (leftSkin == null){
            leftSkin = ((DraggableTabPaneSkin)leftTabPane.getSkin());
            leftSkin.setLeftPart(true);
        }

        if(rightSkin == null) {
            rightSkin = ((DraggableTabPaneSkin) rightTabPane.getSkin());
            rightSkin.setHeaderOrientation(NodeOrientation.RIGHT_TO_LEFT);
            rightSkin.setLeftPart(false);
        }

        double hh = leftSkin.getTabHeaderAreaHeight();

        switch (side) {

            case TOP:

                leftTabPane.setMaxWidth(TabPane.USE_PREF_SIZE);
                rightTabPane.setMaxWidth(TabPane.USE_PREF_SIZE);

                calculateCollapse(leftSkin, rightSkin);

                contentDivider.resize(5, h);

                //layout should be in exact this order
                layoutInArea(leftTabPane, x, y, w, h, 0, HPos.LEFT, VPos.TOP);
                layoutInArea(rightTabPane, x, y, w, h, 0, HPos.RIGHT, VPos.TOP);
                positionInArea(contentDivider, contentDivider.getX(), contentDivider.getY()+hh, w, h, 0, HPos.CENTER, VPos.CENTER);
                break;

            case BOTTOM:

                leftTabPane.setMaxWidth(TabPane.USE_PREF_SIZE);
                rightTabPane.setMaxWidth(TabPane.USE_PREF_SIZE);
                contentDivider.setMaxHeight(TabPane.USE_PREF_SIZE);

                calculateCollapse(leftSkin, rightSkin);

                contentDivider.resize(5, h);

                layoutInArea(leftTabPane, x, y, w, h, 0, HPos.LEFT, VPos.BOTTOM);
                layoutInArea(rightTabPane, x, y, w, h, 0, HPos.RIGHT, VPos.BOTTOM);
                positionInArea(contentDivider, contentDivider.getX(), contentDivider.getY()-hh, w, h, 0, HPos.CENTER, VPos.CENTER);
                break;

            case LEFT:

                leftTabPane.setMaxHeight(TabPane.USE_PREF_SIZE);
                rightTabPane.setMaxHeight(TabPane.USE_PREF_SIZE);

                calculateCollapse(leftSkin, rightSkin);

                contentDivider.resize(w, 5);

                layoutInArea(leftTabPane, x, y, w, h, 0, HPos.LEFT, VPos.TOP);
                layoutInArea(rightTabPane, x, y, w, h, 0, HPos.LEFT, VPos.BOTTOM);
                positionInArea(contentDivider, contentDivider.getX()+hh, contentDivider.getY(), w, h, 0, HPos.CENTER, VPos.CENTER);
                break;

            case RIGHT:

                leftTabPane.setMaxHeight(TabPane.USE_PREF_SIZE);
                rightTabPane.setMaxHeight(TabPane.USE_PREF_SIZE);

                calculateCollapse(leftSkin, rightSkin);

                contentDivider.resize(w, 5);

                layoutInArea(leftTabPane, x, y, w, h, 0, HPos.RIGHT, VPos.TOP);
                layoutInArea(rightTabPane, x, y, w, h, 0, HPos.RIGHT, VPos.BOTTOM);
                positionInArea(contentDivider, contentDivider.getX()-hh, contentDivider.getY(), w, h, 0, HPos.CENTER, VPos.CENTER);
                break;

        }

    }


    private void calculateCollapse(DraggableTabPaneSkin leftSkin, DraggableTabPaneSkin rightSkin){

        Side side = leftTabPane.getSide();


        contentDivider.setVisible(false);

        leftSkin.setAlternativeContentLayout(false);
        rightSkin.setAlternativeContentLayout(false);

        if (side.isHorizontal()) {
            leftSkin.setAlternativeHeaderSize(clipRect.getWidth() * 0.5);
            rightSkin.setAlternativeHeaderSize(clipRect.getWidth() * 0.5);
        } else {
            leftSkin.setAlternativeHeaderSize(clipRect.getHeight() * 0.5);
            rightSkin.setAlternativeHeaderSize(clipRect.getHeight() * 0.5);
        }

        //collapse DoubleSidedTabPane
        if(leftTabPane.isCollapsed() && rightTabPane.isCollapsed()){

            if(side.isHorizontal()) {
                leftTabPane.setPrefWidth(clipRect.getWidth() * 0.5);
                rightTabPane.setPrefWidth(clipRect.getWidth() * 0.5);
            } else {
                leftTabPane.setPrefHeight(clipRect.getHeight() * 0.5);
                rightTabPane.setPrefHeight(clipRect.getHeight() * 0.5);
            }

        } else if(leftTabPane.isCollapsed() && !rightTabPane.isCollapsed()) {

            leftTabPane.setViewOrder(1);
            rightTabPane.setViewOrder(2);

            if(side.isHorizontal()) {
                leftTabPane.setPrefWidth(clipRect.getWidth() * 0.5);
                rightTabPane.setPrefWidth(clipRect.getWidth());
            } else {
                leftTabPane.setPrefHeight(clipRect.getHeight() * 0.5);
                rightTabPane.setPrefHeight(clipRect.getHeight());
            }

            leftSkin.toDefault();
            rightSkin.toDefault();

        } else if (!leftTabPane.isCollapsed() && rightTabPane.isCollapsed()){

            leftTabPane.setViewOrder(2);
            rightTabPane.setViewOrder(1);

            if(side.isHorizontal()) {
                leftTabPane.setPrefWidth(clipRect.getWidth());
                rightTabPane.setPrefWidth(clipRect.getWidth() * 0.5);
            } else {
                leftTabPane.setPrefHeight(clipRect.getHeight());
                rightTabPane.setPrefHeight(clipRect.getHeight() * 0.5);
            }

        } else {

            contentDivider.setVisible(true);

            leftSkin.setAlternativeContentLayout(true);
            rightSkin.setAlternativeContentLayout(true);

            if(side.isHorizontal()) {

                leftTabPane.setPrefWidth(clipRect.getWidth() * 0.5);
                rightTabPane.setPrefWidth(clipRect.getWidth() * 0.5);

                leftSkin.setAlternativeSize(clipRect.getWidth() * contentDivider.getRelativePos() - contentDivider.getWidth() / 2);
                rightSkin.setAlternativeSize(clipRect.getWidth() * (1 - contentDivider.getRelativePos()) - contentDivider.getWidth() / 2);

            } else {

                leftTabPane.setPrefHeight(clipRect.getHeight() * 0.5);
                rightTabPane.setPrefHeight(clipRect.getHeight() * 0.5);

                leftSkin.setAlternativeSize(clipRect.getHeight() * contentDivider.getRelativePos() - contentDivider.getHeight()/2);
                rightSkin.setAlternativeSize(clipRect.getHeight() * (1 - contentDivider.getRelativePos()) - contentDivider.getHeight()/2);

            }

            if(contentDivider.getRelativePos() <= 0.5){
                leftTabPane.setViewOrder(2);
                rightTabPane.setViewOrder(1);

            } else {
                leftTabPane.setViewOrder(1);
                rightTabPane.setViewOrder(2);
            }

        }

    }

    private double getSize(){

        if(getSkinnable().getSide().isHorizontal()){
            return getSkinnable().getWidth();
        } else {
            return getSkinnable().getHeight();
        }

    }



    private class ContentDivider extends StackPane {
        
        private double initialPos;
        private double pressPos;
        private StackPane grabber;
        private DoubleProperty relativePos;
        private double absolutePos;
        private boolean horizontal;

        public ContentDivider() {

            getStyleClass().setAll(new String[]{"split-pane"});
            getStyleClass().add("split-pane-divider");

            this.initialPos = 0;
            this.pressPos = 0;
            this.absolutePos = 0;

            grabber = new StackPane() {

                @Override protected double computeMinWidth(double height) {
                    return 0;
                }

                @Override protected double computeMinHeight(double width) {
                    return 0;
                }

                @Override protected double computePrefWidth(double height) {
                    return snappedLeftInset() + snappedRightInset();
                }

                @Override protected double computePrefHeight(double width) {
                    return snappedTopInset() + snappedBottomInset();
                }

                @Override protected double computeMaxWidth(double height) {
                    return computePrefWidth(-1);
                }

                @Override protected double computeMaxHeight(double width) {
                    return computePrefHeight(-1);
                }

            };

            setGrabberStyle(getSkinnable().getSide().isHorizontal());
            getChildren().add(grabber);

            initializeDividerEventHandlers(this);

            // TODO register a listener for Divider position

        }

        public final void setGrabberStyle(boolean horizontal) {
            this.horizontal = horizontal;
            grabber.getStyleClass().clear();
            grabber.getStyleClass().setAll("vertical-grabber");
            setCursor(Cursor.V_RESIZE);
            if (horizontal) {
                grabber.getStyleClass().setAll("horizontal-grabber");
                setCursor(Cursor.H_RESIZE);
            }
            grabber.setStyle("-fx-background-color:#AA0000;");
        }


        public final void setRelativePos(double var1) {
            if(var1 >= 0.1 & var1 <= 0.9)
            this.relativePosProperty().set(var1);
        }

        public final double getRelativePos() {
            return this.relativePos == null ? 0.5D : this.relativePos.get();
        }

        public final DoubleProperty relativePosProperty() {

            if (this.relativePos == null) {
                this.relativePos = new SimpleDoubleProperty(this, "position", 0.5D);
            }

            return this.relativePos;

        }


        public final void setAbsolutePos(double pos){
            if(pos >= 0.1 * getSize() & pos <= 0.9 * getSize())
            this.absolutePos = pos;
        }

        public final double getAbsolutePos(){
            if (absolutePos == 0) {
                return getRelativePos() * getSize();
            } else {
                return absolutePos;
            }
        }

        public double getInitialPos() {
            return initialPos;
        }

        public void setInitialPos(double initialPos) {
            this.initialPos = initialPos;
        }

        public double getPressPos() {
            return pressPos;
        }

        public void setPressPos(double pressPos) {
            this.pressPos = pressPos;
        }

        public double getX() {

            if(horizontal){
                return getAbsolutePos() - getSize()/2;
            } else {
                return 0;
            }

        }

        public double getY() {

            if(horizontal){
                return 0;
            } else {
                return getAbsolutePos() - getSize()/2;
            }

        }

        public void recalculatePos(){
            absolutePos = getRelativePos() * getSize();
        }


        @Override protected double computeMinWidth(double height) {
            return computePrefWidth(height);
        }

        @Override protected double computeMinHeight(double width) {
            return computePrefHeight(width);
        }

        @Override protected double computePrefWidth(double height) {
            return snappedLeftInset() + snappedRightInset();
        }

        @Override protected double computePrefHeight(double width) {
            return snappedTopInset() + snappedBottomInset();
        }

        @Override protected double computeMaxWidth(double height) {
            return computePrefWidth(height);
        }

        @Override protected double computeMaxHeight(double width) {
            return computePrefHeight(width);
        }

        @Override protected void layoutChildren() {
            double grabberWidth = grabber.prefWidth(-1);
            double grabberHeight = grabber.prefHeight(-1);
            double grabberX = (getWidth() - grabberWidth)/2;
            double grabberY = (getHeight() - grabberHeight)/2;
            grabber.resize(grabberWidth, grabberHeight);
            positionInArea(grabber, grabberX, grabberY, grabberWidth, grabberHeight, 0, HPos.CENTER, VPos.CENTER);
        }
        
    }

    private void initializeDividerEventHandlers(final ContentDivider divider) {

        divider.addEventHandler(MouseEvent.ANY, event -> {
            event.consume();
        });

        divider.setOnMousePressed(e -> {
            if (getSkinnable().getSide().isHorizontal()) {
                divider.setInitialPos(divider.getAbsolutePos());
                divider.setPressPos(e.getSceneX());
                divider.setPressPos(getSkinnable().getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT
                        ? getSkinnable().getWidth() - e.getSceneX() : e.getSceneX());
            } else {
                divider.setInitialPos(divider.getAbsolutePos());
                divider.setPressPos(e.getSceneY());
            }
            e.consume();
        });

        divider.setOnMouseDragged(e -> {

            double delta;
            if (getSkinnable().getSide().isHorizontal()) {
                delta = getSkinnable().getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT
                        ? getSkinnable().getWidth() - e.getSceneX() : e.getSceneX();
            } else {
                delta = e.getSceneY();
            }
            delta -= divider.getPressPos();
            double value = Math.ceil(divider.getInitialPos() + delta);

            if (getSkinnable().getWidth() > 0 && getSkinnable().getHeight() > 0) {
                double size = getSize();
                if (size != 0) {
                    double pos = value + divider.prefWidth(-1)/2;
                    divider.setAbsolutePos(pos);
                    divider.setRelativePos(pos / size);
                } else {
                    divider.setRelativePos(0);
                }

            }

            getSkinnable().requestLayout();
            e.consume();

        });

    }

}
