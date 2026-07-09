package cl.duoc.guias;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.cloud.aws.region.static=us-east-1",
		"spring.cloud.aws.credentials.access-key=test",
		"spring.cloud.aws.credentials.secret-key=test",
		"aws.s3.bucket=test-bucket",
		"app.efs.path=./target/efs-test",
		"app.security.enabled=false",
		"spring.rabbitmq.listener.simple.auto-startup=false"
})
class GuiasDespachoApplicationTests {

	@Test
	void contextLoads() {
	}
}
