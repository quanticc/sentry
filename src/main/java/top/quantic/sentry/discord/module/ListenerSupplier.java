package top.quantic.sentry.discord.module;

import sx.blah.discord.api.events.IListener;

import java.util.List;

public interface ListenerSupplier {

    List<IListener<?>> getListeners();
}
