package sk.ukf.aisviewer.controller;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

/**
 * Vektorové ikony (Material Icons, 24×24) – ostré v každej veľkosti a farbia sa cez CSS
 * (na rozdiel od emoji, ktoré JavaFX na Windows kreslí čiernobielo a niektoré vôbec).
 */
final class Icons {

    static final String TROPHY = "M19 5h-2V3H7v2H5c-1.1 0-2 .9-2 2v1c0 2.55 1.92 4.63 4.39 4.94.63 1.5 1.98 "
            + "2.63 3.61 2.96V19H7v2h10v-2h-4v-3.1c1.63-.33 2.98-1.46 3.61-2.96C19.08 12.63 21 10.55 21 8V7c0-1.1"
            + "-.9-2-2-2zM5 8V7h2v3.82C5.84 10.4 5 9.3 5 8zm14 0c0 1.3-.84 2.4-2 2.82V7h2v1z";
    static final String STAR = "M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 "
            + "4.73L5.82 21z";
    static final String TARGET = "M12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm8.94 3c-.46-4.17"
            + "-3.77-7.48-7.94-7.94V1h-2v2.06C6.83 3.52 3.52 6.83 3.06 11H1v2h2.06c.46 4.17 3.77 7.48 7.94 7.94V23h2v"
            + "-2.06c4.17-.46 7.48-3.77 7.94-7.94H23v-2h-2.06zM12 19c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z";
    static final String SPARKLE = "M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5"
            + "L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25"
            + "-2.75L23 19l-2.75-1.25L19 15z";

    private Icons() {
    }

    /** Ikona vo farebnom krúžku; farbu určí CSS trieda (napr. "tile-blue"). */
    static Node circle(String path, double size) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.getStyleClass().add("svg-icon");
        double scale = size * 0.55 / 24.0;
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        StackPane box = new StackPane(svg);
        box.getStyleClass().add("icon-circle");
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        return box;
    }
}
