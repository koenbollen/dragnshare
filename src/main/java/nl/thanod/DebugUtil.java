/**
 * 
 */
package nl.thanod;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * @author nilsdijk
 *
 */
public class DebugUtil {
	public static <T> T getPrintingImplementation(Class<T> iface){
		return (T)Proxy.newProxyInstance(DebugUtil.class.getClassLoader(), new Class[]{iface}, new InvocationHandler() {
			
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				System.out.println(method.getName() + '(' + Arrays.toString(args) + ')');
				return null;
			}
		});
	}
}
