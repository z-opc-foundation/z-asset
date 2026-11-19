package com.zifang.z.asset.api;

/**
 * 资产流转请求：把一件资产从当前状态推到下一个状态。
 *
 * @author zifang
 * @since 1.1.0
 */
public class AssetTransitionRequest {

    /** 资产编码。 */
    private String assetCode;

    /** 目标状态。 */
    private String targetStatus;

    /** 责任人（领用 / 移交时必填）。 */
    private String owner;

    /** 备注。 */
    private String remark;

    public AssetTransitionRequest() {
    }

    public AssetTransitionRequest(String assetCode, String targetStatus, String owner) {
        this.assetCode = assetCode;
        this.targetStatus = targetStatus;
        this.owner = owner;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public void setAssetCode(String assetCode) {
        this.assetCode = assetCode;
    }

    public String getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(String targetStatus) {
        this.targetStatus = targetStatus;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
