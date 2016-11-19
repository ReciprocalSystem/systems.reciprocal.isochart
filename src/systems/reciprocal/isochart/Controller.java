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
import javafx.scene.control.CheckBox;
import systems.reciprocal.Database;
import systems.reciprocal.Rs;
import systems.reciprocal.db.physics.Isotope;

/**
 *
 * @author Bruce Peret
 */
public class Controller implements Initializable {

    int magnetic_ionization_level = 1;
    XYChart.Series seriesStabilityLimit = new XYChart.Series();
    XYChart.Series seriesMinimumMass = new XYChart.Series();
    XYChart.Series seriesMaximumMass = new XYChart.Series();
    XYChart.Series seriesNist;
    XYChart.Series seriesWeight;
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
    private CheckBox checkNist;
    @FXML
    private CheckBox checkWeight;
    @FXML
    private CheckBox checkZone;

    /**
     * Add actual NIST isotope data.
     *
     * This is formatted by chart.css to remove lines and act like a scatter
     * plot. This is the "0" series_mass_limit in chart.css.
     *
     * @return
     * @throws java.sql.SQLException
     */
    XYChart.Series nist_data() throws SQLException {
        PreparedStatement ps = Database.db.prepareStatement(
            "SELECT z,isotope FROM " + Isotope.TABLE
            + " ORDER BY z,isotope"
        );
        seriesNist = Isotope.xychart(ps);
        seriesNist.setName("NIST Isotope Data");
        return seriesNist;
    }

    /**
     * Reference lines for mass limits.
     *
     * @param ion
     * @return
     */
    XYChart.Series stabilityLimit(int ion) {
        int z = Rs.unstable_element(ion);
        seriesStabilityLimit.setName("Stability limit");
        seriesStabilityLimit.getData().add(new XYChart.Data(1, Rs.MASS_LIMIT));
        seriesStabilityLimit.getData().add(new XYChart.Data(z, Rs.MASS_LIMIT));
        seriesStabilityLimit.getData().add(new XYChart.Data(z, 0));
        return seriesStabilityLimit;
    }

    /**
     * Minimum mass for an element.
     *
     * Calculated by the rotational mass, since you cannot have a fraction of a
     * rotation. min_mass = 2z.
     *
     * @return
     */
    XYChart.Series minimum_mass() {
        seriesMinimumMass.setName("Minimum Mass");
        seriesMinimumMass.getData().add(new XYChart.Data(1, 2));
        seriesMinimumMass.getData().add(new XYChart.Data(118, 2 * 118));
        return seriesMinimumMass;
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
     * @return
     */
    XYChart.Series maximum_mass() {
        seriesMaximumMass.setName("Maximum Mass");
        seriesMaximumMass.getData().add(new XYChart.Data(1, 3));
        seriesMaximumMass.getData().add(new XYChart.Data(Rs.Z_LIMIT, 4 * Rs.Z_LIMIT - 1));
        return seriesMaximumMass;
    }

    /**
     * Grab the standard atomic weight from the NIST table.
     *
     * @return
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

    XYChart.Series zone_of_stability(int ion) {
        seriesZoneStability.setName("Calculated Zone of Stability");
        for (int z = 1;
            z <= Rs.Z_LIMIT; z++) {
            seriesZoneStability.getData().add(
                new XYChart.Data(
                    z,
                    Math.floor(Rs.standard_mass(z, ion)
                    )
                )
            );
        }
        return seriesZoneStability;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            ObservableList<XYChart.Series<Number, Number>> lineChartData = FXCollections.observableArrayList();
            lineChartData.add(nist_data());
            lineChartData.add(stabilityLimit(magnetic_ionization_level));
            lineChartData.add(minimum_mass());
            lineChartData.add(maximum_mass());
            lineChartData.add(standard_weight());
            lineChartData.add(zone_of_stability(magnetic_ionization_level));
            lineChart.setData(lineChartData);
            lineChart.createSymbolsProperty();
        } catch (SQLException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    private void handleCheckStabilityLimits(ActionEvent event) {
        seriesStabilityLimit.getNode().setVisible(
            checkStabilityLimits.isSelected()
        );
    }

    @FXML
    private void handleCheckMinimumMass(ActionEvent event) {
        seriesMinimumMass.getNode().setVisible(
            checkMinimumMass.isSelected()
        );
    }

    @FXML
    private void handleCheckMaximumMass(ActionEvent event) {
        seriesMaximumMass.getNode().setVisible(
            checkMaximumMass.isSelected()
        );
    }

    @FXML
    private void handleCheckNist(ActionEvent event) {
        boolean state = checkNist.isSelected();
        seriesNist.getNode().setVisible(state);
        ObservableList<XYChart.Data<Number,Number>> list = seriesNist.getData();
        list.forEach((item) -> {
            item.getNode().setVisible(state);
        });
    }

    @FXML
    private void handleCheckWeight(ActionEvent event) {
        boolean state = checkWeight.isSelected();
        seriesWeight.getNode().setVisible(state);
        ObservableList<XYChart.Data<Number,Number>> list = seriesWeight.getData();
        list.forEach((item) -> {
            item.getNode().setVisible(state);
        });
    }

    @FXML
    private void handleCheckZone(ActionEvent event) {
        seriesZoneStability.getNode().setVisible(
            checkZone.isSelected()
        );
    }

}
