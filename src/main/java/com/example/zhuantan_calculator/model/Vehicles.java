package com.example.zhuantan_calculator.model;

import com.example.zhuantan_calculator.functionalInterface.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

public abstract class Vehicles {

    @Getter
    @Setter
    private int year;
    @Getter
    @Setter
    private String enterprise;
    @Getter
    @Setter
    private String model;
    @Getter
    @Setter
    private String fuelType;

    @Getter
    @Setter
    private Double energy;

    @Getter
    @Setter
    private Map<String, Double> fuelTypeEnergyMap;

    public String getPhevfuel1()        { return getFuelKeyByIndex(0); }
    public Double getPhevfuel1Energy()  { return getFuelEnergyByIndex(0); }
    public String getPhevfuel2()        { return getFuelKeyByIndex(1); }
    public Double getPhevfuel2Energy()  { return getFuelEnergyByIndex(1); }

    private String getFuelKeyByIndex(int idx) {
        Map.Entry<String, Double> e = getNthEntry(idx);
        return e == null ? null : e.getKey();
    }
    private Double getFuelEnergyByIndex(int idx) {
        Map.Entry<String, Double> e = getNthEntry(idx);
        return e == null ? null : e.getValue();
    }

    /** 仅当 fuelType 为 PHEV 时，从 map 里安全地取第 N 项；不足则返回 null */
    private Map.Entry<String, Double> getNthEntry(int n) {
        if (!"PHEV".equals(getFuelType())) return null;
        Map<String, Double> m = getFuelTypeEnergyMap();
        if (m == null || m.isEmpty()) return null;

        int i = 0;
        for (Map.Entry<String, Double> e : m.entrySet()) {
            if (i++ == n) return e;
        }
        return null;
    }

    @Getter
    @Setter
    private Integer grossWeight;

    @Getter
    @Setter
    private Integer method;

    @Getter
    @Setter
    private String carbonModel;   // 转碳车型

    @Getter
    @Setter
    private String carbonGroup;   // 转碳车组

    @Getter
    @Setter
    private int sales;

    public Vehicles() {
    }


    public abstract String computeCarbonFuelType();

    public final Double computeTarget(TargetProvider provider, int method) {
        return doComputeTarget(provider, method);
    }

    protected abstract Double doComputeTarget(TargetProvider provider, int method);

    public final Double computeOilConsumption(ConvertionProvider provider, int method) {

        return provider.getConvertCoeff(this, method);

    }

    public boolean isNewEnergy() {
        return "BEV".equals(fuelType) || "FCV".equals(fuelType) || "PHEV".equals(fuelType)  || "PHEV-甲醇".equals(fuelType);
    }

    public Double computeNetOilCredit(BonusProvider bonusProvider,
                                      TargetProvider targetProvider,
                                      ConvertionProvider convertionProvider,
                                      int method) {
        // 先取参与运算的值（都用包装类型，避免自动拆箱NPE）
        Double target = this.computeTarget(targetProvider, method);
        Double consumption = this.computeOilConsumption(convertionProvider, method);

        if (target == null || consumption == null) {
            return null;
        }

        double bonus = bonusProvider.calculateBonus(this);

        return computeNetOilCredit(bonus, target, consumption);
    }

    public double computeNetOilCredit(double bonus, double target, double oilConsumption){
        return (target* bonus - oilConsumption) * sales;
    }

    public Double computeNetCarbonCredit(CarbonFactorProvider provider, double netOilCredit){
        return provider.getCarbonFactor(this)*netOilCredit;
    }
}


