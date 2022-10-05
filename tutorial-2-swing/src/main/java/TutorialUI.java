import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class TutorialUI {
    JButton upButton;
    JButton downButton;
    JLabel heatStatus;
    JLabel currentTemp;
    JLabel desiredTemp;
    Timer heatTimer;

    public TutorialUI() {
        upButton = new JButton("Up");
        downButton = new JButton("Down");
        heatStatus = new JLabel();
        currentTemp = new JLabel();
        desiredTemp = new JLabel();
    }

    public void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Behavior Graph Tutorial 2");
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        GridBagConstraints c;
        GridBagLayout layout;

        Container mainPane = frame.getContentPane();
        layout = new GridBagLayout();
        mainPane.setLayout(layout);
        JPanel innerPane = new JPanel();
        c = new GridBagConstraints();
        c.insets = new Insets(8,8,8,8);
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = 1;
        c.gridheight = 1;
        innerPane.setSize(300,400);
        mainPane.add(innerPane, c);

        layout = new GridBagLayout();
        innerPane.setLayout(layout);
        layout.rowHeights = new int[]{30,30,30,30};
        c = new GridBagConstraints();
        JLabel label = new JLabel("Tempwell");
        label.setFont(new Font("Helvetica", Font.BOLD, 30));
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = 3;
        c.gridheight = 2;
        c.gridx = 0;
        c.gridy = 0;
        innerPane.add(label, c);

        c = new GridBagConstraints();
        JPanel display = new JPanel();
        display.setBackground(Color.LIGHT_GRAY);
        c.insets = new Insets(4,4,4,4);
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = 2;
        c.gridheight = 2;
        c.gridx = 0;
        c.gridy = 2;
        innerPane.add(display, c);

        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 2;
        c.gridy = 2;
        innerPane.add(upButton, c);

        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 2;
        c.gridy = 3;
        innerPane.add(downButton, c);


        layout = new GridBagLayout();
        layout.columnWidths = new int[]{100, 100};
        layout.rowHeights = new int[]{25,25,25};
        display.setLayout(layout);
        c = new GridBagConstraints();
        c.insets = new Insets(2,4,2,4);
        heatStatus.setFont(new Font("Helvetica", Font.BOLD, 16));
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;
        display.add(heatStatus, c);

        c = new GridBagConstraints();
        JLabel currentLabel = new JLabel("Current");
        currentLabel.setFont(new Font("Helvetica", Font.PLAIN, 14));
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 1;
        display.add(currentLabel, c);

        c = new GridBagConstraints();
        JLabel desiredLabel = new JLabel("Desired");
        currentLabel.setFont(new Font("Helvetica", Font.PLAIN, 14));
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 1;
        c.gridy = 1;
        display.add(desiredLabel, c);

        c = new GridBagConstraints();
        currentTemp.setFont(new Font("Helvetica", Font.BOLD, 20));
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 2;
        display.add(currentTemp, c);

        c = new GridBagConstraints();
        desiredTemp.setFont(new Font("Helvetica", Font.BOLD, 20));
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 1;
        c.gridy = 2;
        display.add(desiredTemp, c);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public void turnOnHeat(ActionListener listener) {
        heatTimer = new Timer(1500, listener);
        heatTimer.setRepeats(true);
        heatTimer.start();
    }

    public void turnOffHeat() {
        if (heatTimer != null) {
            heatTimer.stop();
            heatTimer = null;
        }
    }
}
