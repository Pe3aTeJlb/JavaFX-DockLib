package docklib.draggabletabpane;

import com.sun.javafx.scene.input.InputEventUtils;
import docklib.dock.DockEvent;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.input.PickResult;

public class DraggableTabEvent extends Event {

    public static final EventType<DraggableTabEvent> ANY = new EventType<>(Event.ANY, "DRAGGABLE_TAB_EVENT");
    public static final EventType<DraggableTabEvent> DETACHED = new EventType<>(DraggableTabEvent.ANY, "DETACHED");
    public static final EventType<DraggableTabEvent> ATTACHED = new EventType<>(DraggableTabEvent.ANY, "ATTACHED");
    public static final EventType<DraggableTabEvent> INTO_SEPARATED_WINDOW = new EventType<>(DraggableTabEvent.ANY, "INTO_SEPARATED_WINDOW");
    public static final EventType<DraggableTabEvent> DETACH_INTERRUPTED = new EventType<>(DraggableTabEvent.ANY, "DETACH_INTERRUPTED");


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


    public DraggableTabEvent(EventType<? extends DockEvent> eventType, double x, double y, double screenX,
                     double screenY, PickResult pickResult) {
        this(null, null, eventType, x, y, screenX, screenY, pickResult);
    }

    public DraggableTabEvent(Object source, EventTarget target, EventType<? extends DockEvent> eventType,
                     double x, double y, double screenX, double screenY, PickResult pickResult) {
        this(source, target, eventType, x, y, screenX, screenY, pickResult, null);
    }

    public DraggableTabEvent(Object source, EventTarget target, EventType<? extends DockEvent> eventType,
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
