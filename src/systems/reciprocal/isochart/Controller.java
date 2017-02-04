/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package systems.reciprocal.isochart;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import systems.reciprocal.Database;
import static systems.reciprocal.Database.*;
import systems.reciprocal.Rs;
import systems.reciprocal.db.physics.Isotope;
import systems.reciprocal.db.physics.Nubase;

/**
 *
 * @author Bruce Peret
 */
public class Controller implements Initializable {

    /**
     * Magnetic ionization level, which controls the capture of neutrinos by an
     * atomic rotational system.
     */
    int magnetic_ionization_level = 1;
    /**
     * Time (in seconds) of what is considered to be a "stable" isotope
     * half-life.
     */
    double stable_half_life = 1E100;
    /**
     * Data for Stability Limit line.
     */
    XYChart.Series seriesStabilityLimit = new XYChart.Series();
    /**
     * Data for Minimum mass line.
     */
    XYChart.Series seriesMinimumMass = new XYChart.Series();
    /**
     * Data for Maximum mass line.
     */
    XYChart.Series seriesMaximumMass = new XYChart.Series();
    /**
     * Data for NIST unstable isotopic mass line.
     */
    XYChart.Series seriesIsotopeUnstable = new XYChart.Series();
    /**
     * Data for NIST stable isotopic mass line.
     */
    XYChart.Series seriesIsotopeStable = new XYChart.Series();
    /**
     * Data for NIST Standard Atomic Weight line.
     */
    XYChart.Series seriesWeight = new XYChart.Series();
    /**
     * Data for zone of isotopic stability, per Basic Properties of Matter.
     */
    XYChart.Series seriesZoneStability = new XYChart.Series();

    @FXML
    private LineChart<Number, Number> lineChart;
    @FXML
    private CheckBox checkStabilityLimits;
    @FXML
    private CheckBox checkMinimumMass;
    @FXML
    private CheckBox checkMaximumMass;
    @FXML
    private CheckBox checkWeight;
    @FXML
    private CheckBox checkZone;
    @FXML
    private Slider sliderMagIonLevel;
    @FXML
    private Button buttonEarthNorm;
    @FXML
    private Label textMagIonLevel;
    @FXML
    private CheckBox checkStable;
    @FXML
    private CheckBox checkUnstable;

    protected void updateMagIonLevel() {
        sliderMagIonLevel.setValue((double) magnetic_ionization_level);
        textMagIonLevel.setText(Integer.toString(magnetic_ionization_level));
        seriesZoneStability.setData(zone_of_stability());
        seriesStabilityLimit.setData(stabilityLimit());
        seriesMinimumMass.setData(minimum_mass());
        seriesMaximumMass.setData(maximum_mass());
    }

    protected void seriesVisible(XYChart.Series series, boolean state) {
        series.getNode().setVisible(state);
        ObservableList<XYChart.Data<Number, Number>> list = series.getData();
        list.forEach((item) -> {
            item.getNode().setVisible(state);
        });
    }

    /**
     * Grab stable isotope data from database and build line graph.
     *
     * @return
     * @throws SQLException
     */
    ObservableList<XYChart.Data<Number, Number>> unstableIsotopes() throws SQLException {
        ObservableList<XYChart.Data<Number, Number>> data
            = FXCollections.observableArrayList();
        PreparedStatement ps = Database.db.prepareStatement(
            "SELECT z,a FROM " + Nubase.TABLE
            + " WHERE half_life>0 AND half_life<="
            + Double.toString(stable_half_life)
            + " ORDER BY z,a"
        );
        return xychartdata(ps, data);
    }

    /**
     * Grab stable isotope data from database and build line graph.
     *
     * @return
     * @throws SQLException
     */
    ObservableList<XYChart.Data<Number, Number>> stableIsotopes() throws SQLException {
        ObservableList<XYChart.Data<Number, Number>> data
            = FXCollections.observableArrayList();
        PreparedStatement ps = Database.db.prepareStatement(
            "SELECT z,a FROM " + Nubase.TABLE
            + " WHERE half_life<=0 OR half_life>"
            + Double.toString(stable_half_life)
            + " ORDER BY z,a"
        );
        return xychartdata(ps, data);
    }

