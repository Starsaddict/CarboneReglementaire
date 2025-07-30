package com.example.zhuantan_calculator.model;

import com.example.zhuantan_calculator.functionalInterface.ConvertionProvider;
import com.example.zhuantan_calculator.functionalInterface.TargetProvider;
public class HeavyVehicle extends Vehicles{

    public HeavyVehicle() { }

    @Override
    public Double doComputeTarget(TargetProvider targetProvider, int method) {
        return targetProvider.getTarget(this,method);
    }

    @Override
    protected Double doComputeOilComsumption(ConvertionProvider provider, int method) {
        String energyType = getFuelType();

        if(energyType.equals("天然气")){
            if(getCarbonGroup().equals("牵引车") | getCarbonGroup().equals("中重型载货")|getCarbonGroup().equals("自卸车")){
                energyType = "天然气-LNG";
            }else{
                energyType = "天然气-CNG";
            }
        }
        Double coeff = provider.getConvertCoeff(energyType,computeCarbonFuelType(),method);
        if(coeff != null){
            return coeff * getEnergy();
        }
        return null;
    }

}
