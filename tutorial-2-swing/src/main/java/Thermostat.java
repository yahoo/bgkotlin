import behaviorgraph.Extent;
import behaviorgraph.Graph;
import behaviorgraph.Moment;
import behaviorgraph.State;

public class Thermostat extends Extent<Thermostat> {
  State<Integer> desiredTemperature;
  State<Integer> currentTemperature;
  State<Boolean> heatOn;

  Moment up;
  Moment down;
  TutorialUI ui;

  public Thermostat(Graph g, TutorialUI uiParam) {
    super(g);
    ui = uiParam;

    up = moment();
    ui.upButton.addActionListener(e -> {
      up.updateWithAction();
    });

    down = moment();
    ui.downButton.addActionListener(e -> {
      down.updateWithAction();
    });

    desiredTemperature = state(60);
    currentTemperature = state(60);
    heatOn = state(false);

    // Set the desired Temperature
    behavior()
      .supplies(desiredTemperature)
      .demands(up, down, getDidAdd())
      .runs(ctx -> {
        if (up.justUpdated()) {
          desiredTemperature.update(desiredTemperature.value() + 1);
        } else if (down.justUpdated()) {
          desiredTemperature.update(desiredTemperature.value() - 1);
        }
        sideEffect(ctx1 -> {
          ui.desiredTemp.setText(desiredTemperature.value().toString());
        });
      });

    // Update current temperature display
    behavior()
      .demands(currentTemperature, getDidAdd())
      .runs(ctx -> {
        sideEffect(ctx1 -> {
          ui.currentTemp.setText(currentTemperature.value().toString());
        });
      });

    // Determine if heat is on
    behavior()
      .supplies(heatOn)
      .demands(currentTemperature, desiredTemperature, getDidAdd())
      .runs(ctx -> {
        boolean on = desiredTemperature.value() > currentTemperature.value();
        heatOn.update(on);
        sideEffect(ctx1 -> {
          ui.heatStatus.setText("Heat " + (heatOn.value() ? "On" : "Off"));
        });
      });

    // Control heating equipment
    behavior()
      .demands(heatOn)
      .runs(ctx -> {
        if (heatOn.justUpdatedTo(true)) {
          sideEffect(ctx1 -> {
            ui.turnOnHeat(e -> {
              currentTemperature.updateWithAction(currentTemperature.value() + 1);
            });
          });
        } else if (heatOn.justUpdatedTo(false)) {
          sideEffect(ctx1 -> {
            ui.turnOffHeat();
          });
        }
      });

  }
}
