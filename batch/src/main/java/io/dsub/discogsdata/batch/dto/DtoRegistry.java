package io.dsub.discogsdata.batch.dto;

import io.dsub.discogsdata.common.exception.UnsupportedOperationException;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class DtoRegistry implements InitializingBean {
    public static final String BASE_DTO_PACKAGE = "io.dsub.discogsdata.batch";

    private final Map<Class<? extends BaseDTO>, BaseDTO> registry = new ConcurrentHashMap<>();

    private final AtomicInteger size = new AtomicInteger();

    public DtoRegistry() {
        init();
    }

    public void init() {
        for (Class<? extends BaseDTO> aClass : new Reflections(BASE_DTO_PACKAGE).getSubTypesOf(BaseDTO.class)) {
            registerInstance(aClass);
        }

    }

    public int getSize() {
        return size.get();
    }

    public List<? extends BaseDTO> getAllInstances() {
        return List.copyOf(registry.values());
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseDTO> T getInstanceOf(Class<T> clazz) {
        try {
            return (T) registry.get(clazz).clone();
        } catch (CloneNotSupportedException ignored) {
            return null;
        }
    }

    private <T extends BaseDTO> void registerInstance(Class<T> clazz) {

        if (!checkInherited(clazz)) {
            throw new UnsupportedOperationException("class " + clazz.getSimpleName() + " is does not inherit " + BaseDTO.class);
        }

        if (isAbstractClassOrInterface(clazz)) {
            return;
        }

        T instance = buildInstanceOf(clazz);

        if (instance == null) {
            throw new UnsupportedOperationException("failed to instantiate class " + clazz.getSimpleName());
        }

        size.incrementAndGet();
        registry.put(clazz, instance);
    }

    private boolean isAbstractClassOrInterface(Class<?> clazz) {
        int mod = clazz.getModifiers();
        return Modifier.isAbstract(mod) || Modifier.isInterface(mod);
    }

    private boolean checkInherited(Class<?> toExamine) {
        return BaseDTO.class.isAssignableFrom(toExamine);
    }

    private <T extends BaseDTO> T buildInstanceOf(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getConstructor();
            if (constructor.trySetAccessible()) {
                return constructor.newInstance();
            }
            return null;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }
}
