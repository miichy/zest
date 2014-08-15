package com.github.bookong.zest;

import java.sql.Connection;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.bookong.zest.core.ZestLauncher;
import com.github.bookong.zest.core.annotations.ZestTest;

/**
 * @author jiangxu
 *
 */
public class ZestSpringJUnit4ClassRunner extends SpringJUnit4ClassRunner implements ZestClassRunner{
	protected static final Log logger = LogFactory.getLog(ZestSpringJUnit4ClassRunner.class);
	protected ZestLauncher zestLauncher = new ZestLauncher();

	public ZestSpringJUnit4ClassRunner(Class<?> clazz) throws InitializationError {
		super(clazz);
		zestLauncher.junit4ClassRunnerConstructor(getTestClass(), this);
	}

	@Override
	protected void collectInitializationErrors(List<Throwable> errors) {
		super.collectInitializationErrors(errors);
		ZestLauncher.junit4ClassRunnerCollectInitializationErrors(errors);
	}

	@Override
	protected List<FrameworkMethod> getChildren() {
		return zestLauncher.junit4ClassRunnerGetChildren(computeTestMethods());
	}

	@Override
	protected void runChild(FrameworkMethod frameworkMethod, RunNotifier notifier) {
		ZestTest ztest = frameworkMethod.getAnnotation(ZestTest.class);
		if (ztest == null) {
			super.runChild(frameworkMethod, notifier);
		} else {
			zestLauncher.junit4ClassRunnerRunChild(frameworkMethod, notifier);
		}
	}

	@Override
	public Connection getConnection(DataSource dataSource) {
		return DataSourceUtils.getConnection(dataSource);
	}
	
	@Override
	public Object createTest() throws Exception {
		return super.createTest();
	}
}
