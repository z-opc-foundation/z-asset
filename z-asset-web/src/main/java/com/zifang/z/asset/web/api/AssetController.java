package com.zifang.z.asset.web.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zifang.util.core.meta.Result;
import com.zifang.z.asset.api.AssetTransitionRequest;
import com.zifang.z.asset.api.enums.AssetLifecycle;
import com.zifang.z.asset.core.domain.entity.ZAssetDO;
import com.zifang.z.asset.core.domain.mapper.ZAssetMapper;
import com.zifang.z.asset.core.service.AssetService;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 企业资产台账控制器：资产清单、到期提醒、生命周期流转。
 *
 * <p>技术资源（数据库 / 缓存 / 队列 / 存储桶）不在这里 —— 那是各模块主动上报的运行态，
 * 归 z-meta 的资源台账。</p>
 *
 * @author zifang
 * @since 1.1.0
 */
@RestController
@RequestMapping("/api/asset")
public class AssetController {

    /** 资产数据访问接口。 */
    @Resource
    private ZAssetMapper assetMapper;

    /** 资产台账服务。 */
    @Resource
    private AssetService assetService;

    /**
     * 查询资产清单。
     *
     * @param category 资产大类（可选）
     * @param status   生命周期状态（可选）
     * @param owner    责任人（可选）
     * @return 资产列表
     */
    @GetMapping("/list")
    @BizApi(summary = "查资产清单", description = "按大类 / 状态 / 责任人过滤，按更新时间倒序", tags = {"资产"})
    public Result<List<ZAssetDO>> list(
            @BizParam(name = "资产大类", type = BizField.FieldType.ENUM, dictCode = "asset_category", required = false, position = BizParam.Position.QUERY) @RequestParam(required = false) String category,
            @BizParam(name = "生命周期状态", type = BizField.FieldType.ENUM, dictCode = "asset_lifecycle", required = false, position = BizParam.Position.QUERY) @RequestParam(required = false) String status,
            @BizParam(name = "责任人", type = BizField.FieldType.STRING, required = false, position = BizParam.Position.QUERY) @RequestParam(required = false) String owner) {
        LambdaQueryWrapper<ZAssetDO> qw = new LambdaQueryWrapper<>();
        if (category != null) qw.eq(ZAssetDO::getAssetCategory, category);
        if (status != null) qw.eq(ZAssetDO::getStatus, status);
        if (owner != null) qw.eq(ZAssetDO::getOwner, owner);
        qw.orderByDesc(ZAssetDO::getGmtModified);
        return Result.success(assetMapper.selectList(qw));
    }

    /**
     * 查单个资产。
     *
     * @param assetCode 资产编码
     * @return 资产详情
     */
    @GetMapping("/detail/{assetCode}")
    @BizApi(summary = "查单个资产", tags = {"资产"})
    public Result<ZAssetDO> detail(
            @BizParam(name = "资产编码", type = BizField.FieldType.STRING, position = BizParam.Position.PATH) @PathVariable String assetCode) {
        ZAssetDO asset = assetService.findByCode(assetCode);
        if (asset == null) return Result.<ZAssetDO>fail("资产不存在").code(400);
        return Result.success(asset);
    }

    /**
     * 即将到期的资产：权益资产看到期日，实物资产看保修到期。
     *
     * @param days 提前多少天（默认 30）
     * @return 即将到期的资产
     */
    @GetMapping("/expiring")
    @BizApi(summary = "查即将到期的资产", description = "权益资产看到期日，实物资产看保修到期", tags = {"资产", "到期提醒"})
    public Result<List<ZAssetDO>> expiring(
            @BizParam(name = "提前天数", type = BizField.FieldType.INTEGER, required = false, position = BizParam.Position.QUERY) @RequestParam(required = false, defaultValue = "30") int days) {
        return Result.success(assetService.listExpiring(days));
    }

    /**
     * 按大类统计资产数量。
     *
     * @return 大类 → 数量
     */
    @GetMapping("/summary")
    @BizApi(summary = "按大类统计资产数量", tags = {"资产"})
    public Result<Map<String, Long>> summary() {
        return Result.success(assetService.countByCategory());
    }

    /**
     * 流转资产生命周期状态。
     *
     * @param request 流转请求
     * @return 流转后的资产
     */
    @PostMapping("/transition")
    @BizApi(summary = "流转资产生命周期", description = "非法流转直接拒绝（400），不落库", tags = {"资产"})
    public Result<ZAssetDO> transition(
            @BizParam(name = "流转请求", type = BizField.FieldType.JSON, position = BizParam.Position.BODY) @RequestBody AssetTransitionRequest request) {
        try {
            AssetLifecycle target = AssetLifecycle.valueOf(request.getTargetStatus());
            return Result.success(assetService.transition(request.getAssetCode(), target,
                    request.getOwner(), request.getRemark()));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return Result.<ZAssetDO>fail(e.getMessage()).code(400);
        }
    }
}
