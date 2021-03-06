package com.github.bookong.zest.core;

import java.lang.annotation.Annotation;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import com.github.bookong.zest.core.annotations.ZestJdbcConn;
import com.github.bookong.zest.core.testcase.TestCaseData;
import com.github.bookong.zest.core.testcase.ZestTestParam;

/**
 * @author jiangxu
 */
public abstract class AbstractStatement extends Statement {

    private final Launcher launcher;
    private final Object   target;

    public AbstractStatement(Launcher launcher, Object target){
        this.launcher = launcher;
        this.target = target;
    }

    public void invokeMethod(FrameworkMethod method) throws Throwable {
        method.invokeExplosively(target, genParamArrayOfObject(method));
    }

    /** 产生调用方法的参数数组 */
    protected Object[] genParamArrayOfObject(FrameworkMethod method) throws Exception {
        Class<?>[] paramClasses = method.getMethod().getParameterTypes();
        Annotation[][] allParamAnnotations = method.getMethod().getParameterAnnotations();
        Object[] paramArrayOfObject = new Object[paramClasses.length];
        for (int i = 0; i < paramClasses.length; i++) {
            paramArrayOfObject[i] = genParamObject(launcher, paramClasses[i], allParamAnnotations[i]);
        }
        return paramArrayOfObject;
    }

    private Object genParamObject(Launcher launcher, Class<?> paramClass, Annotation[] paramAnnotations) throws Exception {
        for (Annotation item : paramAnnotations) {
            if (item instanceof ZestJdbcConn) {
                ZestJdbcConn zestJdbcConn = (ZestJdbcConn) item;
                return launcher.getJdbcConn(zestJdbcConn.value());
            }
        }

        if (ZestTestParam.class.isAssignableFrom(paramClass)) {
            return launcher.getTestCaseData().getTestParam();
        } else if (TestCaseData.class.isAssignableFrom(paramClass)) {
            return launcher.getTestCaseData();
        } else {
            throw new RuntimeException("Unknown test param class " + paramClass.getName());
        }

    }

    public Launcher getLauncher() {
        return launcher;
    }

    public Object getTarget() {
        return target;
    }

}
