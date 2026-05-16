package com.nhom3.client.event;

import java.util.concurrent.atomic.AtomicBoolean;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;

public final class ControllerLifecycle {
    private ControllerLifecycle() {
    }

    public static void unsubscribeOnDetach(Node anchor, Object owner) {
        if (anchor == null || owner == null) {
            return;
        }

        AtomicBoolean unsubscribed = new AtomicBoolean(false);
        Runnable unsubscribe = () -> {
            if (unsubscribed.compareAndSet(false, true)) {
                ClientEventBus.getDefault().unsubscribeOwner(owner);
            }
        };

        ChangeListener<Window> windowListener = (observable, oldWindow, newWindow) ->
                observeWindow(newWindow, unsubscribe);

        ChangeListener<Scene> sceneListener = (observable, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                unsubscribe.run();
                return;
            }

            if (newScene != null) {
                observeWindow(newScene.getWindow(), unsubscribe);
                newScene.windowProperty().addListener(windowListener);
            }
        };

        anchor.sceneProperty().addListener(sceneListener);
        Scene currentScene = anchor.getScene();
        if (currentScene != null) {
            observeWindow(currentScene.getWindow(), unsubscribe);
            currentScene.windowProperty().addListener(windowListener);
        }
    }

    private static void observeWindow(Window window, Runnable unsubscribe) {
        if (window == null) {
            return;
        }

        window.showingProperty().addListener((observable, wasShowing, isShowing) -> {
            if (wasShowing && !isShowing) {
                unsubscribe.run();
            }
        });
    }
}
