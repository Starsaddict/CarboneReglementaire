package com.example.zhuantan_calculator.model;

import com.example.zhuantan_calculator.functionalInterface.ConvertionProvider;
import com.example.zhuantan_calculator.functionalInterface.TargetProvider;
import jakarta.persistence.*;

public abstract class Vehicles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int year;
    private String enterprise;
    private String model;
    private String fuelType;
    private Float energy;
    private Integer grossWeight;

    private Integer method;
    private String carbonModel;   // 转碳车型
    private String carbonGroup;   // 转碳车组

    public Vehicles() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Float getEnergy() {
        return energy;
    }

    public void setEnergy(Float energy) {
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



    public String computeCarbonFuelType(){
        if(this.carbonGroup == "N1" | this.carbonGroup == "M2" ){
            if(this.fuelType.equals("柴油") ){
                return "柴油";
            }
            return "汽油";
        }
        else{
            if(this.fuelType.equals("汽油")){
                return "汽油";
            }
            return "柴油";
        }
    };


    public final Double computeTarget(TargetProvider provider, int method) {
        return doComputeTarget(provider, method);
    }

    protected abstract Double doComputeTarget(TargetProvider provider, int method);

    public final Double computeOilComsumption(ConvertionProvider provider, int method) {
        return doComputeOilComsumption(provider, method);
    }

    protected abstract Double doComputeOilComsumption(ConvertionProvider provider, int method);



}


