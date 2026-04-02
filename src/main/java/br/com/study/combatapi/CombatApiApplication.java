package br.com.study.combatapi;

import br.com.study.genericauthorization.configuration.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableFeignClients(basePackages = {
        "br.com.study.combatapi",
        "br.com.study.genericauthorization"
})
public class CombatApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CombatApiApplication.class, args);
    }
}