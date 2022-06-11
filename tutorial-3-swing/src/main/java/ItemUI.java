import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.Map;

public class ItemUI extends JPanel {
    JCheckBox completedCheckbox;
    JLabel itemText;
    JButton itemDelete;

    public ItemUI() {
        super(new FlowLayout(FlowLayout.LEFT));
        completedCheckbox = new JCheckBox();
        add(completedCheckbox);

        itemText = new JLabel();
        add(itemText);

        itemDelete = new JButton("Delete");
        add(itemDelete);
    }

    public void setCompleted(boolean completed) {
        Map textAttributes = itemText.getFont().getAttributes();
        textAttributes.put(TextAttribute.STRIKETHROUGH, completed ? Boolean.TRUE : Boolean.FALSE);
        itemText.setFont(itemText.getFont().deriveFont(textAttributes));
    }

    public void setSelected(boolean selected) {
        this.setBackground(selected ? Color.GREEN : null);
    }
}
