package com.nhom3.client.utils;

import javafx.scene.Node;
import javafx.scene.control.Dialog;
import javafx.stage.Modality;

public final class DialogUtils {
    private DialogUtils() {
    }

    public static void initOwner(Dialog<?> dialog, Node ownerNode) {
        if (dialog == null || ownerNode == null || ownerNode.getScene() == null) {
            return;
        }
        dialog.initOwner(ownerNode.getScene().getWindow());
        dialog.initModality(Modality.WINDOW_MODAL);
    }
}
