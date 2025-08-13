package com.example.zhuantan_calculator.model;

import com.example.zhuantan_calculator.functionalInterface.ConvertionProvider;
import com.example.zhuantan_calculator.functionalInterface.TargetProvider;
public class HeavyVehicle extends Vehicles{

    private String gvwArea;

    public HeavyVehicle() { }

    @Override
    public String computeCarbonFuelType() {
        if("汽油".equals(getFuelType())){
            return "汽油";
        }
        if("PHEV".equals(getFuelType())){
            if("汽油".equals(getPhevfuel1()) || "汽油".equals(getPhevfuel2())){
                return "汽油";
            }
        }
        return "柴油";
    }

    @Override
    public Double doComputeTarget(TargetProvider targetProvider, int method) {
        return targetProvider.getTarget(this,method);
    }


    public String getGvwArea() {
        return gvwArea;
    }

    public void setGvwArea(String gvwArea) {
        this.gvwArea = gvwArea;
    }
}
