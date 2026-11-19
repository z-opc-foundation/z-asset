package com.zifang.z.asset.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 企业资产实体（表 {@code z_asset}）。
 *
 * <p>管的是**公司买了什么、归谁、什么时候到期**：实物设备走采购→入库→领用→在用→归还→报废；
 * 权益项（域名 / 证书 / 许可 / 订阅）有到期日，到期前必须提醒。</p>
 *
 * <p>技术资源（数据库 / 缓存 / 队列 / 存储桶）不在这张表 —— 那是各模块主动上报的运行态，
 * 归 z-meta 的 {@code z_resource}。</p>
 *
 * @author zifang
 * @since 1.1.0
 */
@TableName("z_asset")
@BizModel(name = "企业资产", module = "asset",
        description = "公司持有的实物设备与权益项：服务器 / 网络设备 / 办公设备 / 域名 / 证书 / 许可 / 订阅")
public class ZAssetDO {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;

    /** 资产编码（唯一）。 */
    @BizField(name = "资产编码", type = BizField.FieldType.STRING)
    private String assetCode;

    /** 资产名称。 */
    @BizField(name = "资产名称", type = BizField.FieldType.STRING)
    private String assetName;

    /** 资产大类：PHYSICAL / ENTITLEMENT。 */
    @BizField(name = "资产大类", type = BizField.FieldType.ENUM, dictCode = "asset_category")
    private String assetCategory;

    /** 资产细类（server / switch / laptop / domain / certificate / license / subscription ...）。 */
    @BizField(name = "资产细类", type = BizField.FieldType.ENUM, dictCode = "asset_type")
    private String assetType;

    /** 序列号 / 编号（实物资产唯一标识）。 */
    @BizField(name = "序列号", type = BizField.FieldType.STRING)
    private String serialNo;

    /** 供应方。 */
    @BizField(name = "供应方", type = BizField.FieldType.STRING)
    private String vendor;

    /** 责任人。 */
    @BizField(name = "责任人", type = BizField.FieldType.STRING)
    private String owner;

    /** 生命周期状态。 */
    @BizField(name = "生命周期状态", type = BizField.FieldType.ENUM, dictCode = "asset_lifecycle")
    private String status;

    /** 存放位置 / 部署位置。 */
    @BizField(name = "位置", type = BizField.FieldType.STRING)
    private String location;

    /** 成本。 */
    @BizField(name = "成本", type = BizField.FieldType.DECIMAL)
    private BigDecimal cost;

    /** 采购日期。 */
    @BizField(name = "采购日期", type = BizField.FieldType.DATE)
    private Date purchaseDate;

    /** 到期时间（权益资产必有）。 */
    @BizField(name = "到期时间", type = BizField.FieldType.DATETIME)
    private Date expireAt;

    /** 保修到期时间（实物资产）。 */
    @BizField(name = "保修到期", type = BizField.FieldType.DATETIME)
    private Date warrantyExpireAt;

    /** 备注。 */
    @BizField(name = "备注", type = BizField.FieldType.TEXT)
    private String description;

    /** 租户编码。 */
    @BizField(name = "租户编码", type = BizField.FieldType.STRING)
    private String tenantCode;

    /** 创建时间。 */
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private Date gmtCreate;

    /** 更新时间。 */
    @BizField(name = "更新时间", type = BizField.FieldType.DATETIME)
    private Date gmtModified;

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

    public String getAssetCategory() {
        return assetCategory;
    }

    public void setAssetCategory(String assetCategory) {
        this.assetCategory = assetCategory;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public Date getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(Date purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public Date getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(Date expireAt) {
        this.expireAt = expireAt;
    }

    public Date getWarrantyExpireAt() {
        return warrantyExpireAt;
    }

    public void setWarrantyExpireAt(Date warrantyExpireAt) {
        this.warrantyExpireAt = warrantyExpireAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }
}
