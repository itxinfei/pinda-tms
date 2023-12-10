package com.itheima.pinda.authority.biz.dao.defaults;

import com.baomidou.mybatisplus.annotation.SqlParser;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 初始化数据库DAO
 *
 */
@Repository
@SqlParser(filter = true)
public interface InitDbMapper {
    /**
     * 创建数据库
     *
     * @param database
     * @return
     */
    int createDatabase(@Param("database") String database);

}
