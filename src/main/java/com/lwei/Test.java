package com.lwei;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;


public class Test {

	public static void main(String[] args) throws IOException, InterruptedException {
		IUserDao target = new UserDao();
		System.out.println(target.getClass());  //输出目标对象信息
		IUserDao proxy = (IUserDao) new ProxyFactory(target).getProxyInstance();
		System.out.println(proxy.getClass());  //输出代理对象信息
		proxy.save();  //执行代理方法
		proxy.delete();

	}

}


interface IUserDao {
	public void save();

	void delete();
}


class UserDao implements IUserDao{

	@Override
	public void save() {
		System.out.println("保存数据");
	}

	@Override
	public void delete() {
		System.out.println("删除数据");
	}
}

class ProxyFactory {

	private Object target;// 维护一个目标对象

	public ProxyFactory(Object target) {
		this.target = target;
	}

	// 为目标对象生成代理对象
	public Object getProxyInstance() {
		return Proxy.newProxyInstance(target.getClass().getClassLoader(), target.getClass().getInterfaces(),
				new InvocationHandler() {

					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						System.out.println("开启事务");

						// 执行目标对象方法
						Object returnValue = method.invoke(target, args);

						System.out.println("提交事务");
						return null;
					}
				});
	}
}