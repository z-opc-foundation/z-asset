package com.zifang.z.asset.web.config;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zifang.z.boot.datasource.starter.ModuleDataSourceTemplate;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@ComponentScan("com.zifang.z.asset")
@MapperScan(
        basePackages = "com.zifang.z.asset.core.domain.mapper",
        sqlSessionFactoryRef = "sqlSessionFactoryAsset"
)
public class AssetAutoConfiguration extends ModuleDataSourceTemplate {

    @Bean(name = "dataSourceAsset")
    public DataSource dataSourceAsset(Environment env) {
        return buildDataSource(env, "asset");
    }

    @Bean(name = "sqlSessionFactoryAsset")
    public SqlSessionFactory sqlSessionFactoryAsset(
            @org.springframework.beans.factory.annotation.Qualifier("dataSourceAsset")
            DataSource dataSourceAsset) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSourceAsset);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/**/*.xml"));
        return factoryBean.getObject();
    }
}
