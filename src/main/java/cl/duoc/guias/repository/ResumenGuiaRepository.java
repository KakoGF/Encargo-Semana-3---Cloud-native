package cl.duoc.guias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.duoc.guias.model.ResumenGuia;

@Repository
public interface ResumenGuiaRepository extends JpaRepository<ResumenGuia, Long> {

	boolean existsByNumeroGuia(String numeroGuia);
}
