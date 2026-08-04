package com.aitechskill.book.user.mapper;

import com.aitechskill.book.user.domain.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户数据访问接口。
 */
public interface UserMapper extends BaseMapper<UserEntity> {

    /**
     * 按用户名、手机号或邮箱查询登录用户。
     *
     * @param account 登录账号
     * @return 用户实体
     */
    UserEntity selectByLoginAccount(@Param("account") String account);
}
