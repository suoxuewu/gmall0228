package com.atguigu.gmall.bean;

import java.io.Serializable;
import java.util.List;

public class SkuLsResult implements Serializable {
    List<SkuLsInfo> skuLsInfoList;
    long total;
    long tatalPages;
    List<String> attrValueIdList;

    public List<SkuLsInfo> getSkuLsInfoList() {
        return skuLsInfoList;
    }

    public void setSkuLsInfoList(List<SkuLsInfo> skuLsInfoList) {
        this.skuLsInfoList = skuLsInfoList;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getTatalPages() {
        return tatalPages;
    }

    public void setTatalPages(long tatalPages) {
        this.tatalPages = tatalPages;
    }

    public List<String> getAttrValueIdList() {
        return attrValueIdList;
    }

    public void setAttrValueIdList(List<String> attrValueIdList) {
        this.attrValueIdList = attrValueIdList;
    }
}
