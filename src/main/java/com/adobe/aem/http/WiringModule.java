package com.adobe.aem.http;

import com.adobe.aem.http.connection.ConnectionHandler;
import com.adobe.aem.http.connection.ConnectionHandlerFactory;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class WiringModule extends AbstractModule {

    @Override
    protected void configure() {

        //allows a class to be injected even though it has contextual dependencies.
        install(new FactoryModuleBuilder()
                .implement(ConnectionHandler.class, ConnectionHandler.class)
                .build(ConnectionHandlerFactory.class));

    }
}
