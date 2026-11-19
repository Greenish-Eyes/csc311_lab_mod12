package factory;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * Controller for factory-view.fxml. Worker threads never touch the UI:
 * an AnimationTimer redraws the canvas each frame by reading thread-safe snapshots.
 */
public class FactoryController {
    private static final Color[] PALETTE = {
            Color.web("#ffb74d"), Color.web("#4dd0e1"), Color.web("#ba68c8"),
            Color.web("#aed581"), Color.web("#f06292"), Color.web("#fff176")
    };

    @FXML private Spinner<Integer> assemblersSpinner;
    @FXML private Spinner<Integer> packagersSpinner;
    @FXML private Spinner<Integer> beltSpinner;
    @FXML private Spinner<Integer> partsSpinner;
    @FXML private Slider speedSlider;
    @FXML private Canvas canvas;
    @FXML private Label statsLabel;
    @FXML private Label statusLabel;

    private Factory factory;
    private AnimationTimer timer;
    private String lastStatus = "";

    @FXML
    private void initialize() {
        assemblersSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 6, 3));
        packagersSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 6, 2));
        beltSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 8));
        partsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 500, 40));

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) { draw(now); }
        };
        timer.start();
    }

    @FXML
    private void onStart() {
        if (factory != null) factory.stop();
        factory = new Factory(assemblersSpinner.getValue(), packagersSpinner.getValue(),
                beltSpinner.getValue(), partsSpinner.getValue(), speedSlider.getValue());
        factory.start();
    }

    @FXML
    private void onStop() {
        if (factory != null) factory.stop();
    }

    /** Called by FactoryApp when the window closes. */
    public void shutdown() {
        if (timer != null) timer.stop();
        if (factory != null) factory.stop();
    }

    // ------------------------------------------------------------------ drawing

    private void draw(long nowNanos) {
        double w = canvas.getWidth(), h = canvas.getHeight();
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.web("#1e1e24"));
        g.fillRect(0, 0, w, h);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setFont(Font.font(13));

        if (factory == null) {
            g.setFill(Color.WHITE);
            g.fillText("Press Start", w / 2, h / 2);
            return;
        }

        double beltX = 210, beltW = 480, beltH = 54;
        double inputY = h * 0.35 - beltH / 2;
        double outputY = h * 0.70 - beltH / 2;
        int cap = factory.getInputBelt().capacity();

        drawWorkers(g, h, 25, true, beltX, inputY + beltH / 2,
                outputY + beltH / 2);
        drawWorkers(g, h, 745, false, beltX + beltW, outputY + beltH / 2,
                outputY + beltH / 2);

        drawBelt(g, nowNanos, beltX, inputY, beltW, beltH,
                factory.getInputBelt().snapshot(), cap, true);
        drawBelt(g, nowNanos, beltX, outputY, beltW, beltH,
                factory.getPackagingBelt().snapshot(), cap, false);
        g.setFill(Color.WHITE);
        g.fillText("ASSEMBLERS  \u2190  RAW PARTS", beltX + beltW / 2, inputY - 18);
        g.fillText("ASSEMBLERS  \u2192  PACKAGERS", beltX + beltW / 2, outputY - 18);

        updateLabels(cap);
    }

    private void drawBelt(GraphicsContext g, long nowNanos, double x, double y,
                          double width, double height, List<Part> parts, int capacity,
                          boolean incoming) {
        g.setFill(Color.web("#3a3a42"));
        g.fillRect(x, y, width, height);
        double stripeOffset = (nowNanos / 40_000_000.0) % 24;
        double offset = factory.isRunning()
                ? (incoming ? 24 - stripeOffset : stripeOffset)
                : 0;
        g.setStroke(Color.web("#55555f"));
        g.setLineWidth(2);
        for (double stripe = x + offset; stripe < x + width; stripe += 24) {
            g.strokeLine(stripe, y, stripe, y + height);
        }
        g.setStroke(Color.web("#888888"));
        g.strokeRect(x, y, width, height);

        List<Part> visibleParts = parts;
        for (int i = 0; i < visibleParts.size() && i < capacity; i++) {
            Part part = visibleParts.get(i);
            double size = Math.min(34, height - 10);
            double progress = Math.min(1, (nowNanos - part.beltEntryNanos())
                    / (factory.beltTravelMillis() * 1_000_000.0));
            double partCenterX = incoming
                    ? x + width * (1 - progress)
                    : x + width * progress;
            double partX = partCenterX - size / 2;
            g.setFill(incoming ? Color.web("#b0bec5") : PALETTE[part.assembler() % PALETTE.length]);
            g.fillRoundRect(partX, y + (height - size) / 2, size, size, 8, 8);
            g.setFill(Color.BLACK);
            g.setFont(Font.font(Math.min(13, Math.max(9, size * 0.42))));
            g.fillText(String.valueOf(part.id()), partX + size / 2, y + height / 2);
        }
        g.setFont(Font.font(13));
    }

    private void updateLabels(int cap) {
        statsLabel.setText(String.format(
                "Built: %d / %d   Packaged: %d   Raw belt: %d / %d   Finished belt: %d / %d   Avg belt time: %.2f s   Elapsed: %.1f s",
                factory.builtCount(), factory.totalParts(), factory.packagedCount(),
                factory.getInputBelt().size(), cap, factory.getPackagingBelt().size(), cap,
                factory.avgBeltSeconds(), factory.elapsedSeconds()));

        String status = factory.isRunning() ? "RUNNING" : (factory.isStopped() ? "STOPPED" : "FINISHED");
        if (!status.equals(lastStatus)) {           // only touch the style classes when it changes
            lastStatus = status;
            statusLabel.setText(status);
            statusLabel.getStyleClass().setAll("label", "status-label", "status-" + status.toLowerCase());
        }
    }

    private void drawWorkers(GraphicsContext g, double h, double x, boolean assemblers,
                             double beltEdgeX, double firstBeltCenterY,
                             double secondBeltCenterY) {
        int n = assemblers ? factory.assemblerCount() : factory.packagerCount();
        double spacing = (h - 60) / n;
        double boxH = Math.min(50, spacing - 10);
        double boxW = 130;
        for (int i = 0; i < n; i++) {
            double y = 30 + spacing * i + (spacing - boxH) / 2;
            Factory.State s = assemblers ? factory.assemblerState(i) : factory.packagerState(i);
            Color c = colorFor(s, assemblers);

            g.setStroke(c);
            g.setLineWidth(2);
            double stationCenterY = y + boxH / 2;
            if (assemblers) {
                g.strokeLine(x + boxW, stationCenterY, beltEdgeX, firstBeltCenterY);
                g.strokeLine(x + boxW, stationCenterY, beltEdgeX, secondBeltCenterY);
            } else {
                g.strokeLine(beltEdgeX, secondBeltCenterY, x, stationCenterY);
            }

            g.setFill(c);
            g.fillRoundRect(x, y, boxW, boxH, 10, 10);
            g.setFill(Color.WHITE);
            g.fillText((assemblers ? "Assembler " : "Packager ") + (i + 1), x + boxW / 2, y + boxH / 2 - 9);
            Part part = assemblers ? null : factory.packagerPart(i);
            String activity = part == null ? labelFor(s, assemblers) : "Packing #" + part.id();
            g.fillText(activity, x + boxW / 2, y + boxH / 2 + 9);
        }
    }

    private static Color colorFor(Factory.State s, boolean assembler) {
        return switch (s) {
            case WORKING -> assembler ? Color.web("#43a047") : Color.web("#1e88e5");
            case BLOCKED -> Color.web("#e53935");
            case IDLE -> Color.web("#757575");
            case DONE -> Color.web("#424242");
        };
    }

    private static String labelFor(Factory.State s, boolean assembler) {
        return switch (s) {
            case WORKING -> assembler ? "Building" : "Packing";
            case BLOCKED -> "Belt full!";
            case IDLE -> "Waiting";
            case DONE -> "Done";
        };
    }
}