/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.core.thread;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.service.EPRuntimeImpl;
import com.espertech.esper.core.service.EPServicesContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inbound work unit processing a map event.
 */
public class InboundUnitSendObjectArray implements InboundUnitRunnable
{
    private static final Logger log = LoggerFactory.getLogger(InboundUnitSendObjectArray.class);
    private final Object[] properties;
    private final String eventTypeName;
    private final EPServicesContext services;
    private final EPRuntimeImpl runtime;

    /**
     * Ctor.
     * @param properties to send
     * @param eventTypeName type name
     * @param services to wrap
     * @param runtime to process
     */
    public InboundUnitSendObjectArray(Object[] properties, String eventTypeName, EPServicesContext services, EPRuntimeImpl runtime)
    {
        this.eventTypeName = eventTypeName;
        this.properties = properties;
        this.services = services;
        this.runtime = runtime;
    }

    public void run()
    {
        try
        {
            EventBean eventBean = services.getEventAdapterService().adapterForObjectArray(properties, eventTypeName);
            runtime.processWrappedEvent(eventBean);
        }
        catch (RuntimeException e)
        {
            log.error("Unexpected error processing Object-array event: " + e.getMessage(), e);
        }
    }
}
