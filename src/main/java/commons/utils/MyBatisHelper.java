package commons.utils;

import java.util.List;
import java.lang.reflect.Method;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

public class MyBatisHelper {

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static <T> void registerEnumHandler(TypeHandlerRegistry register, Class handlerType, String packageName) {
    List<Class<? extends Enum<?>>> enums = ReflectUtil.findEnums(packageName);
    for (Class<? extends Enum<?>> clazz : enums) {
      try {
        Method method = clazz.getMethod("getValue");
        if (method.getReturnType() != int.class) continue;
      } catch (Exception e) {
        continue;
      }
      try {
        TypeHandler typeHandler = (TypeHandler) handlerType.getConstructor(Class.class).newInstance(clazz);
        // we must appoint JavaType
        register.register(clazz, typeHandler);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
