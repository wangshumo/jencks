/**
 * 
 * Copyright 2005 LogicBlaze, Inc. http://www.logicblaze.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.jencks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.transaction.manager.GeronimoTransactionManager;
import org.apache.geronimo.connector.work.GeronimoWorkManager;
import org.jencks.factory.BootstrapContextFactoryBean;
import org.jencks.factory.GeronimoDefaults;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.work.WorkManager;

/**
 * Represents a base JCA container which has no dependency on Geronimo
 * and requires a mandatory {@link BootstrapContext} and {@link ResourceAdapter}
 * properties to be configured.
 * <p/>
 * Typically Spring users will use the {@link BootstrapContextFactoryBean} to create
 * the {@link BootstrapContext} instance, with the work manager and transaction manager.
 *
 * @version $Revision$
 * @org.apache.xbean.XBean element="jcaContainer"
 */
public class JCAContainer implements InitializingBean, DisposableBean, ApplicationContextAware {
    private static final Log log = LogFactory.getLog(JCAContainer.class);
    private BootstrapContext bootstrapContext;
    private ResourceAdapter resourceAdapter;
    private ApplicationContext applicationContext;
    private boolean lazyLoad = false;

    // optional - used to create bootstrap context when not specified
    private GeronimoTransactionManager transactionManager;
    private WorkManager workManager;
    private boolean createdWorkManager;

    // optional - used to create work manager when bootstrap context and work manager are not specified
    private int threadPoolSize;

    public JCAContainer() {
    }

    public JCAConnector addConnector() {
        return new JCAConnector(getBootstrapContext(), getResourceAdapter());
    }

    public void afterPropertiesSet() throws Exception {
        if (resourceAdapter == null) {
            throw new IllegalArgumentException("resourceAdapter must be set");
        }
        if (bootstrapContext == null) {
            if (transactionManager == null) {
                throw new IllegalArgumentException("bootstrapContext or transactionManager must be set");
            }

            if (workManager == null) {
                workManager = GeronimoDefaults.createWorkManager(transactionManager, threadPoolSize);
                createdWorkManager = true;
            }
            bootstrapContext = GeronimoDefaults.createBootstrapContext(transactionManager, workManager);
        }

        resourceAdapter.start(bootstrapContext);

        // now lets start all of the JCAConnector instances
        if (!lazyLoad) {
            if (applicationContext == null) {
                throw new IllegalArgumentException("applicationContext should have been set by Spring");
            }
            applicationContext.getBeansOfType(JCAConnector.class);
        }

        String version = null;
        Package aPackage = Package.getPackage("org.jencks");
        if (aPackage != null) {
            version = aPackage.getImplementationVersion();
        }

        log.info("Jencks JCA Container (http://jencks.org/) has started running version: " + version);
    }

    public void destroy() throws Exception {
        if (resourceAdapter != null) {
            resourceAdapter.stop();
        }
        if (createdWorkManager && workManager instanceof GeronimoWorkManager) {
            GeronimoWorkManager geronimoWorkManager = (GeronimoWorkManager) workManager;
            geronimoWorkManager.doStop();
            geronimoWorkManager = null;
        }
    }

    // Properties
    //-------------------------------------------------------------------------
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    public void setResourceAdapter(ResourceAdapter resourceAdapter) {
        this.resourceAdapter = resourceAdapter;
    }

    public BootstrapContext getBootstrapContext() {
        return bootstrapContext;
    }

    public void setBootstrapContext(BootstrapContext bootstrapContext) {
        this.bootstrapContext = bootstrapContext;
    }

    public boolean isLazyLoad() {
        return lazyLoad;
    }

    public void setLazyLoad(boolean lazyLoad) {
        this.lazyLoad = lazyLoad;
    }

    public GeronimoTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(GeronimoTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public WorkManager getWorkManager() {
        return workManager;
    }

    public void setWorkManager(WorkManager workManager) {
        this.workManager = workManager;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }
}
