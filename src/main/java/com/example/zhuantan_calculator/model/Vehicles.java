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
    public void setPhevfuel1(String val) {
        if (!"PHEV".equals(getFuelType())) return;

        // 保证 map 存在
        Map<String, Double> m = getFuelTypeEnergyMap();
        if (m == null) {
            m = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        } else {
            m = new java.util.TreeMap<>(m); // 复制一份，防止迭代修改问题
        }

        // 取当前能耗
        Double energy = null;
        if (val != null && m.containsKey(val)) {
            energy = m.get(val);
        } else {
            // 原来 fuel1 的能耗
            Double oldEnergy = getPhevfuel1Energy();
            energy = (oldEnergy != null) ? oldEnergy : 0.0;
        }

        // 删除旧的 fuel1
        String oldFuel1 = getPhevfuel1();
        if (oldFuel1 != null) {
            m.remove(oldFuel1);
        }

        // 插入新的 fuel1，放在第一位
        java.util.LinkedHashMap<String, Double> newMap = new java.util.LinkedHashMap<>();
        if (val != null) {
            newMap.put(val, energy);
        }
        // 保留 fuel2
        String fuel2 = getPhevfuel2();
        if (fuel2 != null && !fuel2.equals(val)) {
            newMap.put(fuel2, getPhevfuel2Energy() != null ? getPhevfuel2Energy() : 0.0);
        }

        setFuelTypeEnergyMap(new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER) {{
            putAll(newMap);
        }});
    }

    public void setPhevfuel2(String val) {
        if (!"PHEV".equals(getFuelType())) return;

        // 保证 map 存在
        Map<String, Double> m = getFuelTypeEnergyMap();
        if (m == null) {
            m = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        } else {
            m = new java.util.TreeMap<>(m);
        }

        // 取当前能耗
        Double energy = null;
        if (val != null && m.containsKey(val)) {
            energy = m.get(val);
        } else {
            Double oldEnergy = getPhevfuel2Energy();
            energy = (oldEnergy != null) ? oldEnergy : 0.0;
        }

        // 删除旧的 fuel2
        String oldFuel2 = getPhevfuel2();
        if (oldFuel2 != null) {
            m.remove(oldFuel2);
        }

        // 保留 fuel1
        String fuel1 = getPhevfuel1();
        java.util.LinkedHashMap<String, Double> newMap = new java.util.LinkedHashMap<>();
        if (fuel1 != null) {
            newMap.put(fuel1, getPhevfuel1Energy() != null ? getPhevfuel1Energy() : 0.0);
        }
        // 插入 fuel2（不能和 fuel1 一样）
        if (val != null && !val.equals(fuel1)) {
            newMap.put(val, energy);
        }

        setFuelTypeEnergyMap(new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER) {{
            putAll(newMap);
        }});
    }

    public void setPhevfuel1Energy(double val) {
        if (!"PHEV".equals(getFuelType())) return;
        if(getPhevfuel1() != null){
            fuelTypeEnergyMap.put(getPhevfuel1(), val);
        }
    }

    public void setPhevfuel2Energy(double val) {
        if (!"PHEV".equals(getFuelType())) return;
        if(getPhevfuel2() != null){
            fuelTypeEnergyMap.put(getPhevfuel2(), val);
        }
    }
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


