import behaviorgraph.*;

import java.util.ArrayList;

public class ListExtent extends Extent<ListExtent> {
    TypedMoment<String> save = typedMoment();
    State<ArrayList<ItemExtent>> allItems = state(new ArrayList<>());
    TypedMoment<ItemExtent> removeItem = typedMoment();
    TypedMoment<ItemExtent> selectRequest = typedMoment();
    State<ItemExtent> selected = state(null);

    public ListExtent(Graph graph, ListUI listUI) {
        super(graph);

        listUI.save.addActionListener(actionEvent -> {
            save.updateWithAction(listUI.newItemText.getText());
        });

        behavior()
            .supplies(allItems)
            .demands(save, removeItem)
            .runs(ctx -> {
                if (save.justUpdated() && selected.traceValue() == null) {
                    var item = new ItemExtent(graph, save.value(), this);
                    addChildLifetime(item);
                    item.addToGraph();
                    allItems.value().add(item);
                    allItems.updateForce(allItems.value());
                    sideEffect(ctx1 -> {
                        listUI.addItem(item.itemUI);
                        listUI.newItemText.setText("");
                    });
                } else if (removeItem.justUpdated()) {
                    var item = removeItem.value();
                    item.removeFromGraph();
                    allItems.value().remove(item);
                    allItems.updateForce(allItems.value());
                    sideEffect(ctx1 -> {
                        listUI.removeItem(item.itemUI);
                    });
                }
            });
        
        behavior()
            .demands(allItems, getDidAdd())
            .dynamicDemands(new Linkable[]{allItems}, (ctx, demands) -> {
                for (ItemExtent item: allItems.value()) {
                    demands.add(item.completed);
                }
            })
            .runs(ctx -> {
                sideEffect(ctx1 -> {
                    long count = allItems.value().stream().filter(itemExtent -> !itemExtent.completed.value()).count();
                    listUI.setRemainingCount(count);
                });
            });

        behavior()
            .supplies(selected)
            .demands(selectRequest, save)
            .runs(ctx -> {
                if (selectRequest.justUpdated()) {
                    if (selected.value() == selectRequest.value()) {
                        selected.update(null);
                    } else {
                        selected.update(selectRequest.value());
                    }
                } else if (save.justUpdated()) {
                    selected.update(null);
                }

                if (selected.justUpdated()) {
                    sideEffect(ctx1 -> {
                        listUI.setSelected(selected.value());
                    });
                }
            });

        behavior()
            .dynamicSupplies(new Linkable[]{allItems}, (ctx, supplies) -> {
                for (ItemExtent item: allItems.value()) {
                    supplies.add(item.itemText);
                }
            })
            .demands(save)
            .runs(ctx -> {
                if (save.justUpdated() && selected.traceValue() != null) {
                    selected.traceValue().itemText.update(save.value());
                }
            });


    }
}
