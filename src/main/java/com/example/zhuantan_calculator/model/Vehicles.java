package com.example.zhuantan_calculator.model;

import com.example.zhuantan_calculator.functionalInterface.*;

public abstract class Vehicles {

    private int year;
    private String enterprise;
    private String model;
    private String fuelType;
    private Double energy;
    private Integer grossWeight;

    private Integer method;
    private String carbonModel;   // 转碳车型
    private String carbonGroup;   // 转碳车组

    private Integer sales;

    public Integer getSales() {
        return sales;
    }

    public void setSales(Integer sales) {
        this.sales = sales;
    }

    public Vehicles() {
    }


    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getEnterprise() {
        return enterprise;
    }

    public void setEnterprise(String enterprise) {
        this.enterprise = enterprise;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public Double getEnergy() {
        return energy;
    }

    public void setEnergy(Double energy) {
        this.energy = energy;
    }

    public Integer getGrossWeight() {
        return grossWeight;
    }

    public void setGrossWeight(Integer grossWeight) {
        this.grossWeight = grossWeight;
    }

    public Integer getMethod() {
        return method;
    }

    public void setMethod(Integer method) {
        this.method = method;
    }

    public String getCarbonModel() {
        return carbonModel;
    }

    public void setCarbonModel(String carbonModel) {
        this.carbonModel = carbonModel;
    }

    public String getCarbonGroup() {
        return carbonGroup;
    }

    public void setCarbonGroup(String carbonGroup) {
        this.carbonGroup = carbonGroup;
    }

    public String computeCarbonFuelType() {
        if ("N1".equals(this.carbonGroup) || "M2".equals(this.carbonGroup)) {
            if ("柴油".equals(this.fuelType)) {
                return "柴油";
            }
            return "汽油";
        } else {
            if ("汽油".equals(this.fuelType)) {
                return "汽油";
            }
            return "柴油";
        }
    }

    public final Double computeTarget(TargetProvider provider, int method) {
        return doComputeTarget(provider, method);
    }

    protected abstract Double doComputeTarget(TargetProvider provider, int method);

    public final Double computeOilConsumption(ConvertionProvider provider, int method) {
        return doComputeOilConsumption(provider, method);
    }

    public Double doComputeOilConsumption(ConvertionProvider provider, int method) {
        Double coeff = provider.getConvertCoeff(this, method);
        Double energy = getEnergy();

        Double result = coeff * energy;
        System.out.println(this.getModel()+"的能耗是"+energy+"，转化系数是"+coeff+";油耗结果是"+result);
        return result;
    }
    public boolean isNewEnergy() {
        return "BEV".equals(fuelType) || "FCV".equals(fuelType) || "PHEV".equals(fuelType);
    }
    public Double computeNetOilCredit(BonusProvider bonusProvider,
                                      TargetProvider targetProvider,
                                      ConvertionProvider convertionProvider,
                                      int method) {
        // 先取参与运算的值（都用包装类型，避免自动拆箱NPE）
        Double target = this.computeTarget(targetProvider, method);
        Double consumption = this.computeOilConsumption(convertionProvider, method);
        Integer s = this.getSales();

        // 只要有一个参数为null，整段返回null
        if (target == null || consumption == null || s == null) {
            return null;
        }

        // bonus通常是纯算法数，可保留为primitive
        double bonus = bonusProvider.calculateBonus(this);

        return (target * bonus - consumption) * s;
    }

    public Double computeNetCarbonCredit(CarbonFactorProvider provider, double netOilCredit){ //TODO: 这里没写呢
        return provider.getCarbonFactor(this)*netOilCredit;
    }
}


