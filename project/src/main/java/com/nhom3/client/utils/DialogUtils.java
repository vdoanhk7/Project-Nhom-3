package com.nhom3.client.utils;

import java.util.ArrayDeque;
import java.util.Queue;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public final class DialogUtils {
    private static final Queue<Runnable> SUPPRESSED_ALERT_CALLBACKS = new ArrayDeque<>();
    private static boolean alertVisible;

    private DialogUtils() {
    }

    public static void initOwner(Dialog<?> dialog, Node ownerNode) {
        if (dialog == null || ownerNode == null || ownerNode.getScene() == null) {
            return;
        }
        Window ownerWindow = ownerNode.getScene().getWindow();
        if (ownerWindow != null && ownerWindow.isShowing()) {
            dialog.initOwner(ownerWindow);
        }
        dialog.initModality(Modality.NONE);
    }

    public static void showAlertAsync(Alert.AlertType type, String title, String content, Node ownerNode) {
        showAlertAsync(type, title, content, ownerNode, null);
    }

    public static void showAlertAsync(
            Alert.AlertType type, String title, String content, Node ownerNode, Runnable onHidden) {
        Runnable showTask = () -> showFirstAlertOnly(type, title, content, ownerNode, onHidden);

        if (Platform.isFxApplicationThread()) {
            showTask.run();
        } else {
            Platform.runLater(showTask);
        }
    }

    private static void showFirstAlertOnly(
            Alert.AlertType type, String title, String content, Node ownerNode, Runnable onHidden) {
        if (alertVisible) {
            if (onHidden != null) {
                SUPPRESSED_ALERT_CALLBACKS.offer(onHidden);
            }
            return;
        }

        alertVisible = true;
        try {
            Stage popup = createNonBlockingAlertStage(type, title, content);
            popup.setOnHidden(event -> {
                try {
                    runCallback(onHidden);
                    runSuppressedAlertCallbacks();
                } finally {
                    alertVisible = false;
                    focusAnyTextInput(ownerNode);
                }
            });
            popup.show();
            popup.toFront();
        } catch (RuntimeException e) {
            alertVisible = false;
            runCallback(onHidden);
            runSuppressedAlertCallbacks();
            focusAnyTextInput(ownerNode);
            throw e;
        }
    }

    private static Stage createNonBlockingAlertStage(Alert.AlertType type, String title, String content) {
        Stage stage = new Stage(StageStyle.UTILITY);
        stage.setTitle(title);
        stage.initModality(Modality.NONE);
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);

        Label iconLabel = new Label(alertIcon(type));
        iconLabel.setMinWidth(32);
        iconLabel.setAlignment(Pos.TOP_CENTER);
        iconLabel.setStyle("-fx-font-size: 24px;");

        Label titleLabel = new Label(title == null || title.isBlank() ? defaultTitle(type) : title);
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label contentLabel = new Label(content == null ? "" : content);
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(360);
        contentLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");

        VBox messageBox = new VBox(8, titleLabel, contentLabel);
        messageBox.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(messageBox, Priority.ALWAYS);

        HBox body = new HBox(12, iconLabel, messageBox);
        body.setAlignment(Pos.TOP_LEFT);

        Button okButton = new Button("OK");
        okButton.setDefaultButton(true);
        okButton.setMinWidth(84);
        okButton.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;");
        okButton.setOnAction(event -> stage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(spacer, okButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(16, body, actions);
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; "
                + "-fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6;");

        stage.setScene(new Scene(root, 460, Region.USE_COMPUTED_SIZE));
        stage.setOnShown(event -> okButton.requestFocus());
        return stage;
    }

    private static String alertIcon(Alert.AlertType type) {
        return switch (type) {
            case ERROR, WARNING -> "!";
            case CONFIRMATION -> "?";
            case INFORMATION -> "i";
            default -> "";
        };
    }

    private static String defaultTitle(Alert.AlertType type) {
        return switch (type) {
            case ERROR -> "Lỗi";
            case WARNING -> "Cảnh báo";
            case CONFIRMATION -> "Xác nhận";
            case INFORMATION -> "Thông báo";
            default -> "Thông báo";
        };
    }

    private static void runSuppressedAlertCallbacks() {
        Runnable callback;
        while ((callback = SUPPRESSED_ALERT_CALLBACKS.poll()) != null) {
            runCallback(callback);
        }
    }

    private static void runCallback(Runnable callback) {
        if (callback == null) {
            return;
        }
        try {
            callback.run();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private static void focusAnyTextInput(Node ownerNode) {
        Platform.runLater(() -> {
            TextInputControl input = findPreferredTextInput(ownerNode);
            if (input != null) {
                focusWindow(input.getScene().getWindow());
                input.requestFocus();
                input.positionCaret(input.getLength());
                return;
            }

            Window window = findFocusableAppWindow();
            if (window != null) {
                focusWindow(window);
            }
        });
    }

    private static TextInputControl findPreferredTextInput(Node ownerNode) {
        if (isUsableTextInput(ownerNode)) {
            return (TextInputControl) ownerNode;
        }
        if (ownerNode != null && ownerNode.getScene() != null) {
            TextInputControl input = findTextInput(ownerNode.getScene().getRoot());
            if (input != null) {
                return input;
            }
        }
        for (Window window : Window.getWindows()) {
            if (window == null || !window.isShowing() || window.getScene() == null) {
                continue;
            }
            TextInputControl input = findTextInput(window.getScene().getRoot());
            if (input != null) {
                return input;
            }
        }
        return null;
    }

    private static TextInputControl findTextInput(Node node) {
        if (isUsableTextInput(node)) {
            return (TextInputControl) node;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                TextInputControl input = findTextInput(child);
                if (input != null) {
                    return input;
                }
            }
        }
        return null;
    }

    private static boolean isUsableTextInput(Node node) {
        return node instanceof TextInputControl input
                && input.getScene() != null
                && input.isVisible()
                && !input.isDisabled()
                && input.isEditable();
    }

    private static Window findFocusableAppWindow() {
        for (Window window : Window.getWindows()) {
            if (window != null && window.isShowing() && window.getScene() != null) {
                return window;
            }
        }
        return null;
    }

    private static void focusWindow(Window window) {
        if (window == null || !window.isShowing()) {
            return;
        }
        if (window instanceof Stage stage) {
            stage.setAlwaysOnTop(true);
            stage.toFront();
            stage.requestFocus();
            stage.setAlwaysOnTop(false);
        } else {
            window.requestFocus();
        }
    }
}
