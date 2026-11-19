package com.zifang.z.asset.api.enums;

/**
 * 资产生命周期状态。
 *
 * <p>状态机由 {@code AssetLifecycleService} 守：非法跳转直接拒绝，
 * 否则「谁把一台已报废的机器又领出去了」这种账就再也对不上了。</p>
 *
 * @author zifang
 * @since 1.1.0
 */
public enum AssetLifecycle {

    /** 已采购未入库。 */
    PURCHASED,
    /** 在库（待领用）。 */
    IN_STOCK,
    /** 已领用未启用。 */
    ALLOCATED,
    /** 在用。 */
    IN_USE,
    /** 已归还。 */
    RETURNED,
    /** 已过期（仅权益资产）。 */
    EXPIRED,
    /** 已报废（终态）。 */
    RETIRED;

    /**
     * 是否为终态：不能再流转出去。
     *
     * @return 是否终态
     */
    public boolean terminal() {
        return this == RETIRED;
    }
}
