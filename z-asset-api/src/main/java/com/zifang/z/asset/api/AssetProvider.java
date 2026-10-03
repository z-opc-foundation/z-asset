package com.zifang.z.asset.api;

import java.util.List;
import java.util.Map;

/**
 * 资产提供者接口。
 * 各模块实现此接口，提供资产信息采集能力。
 *
 * @author zifang
 * @see AssetProviderRegistry
 * @see AssetCollector
 * @since 1.0.0
 */
public interface AssetProvider {
    /**
     * 获取模块名称。
     *
     * @return 模块名称
     */
    String getModule();

    /**
     * 获取资产类型。
     *
     * @return 资产类型
     */
    String getAssetType();

    /**
     * 获取资产列表。
     *
     * @return 资产列表
     */
    List<AssetDTO> listAssets();

    /**
     * 获取资产详情。
     *
     * @param assetCode 资产编码
     * @return 资产详情
     */
    AssetDTO getAssetDetail(String assetCode);

    /**
     * 检查资产健康状态。
     *
     * @return 是否健康
     */
    boolean isHealthy();

    /**
     * 资产数据传输对象。
     */
    class AssetDTO {
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
         * 属性信息
         */
        private Map<String, String> attributes;

        /**
         * 租户编码
         */
        private String tenantCode;

        /**
         * 域编码
         */
        private String domainCode;

        public AssetDTO() {
        }

        public AssetDTO(String assetCode, String assetName, String assetType, String module, String status) {
            this.assetCode = assetCode;
            this.assetName = assetName;
            this.assetType = assetType;
            this.module = module;
            this.status = status;
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

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, String> attributes) {
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

        @Override
        public String toString() {
            return "AssetDTO{" +
                    "assetCode='" + assetCode + '\'' +
                    ", assetName='" + assetName + '\'' +
                    ", assetType='" + assetType + '\'' +
                    ", module='" + module + '\'' +
                    ", status='" + status + '\'' +
                    ", endpoint='" + endpoint + '\'' +
                    ", spec='" + spec + '\'' +
                    ", attributes=" + attributes +
                    ", tenantCode='" + tenantCode + '\'' +
                    ", domainCode='" + domainCode + '\'' +
                    '}';
        }
    }
}
