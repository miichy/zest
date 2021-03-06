package com.github.bookong.zest.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注释在被测试的方法方，标识这个方法是一个 Zest 支持的测试方法。<br>
 * 自动匹配和测试类相似的目录结构下查找参数 value 指定的文件。 例如：测试类为 zest.TicketSreviceTest 测试方法为 testApplyTicket 那么自动匹配路径为: <br>
 * $SOURCE/target/test-classes/zest/datas/TicketSreviceTest/testApplyTicket<br>
 * 以包名和类名作为目录名，中间加了一层 datas 目录目的是避免警告(The type XXXX collides with a package)<br>
 * 从找到的这些文件中（xml 格式）解析测试用例需要的数据，循环执行被测试方法。<br>
 * 
 * @author jiangxu
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ZestTest {

    /** 测试用例文件，如果不设置，则默认认为在指定路径下所有文件都是有效的 xml 文件 */
    String value() default "";
}
