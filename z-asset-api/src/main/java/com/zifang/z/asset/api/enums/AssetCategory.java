package com.zifang.z.asset.api.enums;

/**
 * 资产大类。
 *
 * <p>「技术资源」（数据库 / 缓存 / 队列 / 存储桶）由各模块主动上报，2026-11-19 起归 z-meta 的
 * {@code z_resource} 表管；本仓只管**有归属、有生命周期、可能过期**的资产。</p>
 *
 * @author zifang
 * @since 1.1.0
 */
public enum AssetCategory {

    /** 实物资产：服务器、网络设备、办公设备等有形的东西，要采购、要领用、要归还、要报废。 */
    PHYSICAL,

    /** 权益资产：域名、SSL 证书、软件许可、云订阅等无形的权利，到期即失效。 */
    ENTITLEMENT;

    /**
     * 是否会过期。权益资产必有到期日，实物资产可能只有保修期。
     *
     * @return 是否会过期
     */
    public boolean expirable() {
        return this == ENTITLEMENT;
    }
}
