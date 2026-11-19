package factory;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
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

        double beltX = 230, beltW = 440, beltH = 70, beltY = h / 2 - beltH / 2;

        // belt body + scrolling stripes
        g.setFill(Color.web("#3a3a42"));
        g.fillRect(beltX, beltY, beltW, beltH);
        double offset = factory.isRunning() ? (nowNanos / 40_000_000.0) % 24 : 0;
        g.setStroke(Color.web("#55555f"));
        g.setLineWidth(2);
        for (double x = beltX + offset; x < beltX + beltW; x += 24) {
            g.strokeLine(x, beltY, x, beltY + beltH);
        }
        g.setStroke(Color.web("#888888"));
        g.strokeRect(beltX, beltY, beltW, beltH);

        // workers: assemblers left, packagers right
        drawWorkers(g, h, 30, true, beltX, h / 2);
        drawWorkers(g, h, 740, false, beltX + beltW, h / 2);

        // parts on the belt: oldest part sits at the packager (right) end
        int cap = factory.getBelt().capacity();
        double slotW = beltW / cap;
        List<Part> parts = factory.getBelt().snapshot();
        for (int i = 0; i < parts.size() && i < cap; i++) {
            Part p = parts.get(i);
            if (p.id() < 0) continue;
            double size = Math.min(slotW - 6, beltH - 16);
            double x = beltX + beltW - (i + 1) * slotW + (slotW - size) / 2;
            g.setFill(PALETTE[p.assembler() % PALETTE.length]);
            g.fillRoundRect(x, beltY + (beltH - size) / 2, size, size, 8, 8);
            g.setFill(Color.BLACK);
            g.fillText(String.valueOf(p.id()), x + size / 2, beltY + beltH / 2);
        }

        g.setFill(Color.WHITE);
        g.fillText("Conveyor belt (" + factory.getBelt().size() + " / " + cap + ")",
                beltX + beltW / 2, beltY - 20);

        updateLabels(cap);
    }

    private void updateLabels(int cap) {
        statsLabel.setText(String.format(
                "Built: %d / %d   Packaged: %d   On belt: %d / %d   Avg time on belt: %.2f s   Elapsed: %.1f s",
                factory.builtCount(), factory.totalParts(), factory.packagedCount(),
                factory.getBelt().size(), cap, factory.avgBeltSeconds(), factory.elapsedSeconds()));

        String status = factory.isRunning() ? "RUNNING" : (factory.isStopped() ? "STOPPED" : "FINISHED");
        if (!status.equals(lastStatus)) {           // only touch the style classes when it changes
            lastStatus = status;
            statusLabel.setText(status);
            statusLabel.getStyleClass().setAll("label", "status-label", "status-" + status.toLowerCase());
        }
    }

    private void drawWorkers(GraphicsContext g, double h, double x, boolean assemblers,
                             double beltEdgeX, double beltCenterY) {
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
            double fromX = assemblers ? x + boxW : x;
            g.strokeLine(fromX, y + boxH / 2, beltEdgeX, beltCenterY);

            g.setFill(c);
            g.fillRoundRect(x, y, boxW, boxH, 10, 10);
            g.setFill(Color.WHITE);
            g.fillText((assemblers ? "Assembler " : "Packager ") + (i + 1), x + boxW / 2, y + boxH / 2 - 9);
            g.fillText(labelFor(s, assemblers), x + boxW / 2, y + boxH / 2 + 9);
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