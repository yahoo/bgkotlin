import com.yahoo.behaviorgraph.*;

import java.util.ArrayList;
import java.util.stream.Collectors;

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
            .runs(ext -> {
                if (save.justUpdated() && selected.traceValue() == null) {
                    var item = new ItemExtent(graph, save.value(), this);
                    addChildLifetime(item);
                    item.addToGraph();
                    allItems.value().add(item);
                    allItems.updateForce(allItems.value());
                    sideEffect(ext1 -> {
                        listUI.addItem(item.itemUI);
                        listUI.newItemText.setText("");
                    });
                } else if (removeItem.justUpdated()) {
                    var item = removeItem.value();
                    item.removeFromGraph();
                    allItems.value().remove(item);
                    allItems.updateForce(allItems.value());
                    sideEffect(ext1 -> {
                        listUI.removeItem(item.itemUI);
                    });
                }
            });
        
        behavior()
            .demands(allItems, getDidAdd())
            .dynamicDemands(new Demandable[]{allItems},ext -> {
                return allItems.value().stream().map(itemExtent -> itemExtent.completed).collect(Collectors.toList());
            })
            .runs(ext -> {
                sideEffect(ext1 -> {
                    long count = allItems.value().stream().filter(itemExtent -> !itemExtent.completed.value()).count();
                    listUI.setRemainingCount(count);
                });
            });

        behavior()
            .supplies(selected)
            .demands(selectRequest, save)
            .runs(ext -> {
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
                    sideEffect(ext1 -> {
                        listUI.setSelected(selected.value());
                    });
                }
            });

        behavior()
            .dynamicSupplies(new Demandable[]{allItems}, ext -> {
                return allItems.value().stream().map(item -> item.itemText).collect(Collectors.toList());
            })
            .demands(save)
            .runs(ext -> {
                if (save.justUpdated() && selected.traceValue() != null) {
                    selected.traceValue().itemText.update(save.value());
                }
            });


    }
}
