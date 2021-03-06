package org.wordpress.android;

import org.wordpress.android.mocks.OAuthAuthenticatorFactoryTest;
import org.wordpress.android.mocks.RestClientFactoryTest;
import org.wordpress.android.mocks.SystemServiceFactoryTest;
import org.wordpress.android.mocks.XMLRPCFactoryTest;
import org.codeforafrica.citizenreporter.starreports.networking.OAuthAuthenticatorFactory;
import org.codeforafrica.citizenreporter.starreports.networking.RestClientFactory;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.SystemServiceFactory;
import org.xmlrpc.android.XMLRPCFactory;

import java.lang.reflect.Field;

public class FactoryUtils {
    public static void initWithTestFactories() {
        // create test factories
        forceFactoryInjection(XMLRPCFactory.class, new XMLRPCFactoryTest());
        forceFactoryInjection(RestClientFactory.class, new RestClientFactoryTest());
        forceFactoryInjection(OAuthAuthenticatorFactory.class, new OAuthAuthenticatorFactoryTest());
        forceFactoryInjection(SystemServiceFactory.class, new SystemServiceFactoryTest());
        AppLog.v(T.TESTS, "Mocks factories instantiated");
    }

    private static void forceFactoryInjection(Class klass, Object factory) {
        try {
            Field field = klass.getDeclaredField("sFactory");
            field.setAccessible(true);
            field.set(null, factory);
            AppLog.v(T.TESTS, "Factory " + klass + " injected");
        } catch (Exception e) {
            AppLog.e(T.TESTS, "Can't inject test factory " + klass);
        }
    }
}
