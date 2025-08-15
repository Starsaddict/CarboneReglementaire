package com.example.zhuantan_calculator.factory;

import com.example.zhuantan_calculator.model.HeavyVehicle;
import com.example.zhuantan_calculator.model.LightVehicle;
import com.example.zhuantan_calculator.model.Vehicles;

import java.util.*;

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
        // 提前校验
        List<String> legalCarbonGroup = Arrays.asList("轻型载货","中重型载货","客车","城市客车","N1","M2","牵引车","自卸车");
        if (!legalCarbonGroup.contains(carbonGroup)) {
            return null;
        }
        Vehicles vehicle = createVehicleByType(carbonGroup);

        List<Integer> legalYear =  Arrays.asList(2028,2029,2030);
        if(legalYear.contains(year)){
            vehicle.setYear(year);
        }else{
            vehicle.setYear(999999);
        }

        vehicle.setEnterprise(enterprise);
        vehicle.setModel(model);
        vehicle.setGrossWeight(grossWeight);
        vehicle.setEnergy(energy);

        List<String> legalFuelType = Arrays.asList("PHEV","FCV","BEV","汽油","柴油","天然气","甲醇");
        if (legalFuelType.contains(fuelType)) {
            vehicle.setFuelType(fuelType);
        }else{
            vehicle.setFuelType("非法值");
        }

        vehicle.setCarbonGroup(carbonGroup);



        if ("轻型载货".equals(carbonGroup) || "中重型载货".equals(carbonGroup)) {
            vehicle.setCarbonModel("货车");
        } else {
            vehicle.setCarbonModel(carbonGroup);
        }

        if ("PHEV".equals(fuelType)) {
            Map<String, Double> fuelTypeEnergyMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            List<String> legalPHEVFuelType = Arrays.asList("电","甲醇","汽油","柴油");
            // 校验燃料1
            if (PHEVFuel1Col != null && legalPHEVFuelType.contains(PHEVFuel1Col)) {
                fuelTypeEnergyMap.put(PHEVFuel1Col, (PHEVFuel1EnergyCol != null ? PHEVFuel1EnergyCol : 0.0));
            } else {
                fuelTypeEnergyMap.put("非法燃料1", (PHEVFuel1EnergyCol != null ? PHEVFuel1EnergyCol : 0.0));
            }
            // 校验燃料2（允许为空，但若有则必须不同且合法）
            if (PHEVFuel2Col != null && !PHEVFuel2Col.isEmpty() && !Objects.equals(PHEVFuel2Col, PHEVFuel1Col) && legalPHEVFuelType.contains(PHEVFuel2Col)) {
                fuelTypeEnergyMap.put(PHEVFuel2Col, (PHEVFuel2EnergyCol != null ? PHEVFuel2EnergyCol : 0.0));
            } else if (PHEVFuel2Col != null && !PHEVFuel2Col.isEmpty()) {
                fuelTypeEnergyMap.put("非法燃料2", (PHEVFuel2EnergyCol != null ? PHEVFuel2EnergyCol : 0.0));
            }
            vehicle.setFuelTypeEnergyMap(fuelTypeEnergyMap);
        }
        vehicle.setSales(sales >= 0 ? sales : 0);
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
