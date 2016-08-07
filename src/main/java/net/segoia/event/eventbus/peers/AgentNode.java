package net.segoia.event.eventbus.peers;

import java.util.HashMap;
import java.util.Map;

import net.segoia.event.conditions.TrueCondition;
import net.segoia.event.eventbus.Event;

/**
 * An agent is a node that reacts to events in a certain way </br>
 * It can also post events as a public node, or hiding behind a relay node
 * 
 * @author adi
 *
 */
public abstract class AgentNode extends EventNode {
    protected EventNode mainNode;
    /**
     * if true, it will call {@link #init()} from the constructor, otherwise somebody else will have to call
     * {@link #lazyInit()}
     */
    private boolean autoinit = true;
    private boolean initialized;

    private Map<String, RemoteEventHandler<?>> handlers;
    private Map<Class<?>, RemoteEventHandler<?>> handlersByEventClass;

    public AgentNode() {
	this(true);
    }

    public AgentNode(boolean autoinit) {

	this.autoinit = autoinit;
	handlers = new HashMap<>();
	handlersByEventClass = new HashMap<>();

	if (autoinit) {
	    init();
	}
    }

    protected void init() {
	registerHandlers();
	agentConfig();
	setRequestedEventsCondition();
	agentInit();
	initialized = true;
    }

    public void lazyInit() {
	if (!autoinit && !initialized) {
	    init();
	}
    }

    protected abstract void agentInit();

    protected abstract void agentConfig();

    /**
     * Override this to register handlers
     */
    protected abstract void registerHandlers();

    protected void setRequestedEventsCondition() {
	// TODO: implement this, for now request all
	config.setDefaultRequestedEvents(new TrueCondition());
    }

    @Override
    protected EventRelay buildLocalRelay(String peerId) {
	return new DefaultEventRelay(peerId, this);
    }

    protected void addEventHandler(String eventType, RemoteEventHandler<?> handler) {
	handlers.put(eventType, handler);
    }

    protected void addEventHandler(Class<?> eventClass, RemoteEventHandler<?> handler) {
	handlersByEventClass.put(eventClass, handler);
    }

    protected void removeEventHandler(Class<?> eventClass) {
	handlersByEventClass.remove(eventClass);
    }

    protected void removeEventHandlers(String eventType) {
	handlers.remove(eventType);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.segoia.event.eventbus.peers.EventBusNode#handleRemoteEvent(net.segoia.event.eventbus.peers.PeerEventContext)
     */
    @Override
    protected void handleRemoteEvent(PeerEventContext pc) {
	Event event = pc.getEvent();

	RemoteEventHandler<?> handler = handlersByEventClass.get(event.getClass());

	if (handler == null) {
	    String et = event.getEt();
	    handler = handlers.get(et);
	}

	if (handler != null) {
	    handler.handleRemoteEvent(new RemoteEventContext(this, pc));
	}

	handleEvent(event);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.segoia.event.eventbus.peers.EventNode#onPeerLeaving(java.lang.String)
     */
    @Override
    public void onPeerLeaving(String peerId) {
	super.onPeerLeaving(peerId);
	/* since an agent is bound by the main node, if that leaves we need to finish too */
	terminate();
    }

}
