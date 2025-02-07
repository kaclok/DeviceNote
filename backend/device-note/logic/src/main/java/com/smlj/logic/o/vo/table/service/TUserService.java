package com.smlj.logic.o.vo.table.service;

import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import com.smlj.logic.config.db.EDatasource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@DS(EDatasource.main)
public class TUserService {
}
