package com.example.zhuantan_calculator.model;

import com.example.zhuantan_calculator.functionalInterface.ConvertionProvider;
import com.example.zhuantan_calculator.functionalInterface.TargetProvider;

public class LightVehicle extends Vehicles{

    private Integer curbWeight;
    private Double testMass;

    public LightVehicle(){}

    @Override
    public String computeCarbonFuelType() {
        if("柴油".equals(getFuelType())){
            return "柴油";
        }
        if("PHEV".equals(getFuelType())){
            if("柴油".equals(getPhevfuel1()) || "柴油".equals(getPhevfuel2())){
                return "柴油";
            }
        }
        return "汽油";
    }

    @Override
    protected Double doComputeTarget(TargetProvider provider, int method) {
        return getTarget();
    }


    private Double getTarget() {
        if(testMass==null){
            if(curbWeight==null){
                throw new IllegalStateException("缺少计算所需的 curbWeight 和 testMass");            }
            if(this.getCarbonModel() == "N1"){
                testMass = (curbWeight + 100 + (this.getGrossWeight() - curbWeight - 100)*0.15);
            }
            else if(this.getCarbonModel() == "M2"){
                testMass = (curbWeight + 100 + (this.getGrossWeight() - curbWeight - 100)*0.28);
            }
        }
        String carbonFuelType = computeCarbonFuelType();
        String model = this.getCarbonModel();
        int year = this.getYear();

        double result;

        if ("N1".equals(model) && "汽油".equals(carbonFuelType)) {
            if (testMass <= 1190) {
                result = 4.27;
            } else if (testMass <= 2850) {
                result = 0.00263 * (testMass - 1733) + 5.70;
            } else {
                result = 8.64;
            }

        } else if ("N1".equals(model) && "柴油".equals(carbonFuelType)) {
            if (testMass <= 1190) {
                result = 3.64;
            } else if (testMass <= 2850) {
                result = 0.00208 * (testMass - 1733) + 4.77;
            } else {
                result = 7.09;
            }

        } else if ("M2".equals(model) && "汽油".equals(carbonFuelType)) {
            if (testMass <= 1190) {
                result = 4.11;
            } else if (testMass <= 2850) {
                result = 0.00270 * (testMass - 2098) + 6.56;
            } else {
                result = 8.59;
            }

        } else {
            throw new IllegalArgumentException("不支持的车型或燃料类型: " + model + " / " + carbonFuelType);
        }

        if (year == 2030) {
            return result;
        } else if (year == 2029 || year == 2028) {
            return result * 1.15;
        } else {
            throw new IllegalArgumentException("不支持计算该年份： " + year );
        }
    }

    public Integer getCurbWeight() {
        return curbWeight;
    }

    public void setCurbWeight(Integer curbWeight) {
        this.curbWeight = curbWeight;
    }

    public Double getTestMass() {
        return testMass;
    }

    public void setTestMass(Double testMass) {
        this.testMass = testMass;
    }
}
