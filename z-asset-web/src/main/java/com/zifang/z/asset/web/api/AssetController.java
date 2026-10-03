package com.zifang.z.asset.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.asset.api.AssetProvider;
import com.zifang.z.asset.core.collector.AssetCollector;
import com.zifang.z.asset.core.collector.AssetProviderRegistry;
import com.zifang.z.asset.core.domain.entity.AssetEntity;
import com.zifang.z.asset.core.domain.mapper.AssetMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 资产管理控制器。
 * 提供资产采集、查询、提供者列表等功能。
 *
 * @author zifang
 * @see AssetProvider
 * @see AssetCollector
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/asset")
public class AssetController {
    /**
     * 资产提供者注册表。
     */
    @Resource
    private AssetProviderRegistry registry;

    /**
     * 资产采集器。
     */
    @Resource
    private AssetCollector collector;

    /**
     * 资产数据访问接口。
     */
    @Resource
    private AssetMapper assetMapper;

    /**
     * 采集所有资产。
     *
     * @return 采集结果
     */
    @PostMapping("/collect")
    public Result<String> collect() {
        collector.collectAll();
        return Result.success("ok");
    }

    /**
     * 查询资产列表。
     *
     * @param module    模块名称（可选）
     * @param assetType 资产类型（可选）
     * @return 资产列表
     */
    @GetMapping("/list")
    public Result<List<AssetEntity>> list(@RequestParam(required = false) String module,
                                          @RequestParam(required = false) String assetType) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AssetEntity> qw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (module != null) {
            qw.eq(AssetEntity::getModule, module);
        }

        if (assetType != null) {
            qw.eq(AssetEntity::getAssetType, assetType);
        }

        qw.orderByDesc(AssetEntity::getUpdatedTime);
        return Result.success(assetMapper.selectList(qw));
    }

    /**
     * 获取所有资产提供者。
     *
     * @return 资产提供者列表
     */
    @GetMapping("/providers")
    public Result<List<AssetProvider>> providers() {
        return Result.success(registry.getAllProviders());
    }
}
