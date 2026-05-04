package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.MqFailedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MqFailedMessageRepository extends JpaRepository<MqFailedMessage, Long> {}
