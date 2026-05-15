import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
	java
	id("org.springframework.boot") version "4.0.0"
	id("io.spring.dependency-management") version "1.1.7"
}

val mainSourceSet = sourceSets.main.get()

group = "com.king"
version = "0.0.1-SNAPSHOT"
description = "Learn Spring Boot"

springBoot {
    mainClass.set("com.king.SpringBoot")
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webmvc")
  implementation("org.springframework.boot:spring-boot-starter-restclient")
  implementation("org.springframework.boot:spring-boot-starter-data-rest")
  implementation("org.dom4j:dom4j:2.1.4")
	developmentOnly("org.springframework.boot:spring-boot-devtools")

//    implementation("org.springframework.data:spring-data-geode")
//    implementation("org.projectlombok:lombok")

//    runtimeOnly("org.springframework.shell:spring-shell:1.2.0.RELEASE")

//    testImplementation("org.springframework.boot:spring-boot-starter-test")
//    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
//	implementation("org.springframework.boot:spring-boot-starter")
//	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.register<JavaExec>("runXmlBeanDemo") {
	group = "application"
	description = "Run the XML bean demo"
	classpath = mainSourceSet.runtimeClasspath
	mainClass.set("com.king.learn.XmlBeanDemo")
}

tasks.register<JavaExec>("runXmlBeanCollections") {
	group = "application"
	description = "Run the XML bean collections demo"
	classpath = mainSourceSet.runtimeClasspath
	mainClass.set("com.king.learn.XmlBeanCollections")
}

tasks.register<JavaExec>("uploadFile") {
    group = "application"
    description = "Run the file upload demo"
    classpath = mainSourceSet.runtimeClasspath
    mainClass.set("com.king.learn.uploading_files.storage.UploadFilesApplication")
}

tasks.register<JavaExec>("learnAop") {
    group = "application"
    description = "Run the AOP demo"
    classpath = mainSourceSet.runtimeClasspath
	mainClass.set("com.learn.aop.TestAop")
}

tasks.register<JavaExec>("learnSpringBoot") {
    group = "application"
    description = "Run the Spring Boot demo"
    classpath = mainSourceSet.runtimeClasspath
    mainClass.set("com.king.SpringBoot")
}

