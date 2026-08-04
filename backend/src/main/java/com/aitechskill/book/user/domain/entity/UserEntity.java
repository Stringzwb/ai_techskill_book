package com.aitechskill.book.user.domain.entity;

import com.aitechskill.book.auth.domain.enums.AuthProvider;
import com.aitechskill.book.common.domain.entity.BaseEntity;
import com.aitechskill.book.user.domain.enums.MemberLevel;
import com.aitechskill.book.user.domain.enums.UserRole;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 系统用户实体。
 */
@Getter
@Setter
@ToString(exclude = "passwordHash")
@TableName("sys_user")
public class UserEntity extends BaseEntity {

    /** 用户主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名。 */
    private String username;

    /** 手机号。 */
    private String phone;

    /** 邮箱。 */
    private String email;

    /** BCrypt 密码摘要。 */
    private String passwordHash;

    /** 会员等级。 */
    private MemberLevel memberLevel;

    /** 用户角色。 */
    private UserRole userRole;

    /** 会员到期时间。 */
    private LocalDateTime memberExpireTime;

    /** 头像地址。 */
    private String avatarUrl;

    /** 头像对象存储键。 */
    private String avatarObjectKey;

    /** 认证来源。 */
    private AuthProvider authProvider;

    /** 微信 OpenID，预留。 */
    private String wechatOpenid;

    /** 微信 UnionID，预留。 */
    private String wechatUnionid;

    /** 最近登录时间。 */
    private LocalDateTime lastLoginTime;
}
