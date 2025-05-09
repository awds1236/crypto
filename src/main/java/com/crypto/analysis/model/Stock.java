package com.crypto.analysis.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stock {
    private String symbol;
    private String name;
    private String exchange;
    private double price;
    private double change;
    private double changePercent;
    private long volume;
    private double marketCap;
    private String sector;
    private String industry;
}