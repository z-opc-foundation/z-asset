package com.zifang.z.asset.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 资产实体类，用于采集各模块的资产信息。
 *
 * @author zifang
 * @see AssetProvider
 * @see AssetCollector
 * @since 1.0.0
 */
@TableName("z_asset")
public class AssetEntity {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 资产编码
     */
    private String assetCode;

    /**
     * 资产名称
     */
    private String assetName;

    /**
     * 资产类型
     */
    private String assetType;

    /**
     * 所属模块
     */
    private String module;

    /**
     * 状态
     */
    private String status;

    /**
     * 端点信息
     */
    private String endpoint;

    /**
     * 规格配置
     */
    private String spec;

    /**
     * 属性信息（JSON格式）
     */
    private String attributes;

    /**
     * 租户编码
     */
    private String tenantCode;

    /**
     * 域编码
     */
    private String domainCode;

    /**
     * 采集时间
     */
    private LocalDateTime collectedAt;

    /**
     * 过期时间
     */
    private LocalDateTime expireAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public void setAssetCode(String assetCode) {
        this.assetCode = assetCode;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getDomainCode() {
        return domainCode;
    }

    public void setDomainCode(String domainCode) {
        this.domainCode = domainCode;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    @Override
    public String toString() {
        return "AssetEntity{" +
                "id=" + id +
                ", assetCode='" + assetCode + '\'' +
                ", assetName='" + assetName + '\'' +
                ", assetType='" + assetType + '\'' +
                ", module='" + module + '\'' +
                ", status='" + status + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", spec='" + spec + '\'' +
                ", attributes='" + attributes + '\'' +
                ", tenantCode='" + tenantCode + '\'' +
                ", domainCode='" + domainCode + '\'' +
                ", collectedAt=" + collectedAt +
                ", expireAt=" + expireAt +
                ", createdTime=" + createdTime +
                ", updatedTime=" + updatedTime +
                '}';
    }
}
