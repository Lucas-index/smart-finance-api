package com.smartapi.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String category;

    @Column(name = "monthly_limit", nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyLimit;

    protected Budget() {
    }

    public Budget(String category, BigDecimal monthlyLimit) {
        this.category = category;
        this.monthlyLimit = monthlyLimit;
    }

    public Long getId() { return id; }
    public String getCategory() { return category; }
    public BigDecimal getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(BigDecimal monthlyLimit) { this.monthlyLimit = monthlyLimit; }
}
