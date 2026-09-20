package cn.yzfy.crushcupidserver;

import cn.yzfy.crushcupidserver.agent.proactive.ProactiveProperties;
import cn.yzfy.crushcupidserver.agent.report.ReportProperties;
import cn.yzfy.crushcupidserver.config.OssProperties;
import cn.yzfy.crushcupidserver.config.UploadProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("cn.yzfy.crushcupidserver.mapper")
@EnableScheduling
@EnableConfigurationProperties({ProactiveProperties.class, ReportProperties.class, UploadProperties.class, OssProperties.class})
public class CrushCupidServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrushCupidServerApplication.class, args);
    }

}
