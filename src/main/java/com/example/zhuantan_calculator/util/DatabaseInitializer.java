package com.example.zhuantan_calculator.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class DatabaseInitializer {
    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("zhuantanPU");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        // 不需要写入数据，只是触发表创建
        em.getTransaction().commit();
        em.close();
        emf.close();
        System.out.println("数据库结构已创建完毕。");
    }
}