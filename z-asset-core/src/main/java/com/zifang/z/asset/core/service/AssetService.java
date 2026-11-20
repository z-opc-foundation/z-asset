package com.zifang.z.asset.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zifang.z.asset.api.enums.AssetCategory;
import com.zifang.z.asset.api.enums.AssetLifecycle;
import com.zifang.z.asset.core.domain.entity.ZAssetDO;
import com.zifang.z.asset.core.domain.mapper.ZAssetMapper;
import com.zifang.z.meta.api.annotation.BizCause;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizMethod;
import com.zifang.z.meta.api.annotation.BizParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * 企业资产台账服务：流转校验 + 到期扫描。
 *
 * @author zifang
 * @since 1.1.0
 */
@Service
@BizCause(effect = "资产台账发生变更", because = "有人操作了资产流转", condition = "流转合法")
public class AssetService {

    private final ZAssetMapper assetMapper;

    public AssetService(ZAssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    /**
     * 流转一件资产到目标状态。
     *
     * <p>非法流转直接拒绝，不落库。</p>
     *
     * @param assetCode    资产编码
     * @param targetStatus 目标状态
     * @param owner        责任人（领用 / 移交时填）
     * @param remark       备注
     * @return 流转后的资产
     * @throws IllegalStateException 非法流转时抛出
     */
    @Transactional
    @BizMethod(name = "流转资产生命周期", module = "asset",
            description = "推进资产状态机；非法流转直接拒绝且不落库", operation = BizMethod.Operation.ACTION)
    @BizCause(effect = "资产责任发生转移", because = "资产被流转到新状态", condition = "流转合法且已指定责任人")
    public ZAssetDO transition(
            @BizParam(name = "资产编码", type = BizField.FieldType.STRING, description = "全局唯一资产编码") String assetCode,
            @BizParam(name = "目标状态", type = BizField.FieldType.ENUM, dictCode = "asset_lifecycle") AssetLifecycle targetStatus,
            @BizParam(name = "责任人", type = BizField.FieldType.STRING, required = false) String owner,
            @BizParam(name = "备注", type = BizField.FieldType.TEXT, required = false) String remark) {
        ZAssetDO asset = findByCode(assetCode);
        if (asset == null) throw new IllegalArgumentException("资产不存在：" + assetCode);

        AssetLifecycle from = parseStatus(asset.getStatus());
        if (!AssetLifecycleService.canTransit(from, targetStatus)) {
            throw new IllegalStateException(AssetLifecycleService.explain(from, targetStatus));
        }
        if (targetStatus == AssetLifecycle.ALLOCATED || targetStatus == AssetLifecycle.IN_USE) {
            if (owner == null || owner.isEmpty()) {
                throw new IllegalStateException("流转到 " + targetStatus + " 必须指定责任人");
            }
            asset.setOwner(owner);
        }

        asset.setStatus(targetStatus.name());
        if (remark != null && !remark.isEmpty()) {
            asset.setDescription(asset.getDescription() == null
                    ? remark : asset.getDescription() + " | " + remark);
        }
        asset.setGmtModified(new Date());
        assetMapper.updateById(asset);
        return asset;
    }

    /**
     * 查即将到期的资产：权益资产看 {@code expire_at}，实物资产看保修到期 {@code warranty_expire_at}。
     *
     * @param days 提前多少天算「即将到期」
     * @return 即将到期的资产
     */
    @BizMethod(name = "查即将到期的资产", module = "asset",
            description = "权益资产看到期日，实物资产看保修到期", operation = BizMethod.Operation.QUERY, idempotent = true)
    public List<ZAssetDO> listExpiring(
            @BizParam(name = "提前天数", type = BizField.FieldType.INTEGER, required = false) int days) {
        Instant deadline = Instant.now().plus(Duration.ofDays(days));
        Date limit = Date.from(deadline);
        return assetMapper.selectList(new LambdaQueryWrapper<ZAssetDO>()
                .and(w -> w.isNotNull(ZAssetDO::getExpireAt).le(ZAssetDO::getExpireAt, limit)
                        .or(o -> o.isNotNull(ZAssetDO::getWarrantyExpireAt)
                                .le(ZAssetDO::getWarrantyExpireAt, limit)))
                .orderByAsc(ZAssetDO::getExpireAt));
    }

    /**
     * 按大类统计。
     *
     * @return 大类 → 数量
     */
    @BizMethod(name = "按大类统计资产", module = "asset", operation = BizMethod.Operation.QUERY, idempotent = true)
    public java.util.Map<String, Long> countByCategory() {
        java.util.Map<String, Long> counts = new java.util.LinkedHashMap<>();
        for (AssetCategory category : AssetCategory.values()) counts.put(category.name(), 0L);
        for (ZAssetDO asset : assetMapper.selectList(null)) {
            String category = asset.getAssetCategory();
            if (category != null) counts.merge(category, 1L, Long::sum);
        }
        return counts;
    }

    /**
     * 按编码查资产。
     *
     * @param assetCode 资产编码
     * @return 资产，不存在返回 null
     */
    @BizMethod(name = "按编码查资产", module = "z-asset", description = "按编码查资产。", operation = BizMethod.Operation.QUERY)
    public ZAssetDO findByCode(String assetCode) {
        return assetMapper.selectOne(new LambdaQueryWrapper<ZAssetDO>()
                .eq(ZAssetDO::getAssetCode, assetCode));
    }

    private AssetLifecycle parseStatus(String status) {
        if (status == null) throw new IllegalStateException("资产状态缺失");
        try {
            return AssetLifecycle.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("未知状态：" + status);
        }
    }
}









