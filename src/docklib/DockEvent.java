package docklib;

import com.sun.javafx.scene.input.InputEventUtils;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.input.PickResult;

public class DockEvent extends Event{

    public static final EventType<DockEvent> ANY = new EventType<>(Event.ANY, "DOCK");
    public static final EventType<DockEvent> DOCK_ENTER = new EventType<>(DockEvent.ANY, "DOCK_ENTER");
    public static final EventType<DockEvent> DOCK_OVER = new EventType<>(DockEvent.ANY, "DOCK_OVER");
    public static final EventType<DockEvent> DOCK_EXIT = new EventType<>(DockEvent.ANY, "DOCK_EXIT");
    public static final EventType<DockEvent> DOCK_RELEASED = new EventType<>(DockEvent.ANY, "DOCK_RELEASED");


    private double x, y, z;

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }


    private final double screenX, screenY;

    public double getScreenX() {
        return screenX;
    }

    public double getScreenY() {
        return screenY;
    }


    private final double sceneX, sceneY;

    public double getSceneX() {
        return sceneX;
    }

    public double getSceneY() {
        return sceneY;
    }


    private final PickResult pickResult;

    public PickResult getPickResult() {
        return pickResult;
    }


    private final Node contents;

    public Node getContents() {
        return contents;
    }


    public DockEvent(EventType<? extends DockEvent> eventType, double x, double y, double screenX,
                     double screenY, PickResult pickResult) {
        this(null, null, eventType, x, y, screenX, screenY, pickResult);
    }

    public DockEvent(Object source, EventTarget target, EventType<? extends DockEvent> eventType,
                     double x, double y, double screenX, double screenY, PickResult pickResult) {
        this(source, target, eventType, x, y, screenX, screenY, pickResult, null);
    }

    public DockEvent(Object source, EventTarget target, EventType<? extends DockEvent> eventType,
                     double x, double y, double screenX, double screenY, PickResult pickResult, Node contents) {
        super(source, target, eventType);
        this.x = x;
        this.y = y;
        this.screenX = screenX;
        this.screenY = screenY;
        this.sceneX = x;
        this.sceneY = y;
        this.pickResult = pickResult != null ? pickResult : new PickResult(target, x, y);
        Point3D p = InputEventUtils.recomputeCoordinates(this.pickResult, null);
        this.x = p.getX();
        this.y = p.getY();
        this.z = p.getZ();
        this.contents = contents;
    }

}