    /**
     * Reference lines for mass limits.
     *
     * @return Data for the mass limit lines.
     */
    ObservableList<XYChart.Data<Number, Number>> stabilityLimit() {
        int z = Rs.unstable_element(magnetic_ionization_level);
        ObservableList<XYChart.Data<Number, Number>> data
            = FXCollections.observableArrayList();
        data.add(new XYChart.Data(0, Rs.MASS_LIMIT));
        data.add(new XYChart.Data(z, Rs.MASS_LIMIT));
        data.add(new XYChart.Data(z, 0));
        return data;
    }

    /**
     * Minimum mass for an element.
     *
     * Calculated by the rotational mass, since you cannot have a fraction of a
     * rotation. min_mass = 2z.
     *
     * Deuterium is a special case because of the low rotational displacement,
     * 2-1-(1), the (1) effectively cancels one mass unit. This does not happen
     * with higher elements.
     *
     * @return Data for minimum mass line.
     */
    ObservableList<XYChart.Data<Number, Number>> minimum_mass() {
        ObservableList<XYChart.Data<Number, Number>> data
            = FXCollections.observableArrayList();
        if (magnetic_ionization_level >= 0) {
            data.add(new XYChart.Data(1, 1)); // Deuterium
            data.add(new XYChart.Data(1, 2)); // Helium onwards
            data.add(new XYChart.Data(Rs.Z_LIMIT, Rs.MASS_LIMIT));
        } else {
            data.add(new XYChart.Data(1, 1));
            data.add(new XYChart.Data(1, 2)); // Helium onwards
            data.add(new XYChart.Data(Rs.Z_LIMIT, 2));  // flat line
        }
        return data;
    }

    /**
     * Maximum mass for an element.
     *
     * The maximum mass is reached when the vibrational mass (the gravitational
     * charge) cancels the atomic rotation. Since 2 vibrations = 1 rotation, and
     * 2 amu = 1 rotation, it is at 4z-1 (4z starts disintegration).
     *
     * Note that mass can exceed this, but destroys rotational structure and
     * changes the atomic number in compensation.
     *
     * @return Data to show the maximum stable mass in the Reciprocal System.
     */
    ObservableList<XYChart.Data<Number, Number>> maximum_mass() {
        ObservableList<XYChart.Data<Number, Number>> data
            = FXCollections.observableArrayList();
        seriesMaximumMass.setName("Maximum Mass");
        if (magnetic_ionization_level >= 0) {
            data.add(new XYChart.Data(1, 3));
            data.add(new XYChart.Data(Rs.Z_LIMIT, 4 * Rs.Z_LIMIT - 1));
        } else {
            data.add(new XYChart.Data(1, 2));
            data.add(new XYChart.Data(Rs.Z_LIMIT, Rs.MASS_LIMIT));
        }
        return data;
    }

    /**
     * Grab the standard atomic weight from the NIST table.
     *
     * @return Data for the standard atomic weight by atomic number.
     * @throws SQLException
     */
    XYChart.Series standard_weight() throws SQLException {
        PreparedStatement ps = Database.db.prepareStatement(
            "SELECT distinct z,standard_atomic_weight FROM "
            + Isotope.TABLE
            + " ORDER BY z"
        );
        seriesWeight = Isotope.xychart(ps);
        seriesWeight.setName("Standard Atomic Weight");
        return seriesWeight;
    }

    /**
     * Generate data for zone of isotopic stability.
     *
     * @return The data comprising the zone of stability.
     */
    ObservableList<XYChart.Data<Number, Number>> zone_of_stability() {
        ObservableList<XYChart.Data<Number, Number>> data
            = FXCollections.observableArrayList();
        for (int z = 1;
            z <= Rs.Z_LIMIT; z++) {
            data.add(
                new XYChart.Data(
                    z,
                    Math.floor(Rs.standard_mass(z, magnetic_ionization_level)
                    )
                )
            );
        }
        return data;
    }

