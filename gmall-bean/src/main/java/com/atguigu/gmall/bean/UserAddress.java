package com.atguigu.gmall.bean;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;

public class UserAddress implements Serializable {
    @Column
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;
    @Column
    private String userAddress;
    @Column
    private String userId;
    @Column
    private String consignee;
    @Column
    private String phoneNum;
    @Column
    private String isDefault;

    // get set


    public void setId(String id) {
        this.id = id;
    }

    public void setUserAddress(String userAddress) {
        this.userAddress = userAddress;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setConsignee(String consignee) {
        this.consignee = consignee;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public void setIsDefault(String isDefault) {
        this.isDefault = isDefault;
    }

    public String getId() {
        return id;
    }

    public String getUserAddress() {
        return userAddress;
    }

    public String getUserId() {
        return userId;
    }

    public String getConsignee() {
        return consignee;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public String getIsDefault() {
        return isDefault;
    }

    @Override
    public String toString() {
        return "UserAddress{" +
                "id='" + id + '\'' +
                ", userAddress='" + userAddress + '\'' +
                ", userId='" + userId + '\'' +
                ", consignee='" + consignee + '\'' +
                ", phoneNum='" + phoneNum + '\'' +
                ", isDefault='" + isDefault + '\'' +
                '}';
    }

    public UserAddress() {
    }

}
