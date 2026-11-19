package com.zifang.z.asset.core;

import com.zifang.z.asset.api.enums.AssetLifecycle;
import com.zifang.z.asset.core.service.AssetLifecycleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 生命周期状态机的自守卫：合法流转放行，非法流转拒绝。
 */
class AssetLifecycleServiceTest {

    @Test
    @DisplayName("正常链路：采购→入库→领用→在用→归还→入库")
    void normalChainIsAllowed() {
        assertTrue(AssetLifecycleService.canTransit(AssetLifecycle.PURCHASED, AssetLifecycle.IN_STOCK));
        assertTrue(AssetLifecycleService.canTransit(AssetLifecycle.IN_STOCK, AssetLifecycle.ALLOCATED));
        assertTrue(AssetLifecycleService.canTransit(AssetLifecycle.ALLOCATED, AssetLifecycle.IN_USE));
        assertTrue(AssetLifecycleService.canTransit(AssetLifecycle.IN_USE, AssetLifecycle.RETURNED));
        assertTrue(AssetLifecycleService.canTransit(AssetLifecycle.RETURNED, AssetLifecycle.IN_STOCK));
    }

    @Test
    @DisplayName("报废是终态，不可再流转")
    void retiredIsTerminal() {
        assertTrue(AssetLifecycle.RETIRED.terminal());
        assertFalse(AssetLifecycleService.canTransit(AssetLifecycle.RETIRED, AssetLifecycle.IN_STOCK));
        assertFalse(AssetLifecycleService.canTransit(AssetLifecycle.RETIRED, AssetLifecycle.IN_USE));
    }

    @Test
    @DisplayName("过期是权益资产的终态")
    void expiredIsTerminal() {
        assertFalse(AssetLifecycleService.canTransit(AssetLifecycle.EXPIRED, AssetLifecycle.IN_USE));
        assertTrue(AssetLifecycleService.allowedTargets(AssetLifecycle.EXPIRED).isEmpty());
    }

    @Test
    @DisplayName("不能跳过领用直接从入库到在用")
    void cannotSkipAllocation() {
        assertFalse(AssetLifecycleService.canTransit(AssetLifecycle.IN_STOCK, AssetLifecycle.IN_USE));
    }

    @Test
    @DisplayName("不能从在用直接回在库")
    void cannotJumpBackToStock() {
        assertFalse(AssetLifecycleService.canTransit(AssetLifecycle.IN_USE, AssetLifecycle.IN_STOCK));
    }

    @Test
    @DisplayName("拒掉的流转要给出可读理由")
    void rejectionExplainsWhy() {
        String why = AssetLifecycleService.explain(AssetLifecycle.IN_STOCK, AssetLifecycle.IN_USE);
        assertTrue(why.contains("IN_STOCK") && why.contains("IN_USE"), why);
        assertTrue(AssetLifecycleService.explain(AssetLifecycle.RETIRED, AssetLifecycle.IN_USE).contains("终态"));
    }
}
