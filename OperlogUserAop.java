package com.hnqj.aop;

import com.hnqj.controller.OperAnnotation;
import com.hnqj.core.GetUserIP;
import com.hnqj.core.PageData;
import com.hnqj.model.OperlogUser;
import com.hnqj.model.Sysusermgr;
import com.hnqj.services.OperLogUserServices;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

//@Aspect
//@Component
public class OperlogUserAop {
    //注入service,用来将日志信息保存在数据库
    @Autowired
    private OperLogUserServices operLogUserServices;

    //配置接入点,如果不知道怎么配置,可以百度一下规则
    @Pointcut("(!execution(* com.hnqj.controller.loginController.*(..)))&&" +
            "(execution(* com.hnqj.controller..*.*(..)))")
    private void controllerAspect(){}//定义一个切入点

    @Around("controllerAspect()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {

        Object object = null;
        //常见日志实体对象
        OperlogUser log = new OperlogUser();
        PageData pd=new PageData();
        String uid=UUID.randomUUID().toString();
        pd.put("uid",uid);
        //获取登录用户账户
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Sysusermgr user = (Sysusermgr) request.getSession().getAttribute("user");

            pd.put("loguserid", user.getUid());
            pd.put("logusername", user.getFristname());
//        log.setLoguserid(user.getUid());
//        log.setLogusername(user.getFristname());
            //获取系统时间
            String time = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(new Date());

            pd.put("logcreatetime", new Date());
//        log.setLogcreatetime(new Date());

            //获取系统ip
            String ip = GetUserIP.getIpAddr(request);
            pd.put("logip", ip);

//        log.setLogip(ip);


            // 拦截的实体类，就是当前正在执行的controller
            Object target = pjp.getTarget();
            // 拦截的方法名称。当前正在执行的方法
            String methodName = pjp.getSignature().getName();
            // 拦截的方法参数
            Object[] args = pjp.getArgs();
            // 拦截的参数类型
            Signature sig = pjp.getSignature();
            MethodSignature msig = null;
            if (!(sig instanceof MethodSignature)) {
                throw new IllegalArgumentException("该注解只能用于方法");
            }
            msig = (MethodSignature) sig;
            Class[] parameterTypes = msig.getMethod().getParameterTypes();


            // 获得被拦截的方法
            Method method = null;
            try {
                method = target.getClass().getMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (SecurityException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            if (null != method) {
                // 判断是否包含自定义的注解，说明一下这里的SystemLog就是我自己自定义的注解
                if (method.isAnnotationPresent(OperAnnotation.class)) {
                    OperAnnotation operAnnotation = method.getAnnotation(OperAnnotation.class);
                    pd.put("logmodule",operAnnotation.moduleName());
                    pd.put("logdesc",operAnnotation.option());

//                    log.setLogmodule(operAnnotation.moduleName());//模块名
//                    log.setLogdesc(operAnnotation.option());//模块内容
                    try {
                        object = pjp.proceed();
                        pd.put("logresult", "执行成功！");
//                    log.setLogresult("执行成功！");
                        //保存进数据库
                        operLogUserServices.addOperLogUser(pd);
                    } catch (Throwable e) {
                        // TODO Auto-generated catch block
                        long end = System.currentTimeMillis();
                        pd.put("logresult", "执行失败");
//                    log.setLogresult("执行失败");
                        operLogUserServices.addOperLogUser(pd);
//                    logservice.saveLog(log);
                    }
                } else {//没有包含注解
                    object = pjp.proceed();
                }
            } else { //不需要拦截直接执行
                object = pjp.proceed();
            }

        return object;
    }
}