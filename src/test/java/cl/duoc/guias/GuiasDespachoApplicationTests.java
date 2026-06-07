package cl.duoc.guias;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifica que el contexto de Spring levante correctamente.
 * Se entregan valores de prueba para region/credenciales/bucket de AWS y una
 * ruta local para el EFS, de modo que el cliente S3 se cree de forma diferida
 * sin requerir credenciales reales ni conexion a AWS al arrancar.
 */
@SpringBootTest(properties = {
		"spring.cloud.aws.region.static=us-east-1",
		"spring.cloud.aws.credentials.access-key=test",
		"spring.cloud.aws.credentials.secret-key=test",
		"aws.s3.bucket=test-bucket",
		"app.efs.path=./target/efs-test"
})
class GuiasDespachoApplicationTests {

	@Test
	void contextLoads() {
	}
}
