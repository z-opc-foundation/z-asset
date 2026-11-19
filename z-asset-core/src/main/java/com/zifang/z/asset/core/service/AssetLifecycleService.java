package com.zifang.z.asset.core.service;

import com.zifang.z.asset.api.enums.AssetLifecycle;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 生命周期状态机：定义并校验资产状态的合法流转。
 *
 * <p>非法流转必须被拒绝 —— 否则「已报废的机器又被人领走了」这种事会一路流到账上，
 * 盘点时才发现，那就晚了。</p>
 *
 * @author zifang
 * @since 1.1.0
 */
public final class AssetLifecycleService {

    /** 合法流转表。 */
    private static final Map<AssetLifecycle, Set<AssetLifecycle>> TRANSITIONS = Map.of(
            AssetLifecycle.PURCHASED, EnumSet.of(AssetLifecycle.IN_STOCK, AssetLifecycle.RETIRED),
            AssetLifecycle.IN_STOCK, EnumSet.of(AssetLifecycle.ALLOCATED, AssetLifecycle.RETIRED),
            AssetLifecycle.ALLOCATED, EnumSet.of(AssetLifecycle.IN_USE,
                    AssetLifecycle.RETURNED, AssetLifecycle.RETIRED),
            AssetLifecycle.IN_USE, EnumSet.of(AssetLifecycle.RETURNED, AssetLifecycle.RETIRED),
            AssetLifecycle.RETURNED, EnumSet.of(AssetLifecycle.IN_STOCK, AssetLifecycle.RETIRED),
            AssetLifecycle.EXPIRED, EnumSet.noneOf(AssetLifecycle.class),
            AssetLifecycle.RETIRED, EnumSet.noneOf(AssetLifecycle.class));

    private AssetLifecycleService() {
    }

    /**
     * 该状态允许流转到的目标状态集合。
     *
     * @param from 当前状态
     * @return 允许的目标状态
     */
    public static Set<AssetLifecycle> allowedTargets(AssetLifecycle from) {
        Set<AssetLifecycle> targets = TRANSITIONS.get(from);
        return targets == null ? EnumSet.noneOf(AssetLifecycle.class) : targets;
    }

    /**
     * 校验一次流转是否合法。
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return 合法则返回 true
     */
    public static boolean canTransit(AssetLifecycle from, AssetLifecycle to) {
        if (from == null || to == null) return false;
        return allowedTargets(from).contains(to);
    }

    /**
     * 流转被拒时的说明文案。
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return 说明
     */
    public static String explain(AssetLifecycle from, AssetLifecycle to) {
        if (from == null || to == null) return "状态缺失";
        if (from.terminal()) return from + " 是终态，不可再流转";
        return from + " 不能直接流转到 " + to + "，允许的目标状态："
                + Arrays.toString(allowedTargets(from).toArray());
    }
}
