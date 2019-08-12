package com.asiainfo.framework.aspectj;

import java.lang.reflect.Method;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import com.asiainfo.common.annotation.DataScope;
import com.asiainfo.common.core.domain.BaseEntity;
import com.asiainfo.common.utils.StringUtils;
import com.asiainfo.framework.util.ShiroUtils;
import com.asiainfo.system.domain.SysRole;
import com.asiainfo.system.domain.SysUser;

/**
 * 数据过滤处理
 * 
 * @author yichen
 */
@Aspect
@Component
public class DataScopeAspect
{
    /**
     * 全部数据权限
     */
    public static final String DATA_SCOPE_ALL = "1";

    /**
     * 自定数据权限
     */
    public static final String DATA_SCOPE_CUSTOM = "2";

    /**
     * 数据权限过滤关键字
     */
    public static final String DATA_SCOPE = "dataScope";

    // 配置织入点
    @Pointcut("@annotation(com.asiainfo.common.annotation.DataScope)")
    public void dataScopePointCut()
    {
    }

    @Before("dataScopePointCut()")
    public void doBefore(JoinPoint point) throws Throwable
    {
        handleDataScope(point);
    }

    protected void handleDataScope(final JoinPoint joinPoint)
    {
        // 获得注解
        DataScope controllerDataScope = getAnnotationLog(joinPoint);
        if (controllerDataScope == null)
        {
            return;
        }
        // 获取当前的用户
        SysUser currentUser = ShiroUtils.getSysUser();
        if (currentUser != null)
        {
            // 如果是超级管理员，则不过滤数据
            if (!currentUser.isAdmin())
            {
                dataScopeFilter(joinPoint, currentUser, controllerDataScope.tableAlias());
            }
        }
    }

    /**
     * 数据范围过滤
     * @param joinPoint 切点
     * @param user 用户
     * @param alias 别名
     */
    public static void dataScopeFilter(JoinPoint joinPoint, SysUser user, String alias)
    {
        StringBuilder sqlString = new StringBuilder();

        for (SysRole role : user.getRoles())
        {
            String dataScope = role.getDataScope();
            if (DATA_SCOPE_ALL.equals(dataScope))
            {
                sqlString = new StringBuilder();
                break;
            }
            else if (DATA_SCOPE_CUSTOM.equals(dataScope))
            {
                String s = StringUtils.format(
                        " OR {}.dept_id IN ( SELECT dept_id FROM sys_role_dept WHERE role_id = {} ) ", alias,
                        role.getRoleId());
                sqlString.append(s);
            }
        }

        if (StringUtils.isNotBlank(sqlString.toString()))
        {
            BaseEntity baseEntity = (BaseEntity) joinPoint.getArgs()[0];
            String s = " AND (" + sqlString.substring(4) + ")";
            baseEntity.getParams().put(DATA_SCOPE, s);
        }
    }

    /**
     * 是否存在注解，如果存在就获取
     */
    private DataScope getAnnotationLog(JoinPoint joinPoint)
    {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();

        if (method != null)
        {
            return method.getAnnotation(DataScope.class);
        }
        return null;
    }
}
