package com.brewstack.desktop;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class OrderItemCell extends ListCell<OrderItem> {

    private final ObservableList<OrderItem> order;
    private final Runnable onChanged;

    private final HBox container = new HBox(8);
    private final Label nameLabel = new Label();
    private final Region spacer = new Region();
    private final Button removeBtn = new Button("−");

    public OrderItemCell(ObservableList<OrderItem> order, Runnable onChanged) {
        this.order = order;
        this.onChanged = onChanged;

        HBox.setHgrow(spacer, Priority.ALWAYS);

        nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #3d2c1e;");

        removeBtn.setStyle(
            "-fx-background-color: #e8c8a8; -fx-text-fill: #5a2a0a; " +
            "-fx-background-radius: 12; -fx-cursor: hand; " +
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 1 8 1 8;"
        );

        removeBtn.setOnAction(e -> {
            OrderItem item = getItem();
            if (item == null) return;
            if (item.getQuantity() > 1) {
                item.decrement();
                // Replace at same index to fire ObservableList change event
                int idx = order.indexOf(item);
                if (idx >= 0) order.set(idx, item);
            } else {
                order.remove(item);
            }
            onChanged.run();
        });

        container.setAlignment(Pos.CENTER_LEFT);
        container.getChildren().addAll(nameLabel, spacer, removeBtn);
    }

    @Override
    protected void updateItem(OrderItem item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            nameLabel.setText(item.toString());
            setGraphic(container);
        }
    }
}
