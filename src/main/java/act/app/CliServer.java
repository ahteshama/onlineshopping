package act.app;

import act.Destroyable;
import act.exception.ActException;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.IO;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servicing CLI session
 */
class CliServer extends AppServiceBase<CliServer> implements Runnable {

    private static final Logger log = LogManager.get(CliServer.class);

    private ScheduledThreadPoolExecutor executor;
    private AtomicBoolean running = new AtomicBoolean();
    private ConcurrentMap<Integer, CliSession> sessions = new ConcurrentHashMap<Integer, CliSession>();
    private int port;
    private ServerSocket serverSocket;
    private Thread monitorThread;

    CliServer(App app) {
        super(app);
        port = app.config().cliPort();
        initExecutor(app);
        start();
    }

    @Override
    protected void releaseResources() {
        stop();
        executor.shutdown();
        Destroyable.Util.destroyAll(sessions.values());
        sessions.clear();
        if (null != serverSocket) {
            IO.close(serverSocket);
        }
    }

    void remove(CliSession session) {
        sessions.remove(session.id());
    }

    @Override
    public void run() {
        while (running()) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
                CliSession session = new CliSession(socket, this);
                sessions.put(session.id(), session);
                session.run();
            } catch (Exception e) {
                log.error(e, "Error processing CLI session");
                stop();
                return;
            } finally {
                IO.close(socket);
            }
        }
    }

    void stop() {
        if (!running()) {
            return;
        }
        running.set(false);
        try {
            serverSocket.close();
        } catch (IOException e) {
            log.warn(e, "error closing server socket");
        } finally {
            serverSocket = null;
        }
        if (null != monitorThread) {
            monitorThread.interrupt();
            monitorThread = null;
        }
    }

    void start() {
        if (running()) {
            return;
        }
        try {
            serverSocket = new ServerSocket(port);
            running.set(true);
            // start server thread
            executor.submit(this);
            // start expiration monitor thread
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    monitorThread = Thread.currentThread();
                    int expiration = app().config().cliSessionExpiration();
                    while (running()) {
                        List<CliSession> toBeRemoved = C.newList();
                        for (CliSession session : sessions.values()) {
                            if (session.expired(expiration)) {
                                toBeRemoved.add(session);
                            }
                        }
                        for (CliSession session: toBeRemoved) {
                            session.stop();
                            sessions.remove(session.id());
                        }
                        try {
                            Thread.sleep(60 * 1000);
                        } catch (InterruptedException e) {
                            return;
                        }
                        try {
                            app().detectChanges();
                        } catch (RequestRefreshClassLoader refreshRequest) {
                            app().refresh();
                        }
                    }
                }
            });
            log.info("CLI server started");
        } catch (IOException e) {
            throw new ActException(e, "Cannot start CLI server");
        }
    }

    boolean running() {
        return running.get();
    }

    private void initExecutor(App app) {
        // cli session thread + server thread + expiration monitor thread
        int poolSize = app.config().maxCliSession() + 2;
        executor = new ScheduledThreadPoolExecutor(poolSize, new AppThreadFactory("cli"), new ThreadPoolExecutor.AbortPolicy());
    }

}
