/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.simplepv.vtype;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.csstudio.simplepv.ExceptionHandler;
import org.csstudio.simplepv.IPV;
import org.csstudio.simplepv.IPVListener;
import org.csstudio.vtype.pv.PV;
import org.csstudio.vtype.pv.PVListener;
import org.csstudio.vtype.pv.PVPool;
import org.epics.vtype.VType;

/** Opibuilder {@link IPV} based on vtype {@link PV}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class VTypePV implements IPV
{
    final private String name;
    final private Executor notificationThread;
    final private ExceptionHandler exceptionHandler;
    final private List<IPVListener> listeners = new CopyOnWriteArrayList<>();
    private volatile Optional<PV> pv = Optional.empty();
    final private AtomicBoolean connected = new AtomicBoolean(false);

    final private PVListener listener = new PVListener()
    {
        @Override
        public void valueChanged(PV pv, VType value)
        {
            final boolean first_value = connected.compareAndSet(false, true);
            for (IPVListener l : listeners)
                notificationThread.execute(() ->
                {
                    if (first_value)
                        l.connectionChanged(VTypePV.this);
                    l.valueChanged(VTypePV.this);
                });
        }

        @Override
        public void permissionsChanged(PV pv, boolean readonly)
        {
            for (IPVListener l : listeners)
                notificationThread.execute(() -> l.writePermissionChanged(VTypePV.this));
        }

        @Override
        public void disconnected(PV pv)
        {
            connected.set(false);
            for (IPVListener l : listeners)
                notificationThread.execute(() -> l.connectionChanged(VTypePV.this));
        }
    };

    public VTypePV(final String name, final boolean readOnly,
            final Executor notificationThread,
            final ExceptionHandler exceptionHandler) throws Exception
    {
        this.name = name;
        this.notificationThread = notificationThread;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void addListener(final IPVListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeListener(final IPVListener listener)
    {
        listeners.remove(listener);
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void start() throws Exception
    {
        if (isStarted())
            throw new Exception("PV " + name + " already started");
        final PV the_pv = PVPool.getPV(name);
        the_pv.addListener(listener);
        pv = Optional.of(the_pv);
    }

    @Override
    public void stop()
    {
        final PV safe_pv = pv.orElse(null);
        pv = Optional.empty();
        if (safe_pv != null)
        {
            safe_pv.removeListener(listener);
            PVPool.releasePV(safe_pv);
        }
    }

    @Override
    public boolean isBufferingValues()
    {
        return false;
    }

    @Override
    public boolean isConnected()
    {
        return connected.get();
    }

    @Override
    public boolean isPaused()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isStarted()
    {
        return pv.isPresent();
    }

    @Override
    public boolean isWriteAllowed()
    {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public void setPaused(boolean paused)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setValue(Object value) throws Exception
    {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean setValue(Object value, int timeout) throws Exception
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<VType> getAllBufferedValues()
    {
        return Arrays.asList(getValue());
    }

    @Override
    public VType getValue()
    {
        final PV safe_pv = pv.orElse(null);
        if (safe_pv == null)
            return null;
        return safe_pv.read();
    }
}
