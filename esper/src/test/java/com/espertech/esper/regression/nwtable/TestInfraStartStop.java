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

package com.espertech.esper.regression.nwtable;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.epl.named.NamedWindowLifecycleEvent;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_A;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.epl.SupportNamedWindowObserver;
import junit.framework.TestCase;

public class TestInfraStartStop extends TestCase
{
    private EPServiceProviderSPI epService;
    private SupportUpdateListener listenerWindow;
    private SupportUpdateListener listenerSelect;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = (EPServiceProviderSPI) EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        listenerWindow = new SupportUpdateListener();
        listenerSelect = new SupportUpdateListener();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listenerWindow = null;
        listenerSelect = null;
    }

    public void testStartStopDeleter() {
        runAssertionStartStopDeleter(true);
        runAssertionStartStopDeleter(false);
    }

    public void testStartStopConsumer() {
        runAssertionStartStopConsumer(true);
        runAssertionStartStopConsumer(false);
    }

    public void testStartStopInserter() {
        runAssertionStartStopInserter(true);
        runAssertionStartStopInserter(false);
    }

    private void runAssertionStartStopInserter(boolean namedWindow)
    {
        // create window
        String stmtTextCreate = namedWindow ?
                "create window MyInfra#keepall() as select theString as a, intPrimitive as b from " + SupportBean.class.getName() :
                "create table MyInfra(a string primary key, b int primary key)";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyInfra select theString as a, intPrimitive as b from " + SupportBean.class.getName();
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create consumer
        String[] fields = new String[] {"a", "b"};
        String stmtTextSelect = "select a, b from MyInfra as s1";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtTextSelect);
        stmtSelect.addListener(listenerSelect);

        // send 1 event
        sendSupportBean("E1", 1);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
            EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
        }
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}});

        // stop inserter
        stmtInsert.stop();
        sendSupportBean("E2", 2);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerSelect.isInvoked());

        // start inserter
        stmtInsert.start();

        // consumer receives the next event
        sendSupportBean("E3", 3);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3});
            EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3});
            EPAssertionUtil.assertPropsPerRow(stmtSelect.iterator(), fields, new Object[][]{{"E1", 1}, {"E3", 3}});
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E3", 3}});

        // destroy inserter
        stmtInsert.destroy();
        sendSupportBean("E4", 4);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerSelect.isInvoked());
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void runAssertionStartStopConsumer(boolean namedWindow)
    {
        // create window
        String stmtTextCreate = namedWindow ?
                "create window MyInfra#keepall() as select theString as a, intPrimitive as b from " + SupportBean.class.getName() :
                "create table MyInfra(a string primary key, b int primary key)";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyInfra select theString as a, intPrimitive as b from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create consumer
        String[] fields = new String[] {"a", "b"};
        String stmtTextSelect = "select a, b from MyInfra as s1";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtTextSelect);
        stmtSelect.addListener(listenerSelect);

        // send 1 event
        sendSupportBean("E1", 1);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
            EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
        }
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}});

        // stop consumer
        stmtSelect.stop();
        sendSupportBean("E2", 2);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});
        }
        assertFalse(listenerSelect.isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

        // start consumer: the consumer has the last event even though he missed it
        stmtSelect.start();
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtSelect.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

        // consumer receives the next event
        sendSupportBean("E3", 3);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3});
            EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3});
            EPAssertionUtil.assertPropsPerRow(stmtSelect.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});

        // destroy consumer
        stmtSelect.destroy();
        sendSupportBean("E4", 4);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E4", 4});
        }
        assertFalse(listenerSelect.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void runAssertionStartStopDeleter(boolean namedWindow)
    {
        SupportNamedWindowObserver observer = new SupportNamedWindowObserver();
        NamedWindowLifecycleEvent theEvent;
        if (namedWindow) {
            epService.getNamedWindowMgmtService().addObserver(observer);
        }

        // create window
        String stmtTextCreate = namedWindow ?
                "create window MyInfra#keepall() as select theString as a, intPrimitive as b from " + SupportBean.class.getName() :
                "create table MyInfra(a string primary key, b int primary key)";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        assertEquals(namedWindow ? StatementType.CREATE_WINDOW : StatementType.CREATE_TABLE, ((EPStatementSPI) stmtCreate).getStatementMetadata().getStatementType());
        stmtCreate.addListener(listenerWindow);
        if (namedWindow) {
            theEvent = observer.getFirstAndReset();
            assertEquals(NamedWindowLifecycleEvent.LifecycleEventType.CREATE, theEvent.getEventType());
            assertEquals("MyInfra", theEvent.getName());
        }

        // stop and start, no consumers or deleters
        stmtCreate.stop();
        if (namedWindow) {
            theEvent = observer.getFirstAndReset();
            assertEquals(NamedWindowLifecycleEvent.LifecycleEventType.DESTROY, theEvent.getEventType());
            assertEquals("MyInfra", theEvent.getName());
        }

        stmtCreate.start();
        if (namedWindow) {
            assertEquals(NamedWindowLifecycleEvent.LifecycleEventType.CREATE, observer.getFirstAndReset().getEventType());
        }

        // create delete stmt
        String stmtTextDelete = "on " + SupportBean_A.class.getName() + " delete from MyInfra";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);

        // create insert into
        String stmtTextInsertOne = "insert into MyInfra select theString as a, intPrimitive as b from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create consumer
        String[] fields = new String[] {"a", "b"};
        String stmtTextSelect = "select irstream a, b from MyInfra as s1";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtTextSelect);
        stmtSelect.addListener(listenerSelect);

        // send 1 event
        sendSupportBean("E1", 1);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
            EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
        }
        else {
            assertFalse(listenerWindow.isInvoked());
            assertFalse(listenerSelect.isInvoked());
        }
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}});

        // Delete all events, 1 row expected
        sendSupportBean_A("A2");
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1", 1});
            EPAssertionUtil.assertProps(listenerSelect.assertOneGetOldAndReset(), fields, new Object[]{"E1", 1});
        }
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendSupportBean("E2", 2);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});
            EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});
        }
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2", 2}});

        // Stop the deleting statement
        stmtDelete.stop();
        sendSupportBean_A("A2");
        assertFalse(listenerWindow.isInvoked());

        // Start the deleting statement
        stmtDelete.start();

        sendSupportBean_A("A3");
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2});
            EPAssertionUtil.assertProps(listenerSelect.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2});
        }
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendSupportBean("E3", 3);
        if (namedWindow) {
            EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3});
            EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3});
        }
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 3}});

        stmtDelete.destroy();
        sendSupportBean_A("A3");
        assertFalse(listenerWindow.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private SupportBean_A sendSupportBean_A(String id)
    {
        SupportBean_A bean = new SupportBean_A(id);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private SupportBean sendSupportBean(String theString, int intPrimitive)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }
}
