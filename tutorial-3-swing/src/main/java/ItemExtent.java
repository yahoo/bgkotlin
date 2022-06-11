import com.yahoo.behaviorgraph.*;

import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ItemExtent extends Extent<ItemExtent> {
    ListExtent list;
    State<String> itemText;
    ItemUI itemUI;
    State<Boolean> completed;

    public ItemExtent(Graph g, String inText, ListExtent inList) {
        super(g);

        list = inList;
        itemText = state(inText);
        itemUI = new ItemUI();
        completed = state(false);
        itemUI.completedCheckbox.addItemListener(itemEvent -> {
            completed.updateWithAction(itemEvent.getStateChange() == ItemEvent.SELECTED);
        });
        itemUI.itemDelete.addActionListener(actionEvent -> {
            list.removeItem.updateWithAction(this);
        });
        itemUI.addMouseListener(new MouseAdapter() {
                                    @Override
                                    public void mouseClicked(MouseEvent e) {
                                        list.selectRequest.updateWithAction(ItemExtent.this);
                                    }
                                });
        behavior()
            .demands(itemText, getDidAdd())
            .runs(ext -> {
                sideEffect(ext1 -> {
                    itemUI.itemText.setText(itemText.value());
                });
            });

        behavior()
            .demands(completed, getDidAdd())
            .runs(ext -> {
                sideEffect(ext1 -> {
                    itemUI.setCompleted(completed.value());
                });
            });

        behavior()
            .demands(list.selected, getDidAdd())
            .runs(ext -> {
                var selected = list.selected.value() == this;
                sideEffect(ext1 -> {
                    itemUI.setSelected(selected);
                });
            });
    }
}
