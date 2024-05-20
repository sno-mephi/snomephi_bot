package ru.idfedorov09.telegram.bot.repo

import org.springframework.data.jpa.repository.JpaRepository
import ru.idfedorov09.telegram.bot.data.model.Certificate

interface CertificateRepository : JpaRepository<Certificate, Long>
