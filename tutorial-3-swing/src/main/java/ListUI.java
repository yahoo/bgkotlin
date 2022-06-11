import javax.swing.*;
import java.awt.*;

public class ListUI {
    JTextField newItemText;
    JButton save;
    JPanel itemsPanel;
    JLabel remainingItems;
    JLabel actionLabel;
    JFrame frame;
    public ListUI() {
        newItemText = new JTextField("", 20);
        save = new JButton("Save");
        itemsPanel = new JPanel();
        remainingItems = new JLabel();
        actionLabel = new JLabel("Add Item:");
    }

    public void createAndShowGUI() {
        //Create and set up the window.
        frame = new JFrame("Behavior Graph Tutorial 3");
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        GridBagConstraints c;
        BoxLayout layout;

        Container mainPane = frame.getContentPane();
        layout = new BoxLayout(mainPane, BoxLayout.PAGE_AXIS);

        JLabel title = new JLabel("Todo List");
        title.setFont(new Font("Helvetica", Font.BOLD, 16));
        mainPane.add(title);
        mainPane.setLayout(layout);

        JPanel headerPane = new JPanel();
        headerPane.setBackground(Color.LIGHT_GRAY);
        headerPane.add(actionLabel);
        headerPane.add(newItemText);
        headerPane.add(save);
        mainPane.add(headerPane);

        BoxLayout itemsLayout = new BoxLayout(itemsPanel, BoxLayout.PAGE_AXIS);
        itemsPanel.setLayout(itemsLayout);
        mainPane.add(itemsPanel);

        JPanel footerPane = new JPanel();
        footerPane.setBackground(Color.LIGHT_GRAY);
        footerPane.add(remainingItems);
        mainPane.add(footerPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public void addItem(ItemUI inItem) {
        itemsPanel.add(inItem);
        frame.pack();
    }

    public void removeItem(ItemUI inItem) {
        itemsPanel.remove(inItem);
        frame.pack();
    }

    public void setSelected(ItemExtent itemExtent) {
        newItemText.setText(itemExtent == null ? "" : itemExtent.itemText.value());
        actionLabel.setText(itemExtent == null ? "Add Item:" : "Edit Item:");
        newItemText.requestFocus();
    }

    public void setRemainingCount(long count) {
        remainingItems.setText("Remaining items: " + count);
    }

}
