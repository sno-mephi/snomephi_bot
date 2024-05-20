package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.idfedorov09.telegram.bot.data.model.Certificate

interface CertificateRepository : JpaRepository<Certificate, Long> {

    fun findByCandidateOwnerId(candidateOwnerId: Long): Certificate?

    fun findByCertificateOwnerId(certificateOwnerId: Long): Certificate?

   @Query(
        """
            SELECT * FROM certificates_table
            WHERE 1 = 1
                AND poll_start_dttm IS NOT NULL
                AND poll_start_dttm <= TIMEZONE('Europe/Moscow', CURRENT_TIMESTAMP) - interval '1 hour'
            ORDER BY certificate_id
            LIMIT 1
        """,
        nativeQuery = true,
    )
    fun findFirstExpiredCertificate(): Certificate?
}
