package com.college.OnlineCourseManagement.LearningPlatform;

import com.college.OnlineCourseManagement.LearningPlatform.config.DotEnv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class Application {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(Application.class);
		application.setEnvironment(DotEnv.environmentWithEnvFile());
		application.run(args);
	}

}