    /**
     * Called once to initialize the chart display.
     *
     * Draws all the lines for the chart that we can then manipulate with
     * events.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ObservableList<XYChart.Series<Number, Number>> lineChartData
            = FXCollections.observableArrayList();
        try {
            // Nuclear unstable isotope data
            seriesIsotopeUnstable.setName("Unstable Isotopes");
            seriesIsotopeUnstable.setData(unstableIsotopes());
            lineChartData.add(seriesIsotopeUnstable);

            // Nuclear stable isotope data
            seriesIsotopeStable.setName("Stable Isotopes");
            seriesIsotopeStable.setData(stableIsotopes());
            lineChartData.add(seriesIsotopeStable);

            // Stability Limit
            seriesStabilityLimit.setName("Stability limit");
            seriesStabilityLimit.setData(stabilityLimit());
            lineChartData.add(seriesStabilityLimit);

            // Minimum mass
            seriesMinimumMass.setName("Minimum Mass");
            seriesMinimumMass.setData(minimum_mass());
            lineChartData.add(seriesMinimumMass);

            // Maximum mass
            seriesMaximumMass.setName("Maximum Mass");
            seriesMaximumMass.setData(maximum_mass());
            lineChartData.add(seriesMaximumMass);

            // Standard atomic weights (static from database)
            lineChartData.add(standard_weight());

            // Zone of Isotopic Stability
            seriesZoneStability.setName("Calculated Zone of Stability");
            seriesZoneStability.setData(zone_of_stability());
            lineChartData.add(seriesZoneStability);

            lineChart.setData(lineChartData);
            lineChart.createSymbolsProperty();
        } catch (SQLException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Checkbox to toggle the display of isotopic stability limits.
     *
     * @param event
     */
    @FXML
    private void handleCheckStabilityLimits(ActionEvent event) {
        seriesVisible(seriesStabilityLimit, checkStabilityLimits.isSelected());
    }

    /**
     * Checkbox to toggle the display of the minimum mass (2Z) in the RS.
     *
     * @param event
     */
    @FXML
    private void handleCheckMinimumMass(ActionEvent event) {
        seriesVisible(seriesMinimumMass, checkMinimumMass.isSelected());
    }

    /**
     * Checkbox to toggle the display of the maximum mass in the RS.
     *
     * @param event
     */
    @FXML
    private void handleCheckMaximumMass(ActionEvent event) {
        seriesVisible(seriesMaximumMass, checkMaximumMass.isSelected());
    }

    /**
     * Checkbox to toggle the display of NIST isotopic mass data.
     *
     * @param event
     */
    @FXML
    private void handleCheckStable(ActionEvent event) {
        seriesVisible(seriesIsotopeStable, checkStable.isSelected());
    }

    /**
     * Checkbox to toggle the display of the zone of isotopic stability.
     *
     * @param event
     */
    @FXML
    private void handleCheckUnstable(ActionEvent event) {
        seriesVisible(seriesIsotopeUnstable, checkUnstable.isSelected());
    }

    /**
     * Checkbox to toggle the display of standard atomic weight.
     *
     * @param event
     */
    @FXML
    private void handleCheckWeight(ActionEvent event) {
        seriesVisible(seriesWeight, checkWeight.isSelected());
    }

    /**
     * Checkbox to toggle the display of the zone of isotopic stability.
     *
     * @param event
     */
    @FXML
    private void handleCheckZone(ActionEvent event) {
        seriesVisible(seriesZoneStability, checkZone.isSelected());
    }

    /**
     * Set the magnetic ionization level to 1, the norm for the Earth's surface,
     * and update the chart.
     *
     * @param event
     */
    @FXML
    private void handleEarthNorm(ActionEvent event) {
        magnetic_ionization_level = 1;
        updateMagIonLevel();
    }

    /**
     * What to do when the magnetic ionization slider is moved. This event gets
     * called many times as the slider is sliding, not just when the value
     * changes, so watch for the value to change before we actually try to
     * update anything.
     *
     * Updates the global variable "magnetic_ionization_level," then adjusts the
     * graph to match (the isotopic mass curve and the unstable atomic number).
     *
     * @param event Slider event changing magnetic ionization level.
     */
    @FXML
    private void handleMagIonDrag(MouseEvent event) {
        int magion = (int) sliderMagIonLevel.getValue();
        if (magion != magnetic_ionization_level) {
            magnetic_ionization_level = magion;
            textMagIonLevel.setText(Integer.toString(magnetic_ionization_level));
            updateMagIonLevel();
        }
    }

}
