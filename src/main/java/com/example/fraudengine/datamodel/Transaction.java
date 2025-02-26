package com.example.fraudengine.datamodel;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

public class Transaction {

    private Long transId;
    private Long userId;
    private BigDecimal amount;
    private Timestamp timestamp;
    private String country;
    private Double latitudeCoord;
    private Double longtudeCoord;

    public Transaction() {
    }

    public Transaction(Long transId, Long userId, BigDecimal amount, Timestamp timestamp, String country, Double latitudeCoord, Double longtudeCoord) {
        this.transId = transId;
        this.userId = userId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.country = country;
        this.latitudeCoord = latitudeCoord;
        this.longtudeCoord = longtudeCoord;
    }

    public Long getTransId() {
        return transId;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getCountry() {
        return country;
    }

    public Double getLatitudeCoord() {
        return latitudeCoord;
    }

    public Double getLongtudeCoord() {
        return longtudeCoord;
    }

    public void setTransId(Long transId) {
        this.transId = transId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setLatitudeCoord(Double latitudeCoord) {
        this.latitudeCoord = latitudeCoord;
    }

    public void setLongtudeCoord(Double longtudeCoord) {
        this.longtudeCoord = longtudeCoord;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transId=" + transId +
                ", userId=" + userId +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                ", country='" + country + '\'' +
                ", latitudeCoord=" + latitudeCoord +
                ", longtudeCoord=" + longtudeCoord +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(transId, that.transId) && Objects.equals(userId, that.userId) && Objects.equals(amount, that.amount) && Objects.equals(timestamp, that.timestamp) && Objects.equals(country, that.country) && Objects.equals(latitudeCoord, that.latitudeCoord) && Objects.equals(longtudeCoord, that.longtudeCoord);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transId, userId, amount, timestamp, country, latitudeCoord, longtudeCoord);
    }
}
