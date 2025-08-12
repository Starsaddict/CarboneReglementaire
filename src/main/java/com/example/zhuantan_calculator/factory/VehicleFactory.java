package com.example.zhuantan_calculator.factory;

import com.example.zhuantan_calculator.model.HeavyVehicle;
import com.example.zhuantan_calculator.model.LightVehicle;
import com.example.zhuantan_calculator.model.Vehicles;

public class VehicleFactory {

    public static Vehicles createVehicleByType(String modelType) {
        if ("N1".equals(modelType) || "M2".equals(modelType)) {
            return new LightVehicle();
        } else {
            return new HeavyVehicle();
        }
    }

    public static Vehicles createVehicleFromData(int year, String enterprise, String model, Integer curbWeight, Integer grossWeight, Double testMass, String gvmArea, Double energy, String fuelType, String carbonGroup, Integer sales){
        // 共同参数
        Vehicles vehicle = createVehicleByType(carbonGroup);
        vehicle.setYear(year);
        vehicle.setEnterprise(enterprise);
        vehicle.setModel(model);
        vehicle.setGrossWeight(grossWeight);
        vehicle.setEnergy(energy);
        vehicle.setFuelType(fuelType);
        vehicle.setCarbonGroup(carbonGroup);
        if(carbonGroup.equals("轻型载货")|carbonGroup.equals("中重型载货")){
            vehicle.setCarbonModel("货车");
        }else{
            vehicle.setCarbonModel(carbonGroup);
        }
        if(sales == null){
            sales = 0;
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
