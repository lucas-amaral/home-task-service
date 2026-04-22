package com.amaral.hometask.repository

import com.amaral.hometask.model.FamilyConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FamilyConfigRepository : JpaRepository<FamilyConfig, Long>
