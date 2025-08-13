package com.example.zhuantan_calculator.factory;

import com.example.zhuantan_calculator.model.HeavyVehicle;
import com.example.zhuantan_calculator.model.LightVehicle;
import com.example.zhuantan_calculator.model.Vehicles;

import java.util.Objects;
import java.util.TreeMap;
import java.util.Map;

public class VehicleFactory {

    public static Vehicles createVehicleByType(String modelType) {
        if ("N1".equals(modelType) || "M2".equals(modelType)) {
            return new LightVehicle();
        } else {
            return new HeavyVehicle();
        }
    }

    public static Vehicles createVehicleFromData(int year, String enterprise, String model, Integer curbWeight, Integer grossWeight, Double testMass, String gvmArea, Double energy, String fuelType, String carbonGroup, Integer sales, String PHEVFuel1Col, Double PHEVFuel1EnergyCol, String PHEVFuel2Col, Double PHEVFuel2EnergyCol){
        // 共同参数
        Vehicles vehicle = createVehicleByType(carbonGroup);
        vehicle.setYear(year);
        vehicle.setEnterprise(enterprise);
        vehicle.setModel(model);
        vehicle.setGrossWeight(grossWeight);
        vehicle.setEnergy(energy);
        vehicle.setFuelType(fuelType);
        vehicle.setCarbonGroup(carbonGroup);
        if ("轻型载货".equals(carbonGroup) || "中重型载货".equals(carbonGroup)) {
            vehicle.setCarbonModel("货车");
        } else {
            vehicle.setCarbonModel(carbonGroup);
        }

        if ("PHEV".equals(fuelType)) {
            Map<String, Double> fuelTypeEnergyMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            if (PHEVFuel1Col != null) {
                fuelTypeEnergyMap.put(PHEVFuel1Col, PHEVFuel1EnergyCol);
            }
            if (PHEVFuel2Col != null && !Objects.equals(PHEVFuel2Col, PHEVFuel1Col)) {
                fuelTypeEnergyMap.put(PHEVFuel2Col, PHEVFuel2EnergyCol);
            }
            vehicle.setFuelTypeEnergyMap(fuelTypeEnergyMap);
        }
        vehicle.setSales(sales);

        //轻型车
        if( vehicle instanceof LightVehicle){
            ((LightVehicle)vehicle).setCurbWeight(curbWeight);
            ((LightVehicle)vehicle).setTestMass(testMass);
        }
        //重型车
        else if( vehicle instanceof HeavyVehicle){
            ((HeavyVehicle)vehicle).setGvwArea(gvmArea);
        }

        return vehicle;
    }

}
